package de.starlightunit.wrapper.web;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class WebViewProfileHeadersTest {

    @Test
    public void trustedOriginUsesHttpsStartOrigin() {
        assertEquals(
                "https://game.starlight-unit.de",
                WebViewProfileHeaders.trustedOrigin("https://game.starlight-unit.de/index_01.html")
        );
    }

    @Test
    public void trustedOriginKeepsNonDefaultHttpsPort() {
        assertEquals(
                "https://example.test:8443",
                WebViewProfileHeaders.trustedOrigin("https://example.test:8443/app/")
        );
    }

    @Test
    public void trustedOriginRejectsHttpAndUserInfo() {
        assertNull(WebViewProfileHeaders.trustedOrigin("http://example.test/"));
        assertNull(WebViewProfileHeaders.trustedOrigin("https://user@example.test/"));
    }
}
