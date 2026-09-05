package de.starlightunit.wrapper.web;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.webkit.SafeBrowsingResponse;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.LinkedHashMap;
import java.util.Map;

import de.starlightunit.wrapper.navigation.NavigationPolicy;

public final class GameWebViewClient extends WebViewClient {

    private final Context context;
    private final NavigationPolicy navigationPolicy;
    private final PageErrorHandler errorHandler;
    private final Map<String, String> requestHeaders;

    public GameWebViewClient(
            Context context,
            NavigationPolicy navigationPolicy,
            PageErrorHandler errorHandler,
            Map<String, String> requestHeaders
    ) {
        this.context = context;
        this.navigationPolicy = navigationPolicy;
        this.errorHandler = errorHandler;
        this.requestHeaders = Map.copyOf(new LinkedHashMap<>(requestHeaders));
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        Uri uri = request.getUrl();

        if (navigationPolicy.isTrustedHttps(uri)) {
            if (request.isForMainFrame()
                    && "GET".equalsIgnoreCase(request.getMethod())
                    && !WrapperRequestHeaders.containsConfiguredHeaders(request.getRequestHeaders())) {
                view.loadUrl(uri.toString(), requestHeaders);
                return true;
            }
            return false;
        }

        if (navigationPolicy.shouldStayInWebView(uri)) {
            return false;
        }

        if (!request.isForMainFrame()) {
            return false;
        }

        return openExternal(uri);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Uri uri = Uri.parse(url);

        if (navigationPolicy.shouldStayInWebView(uri)) {
            return false;
        }

        return openExternal(uri);
    }

    private boolean openExternal(Uri uri) {
        if (!navigationPolicy.shouldOpenExternally(uri)) {
            return true;
        }

        Intent intent = navigationPolicy.buildExternalIntent(uri);
        if (intent == null) {
            return true;
        }

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
            // No compatible Android activity is installed.
        }

        return true;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        errorHandler.hidePageError();
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        errorHandler.hidePageError();
    }

    @Override
    public void onReceivedError(
            WebView view,
            WebResourceRequest request,
            WebResourceError error
    ) {
        if (request.isForMainFrame()) {
            errorHandler.showPageError("Die Spielseite konnte nicht geladen werden.");
        }
    }

    @Override
    public void onReceivedHttpError(
            WebView view,
            WebResourceRequest request,
            WebResourceResponse errorResponse
    ) {
        if (request.isForMainFrame() && errorResponse.getStatusCode() >= 400) {
            errorHandler.showPageError(
                    "Der Server antwortet mit HTTP " + errorResponse.getStatusCode() + "."
            );
        }
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        handler.cancel();

        String currentUrl = view.getUrl();
        if (currentUrl != null && currentUrl.equals(error.getUrl())) {
            errorHandler.showPageError("TLS-Zertifikatsfehler. Verbindung wurde abgebrochen.");
        }
    }

    @Override
    public void onSafeBrowsingHit(
            WebView view,
            WebResourceRequest request,
            int threatType,
            SafeBrowsingResponse callback
    ) {
        callback.backToSafety(true);
    }

    public interface PageErrorHandler {
        void showPageError(String message);
        void hidePageError();
    }
}
