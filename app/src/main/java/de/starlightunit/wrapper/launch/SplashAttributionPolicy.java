package de.starlightunit.wrapper.launch;

public final class SplashAttributionPolicy {
    private SplashAttributionPolicy() {
    }

    public static int clampDurationMs(int requested) {
        return Math.max(2000, Math.min(4000, requested));
    }

    public static int footerInsetDp(boolean bottomTabsEnabled, boolean contextualToolbarEnabled) {
        int inset = 16;
        if (bottomTabsEnabled) {
            inset += 58;
        }
        if (contextualToolbarEnabled) {
            inset += 46;
        }
        return inset;
    }

    public static boolean shouldShow(
            boolean enabled,
            boolean introFinished,
            boolean customSplashUsed,
            boolean firstPageReady,
            boolean shown,
            boolean cancelled
    ) {
        return enabled
                && introFinished
                && customSplashUsed
                && firstPageReady
                && !shown
                && !cancelled;
    }
}
