package de.starlightunit.wrapper.navigation;

import android.content.Intent;
import android.net.Uri;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

public final class NavigationPolicy {

    private static final Set<String> INTERNAL_SPECIAL_SCHEMES =
            Set.of("about", "data", "blob", "javascript");

    private static final Set<String> EXTERNAL_SCHEMES =
            Set.of("http", "mailto", "tel", "geo", "market", "intent");

    private final String trustedDomain;

    public NavigationPolicy(String trustedDomain) {
        this.trustedDomain = normalizeDomain(trustedDomain);
    }

    public boolean shouldStayInWebView(Uri uri) {
        if (uri == null) {
            return false;
        }

        String scheme = normalizedScheme(uri.getScheme());

        if (INTERNAL_SPECIAL_SCHEMES.contains(scheme)) {
            return true;
        }

        return isTrustedHttps(uri);
    }

    public boolean isTrustedHttps(Uri uri) {
        if (uri == null || !"https".equals(normalizedScheme(uri.getScheme()))) {
            return false;
        }

        return isTrustedHost(lower(uri.getHost()), trustedDomain);
    }

    public boolean isTrustedHttps(String rawUrl) {
        return isTrustedHttpsUrlForTest(rawUrl, trustedDomain);
    }

    public boolean shouldOpenExternally(Uri uri) {
        if (uri == null) {
            return false;
        }

        String scheme = normalizedScheme(uri.getScheme());

        if (EXTERNAL_SCHEMES.contains(scheme)) {
            return true;
        }

        return "https".equals(scheme) && !shouldStayInWebView(uri);
    }

    public Intent buildExternalIntent(Uri uri) {
        if (uri == null || !shouldOpenExternally(uri)) {
            return null;
        }

        String scheme = normalizedScheme(uri.getScheme());

        if ("intent".equals(scheme)) {
            try {
                Intent intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                intent.setComponent(null);
                intent.setSelector(null);
                return intent;
            } catch (java.net.URISyntaxException ignored) {
                return null;
            }
        }

        return new Intent(Intent.ACTION_VIEW, uri);
    }

    static boolean isTrustedHttpsUrlForTest(String rawUrl, String trustedDomain) {
        if (rawUrl == null) {
            return false;
        }

        try {
            URI uri = new URI(rawUrl);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && isTrustedHost(lower(uri.getHost()), normalizeDomain(trustedDomain));
        } catch (URISyntaxException ignored) {
            return false;
        }
    }

    static boolean isExternalSchemeForTest(String rawUrl) {
        if (rawUrl == null) {
            return false;
        }

        String scheme;

        try {
            scheme = normalizedScheme(new URI(rawUrl).getScheme());
        } catch (URISyntaxException ignored) {
            return false;
        }

        return EXTERNAL_SCHEMES.contains(scheme);
    }

    static boolean isInternalSpecialSchemeForTest(String rawUrl) {
        if (rawUrl == null) {
            return false;
        }

        String scheme;

        try {
            scheme = normalizedScheme(new URI(rawUrl).getScheme());
        } catch (URISyntaxException ignored) {
            return false;
        }

        return INTERNAL_SPECIAL_SCHEMES.contains(scheme);
    }

    private static boolean isTrustedHost(String host, String trustedDomain) {
        if (host == null || trustedDomain.isEmpty()) {
            return false;
        }

        return host.equals(trustedDomain) || host.endsWith("." + trustedDomain);
    }

    private static String normalizeDomain(String domain) {
        if (domain == null) {
            return "";
        }

        String normalized = lower(domain.trim());

        while (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }

        return normalized;
    }

    private static String normalizedScheme(String scheme) {
        return scheme == null ? "" : scheme.toLowerCase(Locale.ROOT);
    }

    private static String lower(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }
}
