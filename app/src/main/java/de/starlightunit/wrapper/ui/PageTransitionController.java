package de.starlightunit.wrapper.ui;

import android.view.View;

public final class PageTransitionController {
    private static final long DURATION_MS = 140L;

    private final View content;
    private final boolean enabled;

    public PageTransitionController(View content, boolean enabled) {
        this.content = content;
        this.enabled = enabled;
    }

    public void onPageLoading() {
        content.animate().cancel();
        if (!enabled) {
            content.setAlpha(1f);
            return;
        }
        content.setAlpha(0.72f);
    }

    public void onPageReady() {
        content.animate().cancel();
        if (!enabled) {
            content.setAlpha(1f);
            return;
        }
        content.animate().alpha(1f).setDuration(DURATION_MS).start();
    }

    public void reset() {
        content.animate().cancel();
        content.setAlpha(1f);
    }
}
