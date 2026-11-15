package de.starlightunit.wrapper.web;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class WebThemeControllerTest {

    @Test
    public void knownModesNormalizeAndUnknownFallsBackToDark() {
        assertEquals("dark", WebThemeController.normalize("dark"));
        assertEquals("light", WebThemeController.normalize(" LIGHT "));
        assertEquals("auto", WebThemeController.normalize("auto"));
        assertEquals("dark", WebThemeController.normalize(null));
        assertEquals("dark", WebThemeController.normalize("unexpected"));
    }

    @Test
    public void autoFollowsSystemTheme() {
        assertTrue(WebThemeController.resolveDark("auto", true));
        assertFalse(WebThemeController.resolveDark("auto", false));
        assertTrue(WebThemeController.resolveDark("dark", false));
        assertFalse(WebThemeController.resolveDark("light", true));
    }
}
