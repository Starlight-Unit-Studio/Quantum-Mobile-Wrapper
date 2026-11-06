package de.starlightunit.wrapper.ui;

import java.util.Locale;

public final class SystemBarStyle {
    private SystemBarStyle() {
    }

    public static int parseRgb(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^#[0-9a-f]{6}$")) {
            return fallback;
        }
        try {
            return 0xff000000 | Integer.parseInt(normalized.substring(1), 16);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static boolean shouldUseDarkIcons(int argb) {
        double r = ((argb >> 16) & 0xff) / 255.0;
        double g = ((argb >> 8) & 0xff) / 255.0;
        double b = (argb & 0xff) / 255.0;
        double luminance = (0.2126 * linearize(r))
                + (0.7152 * linearize(g))
                + (0.0722 * linearize(b));
        return luminance > 0.55;
    }

    private static double linearize(double value) {
        return value <= 0.03928
                ? value / 12.92
                : Math.pow((value + 0.055) / 1.055, 2.4);
    }
}
