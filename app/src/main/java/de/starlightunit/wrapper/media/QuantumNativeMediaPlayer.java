package de.starlightunit.wrapper.media;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    // Playlist ownership stays native so it survives full-page WebView navigation.
    private List<String> playlistSources = Collections.emptyList();
    private String playlistSignature;
    private int playlistIndex = -1;
    private boolean playlistActive;
    private int consecutivePlaylistFailures;

    // Invalidates stale asynchronous media-store / MediaPlayer callbacks whenever
    // the requested playback target changes.
    private long requestGeneration;

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

        clearPlaylistState();

        String normalizedSource = source.trim();
        requestedSource = normalizedSource;
        requestedLoop = loop;
        long generation = ++requestGeneration;

        if (!enabled) {
            return;
        }

        if (normalizedSource.equals(currentSource) && mediaPlayer != null) {
            updateExistingPlayer(loop);
            return;
        }

        resolveAndPlay(normalizedSource, generation);
    }

    /**
     * Starts an endlessly repeating native playlist. A repeated call with the
     * same logical playlist is idempotent: the current track is resumed instead
     * of restarting or reshuffling the queue. This is what lets WebView pages
     * re-bootstrap safely while the Activity-owned soundtrack keeps playing.
     */
    public void playPlaylist(List<String> sources, boolean shuffle) {
        List<String> normalizedSources = normalizePlaylist(sources);
        if (normalizedSources.isEmpty()) {
            return;
        }

        String signature = buildPlaylistSignature(normalizedSources, shuffle);
        if (playlistActive && signature.equals(playlistSignature) && requestedSource != null) {
            if (!enabled) {
                return;
            }

            if (mediaPlayer == null) {
                resolveAndPlay(requestedSource, ++requestGeneration);
            } else {
                updateExistingPlayer(false);
            }
            return;
        }

        List<String> playbackOrder = new ArrayList<>(normalizedSources);
        if (shuffle && playbackOrder.size() > 1) {
            Collections.shuffle(playbackOrder);
        }

        playlistSources = Collections.unmodifiableList(playbackOrder);
        playlistSignature = signature;
        playlistIndex = 0;
        playlistActive = true;
        consecutivePlaylistFailures = 0;

        requestedSource = playlistSources.get(playlistIndex);
        requestedLoop = false;
        long generation = ++requestGeneration;

        if (!enabled) {
            return;
        }

        if (requestedSource.equals(currentSource) && mediaPlayer != null) {
            updateExistingPlayer(false);
            return;
        }

        resolveAndPlay(requestedSource, generation);
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
            resolveAndPlay(requestedSource, ++requestGeneration);
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
            resolveAndPlay(requestedSource, ++requestGeneration);
        }
    }

    public void stop() {
        ++requestGeneration;
        requestedSource = null;
        currentSource = null;
        requestedLoop = false;
        clearPlaylistState();
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
        ++requestGeneration;
        requestedSource = null;
        currentSource = null;
        clearPlaylistState();
        releasePlayerOnly();
        mediaStore.close();
    }

    private void resolveAndPlay(String logicalSource, long generation) {
        mediaStore.resolve(logicalSource, playbackSource -> {
            if (!enabled
                    || generation != requestGeneration
                    || requestedSource == null
                    || !logicalSource.equals(requestedSource)) {
                return;
            }

            if (logicalSource.equals(currentSource) && mediaPlayer != null) {
                updateExistingPlayer(requestedLoop);
                return;
            }

            prepareAndPlay(logicalSource, playbackSource, requestedLoop, generation);
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
                resolveAndPlay(requestedSource, ++requestGeneration);
            }
        }
    }

    private void prepareAndPlay(
            String logicalSource,
            String playbackSource,
            boolean loop,
            long generation
    ) {
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
                if (mediaPlayer != player || generation != requestGeneration) {
                    if (mediaPlayer == player) {
                        releasePlayerOnly();
                    }
                    return;
                }

                prepared = true;
                consecutivePlaylistFailures = 0;

                if (enabled && logicalSource.equals(requestedSource)) {
                    try {
                        player.setLooping(requestedLoop);
                        player.start();
                    } catch (IllegalStateException ignored) {
                        releasePlayerOnly();
                    }
                }
            });
            candidate.setOnCompletionListener(player -> {
                if (mediaPlayer == player
                        && generation == requestGeneration
                        && playlistActive) {
                    advancePlaylist();
                }
            });
            candidate.setOnErrorListener((player, what, extra) -> {
                if (mediaPlayer == player) {
                    releasePlayerOnly();
                }
                handlePlaybackFailure(generation);
                return true;
            });
            candidate.setDataSource(playbackSource);
            candidate.prepareAsync();
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            if (mediaPlayer == candidate) {
                releasePlayerOnly();
            }
            handlePlaybackFailure(generation);
        }
    }

    private void advancePlaylist() {
        if (!playlistActive || playlistSources.isEmpty()) {
            return;
        }

        playlistIndex = (playlistIndex + 1) % playlistSources.size();
        requestedSource = playlistSources.get(playlistIndex);
        requestedLoop = false;
        long generation = ++requestGeneration;

        if (enabled) {
            resolveAndPlay(requestedSource, generation);
        }
    }

    private void handlePlaybackFailure(long failedGeneration) {
        if (failedGeneration != requestGeneration || !playlistActive || playlistSources.isEmpty()) {
            return;
        }

        consecutivePlaylistFailures++;
        if (consecutivePlaylistFailures >= playlistSources.size()) {
            requestedSource = null;
            currentSource = null;
            clearPlaylistState();
            return;
        }

        advancePlaylist();
    }

    private void clearPlaylistState() {
        playlistSources = Collections.emptyList();
        playlistSignature = null;
        playlistIndex = -1;
        playlistActive = false;
        consecutivePlaylistFailures = 0;
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

    private static List<String> normalizePlaylist(List<String> sources) {
        if (sources == null || sources.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> normalized = new ArrayList<>(sources.size());
        for (String source : sources) {
            if (source == null) {
                continue;
            }
            String trimmed = source.trim();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        return normalized;
    }

    private static String buildPlaylistSignature(List<String> sources, boolean shuffle) {
        return (shuffle ? "shuffle\n" : "ordered\n") + String.join("\n", sources);
    }

    private static float clamp(float value) {
        if (Float.isNaN(value)) {
            return 1.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
