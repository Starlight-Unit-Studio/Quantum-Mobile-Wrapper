package de.starlightunit.wrapper.media;

import android.util.Log;

import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

public final class QuantumMediaPlaybackService extends MediaSessionService {

    private static final String TAG = "QuantumNMP";

    private ExoPlayer player;
    private MediaSession mediaSession;

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build();

            player = new ExoPlayer.Builder(this).build();
            player.setAudioAttributes(audioAttributes, true);
            mediaSession = new MediaSession.Builder(this, player).build();
        } catch (RuntimeException exception) {
            Log.e(TAG, "Media3 playback service initialization failed", exception);
            releaseMediaObjects();
        }
    }

    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public void onDestroy() {
        releaseMediaObjects();
        super.onDestroy();
    }

    private void releaseMediaObjects() {
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
