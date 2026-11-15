package de.starlightunit.wrapper.web;

import android.content.Context;
import android.content.res.Configuration;
import android.webkit.WebSettings;

import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import java.util.Locale;

import de.starlightunit.wrapper.config.AppConfig;

public final class WebThemeController {
    public static final String MODE_DARK = "dark";
    public static final String MODE_LIGHT = "light";
    public static final String MODE_AUTO = "auto";

    private WebThemeController() {
    }

    public static void configure(Context context, WebSettings settings) {
        String mode = normalize(AppConfig.WEB_DARK_MODE);
        boolean systemDark = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        boolean dark = resolveDark(mode, systemDark);

        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, dark);
            return;
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(
                    settings,
                    dark ? WebSettingsCompat.FORCE_DARK_ON : WebSettingsCompat.FORCE_DARK_OFF
            );
        }
    }

    static String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case MODE_LIGHT:
            case MODE_AUTO:
            case MODE_DARK:
                return normalized;
            default:
                return MODE_DARK;
        }
    }

    static boolean resolveDark(String configuredMode, boolean systemDark) {
        String mode = normalize(configuredMode);
        if (MODE_LIGHT.equals(mode)) {
            return false;
        }
        if (MODE_AUTO.equals(mode)) {
            return systemDark;
        }
        return true;
    }
}
