package de.starlightunit.wrapper.navigation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class NewWindowPolicyTest {

    @Test
    public void supportedModesNormalizeAndUnknownBlocks() {
        assertEquals("blocked", NewWindowPolicy.normalize(null));
        assertEquals("blocked", NewWindowPolicy.normalize("unexpected"));
        assertEquals("internal", NewWindowPolicy.normalize(" INTERNAL "));
        assertEquals("external", NewWindowPolicy.normalize("external"));
    }

    @Test
    public void onlyNonBlockedModesEnableTemporaryPopupCapture() {
        assertFalse(NewWindowPolicy.supportsMultipleWindows("blocked"));
        assertTrue(NewWindowPolicy.supportsMultipleWindows("internal"));
        assertTrue(NewWindowPolicy.supportsMultipleWindows("external"));
    }
}
