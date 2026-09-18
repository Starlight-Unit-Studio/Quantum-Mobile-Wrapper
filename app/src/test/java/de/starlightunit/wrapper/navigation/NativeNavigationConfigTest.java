package de.starlightunit.wrapper.navigation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class NativeNavigationConfigTest {

    @Test
    public void parsesValidItemsAndSkipsInvalidEntries() {
        NativeNavigationConfig config = NativeNavigationConfig.parse(
                " Quantum ",
                "[{\"label\":\"Home\",\"url\":\"/\"},"
                        + "{\"label\":\"News\",\"url\":\"/news\"},"
                        + "{\"label\":\"\",\"url\":\"/bad\"},"
                        + "{\"label\":\"Missing URL\"}]"
        );

        assertEquals("Quantum", config.title);
        assertEquals(2, config.items.size());
        assertEquals("Home", config.items.get(0).label);
        assertEquals("/", config.items.get(0).target);
        assertEquals("News", config.items.get(1).label);
    }

    @Test
    public void malformedJsonProducesEmptyMenuInsteadOfCrashing() {
        NativeNavigationConfig config = NativeNavigationConfig.parse("App", "{bad");
        assertTrue(config.items.isEmpty());
    }

    @Test
    public void resolvesRelativeTargetsAgainstStartUrl() {
        assertEquals(
                "https://example.test/news",
                NativeNavigationConfig.resolveTarget(
                        "https://example.test/app/index.html",
                        "/news"
                )
        );
        assertEquals(
                "https://example.test/app/settings",
                NativeNavigationConfig.resolveTarget(
                        "https://example.test/app/index.html",
                        "settings"
                )
        );
    }
}
