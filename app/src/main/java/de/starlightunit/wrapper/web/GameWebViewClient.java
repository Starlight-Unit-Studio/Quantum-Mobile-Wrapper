package de.starlightunit.wrapper.web;

import android.content.Context;
import android.graphics.Bitmap;
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
import de.starlightunit.wrapper.assets.QuantumStartupAssetDownloader;
import de.starlightunit.wrapper.config.AppConfig;
import de.starlightunit.wrapper.navigation.ExternalLinkLauncher;
import de.starlightunit.wrapper.navigation.LinkRoutingPolicy;
import de.starlightunit.wrapper.navigation.NavigationPolicy;

public final class GameWebViewClient extends WebViewClient {
    public interface Callbacks {
        void onPageLoading();
        void onPageReady();
        void onMainFrameError();
    }

    private final Context context;
    private final NavigationPolicy navigationPolicy;
    private final LinkRoutingPolicy linkRoutingPolicy;
    private final Callbacks callbacks;
    private final Map<String, String> requestHeaders;
    private final CampaignAudioHandoff campaignAudioHandoff;
    private final QuantumAssetStore assetStore;
    private final QuantumStartupAssetDownloader startupAssetDownloader;

    public GameWebViewClient(
            Context context,
            NavigationPolicy navigationPolicy,
            LinkRoutingPolicy linkRoutingPolicy,
            Callbacks callbacks,
            Map<String, String> requestHeaders
    ) {
        this.context = context;
        this.navigationPolicy = navigationPolicy;
        this.linkRoutingPolicy = linkRoutingPolicy;
        this.callbacks = callbacks;
        this.requestHeaders = Collections.unmodifiableMap(new LinkedHashMap<>(requestHeaders));
        this.campaignAudioHandoff = new CampaignAudioHandoff(context);
        this.assetStore = AppConfig.QUANTUM_ASSET_STORE_ENABLED
                ? new QuantumAssetStore(
                        context,
                        AppConfig.ASSET_STORE_TRUSTED_HOST,
                        AppConfig.ASSET_STORE_PATH_PREFIX,
                        AppConfig.ASSET_STORE_EXCLUDED_PATH_PREFIX
                )
                : null;
        this.startupAssetDownloader = assetStore != null && AppConfig.NATIVE_ASSET_DOWNLOADER_ENABLED
                ? new QuantumStartupAssetDownloader(
                        assetStore,
                        AppConfig.ASSET_STORE_TRUSTED_HOST,
                        AppConfig.ASSET_STORE_PATH_PREFIX,
                        AppConfig.ASSET_STORE_EXCLUDED_PATH_PREFIX,
                        AppConfig.ASSET_MANIFEST_URL,
                        AppConfig.ASSET_DOWNLOADER_ROOTS
                )
                : null;
        if (startupAssetDownloader != null) {
            startupAssetDownloader.start(this.requestHeaders);
        }
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        callbacks.onPageLoading();
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        if (navigationPolicy.isTrustedHttps(url)) {
            WebOverrides.apply(view);
            campaignAudioHandoff.inject(view);
            if (assetStore != null && AppConfig.ASSET_STORE_PAGE_WARMUP_ENABLED) {
                view.postDelayed(
                        () -> {
                            if (navigationPolicy.isTrustedHttps(view.getUrl())) {
                                QuantumAssetWarmup.capture(view, assetStore, requestHeaders);
                            }
                        },
                        AppConfig.ASSET_STORE_PAGE_WARMUP_DELAY_MS
                );
            }
        }
        callbacks.onPageReady();
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        if (assetStore != null
                && !request.isForMainFrame()
                && "GET".equalsIgnoreCase(request.getMethod())
                && !hasHeader(request.getRequestHeaders(), "Range")) {
            String source = request.getUrl().toString();
            WebResourceResponse cached = assetStore.openCachedResponse(source);
            if (cached != null) {
                return cached;
            }

            // Dynamic images can appear long after onPageFinished(), so they
            // are invisible to QuantumAssetWarmup.capture(). Warm a trusted
            // image miss in parallel while WebView still performs its normal
            // request. A later retry/navigation can then be served natively.
            assetStore.prefetchImageMiss(source, requestHeaders);
        }
        return super.shouldInterceptRequest(view, request);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        LinkRoutingPolicy.Action action = linkRoutingPolicy.actionFor(url);
        if (action == LinkRoutingPolicy.Action.INTERNAL) {
            if (navigationPolicy.isTrustedHttps(url)
                    && request.isForMainFrame()
                    && "GET".equalsIgnoreCase(request.getMethod())
                    && !WrapperRequestHeaders.containsConfiguredHeaders(request.getRequestHeaders())) {
                view.loadUrl(url, requestHeaders);
                return true;
            }
            return false;
        }
        if (action == LinkRoutingPolicy.Action.EXTERNAL) {
            ExternalLinkLauncher.open(context, url);
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        LinkRoutingPolicy.Action action = linkRoutingPolicy.actionFor(url);
        if (action == LinkRoutingPolicy.Action.INTERNAL) {
            if (navigationPolicy.isTrustedHttps(url)) {
                view.loadUrl(url, requestHeaders);
                return true;
            }
            return false;
        }
        if (action == LinkRoutingPolicy.Action.EXTERNAL) {
            ExternalLinkLauncher.open(context, url);
        }
        return true;
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
        if (startupAssetDownloader != null) {
            startupAssetDownloader.close();
        }
        if (assetStore != null) {
            assetStore.close();
        }
    }

    private static boolean hasHeader(Map<String, String> headers, String expectedName) {
        if (headers == null || headers.isEmpty()) {
            return false;
        }
        for (String name : headers.keySet()) {
            if (name != null && name.equalsIgnoreCase(expectedName)) {
                return true;
            }
        }
        return false;
    }

}
