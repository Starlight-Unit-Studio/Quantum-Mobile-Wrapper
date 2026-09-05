package de.starlightunit.wrapper.bridge;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import java.net.URI;
import java.net.URISyntaxException;

import de.starlightunit.wrapper.config.AppConfig;
import de.starlightunit.wrapper.media.QuantumNativeMediaPlayer;
import de.starlightunit.wrapper.navigation.NavigationPolicy;

public final class QuantumNativeMediaBridge {

    private final WebView webView;
    private final NavigationPolicy navigationPolicy;
    private final QuantumNativeMediaPlayer mediaPlayer;

    public QuantumNativeMediaBridge(
            WebView webView,
            NavigationPolicy navigationPolicy,
            QuantumNativeMediaPlayer mediaPlayer
    ) {
        this.webView = webView;
        this.navigationPolicy = navigationPolicy;
        this.mediaPlayer = mediaPlayer;
    }

    @JavascriptInterface
    public boolean isAvailable() {
        return true;
    }

    @JavascriptInterface
    public String version() {
        return AppConfig.VERSION_NAME;
    }

    @JavascriptInterface
    public void play(String source, boolean loop) {
        if (source == null || source.trim().isEmpty()) {
            return;
        }

        webView.post(() -> {
            String currentUrl = webView.getUrl();
            if (!navigationPolicy.isTrustedHttps(currentUrl)) {
                return;
            }

            String trustedSource = resolveTrustedSource(currentUrl, source);
            if (trustedSource != null) {
                mediaPlayer.play(trustedSource, loop);
            }
        });
    }

    @JavascriptInterface
    public void pause() {
        postIfTrusted(mediaPlayer::pause);
    }

    @JavascriptInterface
    public void resume() {
        postIfTrusted(mediaPlayer::resume);
    }

    @JavascriptInterface
    public void stop() {
        postIfTrusted(mediaPlayer::stop);
    }

    @JavascriptInterface
    public void setEnabled(boolean enabled) {
        postIfTrusted(() -> mediaPlayer.setEnabled(enabled));
    }

    @JavascriptInterface
    public boolean isEnabled() {
        return mediaPlayer.isEnabled();
    }

    @JavascriptInterface
    public void setVolume(double volume) {
        postIfTrusted(() -> mediaPlayer.setVolume(volume));
    }

    @JavascriptInterface
    public double getVolume() {
        return mediaPlayer.getVolume();
    }

    private void postIfTrusted(Runnable action) {
        webView.post(() -> {
            if (navigationPolicy.isTrustedHttps(webView.getUrl())) {
                action.run();
            }
        });
    }

    private String resolveTrustedSource(String currentUrl, String source) {
        try {
            URI baseUri = new URI(currentUrl);
            URI resolvedUri = baseUri.resolve(source);
            String resolved = resolvedUri.toString();
            return navigationPolicy.isTrustedHttps(resolved) ? resolved : null;
        } catch (URISyntaxException | IllegalArgumentException ignored) {
            return null;
        }
    }
}
