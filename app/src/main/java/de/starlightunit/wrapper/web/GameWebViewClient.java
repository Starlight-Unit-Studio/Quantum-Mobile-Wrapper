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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import de.starlightunit.wrapper.assets.QuantumAssetStore;
import de.starlightunit.wrapper.config.AppConfig;
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
    private final Map<String, String> requestHeaders;
    private final CampaignAudioHandoff campaignAudioHandoff;
    private final QuantumAssetStore assetStore;

    public GameWebViewClient(
            Context context,
            NavigationPolicy navigationPolicy,
            Callbacks callbacks,
            Map<String, String> requestHeaders
    ) {
        this.context = context;
        this.navigationPolicy = navigationPolicy;
        this.callbacks = callbacks;
        this.requestHeaders = Collections.unmodifiableMap(new LinkedHashMap<>(requestHeaders));
        this.campaignAudioHandoff = new CampaignAudioHandoff(context);
        this.assetStore = new QuantumAssetStore(
                context,
                AppConfig.ASSET_STORE_TRUSTED_HOST,
                AppConfig.ASSET_STORE_PATH_PREFIX,
                AppConfig.ASSET_STORE_EXCLUDED_PATH_PREFIX
        );
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        callbacks.onPageLoading();
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        if (navigationPolicy.isTrustedHttps(url)) {
            campaignAudioHandoff.inject(view);
            QuantumAssetWarmup.capture(view, assetStore, requestHeaders);
        }
        callbacks.onPageReady();
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        if (!request.isForMainFrame() && "GET".equalsIgnoreCase(request.getMethod())) {
            WebResourceResponse cached = assetStore.openCachedResponse(request.getUrl().toString());
            if (cached != null) {
                return cached;
            }
        }
        return super.shouldInterceptRequest(view, request);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        if (navigationPolicy.isTrustedHttps(url)) {
            if (request.isForMainFrame()
                    && "GET".equalsIgnoreCase(request.getMethod())
                    && !WrapperRequestHeaders.containsConfiguredHeaders(request.getRequestHeaders())) {
                view.loadUrl(url, requestHeaders);
                return true;
            }
            return false;
        }
        return handleNavigation(url);
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

    public void close() {
        assetStore.close();
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
