package de.starlightunit.wrapper;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.Map;

import de.starlightunit.wrapper.bridge.QuantumNativeMediaBridge;
import de.starlightunit.wrapper.config.AppConfig;
import de.starlightunit.wrapper.download.AppDownloadListener;
import de.starlightunit.wrapper.download.DownloadDestinationPolicy;
import de.starlightunit.wrapper.launch.QuantumIntroController;
import de.starlightunit.wrapper.media.QuantumNativeMediaPlayer;
import de.starlightunit.wrapper.navigation.NavigationPolicy;
import de.starlightunit.wrapper.session.CookiePersistencePolicy;
import de.starlightunit.wrapper.session.QuantumSessionCookieStore;
import de.starlightunit.wrapper.ui.LoadingIndicatorController;
import de.starlightunit.wrapper.ui.PageTransitionController;
import de.starlightunit.wrapper.ui.SystemBarStyle;
import de.starlightunit.wrapper.web.GameWebChromeClient;
import de.starlightunit.wrapper.web.GameWebViewClient;
import de.starlightunit.wrapper.web.WebViewConfigurator;
import de.starlightunit.wrapper.web.WebViewProfileHeaders;
import de.starlightunit.wrapper.web.WrapperRequestHeaders;

public final class MainActivity extends Activity
        implements GameWebChromeClient.FileChooserHost,
        GameWebViewClient.Callbacks,
        GameWebChromeClient.Callbacks {

    private static final int FILE_CHOOSER_REQUEST = 7001;
    private static final int LEGACY_DOWNLOAD_PERMISSION_REQUEST = 7002;

    private WebView webView;
    private LoadingIndicatorController loadingIndicator;
    private PageTransitionController pageTransition;
    private SwipeRefreshLayout refreshLayout;
    private View errorPanel;
    private GameWebChromeClient chromeClient;
    private GameWebViewClient webViewClient;
    private ValueCallback<android.net.Uri[]> pendingFileCallback;
    private boolean mainFrameFailed;
    private NavigationPolicy navigationPolicy;
    private Map<String, String> requestHeaders;
    private QuantumNativeMediaPlayer nativeMediaPlayer;
    private QuantumIntroController introController;
    private QuantumSessionCookieStore sessionCookieStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWindow();
        setContentView(R.layout.activity_main);
        applyConfiguredSystemUi(false);

        webView = findViewById(R.id.web_view);
        pageTransition = new PageTransitionController(webView, AppConfig.PAGE_TRANSITIONS_ENABLED);
        refreshLayout = findViewById(R.id.refresh_container);
        ProgressBar horizontalProgress = findViewById(R.id.progress);
        FrameLayout loadingOverlay = findViewById(R.id.loading_overlay);
        ProgressBar loadingSpinner = findViewById(R.id.loading_spinner);
        errorPanel = findViewById(R.id.error_panel);
        FrameLayout fullscreenContainer = findViewById(R.id.fullscreen_container);
        loadingIndicator = new LoadingIndicatorController(
                this,
                horizontalProgress,
                loadingOverlay,
                loadingSpinner,
                AppConfig.LOADING_INDICATOR_STYLE,
                AppConfig.LOADING_INDICATOR_COLOR,
                AppConfig.LOADING_BAR_THICKNESS_DP,
                AppConfig.LOADING_SPINNER_SIZE_DP,
                AppConfig.LOADING_OVERLAY_DIM_PERCENT
        );
        refreshLayout.setEnabled(AppConfig.PULL_TO_REFRESH_ENABLED);
        refreshLayout.setOnRefreshListener(() -> {
            String currentUrl = webView == null ? null : webView.getUrl();
            if (webView == null) {
                refreshLayout.setRefreshing(false);
                return;
            }
            loadTrustedUrl(currentUrl);
        });
        ImageView introOverlay = findViewById(R.id.intro_overlay);
        introOverlay.setBackgroundColor(
                SystemBarStyle.parseRgb(AppConfig.SPLASH_BACKGROUND_COLOR, Color.BLACK)
        );
        Button retryButton = findViewById(R.id.retry_button);

        introController = new QuantumIntroController(this, introOverlay);
        introController.start(savedInstanceState != null);

        WebViewConfigurator.configure(this, webView);
        if (CookiePersistencePolicy.usesEncryptedPersistence(AppConfig.COOKIE_PERSISTENCE_MODE)) {
            sessionCookieStore = new QuantumSessionCookieStore(this);
            sessionCookieStore.restore();
        } else if (savedInstanceState == null
                && CookiePersistencePolicy.startsFreshSession(AppConfig.COOKIE_PERSISTENCE_MODE)) {
            CookiePersistencePolicy.clearForFreshSession();
        }

        navigationPolicy = new NavigationPolicy(AppConfig.TRUSTED_DOMAIN);
        requestHeaders = WrapperRequestHeaders.create();
        WebViewProfileHeaders.install(webView, AppConfig.START_URL, requestHeaders);
        nativeMediaPlayer = new QuantumNativeMediaPlayer(this);
        webView.addJavascriptInterface(
                new QuantumNativeMediaBridge(webView, navigationPolicy, nativeMediaPlayer),
                AppConfig.NATIVE_MEDIA_BRIDGE_NAME
        );
        webViewClient = new GameWebViewClient(this, navigationPolicy, this, requestHeaders);
        webView.setWebViewClient(webViewClient);
        chromeClient = new GameWebChromeClient(fullscreenContainer, this, this);
        webView.setWebChromeClient(chromeClient);
        webView.setDownloadListener(new AppDownloadListener(this, requestHeaders));
        requestLegacyPublicDownloadPermissionIfNeeded();

        retryButton.setOnClickListener(v -> {
            errorPanel.setVisibility(View.GONE);
            mainFrameFailed = false;
            loadTrustedUrl(webView.getUrl());
        });

        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            loadTrustedUrl(AppConfig.START_URL);
        }

        if (Build.VERSION.SDK_INT >= 33) {
            Api33BackHandler.register(this);
        }
    }

    private void loadTrustedUrl(String requestedUrl) {
        String targetUrl = navigationPolicy.isTrustedHttps(requestedUrl)
                ? requestedUrl
                : AppConfig.START_URL;
        webView.loadUrl(targetUrl, requestHeaders);
    }

    private void requestLegacyPublicDownloadPermissionIfNeeded() {
        boolean granted = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        if (DownloadDestinationPolicy.needsLegacyWritePermission(
                AppConfig.PUBLIC_DOWNLOADS_ENABLED,
                Build.VERSION.SDK_INT,
                granted
        )) {
            requestPermissions(
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    LEGACY_DOWNLOAD_PERMISSION_REQUEST
            );
        }
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(
                SystemBarStyle.parseRgb(AppConfig.STATUS_BAR_COLOR, Color.BLACK)
        );
        window.setNavigationBarColor(
                SystemBarStyle.parseRgb(AppConfig.NAVIGATION_BAR_COLOR, Color.BLACK)
        );
        if (AppConfig.KEEP_SCREEN_ON) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void applyConfiguredSystemUi(boolean forceFullscreen) {
        boolean fullscreen = forceFullscreen || AppConfig.IMMERSIVE_FULLSCREEN_ENABLED;
        View decorView = getWindow().getDecorView();

        int statusColor = SystemBarStyle.parseRgb(AppConfig.STATUS_BAR_COLOR, Color.BLACK);
        int navigationColor = SystemBarStyle.parseRgb(AppConfig.NAVIGATION_BAR_COLOR, Color.BLACK);
        boolean darkStatusIcons = SystemBarStyle.shouldUseDarkIcons(statusColor);
        boolean darkNavigationIcons = SystemBarStyle.shouldUseDarkIcons(navigationColor);

        if (Build.VERSION.SDK_INT >= 30) {
            Api30WindowHandler.setSystemUi(
                    decorView,
                    fullscreen,
                    darkStatusIcons,
                    darkNavigationIcons
            );
            return;
        }

        if (fullscreen) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
            return;
        }

        int flags = View.SYSTEM_UI_FLAG_VISIBLE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && darkStatusIcons) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && darkNavigationIcons) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        decorView.setSystemUiVisibility(flags);
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
        pageTransition.onPageLoading();
        loadingIndicator.show();
    }

    @Override
    public void onPageReady() {
        loadingIndicator.hide();
        pageTransition.onPageReady();
        refreshLayout.setRefreshing(false);
        if (!mainFrameFailed) {
            errorPanel.setVisibility(View.GONE);
        }
        if (sessionCookieStore != null) {
            sessionCookieStore.capture();
        }
    }

    @Override
    public void onMainFrameError() {
        mainFrameFailed = true;
        loadingIndicator.hide();
        pageTransition.reset();
        refreshLayout.setRefreshing(false);
        errorPanel.setVisibility(View.VISIBLE);
    }

    @Override
    public void onProgress(int progress) {
        loadingIndicator.setProgress(progress);
    }

    @Override
    public void onFullscreenChanged(boolean fullscreen) {
        webView.setVisibility(fullscreen ? View.GONE : View.VISIBLE);
        applyConfiguredSystemUi(fullscreen);
    }

    @Override
    @SuppressWarnings("deprecation")
    @android.annotation.SuppressLint("GestureBackNavigation")
    public void onBackPressed() {
        // API 33+ back gestures are handled by Api33BackHandler. This override is
        // intentionally retained only as the platform-compatible fallback for
        // Android 6 through Android 12; lint cannot infer that version split.
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
        boolean customFullscreen = chromeClient != null && chromeClient.isShowingCustomView();
        applyConfiguredSystemUi(customFullscreen);
    }

    @Override
    protected void onPause() {
        if (sessionCookieStore != null) {
            sessionCookieStore.capture();
        }
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

    private static final class Api30WindowHandler {
        private Api30WindowHandler() {
        }

        @android.annotation.TargetApi(30)
        static void setSystemUi(
                View decorView,
                boolean fullscreen,
                boolean darkStatusIcons,
                boolean darkNavigationIcons
        ) {
            // Android 16 can throw inside PhoneWindow.getInsetsController() when it is
            // queried before DecorView has been installed. Obtain the controller from
            // the actual decor view and defer the request until that view is ready.
            decorView.post(() -> {
                android.view.WindowInsetsController controller = decorView.getWindowInsetsController();
                if (controller == null) {
                    return;
                }

                int appearance = 0;
                int appearanceMask = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                if (darkStatusIcons) {
                    appearance |= android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;
                }
                if (darkNavigationIcons) {
                    appearance |= android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                }
                controller.setSystemBarsAppearance(appearance, appearanceMask);

                int types = android.view.WindowInsets.Type.statusBars()
                        | android.view.WindowInsets.Type.navigationBars();
                if (fullscreen) {
                    controller.hide(types);
                    controller.setSystemBarsBehavior(
                            android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    );
                } else {
                    controller.show(types);
                }
            });
        }
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
        if (sessionCookieStore != null) {
            sessionCookieStore.capture();
            sessionCookieStore = null;
        }
        if (introController != null) {
            introController.cancel();
            introController = null;
        }
        if (pendingFileCallback != null) {
            pendingFileCallback.onReceiveValue(null);
            pendingFileCallback = null;
        }
        if (pageTransition != null) {
            pageTransition.reset();
            pageTransition = null;
        }
        if (refreshLayout != null) {
            refreshLayout.setOnRefreshListener(null);
            refreshLayout.setRefreshing(false);
            refreshLayout = null;
        }
        if (webView != null) {
            webView.removeJavascriptInterface(AppConfig.NATIVE_MEDIA_BRIDGE_NAME);
        }
        if (nativeMediaPlayer != null) {
            nativeMediaPlayer.release();
            nativeMediaPlayer = null;
        }
        if (webViewClient != null) {
            webViewClient.close();
            webViewClient = null;
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
