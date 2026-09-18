package de.starlightunit.wrapper.navigation;

import java.util.Locale;

public final class NewWindowPolicy {
    public static final String BLOCKED = "blocked";
    public static final String INTERNAL = "internal";
    public static final String EXTERNAL = "external";

    private NewWindowPolicy() {
    }

    public static String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case INTERNAL:
            case EXTERNAL:
            case BLOCKED:
                return normalized;
            default:
                return BLOCKED;
        }
    }

    public static boolean supportsMultipleWindows(String value) {
        return !BLOCKED.equals(normalize(value));
    }
}
