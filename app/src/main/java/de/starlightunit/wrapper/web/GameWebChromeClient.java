package de.starlightunit.wrapper.web;

import android.os.Message;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import de.starlightunit.wrapper.BuildConfig;

public final class GameWebChromeClient extends WebChromeClient {
    public interface FileChooserHost {
        void openFileChooser(ValueCallback<android.net.Uri[]> callback, FileChooserParams params);
    }

    public interface NewWindowHost {
        void openNewWindow(String url);
    }

    public interface Callbacks {
        void onProgress(int progress);
        void onFullscreenChanged(boolean fullscreen);
    }

    private final FrameLayout fullscreenContainer;
    private final FileChooserHost fileChooserHost;
    private final NewWindowHost newWindowHost;
    private final Callbacks callbacks;
    private View customView;
    private CustomViewCallback customViewCallback;

    public GameWebChromeClient(
            FrameLayout fullscreenContainer,
            FileChooserHost fileChooserHost,
            NewWindowHost newWindowHost,
            Callbacks callbacks
    ) {
        this.fullscreenContainer = fullscreenContainer;
        this.fileChooserHost = fileChooserHost;
        this.newWindowHost = newWindowHost;
        this.callbacks = callbacks;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        callbacks.onProgress(newProgress);
    }

    @Override
    public boolean onCreateWindow(
            WebView view,
            boolean isDialog,
            boolean isUserGesture,
            Message resultMsg
    ) {
        WebView popup = new WebView(view.getContext());
        boolean[] dispatched = new boolean[]{false};
        popup.setWebViewClient(new WebViewClient() {
            private void dispatch(String url) {
                if (dispatched[0] || url == null || url.trim().isEmpty()
                        || "about:blank".equalsIgnoreCase(url.trim())) {
                    return;
                }
                dispatched[0] = true;
                newWindowHost.openNewWindow(url);
                popup.post(popup::destroy);
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                dispatch(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                dispatch(request.getUrl().toString());
                return true;
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                dispatch(url);
                return true;
            }
        });

        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
        transport.setWebView(popup);
        resultMsg.sendToTarget();
        return true;
    }

    @Override
    public void onCloseWindow(WebView window) {
        if (window != null) {
            window.destroy();
        }
    }

    @Override
    public boolean onShowFileChooser(
            WebView webView,
            ValueCallback<android.net.Uri[]> filePathCallback,
            FileChooserParams fileChooserParams
    ) {
        fileChooserHost.openFileChooser(filePathCallback, fileChooserParams);
        return true;
    }

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        if (customView != null) {
            callback.onCustomViewHidden();
            return;
        }

        customView = view;
        customViewCallback = callback;
        fullscreenContainer.addView(
                view,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );
        fullscreenContainer.setVisibility(View.VISIBLE);
        callbacks.onFullscreenChanged(true);
    }

    @Override
    public void onHideCustomView() {
        if (customView == null) {
            return;
        }

        fullscreenContainer.removeView(customView);
        fullscreenContainer.setVisibility(View.GONE);
        customView = null;

        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }
        callbacks.onFullscreenChanged(false);
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d(
                    "STU-WebView",
                    consoleMessage.message() + " @" + consoleMessage.lineNumber()
            );
        }
        return true;
    }

    public boolean isShowingCustomView() {
        return customView != null;
    }
}
