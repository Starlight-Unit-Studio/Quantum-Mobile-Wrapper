package de.starlightunit.wrapper;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import java.util.Map;

import de.starlightunit.wrapper.bridge.QuantumNativeMediaBridge;
import de.starlightunit.wrapper.config.AppConfig;
import de.starlightunit.wrapper.download.AppDownloadListener;
import de.starlightunit.wrapper.media.QuantumNativeMediaPlayer;
import de.starlightunit.wrapper.navigation.NavigationPolicy;
import de.starlightunit.wrapper.web.GameWebChromeClient;
import de.starlightunit.wrapper.web.GameWebViewClient;
import de.starlightunit.wrapper.web.WebViewConfigurator;
import de.starlightunit.wrapper.web.WrapperRequestHeaders;

public final class MainActivity extends Activity implements GameWebViewClient.PageErrorHandler {

    private static final int POST_NOTIFICATIONS_REQUEST_CODE = 1001;

    private WebView webView;
    private View errorView;
    private TextView errorMessageView;
    private GameWebChromeClient chromeClient;
    private android.webkit.ValueCallback<android.net.Uri[]> fileChooserCallback;
    private NavigationPolicy navigationPolicy;
    private Map<String, String> requestHeaders;
    private QuantumNativeMediaPlayer nativeMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (AppConfig.KEEP_SCREEN_ON) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        if (getActionBar() != null) {
            getActionBar().hide();
        }

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.web_view);
        errorView = findViewById(R.id.error_view);
        errorMessageView = findViewById(R.id.error_message);
        Button retryButton = findViewById(R.id.retry_button);

        navigationPolicy = new NavigationPolicy(AppConfig.TRUSTED_DOMAIN);
        requestHeaders = WrapperRequestHeaders.create();
        nativeMediaPlayer = new QuantumNativeMediaPlayer(this);

        WebViewConfigurator.configure(webView, this);
        webView.addJavascriptInterface(
                new QuantumNativeMediaBridge(webView, navigationPolicy, nativeMediaPlayer),
                AppConfig.NATIVE_MEDIA_BRIDGE_NAME
        );

        GameWebViewClient webViewClient = new GameWebViewClient(
                this,
                navigationPolicy,
                this,
                requestHeaders
        );
        webView.setWebViewClient(webViewClient);

        chromeClient = new GameWebChromeClient(this, webView, this::onFileChooserRequested);
        webView.setWebChromeClient(chromeClient);

        webView.setDownloadListener(new AppDownloadListener(this));

        retryButton.setOnClickListener(v -> {
            hidePageError();
            loadTrustedUrl(webView.getUrl());
        });

        registerBackHandler();
        requestNotificationPermissionIfNeeded();

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            loadTrustedUrl(AppConfig.START_URL);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        webView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (fileChooserCallback != null) {
            fileChooserCallback.onReceiveValue(null);
            fileChooserCallback = null;
        }

        if (nativeMediaPlayer != null) {
            nativeMediaPlayer.release();
            nativeMediaPlayer = null;
        }

        if (webView != null) {
            webView.removeJavascriptInterface(AppConfig.NATIVE_MEDIA_BRIDGE_NAME);
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.removeAllViews();
            webView.destroy();
        }

        super.onDestroy();
    }

    @Override
    public void showPageError(String message) {
        errorMessageView.setText(message);
        errorView.setVisibility(View.VISIBLE);
    }

    @Override
    public void hidePageError() {
        errorView.setVisibility(View.GONE);
    }

    private void loadTrustedUrl(String requestedUrl) {
        String targetUrl = navigationPolicy.isTrustedHttps(requestedUrl)
                ? requestedUrl
                : AppConfig.START_URL;
        webView.loadUrl(targetUrl, requestHeaders);
    }

    private void onFileChooserRequested(android.webkit.ValueCallback<android.net.Uri[]> callback) {
        if (fileChooserCallback != null) {
            fileChooserCallback.onReceiveValue(null);
        }
        fileChooserCallback = callback;
        fileChooserLauncher.launch(new String[]{"*/*"});
    }

    private final androidx.activity.result.ActivityResultLauncher<String[]> fileChooserLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
                    result -> {
                        if (fileChooserCallback != null) {
                            fileChooserCallback.onReceiveValue(
                                    result == null ? null : new android.net.Uri[]{result}
                            );
                            fileChooserCallback = null;
                        }
                    });

    private void registerBackHandler() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    this::handleBackPressed
            );
        }
    }

    @Override
    public void onBackPressed() {
        handleBackPressed();
    }

    private void handleBackPressed() {
        if (chromeClient != null && chromeClient.isInCustomView()) {
            chromeClient.onHideCustomView();
            return;
        }

        if (webView.canGoBack()) {
            webView.goBack();
            return;
        }

        finishAfterTransition();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    POST_NOTIFICATIONS_REQUEST_CODE
            );
        }
    }
}
