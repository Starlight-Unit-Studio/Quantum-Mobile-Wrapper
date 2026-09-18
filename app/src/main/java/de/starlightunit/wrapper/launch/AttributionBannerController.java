package de.starlightunit.wrapper.launch;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public final class AttributionBannerController {
    private static final long FADE_IN_MS = 180L;
    private static final long FADE_OUT_MS = 220L;

    private final Activity activity;
    private final ImageView banner;
    private final boolean enabled;
    private final long durationMs;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean introFinished;
    private boolean customSplashUsed;
    private boolean firstPageReady;
    private boolean shown;
    private boolean cancelled;

    public AttributionBannerController(
            Activity activity,
            ImageView banner,
            boolean enabled,
            int durationMs,
            int bottomInsetDp
    ) {
        this.activity = activity;
        this.banner = banner;
        this.enabled = enabled;
        this.durationMs = Math.max(2000L, Math.min(4000L, durationMs));
        applyBottomInset(bottomInsetDp);
        banner.setVisibility(View.GONE);
        banner.setAlpha(0f);
    }

    public void onIntroFinished(boolean customSplashUsed) {
        this.introFinished = true;
        this.customSplashUsed = customSplashUsed;
        maybeShow();
    }

    public void onFirstPageReady() {
        if (firstPageReady) {
            return;
        }
        firstPageReady = true;
        maybeShow();
    }

    public void cancel() {
        cancelled = true;
        handler.removeCallbacksAndMessages(null);
        banner.animate().cancel();
        banner.setVisibility(View.GONE);
    }

    private void maybeShow() {
        if (cancelled
                || shown
                || !enabled
                || !introFinished
                || !customSplashUsed
                || !firstPageReady) {
            return;
        }

        int resource = activity.getResources().getIdentifier(
                "quantum_studio_attribution_banner",
                "drawable",
                activity.getPackageName()
        );
        if (resource == 0) {
            return;
        }

        shown = true;
        banner.setImageResource(resource);
        banner.setVisibility(View.VISIBLE);
        banner.setAlpha(0f);
        banner.setTranslationY(dp(12));
        banner.bringToFront();
        banner.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(FADE_IN_MS)
                .withEndAction(() -> handler.postDelayed(this::hide, durationMs))
                .start();
    }

    private void hide() {
        if (cancelled) {
            return;
        }
        banner.animate().cancel();
        banner.animate()
                .alpha(0f)
                .translationY(dp(8))
                .setDuration(FADE_OUT_MS)
                .withEndAction(() -> {
                    if (!cancelled) {
                        banner.setVisibility(View.GONE);
                    }
                })
                .start();
    }

    private void applyBottomInset(int bottomInsetDp) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) banner.getLayoutParams();
        params.bottomMargin = dp(Math.max(16, bottomInsetDp));
        banner.setLayoutParams(params);
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
