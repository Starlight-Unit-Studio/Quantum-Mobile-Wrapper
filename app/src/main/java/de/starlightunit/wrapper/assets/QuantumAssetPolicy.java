package de.starlightunit.wrapper.assets;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Validates resources before they may enter the native Quantum Asset Store.
 *
 * <p>The store is deliberately limited to HTTPS resources below a configured
 * /assets/ prefix on a trusted host. API, HTML and PHP traffic never enters
 * this cache.</p>
 */
public final class QuantumAssetPolicy {

    public static final class AssetSpec {
        private final String source;
        private final String mimeType;
        private final String encoding;
        private final boolean versioned;

        private AssetSpec(String source, String mimeType, String encoding, boolean versioned) {
            this.source = source;
            this.mimeType = mimeType;
            this.encoding = encoding;
            this.versioned = versioned;
        }

        public String getSource() {
            return source;
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getEncoding() {
            return encoding;
        }

        public boolean isVersioned() {
            return versioned;
        }
    }

    private final String trustedHost;
    private final String pathPrefix;
    private final String excludedPathPrefix;

    public QuantumAssetPolicy(String trustedHost, String pathPrefix, String excludedPathPrefix) {
        this.trustedHost = normalizeHost(trustedHost);
        if (this.trustedHost.isEmpty()) {
            throw new IllegalArgumentException("trustedHost must not be empty");
        }
        this.pathPrefix = normalizePrefix(pathPrefix);
        this.excludedPathPrefix = normalizeOptionalPrefix(excludedPathPrefix);
    }

    public AssetSpec inspect(String source) {
        if (source == null || source.trim().isEmpty()) {
            return null;
        }

        try {
            URI uri = new URI(source.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return null;
            }
            if (uri.getUserInfo() != null) {
                return null;
            }
            if (uri.getPort() != -1 && uri.getPort() != 443) {
                return null;
            }

            String host = normalizeHost(uri.getHost());
            if (!host.equals(trustedHost)) {
                return null;
            }

            String rawPath = uri.getRawPath();
            String path = uri.getPath();
            if (rawPath == null || path == null || path.length() <= pathPrefix.length()) {
                return null;
            }

            String lowerRawPath = rawPath.toLowerCase(Locale.ROOT);
            if (lowerRawPath.contains("%2e")
                    || lowerRawPath.contains("%2f")
                    || lowerRawPath.contains("%5c")
                    || path.indexOf('\\') >= 0) {
                return null;
            }

            URI normalized = uri.normalize();
            String normalizedPath = normalized.getPath();
            if (normalizedPath == null
                    || !normalizedPath.equals(path)
                    || !normalizedPath.startsWith(pathPrefix)) {
                return null;
            }
            if (!excludedPathPrefix.isEmpty() && normalizedPath.startsWith(excludedPathPrefix)) {
                return null;
            }

            String mimeType = mimeTypeForPath(normalizedPath);
            if (mimeType == null) {
                return null;
            }

            URI withoutFragment = new URI(
                    "https",
                    null,
                    host,
                    uri.getPort(),
                    normalizedPath,
                    uri.getRawQuery(),
                    null
            );
            String normalizedSource = withoutFragment.toASCIIString();
            return new AssetSpec(
                    normalizedSource,
                    mimeType,
                    encodingForMimeType(mimeType),
                    hasVersionQuery(uri.getRawQuery())
            );
        } catch (URISyntaxException | IllegalArgumentException ignored) {
            return null;
        }
    }

    static String mimeTypeForPath(String path) {
        if (path == null) {
            return null;
        }
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".js") || lower.endsWith(".mjs")) return "application/javascript";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".avif")) return "image/avif";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".woff2")) return "font/woff2";
        if (lower.endsWith(".woff")) return "font/woff";
        if (lower.endsWith(".ttf")) return "font/ttf";
        if (lower.endsWith(".otf")) return "font/otf";
        if (lower.endsWith(".ogg")) return "audio/ogg";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".m4a")) return "audio/mp4";
        if (lower.endsWith(".aac")) return "audio/aac";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".webm")) return "video/webm";
        if (lower.endsWith(".wasm")) return "application/wasm";
        return null;
    }

    private static String encodingForMimeType(String mimeType) {
        if (mimeType.startsWith("text/")
                || "application/javascript".equals(mimeType)
                || "application/json".equals(mimeType)
                || "image/svg+xml".equals(mimeType)) {
            return "UTF-8";
        }
        return null;
    }

    private static boolean hasVersionQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return false;
        }
        String query = rawQuery.toLowerCase(Locale.ROOT);
        String[] parts = query.split("&");
        for (String part : parts) {
            if (part.startsWith("v=")
                    || part.startsWith("ver=")
                    || part.startsWith("version=")) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeHost(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizePrefix(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("pathPrefix must not be empty");
        }
        String normalized = value.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        return normalized;
    }

    private static String normalizeOptionalPrefix(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        return normalizePrefix(value);
    }
}
