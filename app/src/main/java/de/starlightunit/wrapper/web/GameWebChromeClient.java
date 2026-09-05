package de.starlightunit.wrapper.web;

import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;

import de.starlightunit.wrapper.BuildConfig;

public final class GameWebChromeClient extends WebChromeClient {
    public interface FileChooserHost {
        void openFileChooser(ValueCallback<android.net.Uri[]> callback, FileChooserParams params);
    }

    public interface Callbacks {
        void onProgress(int progress);
        void onFullscreenChanged(boolean fullscreen);
    }

    private final FrameLayout fullscreenContainer;
    private final FileChooserHost fileChooserHost;
    private final Callbacks callbacks;
    private View customView;
    private CustomViewCallback customViewCallback;

    public GameWebChromeClient(
            FrameLayout fullscreenContainer,
            FileChooserHost fileChooserHost,
            Callbacks callbacks
    ) {
        this.fullscreenContainer = fullscreenContainer;
        this.fileChooserHost = fileChooserHost;
        this.callbacks = callbacks;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        callbacks.onProgress(newProgress);
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
