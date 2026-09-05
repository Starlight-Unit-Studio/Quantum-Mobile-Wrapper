package de.starlightunit.wrapper.web;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import de.starlightunit.wrapper.navigation.NavigationPolicy;

public final class GameWebViewClient extends WebViewClient {
    public interface Callbacks {
        void onPageLoading();
        void onPageReady();
        void onMainFrameError();
    }

    private final Context context;
    private final NavigationPolicy navigationPolicy;
    private final Callbacks callbacks;

    public GameWebViewClient(Context context, NavigationPolicy navigationPolicy, Callbacks callbacks) {
        this.context = context;
        this.navigationPolicy = navigationPolicy;
        this.callbacks = callbacks;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        callbacks.onPageLoading();
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        callbacks.onPageReady();
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return handleNavigation(request.getUrl().toString());
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return handleNavigation(url);
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        if (request.isForMainFrame()) {
            callbacks.onMainFrameError();
        }
    }

    @Override
    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
        if (request.isForMainFrame() && errorResponse.getStatusCode() >= 500) {
            callbacks.onMainFrameError();
        }
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        handler.cancel();
        String currentUrl = view.getUrl();
        if (currentUrl != null && currentUrl.equals(error.getUrl())) {
            callbacks.onMainFrameError();
        }
    }

    private boolean handleNavigation(String url) {
        if (navigationPolicy.shouldStayInWebView(url)) {
            return false;
        }

        if (!isAllowedExternalScheme(url)) {
            return true;
        }

        try {
            Intent intent;
            String scheme = Uri.parse(url).getScheme();
            if ("intent".equalsIgnoreCase(scheme)) {
                intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                intent.setComponent(null);
                intent.setSelector(null);
            } else {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            }
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            context.startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException | java.net.URISyntaxException ignored) {
            // Unsupported external targets are blocked instead of being loaded inside the game WebView.
        }
        return true;
    }

    private static boolean isAllowedExternalScheme(String url) {
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        if (scheme == null) {
            return false;
        }
        String normalized = scheme.toLowerCase(java.util.Locale.ROOT);
        return "https".equals(normalized)
                || "http".equals(normalized)
                || "mailto".equals(normalized)
                || "tel".equals(normalized)
                || "geo".equals(normalized)
                || "market".equals(normalized)
                || "intent".equals(normalized);
    }
}
