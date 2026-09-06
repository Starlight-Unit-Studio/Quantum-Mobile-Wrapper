package de.starlightunit.wrapper.media;

import android.content.Context;
import android.content.SharedPreferences;

import de.starlightunit.wrapper.config.AppConfig;

public final class QuantumNativeMediaPlayer {

    private static final String PREFS_NAME = "quantum_nmp";
    private static final String PREF_ENABLED = "enabled";
    private static final String PREF_VOLUME = "volume";

    private final SharedPreferences preferences;
    private final QuantumCampaignMediaStore mediaStore;
    private final QuantumMediaSessionClient sessionClient;

    private String requestedSource;
    private boolean requestedLoop;
    private boolean shouldPlay;
    private volatile boolean enabled;
    private volatile float volume;

    public QuantumNativeMediaPlayer(Context context) {
        Context appContext = context.getApplicationContext();
        preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mediaStore = new QuantumCampaignMediaStore(
                appContext,
                AppConfig.TRUSTED_DOMAIN,
                AppConfig.NATIVE_MEDIA_PATH_PREFIX
        );
        sessionClient = new QuantumMediaSessionClient(appContext);
        enabled = preferences.getBoolean(PREF_ENABLED, true);
        volume = clamp(preferences.getFloat(PREF_VOLUME, 1.0f));
        sessionClient.setVolume(volume);
    }

    public void play(String source, boolean loop) {
        if (source == null || source.trim().isEmpty()) {
            return;
        }

        requestedSource = source.trim();
        requestedLoop = loop;
        shouldPlay = enabled;

        if (shouldPlay) {
            resolveAndPlay(requestedSource);
        }
    }

    public void pause() {
        shouldPlay = false;
        sessionClient.pause();
    }

    public void resume() {
        if (!enabled || requestedSource == null) {
            return;
        }

        shouldPlay = true;
        resolveAndPlay(requestedSource);
    }

    public void stop() {
        requestedSource = null;
        requestedLoop = false;
        shouldPlay = false;
        sessionClient.stop();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        preferences.edit().putBoolean(PREF_ENABLED, enabled).apply();

        if (!enabled) {
            pause();
            return;
        }

        resume();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setVolume(double requestedVolume) {
        float normalized = clamp((float) requestedVolume);
        volume = normalized;
        preferences.edit().putFloat(PREF_VOLUME, normalized).apply();
        sessionClient.setVolume(normalized);
    }

    public double getVolume() {
        return volume;
    }

    public void release() {
        requestedSource = null;
        requestedLoop = false;
        shouldPlay = false;
        sessionClient.release();
        mediaStore.close();
    }

    private void resolveAndPlay(String logicalSource) {
        mediaStore.resolve(logicalSource, playbackSource -> {
            if (!enabled
                    || !shouldPlay
                    || requestedSource == null
                    || !logicalSource.equals(requestedSource)) {
                return;
            }

            sessionClient.playResolved(
                    logicalSource,
                    playbackSource,
                    requestedLoop,
                    volume
            );
        });
    }

    private static float clamp(float value) {
        if (Float.isNaN(value)) {
            return 1.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
