package de.starlightunit.wrapper.launch;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import de.starlightunit.wrapper.R;

/**
 * Shows exactly one startup splash. Generated apps may replace the canonical
 * Starlight production splash with a profile-provided drawable named
 * quantum_custom_splash. The two splash variants are mutually exclusive.
 */
public final class QuantumIntroController {
    public interface Completion {
        void onIntroFinished(boolean customSplashUsed);
    }

    private static final long SPLASH_MS = 2500L;
    private static final long FADE_MS = 180L;

    private final Activity activity;
    private final ImageView overlay;
    private final boolean customSplashEnabled;
    private final Completion completion;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean cancelled;
    private boolean customSplashUsed;
    private boolean completionDelivered;

    public QuantumIntroController(
            Activity activity,
            ImageView overlay,
            boolean customSplashEnabled,
            Completion completion
    ) {
        this.activity = activity;
        this.overlay = overlay;
        this.customSplashEnabled = customSplashEnabled;
        this.completion = completion;
    }

    public void start(boolean restoredActivityState) {
        cancelPending();
        cancelled = false;
        completionDelivered = false;
        customSplashUsed = false;

        int splashResource = R.drawable.quantum_production_splash;
        if (customSplashEnabled) {
            int custom = activity.getResources().getIdentifier(
                    "quantum_custom_splash",
                    "drawable",
                    activity.getPackageName()
            );
            if (custom != 0) {
                splashResource = custom;
                customSplashUsed = true;
            }
        }

        overlay.animate().cancel();
        overlay.setImageResource(splashResource);
        overlay.setAlpha(1f);
        overlay.setVisibility(View.VISIBLE);
        overlay.bringToFront();
        overlay.invalidate();

        // Start the display interval only after Android is about to render the
        // chosen splash. WebView startup therefore cannot consume the interval.
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
                            QuantumIntroController.this::fadeOut,
                            SPLASH_MS
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

    private void fadeOut() {
        if (cancelled) {
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
                    overlay.setVisibility(View.GONE);
                    deliverCompletion();
                })
                .start();
    }

    private void deliverCompletion() {
        if (completionDelivered || completion == null) {
            return;
        }
        completionDelivered = true;
        completion.onIntroFinished(customSplashUsed);
    }

    private void cancelPending() {
        handler.removeCallbacksAndMessages(null);
    }
}
