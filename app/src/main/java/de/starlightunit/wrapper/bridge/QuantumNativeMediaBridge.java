package de.starlightunit.wrapper.bridge;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import de.starlightunit.wrapper.config.AppConfig;
import de.starlightunit.wrapper.media.QuantumMediaSourcePolicy;
import de.starlightunit.wrapper.media.QuantumNativeMediaPlayer;
import de.starlightunit.wrapper.media.QuantumPlaylistCodec;
import de.starlightunit.wrapper.navigation.NavigationPolicy;

public final class QuantumNativeMediaBridge {

    private final WebView webView;
    private final NavigationPolicy navigationPolicy;
    private final QuantumMediaSourcePolicy mediaSourcePolicy;
    private final QuantumNativeMediaPlayer mediaPlayer;

    public QuantumNativeMediaBridge(
            WebView webView,
            NavigationPolicy navigationPolicy,
            QuantumNativeMediaPlayer mediaPlayer
    ) {
        this.webView = webView;
        this.navigationPolicy = navigationPolicy;
        this.mediaSourcePolicy = new QuantumMediaSourcePolicy(
                AppConfig.TRUSTED_DOMAIN,
                AppConfig.NATIVE_MEDIA_PATH_PREFIX
        );
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

            String trustedSource = resolveCampaignSource(currentUrl, source);
            if (trustedSource != null) {
                mediaPlayer.play(trustedSource, loop);
            }
        });
    }

    /**
     * Starts a native repeating playlist. JavaScript passes one source per
     * line because JavascriptInterface has no portable collection transport.
     * Every source is resolved against the current trusted page and validated
     * by the same campaign-media policy as single-track playback.
     */
    @JavascriptInterface
    public void playPlaylist(String serializedSources, boolean shuffle) {
        List<String> requestedSources = QuantumPlaylistCodec.decode(serializedSources);
        if (requestedSources.isEmpty()) {
            return;
        }

        webView.post(() -> {
            String currentUrl = webView.getUrl();
            if (!navigationPolicy.isTrustedHttps(currentUrl)) {
                return;
            }

            List<String> trustedSources = new ArrayList<>(requestedSources.size());
            for (String source : requestedSources) {
                String trustedSource = resolveCampaignSource(currentUrl, source);
                if (trustedSource == null) {
                    return;
                }
                trustedSources.add(trustedSource);
            }

            mediaPlayer.playPlaylist(trustedSources, shuffle);
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

    private String resolveCampaignSource(String currentUrl, String source) {
        try {
            URI baseUri = new URI(currentUrl);
            URI resolvedUri = baseUri.resolve(source);
            return mediaSourcePolicy.validateAndNormalize(resolvedUri.toString());
        } catch (URISyntaxException | IllegalArgumentException ignored) {
            return null;
        }
    }
}
