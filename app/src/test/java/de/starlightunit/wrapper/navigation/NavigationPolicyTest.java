package de.starlightunit.wrapper.navigation;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NavigationPolicyTest {
    private NavigationPolicy policy;

    @Before
    public void setUp() {
        policy = new NavigationPolicy("starlight-unit.de");
    }

    @Test
    public void allowsGameHost() {
        assertTrue(policy.shouldStayInWebView("https://game.starlight-unit.de/index_01.html"));
    }

    @Test
    public void allowsOtherStuSubdomainsForFutureApps() {
        assertTrue(policy.shouldStayInWebView("https://coreui.starlight-unit.de/"));
    }

    @Test
    public void blocksLookalikeHost() {
        assertFalse(policy.shouldStayInWebView("https://starlight-unit.de.evil.example/"));
        assertFalse(policy.shouldStayInWebView("https://evil-starlight-unit.de/"));
    }

    @Test
    public void blocksCleartextHttp() {
        assertFalse(policy.shouldStayInWebView("http://game.starlight-unit.de/"));
    }

    @Test
    public void leavesExternalSchemesToAndroid() {
        assertFalse(policy.shouldStayInWebView("mailto:team@starlight-unit.de"));
        assertFalse(policy.shouldStayInWebView("tel:+49123456789"));
    }

    @Test
    public void keepsWebViewOwnedSchemesInsideWebView() {
        assertTrue(policy.shouldStayInWebView("about:blank"));
        assertTrue(policy.shouldStayInWebView("data:text/plain,hello"));
        assertTrue(policy.shouldStayInWebView("blob:https://game.starlight-unit.de/example"));
    }
}
