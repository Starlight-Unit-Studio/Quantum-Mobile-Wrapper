package de.starlightunit.wrapper.web;

import android.webkit.WebView;

import org.json.JSONObject;

import de.starlightunit.wrapper.config.AppConfig;

/**
 * Applies profile-provided CSS and JavaScript only after the caller has
 * confirmed that the current page belongs to the trusted app origin.
 */
public final class WebOverrides {
    private static final String STYLE_ELEMENT_ID = "quantum-profile-custom-css";

    private WebOverrides() {
    }

    public static void apply(WebView webView) {
        String css = AppConfig.CUSTOM_CSS;
        if (css != null && !css.trim().isEmpty()) {
            webView.evaluateJavascript(cssInjectionScript(css), null);
        }

        String javascript = AppConfig.CUSTOM_JAVASCRIPT;
        if (javascript != null && !javascript.trim().isEmpty()) {
            webView.evaluateJavascript(javascriptInjectionScript(javascript), null);
        }
    }

    static String cssInjectionScript(String css) {
        String quotedCss = JSONObject.quote(css == null ? "" : css);
        return "(function(){"
                + "var d=document;"
                + "var s=d.getElementById('" + STYLE_ELEMENT_ID + "');"
                + "if(!s){s=d.createElement('style');s.id='" + STYLE_ELEMENT_ID + "';"
                + "(d.head||d.documentElement).appendChild(s);}"
                + "s.textContent=" + quotedCss + ";"
                + "})();";
    }

    static String javascriptInjectionScript(String javascript) {
        String source = javascript == null ? "" : javascript;
        return source + "\n//# sourceURL=quantum-profile-custom.js";
    }
}
