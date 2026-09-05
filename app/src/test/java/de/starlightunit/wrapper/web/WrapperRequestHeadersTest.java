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
    public void configuredHeadersIdentifyQuantumWrapperAndGame() {
        Map<String, String> headers = WrapperRequestHeaders.create();

        assertEquals("quantum-mobile-wrapper", headers.get(AppConfig.WRAPPER_HEADER_NAME));
        assertEquals(AppConfig.VERSION_NAME, headers.get(AppConfig.WRAPPER_VERSION_HEADER_NAME));
        assertEquals("starlight-unit-game", headers.get(AppConfig.APP_HEADER_NAME));
    }

    @Test
    public void headerDetectionIsCaseInsensitive() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("x-starlight-wrapper", "quantum-mobile-wrapper");
        headers.put("x-starlight-wrapper-version", AppConfig.VERSION_NAME);
        headers.put("x-starlight-app", "starlight-unit-game");

        assertTrue(WrapperRequestHeaders.containsConfiguredHeaders(headers));
    }

    @Test
    public void incompleteHeaderSetDoesNotMatch() {
        assertFalse(WrapperRequestHeaders.containsConfiguredHeaders(
                Map.of(AppConfig.WRAPPER_HEADER_NAME, AppConfig.WRAPPER_HEADER_VALUE)
        ));
    }
}
