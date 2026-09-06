package de.starlightunit.wrapper.assets;

import android.content.Context;
import android.webkit.CookieManager;
import android.webkit.WebResourceResponse;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

/**
 * App-private persistent cache for trusted static /assets/ resources.
 *
 * <p>Cache misses are never proxied through this class. WebView remains the
 * source of truth for the live request. Resources are warmed after a trusted
 * page has loaded and become native cache hits on later navigations.</p>
 */
public final class QuantumAssetStore {

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int MAX_WARMUP_URLS = 256;
    private static final long MAX_ENTRY_BYTES = 64L * 1024L * 1024L;
    private static final long MAX_TOTAL_BYTES = 256L * 1024L * 1024L;
    private static final long VERSIONED_TTL_MS = 30L * 24L * 60L * 60L * 1000L;
    private static final long UNVERSIONED_TTL_MS = 24L * 60L * 60L * 1000L;

    private final File assetDirectory;
    private final QuantumAssetPolicy policy;
    private final ExecutorService executor;
    private final ConcurrentHashMap<String, Boolean> inFlight = new ConcurrentHashMap<>();
    private volatile boolean closed;

    public QuantumAssetStore(
            Context context,
            String trustedHost,
            String pathPrefix,
            String excludedPathPrefix
    ) {
        Context appContext = context.getApplicationContext();
        assetDirectory = new File(appContext.getFilesDir(), "quantum_assets");
        policy = new QuantumAssetPolicy(trustedHost, pathPrefix, excludedPathPrefix);
        executor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "quantum-asset-store");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Returns a synthetic WebView response only when a fresh verified cache
     * entry already exists. A miss deliberately returns null so WebView can
     * perform its normal network request without a native proxy layer.
     */
    public WebResourceResponse openCachedResponse(String source) {
        if (closed) {
            return null;
        }

        QuantumAssetPolicy.AssetSpec spec = policy.inspect(source);
        if (spec == null) {
            return null;
        }

        File target = targetFor(spec.getSource());
        if (!isFresh(target, spec)) {
            return null;
        }

        try {
            BufferedInputStream input = new BufferedInputStream(new FileInputStream(target));
            Map<String, String> responseHeaders = new LinkedHashMap<>();
            // Native TTL is authoritative. Prevent WebView from pinning a synthetic
            // response longer than the store itself considers it fresh.
            responseHeaders.put("Cache-Control", "no-store");
            responseHeaders.put("X-Quantum-Asset-Store", "HIT");
            return new WebResourceResponse(
                    spec.getMimeType(),
                    spec.getEncoding(),
                    200,
                    "OK",
                    responseHeaders,
                    input
            );
        } catch (IOException ignored) {
            return null;
        }
    }

    public void prefetchAll(Collection<String> sources, Map<String, String> requestHeaders) {
        if (closed || sources == null || sources.isEmpty()) {
            return;
        }

        int accepted = 0;
        for (String source : sources) {
            if (accepted >= MAX_WARMUP_URLS) {
                break;
            }
            QuantumAssetPolicy.AssetSpec spec = policy.inspect(source);
            if (spec == null) {
                continue;
            }
            accepted += 1;
            prefetch(spec, requestHeaders);
        }
    }

    public File getAssetDirectory() {
        return assetDirectory;
    }

    public void close() {
        closed = true;
        inFlight.clear();
        executor.shutdownNow();
    }

    private void prefetch(QuantumAssetPolicy.AssetSpec spec, Map<String, String> requestHeaders) {
        File target = targetFor(spec.getSource());
        if (isFresh(target, spec)) {
            return;
        }
        if (inFlight.putIfAbsent(spec.getSource(), Boolean.TRUE) != null) {
            return;
        }

        Map<String, String> safeHeaders = requestHeaders == null
                ? Collections.emptyMap()
                : new LinkedHashMap<>(requestHeaders);

        executor.execute(() -> {
            try {
                persist(spec, target, safeHeaders);
            } finally {
                inFlight.remove(spec.getSource());
            }
        });
    }

