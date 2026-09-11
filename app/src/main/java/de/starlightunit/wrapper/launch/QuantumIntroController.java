package de.starlightunit.wrapper.launch;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewTreeObserver;
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

        // The production attribution is mandatory for every newly created
        // MainActivity. Do not suppress it just because Android supplied a
        // restored state after an update/process recreation.
        overlay.animate().cancel();
        overlay.setImageResource(R.drawable.quantum_production_splash);
        overlay.setAlpha(1f);
        overlay.setVisibility(View.VISIBLE);
        overlay.bringToFront();
        overlay.invalidate();

        // Start the 2.5 s display window only after Android is about to render
        // the overlay for the first time. Previously the timer started inside
        // onCreate; WebView startup could consume that interval before a frame
        // reached the display, making the splash appear to be skipped.
        overlay.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            private boolean armed;

            @Override
            public boolean onPreDraw() {
                if (armed) {
                    return true;
                }
                armed = true;
                ViewTreeObserver observer = overlay.getViewTreeObserver();
                if (observer.isAlive()) {
                    observer.removeOnPreDrawListener(this);
                }
                if (!cancelled) {
                    handler.postDelayed(
                            QuantumIntroController.this::showOptionalUserIntroOrFinish,
                            PRODUCTION_SPLASH_MS
                    );
                }
                return true;
            }
        });
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
