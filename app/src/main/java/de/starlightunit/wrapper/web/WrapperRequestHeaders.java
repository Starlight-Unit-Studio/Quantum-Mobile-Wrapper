package de.starlightunit.wrapper.web;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import de.starlightunit.wrapper.config.AppConfig;

public final class WrapperRequestHeaders {

    private WrapperRequestHeaders() {
    }

    public static Map<String, String> create() {
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        headers.put(AppConfig.WRAPPER_HEADER_NAME, AppConfig.WRAPPER_HEADER_VALUE);
        headers.put(AppConfig.WRAPPER_VERSION_HEADER_NAME, AppConfig.VERSION_NAME);
        headers.put(AppConfig.APP_HEADER_NAME, AppConfig.APP_HEADER_VALUE);
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

    private static String findIgnoreCase(Map<String, String> headers, String name) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
