package de.starlightunit.wrapper.media;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public final class QuantumMediaSourcePolicy {

    private final String trustedDomain;
    private final String pathPrefix;

    public QuantumMediaSourcePolicy(String trustedDomain, String pathPrefix) {
        this.trustedDomain = normalizeHost(trustedDomain);
        this.pathPrefix = normalizePrefix(pathPrefix);
    }

    public String validateAndNormalize(String source) {
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
            if (!isTrustedHost(host)) {
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

            URI withoutFragment = new URI(
                    "https",
                    null,
                    host,
                    uri.getPort(),
                    normalizedPath,
                    uri.getRawQuery(),
                    null
            );
            return withoutFragment.toASCIIString();
        } catch (URISyntaxException | IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean isTrustedHost(String host) {
        return host.equals(trustedDomain) || host.endsWith("." + trustedDomain);
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
}
