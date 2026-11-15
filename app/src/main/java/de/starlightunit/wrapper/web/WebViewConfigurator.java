package de.starlightunit.wrapper.web;

import android.content.Context;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import de.starlightunit.wrapper.BuildConfig;
import de.starlightunit.wrapper.config.AppConfig;
import de.starlightunit.wrapper.navigation.NewWindowPolicy;

public final class WebViewConfigurator {
    private WebViewConfigurator() {
    }

    public static void configure(Context context, WebView webView) {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(!AppConfig.ALLOW_AUTOPLAY_MEDIA);
        settings.setSupportMultipleWindows(
                NewWindowPolicy.supportsMultipleWindows(AppConfig.NEW_WINDOW_POLICY)
        );
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportZoom(AppConfig.PINCH_TO_ZOOM_ENABLED);
        settings.setBuiltInZoomControls(AppConfig.PINCH_TO_ZOOM_ENABLED);
        settings.setDisplayZoomControls(false);
        settings.setTextZoom(Math.max(50, Math.min(200, AppConfig.FONT_SCALE_PERCENT)));
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUserAgentString(settings.getUserAgentString() + AppConfig.USER_AGENT_SUFFIX);
        WebThemeController.configure(context, settings);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, AppConfig.ALLOW_THIRD_PARTY_COOKIES);

        webView.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
    }
}
