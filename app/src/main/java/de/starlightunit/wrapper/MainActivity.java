package de.starlightunit.wrapper;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import de.starlightunit.wrapper.config.AppConfig;
import de.starlightunit.wrapper.download.AppDownloadListener;
import de.starlightunit.wrapper.navigation.NavigationPolicy;
import de.starlightunit.wrapper.web.GameWebChromeClient;
import de.starlightunit.wrapper.web.GameWebViewClient;
import de.starlightunit.wrapper.web.WebViewConfigurator;

public final class MainActivity extends Activity
        implements GameWebChromeClient.FileChooserHost,
        GameWebViewClient.Callbacks,
        GameWebChromeClient.Callbacks {

    private static final int FILE_CHOOSER_REQUEST = 7001;

    private WebView webView;
    private ProgressBar progressBar;
    private View errorPanel;
    private GameWebChromeClient chromeClient;
    private ValueCallback<android.net.Uri[]> pendingFileCallback;
    private boolean mainFrameFailed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.web_view);
        progressBar = findViewById(R.id.progress);
        errorPanel = findViewById(R.id.error_panel);
        FrameLayout fullscreenContainer = findViewById(R.id.fullscreen_container);
        Button retryButton = findViewById(R.id.retry_button);

        WebViewConfigurator.configure(this, webView);

        NavigationPolicy navigationPolicy = new NavigationPolicy(AppConfig.TRUSTED_DOMAIN);
        webView.setWebViewClient(new GameWebViewClient(this, navigationPolicy, this));
        chromeClient = new GameWebChromeClient(fullscreenContainer, this, this);
        webView.setWebChromeClient(chromeClient);
        webView.setDownloadListener(new AppDownloadListener(this));

        retryButton.setOnClickListener(v -> {
            errorPanel.setVisibility(View.GONE);
            mainFrameFailed = false;
            webView.reload();
        });

        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl(AppConfig.START_URL);
        }

        if (Build.VERSION.SDK_INT >= 33) {
            Api33BackHandler.register(this);
        }
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Color.BLACK);
        window.setNavigationBarColor(Color.BLACK);
        if (AppConfig.KEEP_SCREEN_ON) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        enterImmersiveMode();
    }

    private void enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }

    @Override
    public void openFileChooser(
            ValueCallback<android.net.Uri[]> callback,
            WebChromeClient.FileChooserParams params
    ) {
        if (pendingFileCallback != null) {
            pendingFileCallback.onReceiveValue(null);
        }
        pendingFileCallback = callback;

        try {
            Intent chooserIntent = params.createIntent();
            startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST);
        } catch (ActivityNotFoundException ignored) {
            pendingFileCallback.onReceiveValue(null);
            pendingFileCallback = null;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST || pendingFileCallback == null) {
            return;
        }

        android.net.Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
        pendingFileCallback.onReceiveValue(result);
        pendingFileCallback = null;
    }

    @Override
    public void onPageLoading() {
        mainFrameFailed = false;
        errorPanel.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPageReady() {
        progressBar.setVisibility(View.GONE);
        if (!mainFrameFailed) {
            errorPanel.setVisibility(View.GONE);
        }
    }

    @Override
    public void onMainFrameError() {
        mainFrameFailed = true;
        progressBar.setVisibility(View.GONE);
        errorPanel.setVisibility(View.VISIBLE);
    }

    @Override
    public void onProgress(int progress) {
        progressBar.setProgress(progress);
        progressBar.setVisibility(progress >= 100 ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onFullscreenChanged(boolean fullscreen) {
        webView.setVisibility(fullscreen ? View.GONE : View.VISIBLE);
        enterImmersiveMode();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        handleBackNavigation();
    }

    private void handleBackNavigation() {
        if (chromeClient != null && chromeClient.isShowingCustomView()) {
            chromeClient.onHideCustomView();
            return;
        }
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        finishAfterTransition();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
        enterImmersiveMode();
    }

    @Override
    protected void onPause() {
        if (webView != null) {
            webView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) {
            webView.saveState(outState);
        }
        super.onSaveInstanceState(outState);
    }

    private static final class Api33BackHandler {
        private Api33BackHandler() {
        }

        @android.annotation.TargetApi(33)
        static void register(MainActivity activity) {
            activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    activity::handleBackNavigation
            );
        }
    }

    @Override
    protected void onDestroy() {
        if (pendingFileCallback != null) {
            pendingFileCallback.onReceiveValue(null);
            pendingFileCallback = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
