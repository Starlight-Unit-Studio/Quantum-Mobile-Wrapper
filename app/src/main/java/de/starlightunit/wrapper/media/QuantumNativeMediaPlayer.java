package de.starlightunit.wrapper.media;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;

import java.io.IOException;

public final class QuantumNativeMediaPlayer {

    private static final String PREFS_NAME = "quantum_nmp";
    private static final String PREF_ENABLED = "enabled";
    private static final String PREF_VOLUME = "volume";

    private final SharedPreferences preferences;

    private MediaPlayer mediaPlayer;
    private String requestedSource;
    private String currentSource;
    private boolean requestedLoop;
    private boolean prepared;
    private volatile boolean enabled;
    private volatile float volume;

    public QuantumNativeMediaPlayer(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        enabled = preferences.getBoolean(PREF_ENABLED, true);
        volume = clamp(preferences.getFloat(PREF_VOLUME, 1.0f));
    }

    public void play(String source, boolean loop) {
        if (source == null || source.isBlank()) {
            return;
        }

        requestedSource = source;
        requestedLoop = loop;

        if (!enabled) {
            return;
        }

        if (source.equals(currentSource) && mediaPlayer != null) {
            try {
                mediaPlayer.setLooping(loop);
                if (prepared && !mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                }
                return;
            } catch (IllegalStateException ignored) {
                releasePlayerOnly();
            }
        }

        prepareAndPlay(source, loop);
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
            prepareAndPlay(requestedSource, requestedLoop);
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
            prepareAndPlay(requestedSource, requestedLoop);
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
    }

    private void prepareAndPlay(String source, boolean loop) {
        releasePlayerOnly();

        MediaPlayer candidate = new MediaPlayer();
        mediaPlayer = candidate;
        currentSource = source;
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

                if (enabled && source.equals(requestedSource)) {
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
            candidate.setDataSource(source);
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
