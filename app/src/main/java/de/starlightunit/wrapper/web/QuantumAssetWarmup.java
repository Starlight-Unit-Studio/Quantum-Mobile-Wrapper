package de.starlightunit.wrapper.web;

import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.starlightunit.wrapper.assets.QuantumAssetStore;

/** Collects already-used resource URLs from a trusted page and warms them natively. */
public final class QuantumAssetWarmup {

    private static final String DISCOVERY_SCRIPT =
            "(() => {"
                    + "const u=new Set();"
                    + "try{performance.getEntriesByType('resource').forEach(e=>u.add(e.name));}catch(e){}"
                    + "document.querySelectorAll('[src]').forEach(e=>{try{if(e.src)u.add(e.src);}catch(x){}});"
                    + "document.querySelectorAll('link[href]').forEach(e=>{try{if(e.href)u.add(e.href);}catch(x){}});"
                    + "return Array.from(u).slice(0,256);"
                    + "})()";

    private QuantumAssetWarmup() {
    }

    public static void capture(
            WebView webView,
            QuantumAssetStore assetStore,
            Map<String, String> requestHeaders
    ) {
        if (webView == null || assetStore == null) {
            return;
        }

        webView.evaluateJavascript(DISCOVERY_SCRIPT, value -> {
            if (value == null || value.isBlank() || "null".equals(value)) {
                return;
            }
            try {
                JSONArray array = new JSONArray(value);
                List<String> urls = new ArrayList<>(array.length());
                for (int index = 0; index < array.length(); index += 1) {
                    String url = array.optString(index, "");
                    if (!url.isBlank()) {
                        urls.add(url);
                    }
                }
                assetStore.prefetchAll(urls, requestHeaders);
            } catch (JSONException ignored) {
                // Discovery is opportunistic. The live WebView path remains untouched.
            }
        });
    }
}
