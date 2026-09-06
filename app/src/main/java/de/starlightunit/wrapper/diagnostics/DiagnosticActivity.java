package de.starlightunit.wrapper.diagnostics;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Map;

import de.starlightunit.wrapper.MainActivity;
import de.starlightunit.wrapper.bridge.QuantumNativeMediaBridge;
import de.starlightunit.wrapper.config.AppConfig;
import de.starlightunit.wrapper.media.QuantumNativeMediaPlayer;
import de.starlightunit.wrapper.navigation.NavigationPolicy;
import de.starlightunit.wrapper.web.GameWebViewClient;
import de.starlightunit.wrapper.web.WebViewConfigurator;
import de.starlightunit.wrapper.web.WrapperRequestHeaders;

public final class DiagnosticActivity extends Activity {

    private TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        showInitialState();
    }

    private ScrollView buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(32));
        root.setBackgroundColor(Color.rgb(16, 16, 18));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView title = new TextView(this);
        title.setText("Quantum Mobile Wrapper\nbeta8 startup diagnostics");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24f);
        title.setPadding(0, 0, 0, dp(12));
        root.addView(title);

        TextView device = new TextView(this);
        device.setText(StartupDiagnostics.deviceSummary());
        device.setTextColor(Color.LTGRAY);
        device.setTextSize(14f);
        device.setPadding(0, 0, 0, dp(16));
        root.addView(device);

        root.addView(button("1. Test WebView creation", view -> testWebViewCreation()));
        root.addView(button("2. Test wrapper components", view -> testWrapperComponents()));
        root.addView(button("3. Start fixed full wrapper", view -> startFullWrapper()));
        root.addView(button("Copy diagnostic text", view -> copyDiagnostics()));
        root.addView(button("Clear stored crash", view -> clearDiagnostics()));

        output = new TextView(this);
        output.setTextColor(Color.WHITE);
        output.setTextSize(12f);
        output.setTextIsSelectable(true);
        output.setPadding(0, dp(18), 0, 0);
        root.addView(output);

        return scroll;
    }

    private Button button(String label, android.view.View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(6), 0, dp(6));
        button.setLayoutParams(params);
        return button;
    }

    private void showInitialState() {
        if (StartupDiagnostics.hasCrash(this)) {
            output.setText("A Java crash from the previous run was captured:\n\n"
                    + StartupDiagnostics.read(this));
        } else {
            output.setText("No Java crash is stored. Run the tests from top to bottom.\n\n"
                    + "Beta7 identified the startup crash in Android 16 immersive window setup. "
                    + "Button 3 now starts the patched MainActivity for real-device confirmation.");
        }
    }

    private void testWebViewCreation() {
        WebView webView = null;
        try {
            webView = new WebView(this);
            output.setText("STEP 1 OK\n\nAndroid WebView can be constructed successfully.");
        } catch (Throwable throwable) {
            output.setText("STEP 1 FAILED\n\n" + StartupDiagnostics.stackTrace(throwable));
        } finally {
            if (webView != null) {
                try {
                    webView.destroy();
                } catch (Throwable ignored) {
                    // Diagnostic cleanup only.
                }
            }
        }
    }

    private void testWrapperComponents() {
        WebView webView = null;
        QuantumNativeMediaPlayer mediaPlayer = null;
        GameWebViewClient webViewClient = null;
        try {
            webView = new WebView(this);
            WebViewConfigurator.configure(this, webView);

            NavigationPolicy navigationPolicy = new NavigationPolicy(AppConfig.TRUSTED_DOMAIN);
            Map<String, String> requestHeaders = WrapperRequestHeaders.create();

            mediaPlayer = new QuantumNativeMediaPlayer(this);
            webView.addJavascriptInterface(
                    new QuantumNativeMediaBridge(webView, navigationPolicy, mediaPlayer),
                    AppConfig.NATIVE_MEDIA_BRIDGE_NAME
            );

            webViewClient = new GameWebViewClient(
                    this,
                    navigationPolicy,
                    new NoOpCallbacks(),
                    requestHeaders
            );
            webView.setWebViewClient(webViewClient);

            output.setText("STEP 2 OK\n\nWebView configuration, navigation policy, Quantum NMP, JavaScript bridge and Quantum Asset Store initialized successfully.");
        } catch (Throwable throwable) {
            output.setText("STEP 2 FAILED\n\n" + StartupDiagnostics.stackTrace(throwable));
        } finally {
            if (webViewClient != null) {
                try {
                    webViewClient.close();
                } catch (Throwable ignored) {
                    // Diagnostic cleanup only.
                }
            }
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.release();
                } catch (Throwable ignored) {
                    // Diagnostic cleanup only.
                }
            }
            if (webView != null) {
                try {
                    webView.destroy();
                } catch (Throwable ignored) {
                    // Diagnostic cleanup only.
                }
            }
        }
    }

    private void startFullWrapper() {
        StartupDiagnostics.clear(this);
        output.setText("Starting patched MainActivity now.\n\nIf it still closes, reopen diagnostics and copy the newly persisted crash text.");
        startActivity(new Intent(this, MainActivity.class));
    }

    private void copyDiagnostics() {
        String text = output.getText() == null ? "" : output.getText().toString();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Quantum startup diagnostics", text));
        output.append("\n\n[Diagnostic text copied to clipboard]");
    }

    private void clearDiagnostics() {
        StartupDiagnostics.clear(this);
        output.setText("Stored crash log cleared.");
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class NoOpCallbacks implements GameWebViewClient.Callbacks {
        @Override
        public void onPageLoading() {
        }

        @Override
        public void onPageReady() {
        }

        @Override
        public void onMainFrameError() {
        }
    }
}
