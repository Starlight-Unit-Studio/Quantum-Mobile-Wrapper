package de.starlightunit.wrapper.assets;

import android.webkit.CookieManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

/**
 * Proactive startup sync for manifest-declared assets.
 *
 * The downloader deliberately writes into the existing QuantumAssetStore
 * directory and uses the same URL-derived filenames. It is an upstream feeder
 * for the Asset Store, not a second cache system.
 */
public final class QuantumStartupAssetDownloader {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 20_000;
    private static final int MAX_MANIFEST_BYTES = 2 * 1024 * 1024;
    private static final int MAX_ASSETS = 2_000;
    private static final long MAX_ASSET_BYTES = 64L * 1024L * 1024L;

    private final QuantumAssetStore assetStore;
    private final QuantumAssetPolicy policy;
    private final String manifestUrl;
    private final List<String> roots;
    private final ExecutorService executor;
    private volatile boolean closed;

    public QuantumStartupAssetDownloader(
            QuantumAssetStore assetStore,
            String trustedHost,
            String assetPrefix,
            String excludedPrefix,
            String manifestUrl,
            String configuredRoots
    ) {
        this.assetStore = assetStore;
        this.policy = new QuantumAssetPolicy(trustedHost, assetPrefix, excludedPrefix);
        this.manifestUrl = normalizeManifestUrl(manifestUrl, trustedHost);
        this.roots = parseRoots(configuredRoots);
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "quantum-startup-asset-sync");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start(Map<String, String> requestHeaders) {
        if (closed || manifestUrl.isEmpty()) {
            return;
        }
        Map<String, String> headers = requestHeaders == null
                ? Collections.emptyMap()
                : requestHeaders;
        executor.execute(() -> synchronize(headers));
    }

    public void close() {
        closed = true;
        executor.shutdownNow();
    }

    private void synchronize(Map<String, String> requestHeaders) {
        try {
            String manifest = fetchText(manifestUrl, requestHeaders);
            if (closed || manifest == null) {
                return;
            }
            JSONArray assets = new JSONObject(manifest).optJSONArray("assets");
            if (assets == null || assets.length() > MAX_ASSETS) {
                return;
            }

            for (int index = 0; index < assets.length() && !closed; index += 1) {
                JSONObject item = assets.optJSONObject(index);
                if (item == null) {
                    continue;
                }
                String source = resolveAssetUrl(item.optString("url", ""));
                long expectedSize = item.optLong("size", -1L);
                String expectedSha = item.optString("sha256", "").trim().toLowerCase(Locale.ROOT);
                if (source == null
                        || expectedSize <= 0
                        || expectedSize > MAX_ASSET_BYTES
                        || !expectedSha.matches("[0-9a-f]{64}")) {
                    continue;
                }

                QuantumAssetPolicy.AssetSpec spec = policy.inspect(source);
                if (spec == null || !matchesConfiguredRoot(spec.getSource())) {
                    continue;
                }

                File target = new File(
                        assetStore.getAssetDirectory(),
                        QuantumAssetStore.fileNameForSource(spec.getSource())
                );
                if (matches(target, expectedSize, expectedSha)) {
                    continue;
                }
                downloadVerified(spec, target, expectedSize, expectedSha, requestHeaders);
            }
        } catch (JSONException | RuntimeException ignored) {
            // Startup sync is an optimization. Existing local files and the live
            // WebView request path remain usable when the manifest is invalid.
        }
    }

