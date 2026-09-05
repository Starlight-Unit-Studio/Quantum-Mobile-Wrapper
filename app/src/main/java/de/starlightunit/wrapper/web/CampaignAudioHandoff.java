package de.starlightunit.wrapper.web;

import android.content.Context;
import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class CampaignAudioHandoff {

    private static final String ASSET_NAME = "quantum_campaign_audio.js";

    private final String script;

    public CampaignAudioHandoff(Context context) {
        script = loadScript(context.getApplicationContext());
    }

    public void inject(WebView webView) {
        if (script == null || script.isEmpty()) {
            return;
        }
        webView.evaluateJavascript(script, null);
    }

    private static String loadScript(Context context) {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(ASSET_NAME))
        )) {
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        } catch (IOException ignored) {
            return null;
        }
    }
}
