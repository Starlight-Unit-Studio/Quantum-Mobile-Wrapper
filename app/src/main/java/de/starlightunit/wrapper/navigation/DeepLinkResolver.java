package de.starlightunit.wrapper.navigation;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class DeepLinkResolver {
    private DeepLinkResolver() {
    }

    public static String resolve(
            String rawUri,
            String configuredScheme,
            String startUrl,
            NavigationPolicy navigationPolicy
    ) {
        String scheme = normalizeScheme(configuredScheme);
        if (scheme.isEmpty() || rawUri == null || rawUri.trim().isEmpty()) {
            return "";
        }

        final URI incoming;
        final URI base;
        try {
            incoming = new URI(rawUri.trim());
            base = new URI(startUrl);
        } catch (URISyntaxException | IllegalArgumentException ignored) {
            return "";
        }

        if (!scheme.equals(normalizeScheme(incoming.getScheme()))) {
            return "";
        }

        String queryTarget = queryParameter(incoming.getRawQuery(), "url");
        if (!queryTarget.isEmpty()) {
            return navigationPolicy.isTrustedHttps(queryTarget) ? queryTarget : "";
        }

        String path = incoming.getRawPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }

        String host = incoming.getHost();
        if (host != null && !host.isEmpty() && !"open".equalsIgnoreCase(host)) {
            path = "/" + host + (path.startsWith("/") ? path : "/" + path);
        }

        String query = incoming.getRawQuery();
        String fragment = incoming.getRawFragment();
        StringBuilder relative = new StringBuilder(path.startsWith("/") ? path : "/" + path);
        if (query != null && !query.isEmpty()) {
            relative.append('?').append(query);
        }
        if (fragment != null && !fragment.isEmpty()) {
            relative.append('#').append(fragment);
        }

        try {
            String resolved = base.resolve(relative.toString()).toString();
            return navigationPolicy.isTrustedHttps(resolved) ? resolved : "";
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    public static String normalizeScheme(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.matches("^[a-z][a-z0-9+.-]{1,31}$") ? normalized : "";
    }

    private static String queryParameter(String rawQuery, String key) {
        if (rawQuery == null || rawQuery.isEmpty()) {
            return "";
        }
        for (String pair : rawQuery.split("&")) {
            int split = pair.indexOf('=');
            String rawKey = split >= 0 ? pair.substring(0, split) : pair;
            if (!key.equals(decode(rawKey))) {
                continue;
            }
            return decode(split >= 0 ? pair.substring(split + 1) : "");
        }
        return "";
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (Exception ignored) {
            return "";
        }
    }
}
