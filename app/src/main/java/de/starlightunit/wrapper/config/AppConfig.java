package de.starlightunit.wrapper.config;

public final class AppConfig {
    public static final String START_URL = "https://game.starlight-unit.de/index_01.html";
    public static final String TRUSTED_DOMAIN = "starlight-unit.de";
    public static final String VERSION_NAME = "0.1.0-beta9";
    public static final String USER_AGENT_SUFFIX = " StarlightUnitAndroid/" + VERSION_NAME;

    public static final String NATIVE_MEDIA_BRIDGE_NAME = "QuantumNMP";
    public static final String NATIVE_MEDIA_PATH_PREFIX = "/assets/sounds/campaign/";

    public static final boolean QUANTUM_ASSET_STORE_ENABLED = true;
    public static final boolean ASSET_STORE_PAGE_WARMUP_ENABLED = false;
    public static final long ASSET_STORE_PAGE_WARMUP_DELAY_MS = 2_000L;
    public static final boolean NATIVE_ASSET_DOWNLOADER_ENABLED = false;
    public static final String ASSET_MANIFEST_URL = "";
    public static final String ASSET_DOWNLOADER_ROOTS = "/assets/portraits/";
    public static final String ASSET_STORE_TRUSTED_HOST = "game.starlight-unit.de";
    public static final String ASSET_STORE_PATH_PREFIX = "/assets/";
    public static final String ASSET_STORE_EXCLUDED_PATH_PREFIX = NATIVE_MEDIA_PATH_PREFIX;

    public static final String WRAPPER_HEADER_NAME = "X-Starlight-Wrapper";
    public static final String WRAPPER_HEADER_VALUE = "quantum-mobile-wrapper";
    public static final String WRAPPER_VERSION_HEADER_NAME = "X-Starlight-Wrapper-Version";
    public static final String APP_HEADER_NAME = "X-Starlight-App";
    public static final String APP_HEADER_VALUE = "starlight-unit-game";
    public static final String CUSTOM_REQUEST_HEADERS_JSON = "{}";
    public static final String CUSTOM_CSS = "";
    public static final String CUSTOM_JAVASCRIPT = "";
    public static final String COOKIE_PERSISTENCE_MODE = "persistent";

    public static final String LOADING_INDICATOR_STYLE = "top-bar";
    public static final String LOADING_INDICATOR_COLOR = "#6fc7ff";
    public static final int LOADING_BAR_THICKNESS_DP = 3;
    public static final int LOADING_SPINNER_SIZE_DP = 56;
    public static final int LOADING_OVERLAY_DIM_PERCENT = 35;

    public static final boolean KEEP_SCREEN_ON = true;
    public static final boolean ALLOW_AUTOPLAY_MEDIA = true;
    public static final boolean ALLOW_THIRD_PARTY_COOKIES = true;

    private AppConfig() {
    }
}
