package de.starlightunit.wrapper.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import java.util.Locale;

public final class LoadingIndicatorController {

    public static final String STYLE_NONE = "none";
    public static final String STYLE_TOP_BAR = "top-bar";
    public static final String STYLE_BOTTOM_BAR = "bottom-bar";
    public static final String STYLE_CENTER_SPINNER = "center-spinner";
    public static final String STYLE_FULLSCREEN_SPINNER = "fullscreen-spinner";

    private final ProgressBar horizontalBar;
    private final FrameLayout spinnerOverlay;
    private final ProgressBar spinner;
    private final String style;

    public LoadingIndicatorController(
            Context context,
            ProgressBar horizontalBar,
            FrameLayout spinnerOverlay,
            ProgressBar spinner,
            String configuredStyle,
            String configuredColor,
            int barThicknessDp,
            int spinnerSizeDp,
            int overlayDimPercent
    ) {
        this.horizontalBar = horizontalBar;
        this.spinnerOverlay = spinnerOverlay;
        this.spinner = spinner;
        this.style = normalizeStyle(configuredStyle);

        int color = parseColor(configuredColor);
        ColorStateList tint = ColorStateList.valueOf(color);
        horizontalBar.setProgressTintList(tint);
        spinner.setIndeterminateTintList(tint);

        float density = context.getResources().getDisplayMetrics().density;
        configureHorizontalBar(
                Math.max(1, Math.min(12, barThicknessDp)),
                density
        );
        configureSpinner(
                Math.max(24, Math.min(128, spinnerSizeDp)),
                density
        );

        int dimPercent = Math.max(0, Math.min(90, overlayDimPercent));
        int alpha = Math.round(255f * (dimPercent / 100f));
        spinnerOverlay.setBackgroundColor(
                STYLE_FULLSCREEN_SPINNER.equals(style)
                        ? Color.argb(alpha, 0, 0, 0)
                        : Color.TRANSPARENT
        );
        hide();
    }

    public void show() {
        if (STYLE_NONE.equals(style)) {
            hide();
            return;
        }

        if (isBarStyle()) {
            spinnerOverlay.setVisibility(View.GONE);
            spinner.setVisibility(View.GONE);
            horizontalBar.setVisibility(View.VISIBLE);
            return;
        }

        horizontalBar.setVisibility(View.GONE);
        spinnerOverlay.setVisibility(View.VISIBLE);
        spinner.setVisibility(View.VISIBLE);
    }

    public void hide() {
        horizontalBar.setVisibility(View.GONE);
        spinner.setVisibility(View.GONE);
        spinnerOverlay.setVisibility(View.GONE);
    }

    public void setProgress(int progress) {
        int bounded = Math.max(0, Math.min(100, progress));
        horizontalBar.setProgress(bounded);
        if (bounded >= 100) {
            hide();
        } else {
            show();
        }
    }

    static String normalizeStyle(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case STYLE_NONE:
            case STYLE_TOP_BAR:
            case STYLE_BOTTOM_BAR:
            case STYLE_CENTER_SPINNER:
            case STYLE_FULLSCREEN_SPINNER:
                return normalized;
            default:
                return STYLE_TOP_BAR;
        }
    }

    private boolean isBarStyle() {
        return STYLE_TOP_BAR.equals(style) || STYLE_BOTTOM_BAR.equals(style);
    }

    private void configureHorizontalBar(int thicknessDp, float density) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) horizontalBar.getLayoutParams();
        params.width = FrameLayout.LayoutParams.MATCH_PARENT;
        params.height = Math.max(1, Math.round(thicknessDp * density));
        params.gravity = STYLE_BOTTOM_BAR.equals(style) ? Gravity.BOTTOM : Gravity.TOP;
        horizontalBar.setLayoutParams(params);
        horizontalBar.setMax(100);
        horizontalBar.setIndeterminate(false);
    }

    private void configureSpinner(int spinnerSizeDp, float density) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) spinner.getLayoutParams();
        int pixels = Math.max(1, Math.round(spinnerSizeDp * density));
        params.width = pixels;
        params.height = pixels;
        params.gravity = Gravity.CENTER;
        spinner.setLayoutParams(params);
        spinner.setIndeterminate(true);
    }

    private static int parseColor(String configuredColor) {
        if (configuredColor != null) {
            try {
                return Color.parseColor(configuredColor.trim());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Color.parseColor("#6fc7ff");
    }
}
