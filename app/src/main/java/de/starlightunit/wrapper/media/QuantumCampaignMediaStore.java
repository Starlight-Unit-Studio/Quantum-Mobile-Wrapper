package de.starlightunit.wrapper.media;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public final class QuantumCampaignMediaStore {

    public interface Callback {
        void onResolved(String playbackSource);
    }

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final long MAX_OGG_BYTES = 64L * 1024L * 1024L;
    private static final byte[] OGG_MAGIC = new byte[] {'O', 'g', 'g', 'S'};

    private final File mediaDirectory;
    private final QuantumMediaSourcePolicy sourcePolicy;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private volatile boolean closed;

    public QuantumCampaignMediaStore(
            Context context,
            String trustedDomain,
            String pathPrefix
    ) {
        Context appContext = context.getApplicationContext();
        mediaDirectory = new File(
                new File(appContext.getFilesDir(), "quantum_nmp"),
                "campaign"
        );
        sourcePolicy = new QuantumMediaSourcePolicy(trustedDomain, pathPrefix);
        executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "quantum-campaign-media-store");
            thread.setDaemon(true);
            return thread;
        });
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void resolve(String source, Callback callback) {
        if (callback == null || closed) {
            return;
        }

        String normalized = sourcePolicy.validateAndNormalize(source);
        if (normalized == null) {
            postResult(callback, source);
            return;
        }

        if (!isPersistentOggSource(normalized)) {
            postResult(callback, normalized);
            return;
        }

        File target = new File(mediaDirectory, fileNameForSource(normalized));
        if (isUsableFile(target)) {
            postResult(callback, target.getAbsolutePath());
            return;
        }

        executor.execute(() -> {
            String resolved = persistIfNeeded(normalized, target);
            postResult(callback, resolved);
        });
    }

    public File getMediaDirectory() {
        return mediaDirectory;
    }

    public void close() {
        closed = true;
        executor.shutdownNow();
    }

    private String persistIfNeeded(String source, File target) {
        if (closed) {
            return source;
        }
        if (isUsableFile(target)) {
            return target.getAbsolutePath();
        }

        if (!mediaDirectory.exists() && !mediaDirectory.mkdirs() && !mediaDirectory.isDirectory()) {
            return source;
        }

        File partial = new File(mediaDirectory, target.getName() + ".part");
        if (partial.exists() && !partial.delete()) {
            return source;
        }

        HttpsURLConnection connection = null;
        try {
            URL url = new URL(source);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "audio/ogg, application/ogg;q=0.9, */*;q=0.1");

            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                return source;
            }

            long contentLength = connection.getContentLengthLong();
            if (contentLength > MAX_OGG_BYTES) {
                return source;
            }

            long written = downloadVerifiedOgg(connection, partial);
            if (written <= OGG_MAGIC.length || written > MAX_OGG_BYTES) {
                return source;
            }

            if (target.exists() && !target.delete()) {
                return source;
            }
            if (!partial.renameTo(target)) {
                return source;
            }

            return target.getAbsolutePath();
        } catch (IOException | ClassCastException exception) {
            return source;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (partial.exists() && !partial.equals(target)) {
                // A successful rename makes the partial path disappear.
                partial.delete();
            }
        }
    }

    private static long downloadVerifiedOgg(HttpsURLConnection connection, File partial)
            throws IOException {
        try (BufferedInputStream input = new BufferedInputStream(connection.getInputStream());
             FileOutputStream fileOutput = new FileOutputStream(partial);
             BufferedOutputStream output = new BufferedOutputStream(fileOutput)) {

            byte[] header = new byte[OGG_MAGIC.length];
            int headerRead = readFully(input, header);
            if (headerRead != OGG_MAGIC.length || !matchesOggMagic(header)) {
                throw new IOException("Campaign media is not an OGG stream");
            }

            output.write(header);
            long total = header.length;
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_OGG_BYTES) {
                    throw new IOException("Campaign OGG exceeds storage limit");
                }
                output.write(buffer, 0, read);
            }

            output.flush();
            fileOutput.getFD().sync();
            return total;
        }
    }

    private void postResult(Callback callback, String result) {
        mainHandler.post(() -> {
            if (!closed) {
                callback.onResolved(result);
            }
        });
    }

    static boolean isPersistentOggSource(String source) {
        if (source == null || source.trim().isEmpty()) {
            return false;
        }
        try {
            URI uri = new URI(source.trim());
            String path = uri.getPath();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && path != null
                    && path.toLowerCase(Locale.ROOT).endsWith(".ogg");
        } catch (URISyntaxException ignored) {
            return false;
        }
    }

    static String fileNameForSource(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2 + 4);
            for (byte value : bytes) {
                int unsigned = value & 0xff;
                hex.append(Character.forDigit((unsigned >>> 4) & 0x0f, 16));
                hex.append(Character.forDigit(unsigned & 0x0f, 16));
            }
            return hex.append(".ogg").toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static boolean isUsableFile(File file) {
        return file.isFile() && file.length() > OGG_MAGIC.length;
    }

    private static int readFully(BufferedInputStream input, byte[] target) throws IOException {
        int offset = 0;
        while (offset < target.length) {
            int read = input.read(target, offset, target.length - offset);
            if (read == -1) {
                break;
            }
            offset += read;
        }
        return offset;
    }

    private static boolean matchesOggMagic(byte[] header) {
        if (header.length != OGG_MAGIC.length) {
            return false;
        }
        for (int index = 0; index < OGG_MAGIC.length; index += 1) {
            if (header[index] != OGG_MAGIC[index]) {
                return false;
            }
        }
        return true;
    }
}
