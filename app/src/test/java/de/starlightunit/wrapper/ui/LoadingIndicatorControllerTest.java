package de.starlightunit.wrapper.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class LoadingIndicatorControllerTest {

    @Test
    public void knownStylesArePreserved() {
        assertEquals("none", LoadingIndicatorController.normalizeStyle("none"));
        assertEquals("top-bar", LoadingIndicatorController.normalizeStyle("top-bar"));
        assertEquals("bottom-bar", LoadingIndicatorController.normalizeStyle("bottom-bar"));
        assertEquals("center-spinner", LoadingIndicatorController.normalizeStyle("center-spinner"));
        assertEquals("fullscreen-spinner", LoadingIndicatorController.normalizeStyle("fullscreen-spinner"));
    }

    @Test
    public void invalidOrEmptyStyleFallsBackToTopBar() {
        assertEquals("top-bar", LoadingIndicatorController.normalizeStyle(null));
        assertEquals("top-bar", LoadingIndicatorController.normalizeStyle(""));
        assertEquals("top-bar", LoadingIndicatorController.normalizeStyle("unknown"));
    }
}
