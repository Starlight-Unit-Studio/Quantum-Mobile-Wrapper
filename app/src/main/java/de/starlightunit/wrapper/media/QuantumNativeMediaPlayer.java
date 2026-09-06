package de.starlightunit.wrapper.media;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;

import java.io.IOException;

import de.starlightunit.wrapper.config.AppConfig;

public final class QuantumNativeMediaPlayer {

    private static final String PREFS_NAME = "quantum_nmp";
    private static final String PREF_ENABLED = "enabled";
    private static final String PREF_VOLUME = "volume";

    private final SharedPreferences preferences;
    private final QuantumCampaignMediaStore mediaStore;

    private MediaPlayer mediaPlayer;
    private String requestedSource;
    private String currentSource;
    private boolean requestedLoop;
    private boolean prepared;
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
        enabled = preferences.getBoolean(PREF_ENABLED, true);
        volume = clamp(preferences.getFloat(PREF_VOLUME, 1.0f));
    }

    public void play(String source, boolean loop) {
        if (source == null || source.trim().isEmpty()) {
            return;
        }

        String normalizedSource = source.trim();
        requestedSource = normalizedSource;
        requestedLoop = loop;

        if (!enabled) {
            return;
        }

        if (normalizedSource.equals(currentSource) && mediaPlayer != null) {
            updateExistingPlayer(loop);
            return;
        }

        resolveAndPlay(normalizedSource);
    }

    public void pause() {
        if (mediaPlayer == null || !prepared) {
            return;
        }

        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
        } catch (IllegalStateException ignored) {
            releasePlayerOnly();
        }
    }

    public void resume() {
        if (!enabled || requestedSource == null) {
            return;
        }

        if (mediaPlayer == null) {
            resolveAndPlay(requestedSource);
            return;
        }

        if (!prepared) {
            return;
        }

        try {
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
        } catch (IllegalStateException ignored) {
            resolveAndPlay(requestedSource);
        }
    }

    public void stop() {
        requestedSource = null;
        currentSource = null;
        requestedLoop = false;
        releasePlayerOnly();
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

        if (mediaPlayer != null) {
            try {
                mediaPlayer.setVolume(normalized, normalized);
            } catch (IllegalStateException ignored) {
                releasePlayerOnly();
            }
        }
    }

    public double getVolume() {
        return volume;
    }

    public void release() {
        requestedSource = null;
        currentSource = null;
        releasePlayerOnly();
        mediaStore.close();
    }

    private void resolveAndPlay(String logicalSource) {
        mediaStore.resolve(logicalSource, playbackSource -> {
            if (!enabled || requestedSource == null || !logicalSource.equals(requestedSource)) {
                return;
            }

            if (logicalSource.equals(currentSource) && mediaPlayer != null) {
                updateExistingPlayer(requestedLoop);
                return;
            }

            prepareAndPlay(logicalSource, playbackSource, requestedLoop);
        });
    }

    private void updateExistingPlayer(boolean loop) {
        try {
            mediaPlayer.setLooping(loop);
            if (prepared && !mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
        } catch (IllegalStateException ignored) {
            releasePlayerOnly();
            if (requestedSource != null && enabled) {
                resolveAndPlay(requestedSource);
            }
        }
    }

    private void prepareAndPlay(String logicalSource, String playbackSource, boolean loop) {
        releasePlayerOnly();

        MediaPlayer candidate = new MediaPlayer();
        mediaPlayer = candidate;
        currentSource = logicalSource;
        prepared = false;

        try {
            candidate.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build());
            candidate.setLooping(loop);
            candidate.setVolume(volume, volume);
            candidate.setOnPreparedListener(player -> {
                if (mediaPlayer != player) {
                    return;
                }

                prepared = true;

                if (enabled && logicalSource.equals(requestedSource)) {
                    try {
                        player.setLooping(requestedLoop);
                        player.start();
                    } catch (IllegalStateException ignored) {
                        releasePlayerOnly();
                    }
                }
            });
            candidate.setOnErrorListener((player, what, extra) -> {
                if (mediaPlayer == player) {
                    releasePlayerOnly();
                }
                return true;
            });
            candidate.setDataSource(playbackSource);
            candidate.prepareAsync();
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            if (mediaPlayer == candidate) {
                releasePlayerOnly();
            }
        }
    }

    private void releasePlayerOnly() {
        MediaPlayer player = mediaPlayer;
        mediaPlayer = null;
        prepared = false;

        if (player != null) {
            try {
                player.reset();
            } catch (IllegalStateException ignored) {
                // Release below is still safe.
            }

            player.release();
        }
    }

    private static float clamp(float value) {
        if (Float.isNaN(value)) {
            return 1.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
