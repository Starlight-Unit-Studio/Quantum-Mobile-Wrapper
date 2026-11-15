package de.starlightunit.wrapper.navigation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/** Icon identifiers accepted by compiled profiles. No URL or filesystem resolution. */
public final class NativeNavigationIconPolicy {
    public static final int MAX_PNG_BYTES = 65_536;
    public static final int MAX_PNG_DIMENSION = 256;
    public static final List<String> BUILTIN_IDS = Collections.unmodifiableList(Arrays.asList(
            "home", "search", "grid", "star", "heart", "user", "settings", "info", "mail", "cart",
            "calendar", "camera", "map", "phone", "download", "help", "play", "folder", "globe", "link"
    ));
    private static final Pattern ASSET_REFERENCE = Pattern.compile(
            "asset:quantum_nav_icons/[0-9a-f]{64}\\.png"
    );

    private NativeNavigationIconPolicy() {
    }

    public static String normalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return BUILTIN_IDS.contains(value) || ASSET_REFERENCE.matcher(value).matches() ? value : "";
    }

    public static String assetPath(String value) {
        return value != null && ASSET_REFERENCE.matcher(value).matches() ? value.substring(6) : "";
    }

    public static String forState(String icon, String activeIcon, boolean selected) {
        String active = normalize(activeIcon);
        return selected && !active.isEmpty() ? active : normalize(icon);
    }

    public static boolean hasIcon(String icon, String activeIcon) {
        return !normalize(icon).isEmpty() || !normalize(activeIcon).isEmpty();
    }
}
