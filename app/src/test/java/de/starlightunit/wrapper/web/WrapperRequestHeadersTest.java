package de.starlightunit.wrapper.web;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import de.starlightunit.wrapper.config.AppConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class WrapperRequestHeadersTest {

    @Test
    public void configuredHeadersIdentifyQuantumWrapperAndConfiguredApp() {
        Map<String, String> headers = WrapperRequestHeaders.create();

        assertEquals(AppConfig.WRAPPER_HEADER_VALUE, headers.get(AppConfig.WRAPPER_HEADER_NAME));
        assertEquals(AppConfig.VERSION_NAME, headers.get(AppConfig.WRAPPER_VERSION_HEADER_NAME));
        assertEquals(AppConfig.APP_HEADER_VALUE, headers.get(AppConfig.APP_HEADER_NAME));
    }

    @Test
    public void headerDetectionIsCaseInsensitive() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(AppConfig.WRAPPER_HEADER_NAME.toLowerCase(), AppConfig.WRAPPER_HEADER_VALUE);
        headers.put(AppConfig.WRAPPER_VERSION_HEADER_NAME.toLowerCase(), AppConfig.VERSION_NAME);
        headers.put(AppConfig.APP_HEADER_NAME.toLowerCase(), AppConfig.APP_HEADER_VALUE);

        assertTrue(WrapperRequestHeaders.containsConfiguredHeaders(headers));
    }

    @Test
    public void incompleteHeaderSetDoesNotMatch() {
        assertFalse(WrapperRequestHeaders.containsConfiguredHeaders(
                Map.of(AppConfig.WRAPPER_HEADER_NAME, AppConfig.WRAPPER_HEADER_VALUE)
        ));
    }
}
