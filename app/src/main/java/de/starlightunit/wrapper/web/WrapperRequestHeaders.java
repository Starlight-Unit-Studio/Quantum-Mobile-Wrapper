package de.starlightunit.wrapper.web;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import de.starlightunit.wrapper.config.AppConfig;

public final class WrapperRequestHeaders {

    private static final Pattern HEADER_NAME = Pattern.compile("^[!#$%&'*+.^_`|~0-9A-Za-z-]+$");

    private WrapperRequestHeaders() {
    }

    public static Map<String, String> create() {
        return create(AppConfig.CUSTOM_REQUEST_HEADERS_JSON);
    }

    static Map<String, String> create(String customHeadersJson) {
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        headers.put(AppConfig.WRAPPER_HEADER_NAME, AppConfig.WRAPPER_HEADER_VALUE);
        headers.put(AppConfig.WRAPPER_VERSION_HEADER_NAME, AppConfig.VERSION_NAME);
        headers.put(AppConfig.APP_HEADER_NAME, AppConfig.APP_HEADER_VALUE);

        if (customHeadersJson != null && !customHeadersJson.trim().isEmpty()) {
            try {
                JSONObject custom = new JSONObject(customHeadersJson);
                Iterator<String> names = custom.keys();
                while (names.hasNext()) {
                    String name = names.next();
                    Object rawValue = custom.opt(name);
                    if (rawValue == null || rawValue == JSONObject.NULL) {
                        continue;
                    }
                    String value = String.valueOf(rawValue);
                    if (isSafeHeader(name, value)) {
                        headers.put(name, value);
                    }
                }
            } catch (JSONException ignored) {
                // Invalid generated configuration must not prevent the app from starting.
                // Quantum Builder validates the JSON before it reaches the wrapper.
            }
        }

        return Collections.unmodifiableMap(headers);
    }

    public static boolean containsConfiguredHeaders(Map<String, String> candidateHeaders) {
        if (candidateHeaders == null || candidateHeaders.isEmpty()) {
            return false;
        }

        for (Map.Entry<String, String> configured : create().entrySet()) {
            String actualValue = findIgnoreCase(candidateHeaders, configured.getKey());
            if (!configured.getValue().equals(actualValue)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isSafeHeader(String name, String value) {
        if (name == null || value == null) {
            return false;
        }
        String trimmedName = name.trim();
        if (trimmedName.isEmpty() || !HEADER_NAME.matcher(trimmedName).matches()) {
            return false;
        }
        return value.indexOf('\r') < 0 && value.indexOf('\n') < 0;
    }

    private static String findIgnoreCase(Map<String, String> headers, String name) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
