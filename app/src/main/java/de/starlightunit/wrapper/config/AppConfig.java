package de.starlightunit.wrapper.config;

public final class AppConfig {
    public static final String START_URL = "https://game.starlight-unit.de/index_01.html";
    public static final String TRUSTED_DOMAIN = "starlight-unit.de";
    public static final String VERSION_NAME = "0.1.0-beta9";
    public static final String USER_AGENT_SUFFIX = " StarlightUnitAndroid/" + VERSION_NAME;

    public static final String NATIVE_MEDIA_BRIDGE_NAME = "QuantumNMP";
    public static final String NATIVE_MEDIA_PATH_PREFIX = "/assets/sounds/campaign/";

    public static final String ASSET_STORE_TRUSTED_HOST = "game.starlight-unit.de";
    public static final String ASSET_STORE_PATH_PREFIX = "/assets/";
    public static final String ASSET_STORE_EXCLUDED_PATH_PREFIX = NATIVE_MEDIA_PATH_PREFIX;

    // Quantum Asset Downloader. The manifest lives on the same trusted host as
    // START_URL. A missing manifest is treated as "feature not configured" and
    // never blocks the WebView. Roots are comma separated and are matched as
    // absolute URL path prefixes.
    public static final boolean ASSET_DOWNLOADER_ENABLED = true;
    public static final String ASSET_DOWNLOADER_MANIFEST_PATH = "/quantum-assets.json";
    public static final String ASSET_DOWNLOADER_ROOTS = "/assets/portraits/";
    public static final long ASSET_DOWNLOADER_CHECK_INTERVAL_MS = 0L;

    public static final String WRAPPER_HEADER_NAME = "X-Starlight-Wrapper";
    public static final String WRAPPER_HEADER_VALUE = "quantum-mobile-wrapper";
    public static final String WRAPPER_VERSION_HEADER_NAME = "X-Starlight-Wrapper-Version";
    public static final String APP_HEADER_NAME = "X-Starlight-App";
    public static final String APP_HEADER_VALUE = "starlight-unit-game";

    public static final boolean KEEP_SCREEN_ON = true;
    public static final boolean ALLOW_AUTOPLAY_MEDIA = true;
    public static final boolean ALLOW_THIRD_PARTY_COOKIES = true;

    private AppConfig() {
    }
}
