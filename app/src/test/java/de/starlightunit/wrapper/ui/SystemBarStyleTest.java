package de.starlightunit.wrapper.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class SystemBarStyleTest {

    @Test
    public void parsesStrictRgbHexAndFallsBackOtherwise() {
        assertEquals(0xff020611, SystemBarStyle.parseRgb("#020611", 0xff000000));
        assertEquals(0xffabcdef, SystemBarStyle.parseRgb("#ABCDEF", 0xff000000));
        assertEquals(0xff123456, SystemBarStyle.parseRgb("nope", 0xff123456));
        assertEquals(0xff123456, SystemBarStyle.parseRgb(null, 0xff123456));
    }

    @Test
    public void brightBarsUseDarkIconsAndDarkBarsUseLightIcons() {
        assertTrue(SystemBarStyle.shouldUseDarkIcons(0xffffffff));
        assertTrue(SystemBarStyle.shouldUseDarkIcons(0xfff0f0f0));
        assertFalse(SystemBarStyle.shouldUseDarkIcons(0xff020611));
        assertFalse(SystemBarStyle.shouldUseDarkIcons(0xff000000));
    }
}