    private String fetchText(String source, Map<String, String> requestHeaders) {
        HttpsURLConnection connection = null;
        try {
            connection = open(source, requestHeaders);
            connection.setRequestProperty("Accept", "application/json");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            int announced = connection.getContentLength();
            if (announced > MAX_MANIFEST_BYTES) {
                return null;
            }
            try (BufferedInputStream input = new BufferedInputStream(connection.getInputStream());
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[16 * 1024];
                int total = 0;
                int read;
                while ((read = input.read(buffer)) != -1) {
                    total += read;
                    if (total > MAX_MANIFEST_BYTES) {
                        return null;
                    }
                    output.write(buffer, 0, read);
                }
                return output.toString(StandardCharsets.UTF_8.name());
            }
        } catch (IOException ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void downloadVerified(
            QuantumAssetPolicy.AssetSpec spec,
            File target,
            long expectedSize,
            String expectedSha,
            Map<String, String> requestHeaders
    ) {
        File directory = assetStore.getAssetDirectory();
        if (!directory.exists() && !directory.mkdirs() && !directory.isDirectory()) {
            return;
        }
        File partial = new File(directory, target.getName() + ".sync.part");
        if (partial.exists()) {
            partial.delete();
        }

        HttpsURLConnection connection = null;
        try {
            connection = open(spec.getSource(), requestHeaders);
            connection.setRequestProperty("Accept", spec.getMimeType() + ", */*;q=0.1");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return;
            }
            long announced = connection.getContentLength();
            if (announced > 0 && announced != expectedSize) {
                return;
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long written = 0L;
            try (BufferedInputStream input = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream fileOutput = new FileOutputStream(partial);
                 BufferedOutputStream output = new BufferedOutputStream(fileOutput)) {
                byte[] buffer = new byte[16 * 1024];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    written += read;
                    if (written > expectedSize || written > MAX_ASSET_BYTES || closed) {
                        return;
                    }
                    digest.update(buffer, 0, read);
                    output.write(buffer, 0, read);
                }
                output.flush();
                fileOutput.getFD().sync();
            }

            if (written != expectedSize || !hex(digest.digest()).equals(expectedSha)) {
                return;
            }
            if (target.exists() && !target.delete()) {
                return;
            }
            if (partial.renameTo(target)) {
                target.setLastModified(System.currentTimeMillis());
            }
        } catch (IOException | NoSuchAlgorithmException | RuntimeException ignored) {
            // Keep any previous good local copy on failure.
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (partial.exists()) {
                partial.delete();
            }
        }
    }

    private HttpsURLConnection open(String source, Map<String, String> requestHeaders) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(source).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestMethod("GET");
        for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
            String name = header.getKey();
            String value = header.getValue();
            if (name != null && !name.trim().isEmpty()
                    && value != null && !value.trim().isEmpty()) {
                connection.setRequestProperty(name, value);
            }
        }
        String cookie = CookieManager.getInstance().getCookie(source);
        if (cookie != null && !cookie.trim().isEmpty()) {
            connection.setRequestProperty("Cookie", cookie);
        }
        return connection;
    }

    private String resolveAssetUrl(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            URI manifest = new URI(manifestUrl);
            URI resolved = manifest.resolve(value.trim()).normalize();
            if (!"https".equalsIgnoreCase(resolved.getScheme())
                    || resolved.getUserInfo() != null
                    || resolved.getHost() == null
                    || !resolved.getHost().equalsIgnoreCase(manifest.getHost())) {
                return null;
            }
            return resolved.toASCIIString();
        } catch (URISyntaxException ignored) {
            return null;
        }
    }

    private boolean matchesConfiguredRoot(String source) {
        if (roots.isEmpty()) {
            return true;
        }
        try {
            String path = new URI(source).getPath();
            if (path == null) {
                return false;
            }
            for (String root : roots) {
                if (path.startsWith(root)) {
                    return true;
                }
            }
        } catch (URISyntaxException ignored) {
            return false;
        }
        return false;
    }

    private static boolean matches(File file, long expectedSize, String expectedSha) {
        if (!file.isFile() || file.length() != expectedSize) {
            return false;
        }
        try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(file))) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return hex(digest.digest()).equals(expectedSha);
        } catch (IOException | NoSuchAlgorithmException ignored) {
            return false;
        }
    }

    private static String normalizeManifestUrl(String value, String trustedHost) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        try {
            URI uri = new URI(value.trim()).normalize();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getUserInfo() != null
                    || uri.getHost() == null
                    || !uri.getHost().equalsIgnoreCase(trustedHost)
                    || (uri.getPort() != -1 && uri.getPort() != 443)) {
                return "";
            }
            return uri.toASCIIString();
        } catch (URISyntaxException ignored) {
            return "";
        }
    }

    private static List<String> parseRoots(String configuredRoots) {
        if (configuredRoots == null || configuredRoots.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String raw : configuredRoots.split("[\\r\\n,;]+")) {
            String root = raw.trim();
            if (root.isEmpty() || root.contains("..") || root.indexOf('\\') >= 0) {
                continue;
            }
            if (!root.startsWith("/")) {
                root = "/" + root;
            }
            if (!root.endsWith("/")) {
                root += "/";
            }
            result.add(root);
        }
        return Collections.unmodifiableList(result);
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            int unsigned = value & 0xff;
            result.append(Character.forDigit((unsigned >>> 4) & 0x0f, 16));
            result.append(Character.forDigit(unsigned & 0x0f, 16));
        }
        return result.toString();
    }
}
