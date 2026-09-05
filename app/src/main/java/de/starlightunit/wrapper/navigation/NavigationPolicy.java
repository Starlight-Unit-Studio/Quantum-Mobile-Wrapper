package de.starlightunit.wrapper.navigation;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public final class NavigationPolicy {
    private final String trustedDomain;

    public NavigationPolicy(String trustedDomain) {
        this.trustedDomain = trustedDomain.toLowerCase(Locale.ROOT);
    }

    public boolean shouldStayInWebView(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return false;
        }

        try {
            URI uri = new URI(rawUrl);
            String scheme = normalized(uri.getScheme());

            if (isWebViewOwnedScheme(scheme)) {
                return true;
            }

            return isTrustedHttpsUri(uri, scheme);
        } catch (URISyntaxException ignored) {
            return false;
        }
    }

    public boolean isTrustedHttps(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return false;
        }

        try {
            URI uri = new URI(rawUrl);
            return isTrustedHttpsUri(uri, normalized(uri.getScheme()));
        } catch (URISyntaxException ignored) {
            return false;
        }
    }

    private boolean isTrustedHttpsUri(URI uri, String scheme) {
        if (!"https".equals(scheme)) {
            return false;
        }

        String host = normalized(uri.getHost());
        return host.equals(trustedDomain) || host.endsWith("." + trustedDomain);
    }

    private static boolean isWebViewOwnedScheme(String scheme) {
        return "about".equals(scheme)
                || "data".equals(scheme)
                || "blob".equals(scheme)
                || "javascript".equals(scheme);
    }

    private static String normalized(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