    private void persist(
            QuantumAssetPolicy.AssetSpec spec,
            File target,
            Map<String, String> requestHeaders
    ) {
        if (closed || isFresh(target, spec)) {
            return;
        }

        if (!assetDirectory.exists()
                && !assetDirectory.mkdirs()
                && !assetDirectory.isDirectory()) {
            return;
        }

        File partial = new File(assetDirectory, target.getName() + ".part");
        if (partial.exists() && !partial.delete()) {
            return;
        }

        HttpsURLConnection connection = null;
        try {
            URL url = new URL(spec.getSource());
            connection = (HttpsURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", spec.getMimeType() + ", */*;q=0.1");

            for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
                String name = header.getKey();
                String value = header.getValue();
                if (name != null && !name.trim().isEmpty()
                        && value != null && !value.trim().isEmpty()) {
                    connection.setRequestProperty(name, value);
                }
            }

            String cookie = CookieManager.getInstance().getCookie(spec.getSource());
            if (cookie != null && !cookie.trim().isEmpty()) {
                connection.setRequestProperty("Cookie", cookie);
            }

            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                return;
            }

            // getContentLengthLong() exists only from API 24. The store already
            // caps entries at 64 MiB, so the legacy int accessor is sufficient
            // on Android 6 and the streaming guard remains authoritative.
            long contentLength = connection.getContentLength();
            if (contentLength > MAX_ENTRY_BYTES) {
                return;
            }
            if (isHtmlResponse(connection.getContentType())) {
                return;
            }

            long written = download(connection, partial);
            if (written <= 0 || written > MAX_ENTRY_BYTES) {
                return;
            }

            if (target.exists() && !target.delete()) {
                return;
            }
            if (!partial.renameTo(target)) {
                return;
            }
            target.setLastModified(System.currentTimeMillis());
            evictToBudget();
        } catch (IOException | RuntimeException ignored) {
            // A failed warm-up must never break the live WebView request path.
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (partial.exists()) {
                partial.delete();
            }
        }
    }

    private static long download(HttpsURLConnection connection, File partial) throws IOException {
        try (BufferedInputStream input = new BufferedInputStream(connection.getInputStream());
             FileOutputStream fileOutput = new FileOutputStream(partial);
             BufferedOutputStream output = new BufferedOutputStream(fileOutput)) {

            long total = 0;
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_ENTRY_BYTES) {
                    throw new IOException("Asset exceeds per-entry storage limit");
                }
                output.write(buffer, 0, read);
            }
            output.flush();
            fileOutput.getFD().sync();
            return total;
        }
    }

    private void evictToBudget() {
        File[] files = assetDirectory.listFiles(file ->
                file.isFile() && file.getName().endsWith(".asset"));
        if (files == null || files.length == 0) {
            return;
        }

        List<File> ordered = new ArrayList<>();
        Collections.addAll(ordered, files);
        Collections.sort(ordered, new Comparator<File>() {
            @Override
            public int compare(File left, File right) {
                long leftModified = left.lastModified();
                long rightModified = right.lastModified();
                if (leftModified < rightModified) {
                    return -1;
                }
                if (leftModified > rightModified) {
                    return 1;
                }
                return 0;
            }
        });

        long total = 0;
        for (File file : ordered) {
            total += file.length();
        }
        for (File file : ordered) {
            if (total <= MAX_TOTAL_BYTES) {
                break;
            }
            long length = file.length();
            if (file.delete()) {
                total -= length;
            }
        }
    }

    private File targetFor(String source) {
        return new File(assetDirectory, fileNameForSource(source));
    }

    private static boolean isFresh(File file, QuantumAssetPolicy.AssetSpec spec) {
        if (!file.isFile() || file.length() <= 0) {
            return false;
        }
        long ttl = spec.isVersioned() ? VERSIONED_TTL_MS : UNVERSIONED_TTL_MS;
        long age = Math.max(0L, System.currentTimeMillis() - file.lastModified());
        return age <= ttl;
    }

    static String fileNameForSource(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2 + 6);
            for (byte value : bytes) {
                int unsigned = value & 0xff;
                hex.append(Character.forDigit((unsigned >>> 4) & 0x0f, 16));
                hex.append(Character.forDigit(unsigned & 0x0f, 16));
            }
            return hex.append(".asset").toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static boolean isHtmlResponse(String contentType) {
        if (contentType == null) {
            return false;
        }
        String normalized = contentType.toLowerCase(java.util.Locale.ROOT);
        return normalized.startsWith("text/html")
                || normalized.startsWith("application/xhtml+xml");
    }
}
