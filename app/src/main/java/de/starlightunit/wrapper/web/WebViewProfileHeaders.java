package de.starlightunit.wrapper.web;

import android.webkit.WebView;

import androidx.webkit.CustomHeader;
import androidx.webkit.Profile;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Installs the configured request headers on the WebView profile when the
 * current WebView implementation supports AndroidX custom request headers.
 *
 * Older WebViews keep using the existing navigation/native HTTP fallback.
 */
public final class WebViewProfileHeaders {

    private WebViewProfileHeaders() {
    }

    public static boolean install(WebView webView, String startUrl, Map<String, String> headers) {
        if (webView == null || headers == null || headers.isEmpty()) {
            return false;
        }
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)
                || !WebViewFeature.isFeatureSupported(WebViewFeature.CUSTOM_REQUEST_HEADERS)) {
            return false;
        }

        String origin = trustedOrigin(startUrl);
        if (origin == null) {
            return false;
        }

        try {
            Profile profile = WebViewCompat.getProfile(webView);
            profile.clearAllCustomHeaders();
            Set<String> rules = Collections.singleton(origin);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String name = entry.getKey();
                String value = entry.getValue();
                if (name == null || name.trim().isEmpty() || value == null) {
                    continue;
                }
                profile.addCustomHeader(new CustomHeader(name, value, rules));
            }
            return true;
        } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException ignored) {
            return false;
        }
    }

    static String trustedOrigin(String startUrl) {
        if (startUrl == null || startUrl.trim().isEmpty()) {
            return null;
        }
        try {
            URI uri = new URI(startUrl.trim()).normalize();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getUserInfo() != null
                    || uri.getHost() == null
                    || uri.getHost().trim().isEmpty()) {
                return null;
            }

            int port = uri.getPort();
            StringBuilder origin = new StringBuilder("https://").append(uri.getHost());
            if (port != -1 && port != 443) {
                origin.append(':').append(port);
            }
            return origin.toString();
        } catch (URISyntaxException ignored) {
            return null;
        }
    }
}
