package de.starlightunit.wrapper.session;

import android.webkit.CookieManager;

import java.util.Locale;

public final class CookiePersistencePolicy {
    public static final String MODE_PERSISTENT = "persistent";
    public static final String MODE_SERVER = "server";
    public static final String MODE_SESSION = "session";

    private CookiePersistencePolicy() {
    }

    public static String normalize(String configuredMode) {
        String mode = configuredMode == null
                ? ""
                : configuredMode.trim().toLowerCase(Locale.ROOT);
        switch (mode) {
            case MODE_SERVER:
            case MODE_SESSION:
            case MODE_PERSISTENT:
                return mode;
            case "default":
            case "":
            default:
                // Legacy Quantum builds always persisted trusted-origin session
                // cookies. Keep that behavior unless a profile explicitly opts
                // into server defaults or session-only mode.
                return MODE_PERSISTENT;
        }
    }

    public static boolean usesEncryptedPersistence(String configuredMode) {
        return MODE_PERSISTENT.equals(normalize(configuredMode));
    }

    public static boolean startsFreshSession(String configuredMode) {
        return MODE_SESSION.equals(normalize(configuredMode));
    }

    public static void clearForFreshSession() {
        CookieManager cookies = CookieManager.getInstance();
        cookies.removeAllCookies(null);
        cookies.flush();
    }
}
