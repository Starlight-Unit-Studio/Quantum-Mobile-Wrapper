package de.starlightunit.wrapper.launch;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;

import de.starlightunit.wrapper.R;

/**
 * Controls the mandatory Starlight Quantum production splash and the optional
 * app-owner intro. The production splash is always first. A generated wrapper
 * may optionally provide a drawable named {@code quantum_user_intro}; when it
 * is absent the app proceeds directly to the WebView.
 */
public final class QuantumIntroController {
    private static final long PRODUCTION_SPLASH_MS = 2500L;
    private static final long USER_INTRO_MS = 2000L;
    private static final long FADE_MS = 180L;

    private final Activity activity;
    private final ImageView overlay;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean cancelled;

    public QuantumIntroController(Activity activity, ImageView overlay) {
        this.activity = activity;
        this.overlay = overlay;
    }

    public void start(boolean restoredActivityState) {
        cancelPending();
        cancelled = false;

        if (restoredActivityState) {
            overlay.setVisibility(View.GONE);
            return;
        }

        overlay.setImageResource(R.drawable.quantum_production_splash);
        overlay.setAlpha(1f);
        overlay.setVisibility(View.VISIBLE);
        handler.postDelayed(this::showOptionalUserIntroOrFinish, PRODUCTION_SPLASH_MS);
    }

    public void cancel() {
        cancelled = true;
        cancelPending();
        overlay.animate().cancel();
    }

    private void showOptionalUserIntroOrFinish() {
        if (cancelled) {
            return;
        }

        int userIntro = activity.getResources().getIdentifier(
                "quantum_user_intro",
                "drawable",
                activity.getPackageName()
        );

        if (userIntro == 0) {
            fadeOut();
            return;
        }

        overlay.animate().cancel();
        overlay.animate()
                .alpha(0f)
                .setDuration(FADE_MS)
                .withEndAction(() -> {
                    if (cancelled) {
                        return;
                    }
                    overlay.setImageResource(userIntro);
                    overlay.setAlpha(0f);
                    overlay.animate()
                            .alpha(1f)
                            .setDuration(FADE_MS)
                            .withEndAction(() -> handler.postDelayed(this::fadeOut, USER_INTRO_MS))
                            .start();
                })
                .start();
    }

    private void fadeOut() {
        if (cancelled) {
            return;
        }
        overlay.animate().cancel();
        overlay.animate()
                .alpha(0f)
                .setDuration(FADE_MS)
                .withEndAction(() -> {
                    if (!cancelled) {
                        overlay.setVisibility(View.GONE);
                    }
                })
                .start();
    }

    private void cancelPending() {
        handler.removeCallbacksAndMessages(null);
    }
}
