package de.starlightunit.wrapper.launch;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class SplashAttributionPolicyTest {

    @Test
    public void durationIsClampedToRequestedTwoToFourSecondWindow() {
        assertEquals(2000, SplashAttributionPolicy.clampDurationMs(500));
        assertEquals(2000, SplashAttributionPolicy.clampDurationMs(2000));
        assertEquals(3000, SplashAttributionPolicy.clampDurationMs(3000));
        assertEquals(4000, SplashAttributionPolicy.clampDurationMs(4000));
        assertEquals(4000, SplashAttributionPolicy.clampDurationMs(9000));
    }

    @Test
    public void footerInsetClearsNativeBottomSurfaces() {
        assertEquals(16, SplashAttributionPolicy.footerInsetDp(false, false));
        assertEquals(74, SplashAttributionPolicy.footerInsetDp(true, false));
        assertEquals(62, SplashAttributionPolicy.footerInsetDp(false, true));
        assertEquals(120, SplashAttributionPolicy.footerInsetDp(true, true));
    }

    @Test
    public void bannerRequiresCustomSplashFinishedIntroAndFirstPage() {
        assertTrue(SplashAttributionPolicy.shouldShow(true, true, true, true, false, false));
        assertFalse(SplashAttributionPolicy.shouldShow(true, true, false, true, false, false));
        assertFalse(SplashAttributionPolicy.shouldShow(true, false, true, true, false, false));
        assertFalse(SplashAttributionPolicy.shouldShow(true, true, true, false, false, false));
        assertFalse(SplashAttributionPolicy.shouldShow(true, true, true, true, true, false));
        assertFalse(SplashAttributionPolicy.shouldShow(true, true, true, true, false, true));
    }
}
