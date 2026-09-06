package de.starlightunit.wrapper.media;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public final class QuantumMediaSessionClient {

    private static final String MEDIA_ARTIST = "Starlight Unit Studios";

    private final Handler mainHandler;
    private final ListenableFuture<MediaController> controllerFuture;

    private MediaController controller;
    private PendingPlay pendingPlay;
    private float volume = 1.0f;
    private boolean released;

    public QuantumMediaSessionClient(Context context) {
        Context appContext = context.getApplicationContext();
        mainHandler = new Handler(Looper.getMainLooper());

        SessionToken sessionToken = new SessionToken(
                appContext,
                new ComponentName(appContext, QuantumMediaPlaybackService.class)
        );
        controllerFuture = new MediaController.Builder(appContext, sessionToken).buildAsync();
        controllerFuture.addListener(this::onControllerFutureComplete, Runnable::run);
    }

    public void playResolved(
            String logicalSource,
            String playbackSource,
            boolean loop,
            float requestedVolume
    ) {
        if (released || logicalSource == null || playbackSource == null) {
            return;
        }

        volume = clamp(requestedVolume);
        PendingPlay request = new PendingPlay(logicalSource, playbackSource, loop, true);
        if (controller == null) {
            pendingPlay = request;
            return;
        }

        applyPlay(request);
    }

    public void pause() {
        if (released) {
            return;
        }
        if (pendingPlay != null) {
            pendingPlay.playWhenReady = false;
        }
        if (controller != null) {
            controller.pause();
        }
    }

    public void resume() {
        if (released) {
            return;
        }
        if (pendingPlay != null) {
            pendingPlay.playWhenReady = true;
        }
        if (controller != null && controller.getCurrentMediaItem() != null) {
            controller.play();
        }
    }

    public void stop() {
        pendingPlay = null;
        if (released || controller == null) {
            return;
        }
        controller.stop();
        controller.clearMediaItems();
    }

    public void setVolume(float requestedVolume) {
        volume = clamp(requestedVolume);
        if (!released && controller != null) {
            controller.setVolume(volume);
        }
    }

    public void release() {
        if (released) {
            return;
        }
        released = true;
        pendingPlay = null;

        if (controller != null) {
            controller.release();
            controller = null;
        } else {
            MediaController.releaseFuture(controllerFuture);
        }
    }

    private void onControllerFutureComplete() {
        try {
            MediaController readyController = controllerFuture.get();
            mainHandler.post(() -> attachController(readyController));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | CancellationException ignored) {
            // The bridge remains safe and silent if the media service cannot be reached.
        }
    }

    private void attachController(MediaController readyController) {
        if (released) {
            readyController.release();
            return;
        }

        controller = readyController;
        controller.setVolume(volume);

        PendingPlay request = pendingPlay;
        pendingPlay = null;
        if (request != null) {
            applyPlay(request);
        }
    }

    private void applyPlay(PendingPlay request) {
        if (controller == null || released) {
            pendingPlay = request;
            return;
        }

        controller.setVolume(volume);
        controller.setRepeatMode(request.loop ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);

        MediaItem currentItem = controller.getCurrentMediaItem();
        if (currentItem == null || !request.logicalSource.equals(currentItem.mediaId)) {
            MediaMetadata metadata = new MediaMetadata.Builder()
                    .setTitle(QuantumMediaMetadata.titleFromSource(request.logicalSource))
                    .setArtist(MEDIA_ARTIST)
                    .build();
            MediaItem mediaItem = new MediaItem.Builder()
                    .setMediaId(request.logicalSource)
                    .setUri(toPlaybackUri(request.playbackSource))
                    .setMediaMetadata(metadata)
                    .build();
            controller.setMediaItem(mediaItem);
            controller.prepare();
        }

        if (request.playWhenReady) {
            controller.play();
        } else {
            controller.pause();
        }
    }

    private static Uri toPlaybackUri(String playbackSource) {
        Uri uri = Uri.parse(playbackSource);
        if (uri.getScheme() == null || uri.getScheme().isEmpty()) {
            return Uri.fromFile(new File(playbackSource));
        }
        return uri;
    }

    private static float clamp(float value) {
        if (Float.isNaN(value)) {
            return 1.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static final class PendingPlay {
        private final String logicalSource;
        private final String playbackSource;
        private final boolean loop;
        private boolean playWhenReady;

        private PendingPlay(
                String logicalSource,
                String playbackSource,
                boolean loop,
                boolean playWhenReady
        ) {
            this.logicalSource = logicalSource;
            this.playbackSource = playbackSource;
            this.loop = loop;
            this.playWhenReady = playWhenReady;
        }
    }
}
