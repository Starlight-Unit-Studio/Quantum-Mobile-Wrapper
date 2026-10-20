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
        assertEquals("", config.items.get(0).icon);
        assertEquals("", config.items.get(0).activeIcon);
        assertEquals("News", config.items.get(1).label);
    }

    @Test
    public void malformedJsonProducesEmptyMenuInsteadOfCrashing() {
        NativeNavigationConfig config = NativeNavigationConfig.parse("App", "{bad");
        assertTrue(config.items.isEmpty());
    }

    @Test
    public void preservesIconsAndIgnoresUnsafeIconFieldsWithoutLosingDestinations() {
        NativeNavigationConfig config = NativeNavigationConfig.parse("App",
                "[{\"label\":\"Home\",\"url\":\"/\",\"icon\":\"home\",\"active_icon\":\"star\"},"
                + "{\"label\":\"Search\",\"url\":\"/search\",\"icon\":\"https://example.test/a.png\",\"active_icon\":\"search\"},"
                + "{\"label\":\"Help\",\"url\":\"/help\",\"icon\":{},\"active_icon\":true}]"
        );
        assertEquals(3, config.items.size());
        assertEquals("home", config.items.get(0).icon);
        assertEquals("star", config.items.get(0).activeIcon);
        assertEquals("", config.items.get(1).icon);
        assertEquals("search", config.items.get(1).activeIcon);
        assertEquals("/search", config.items.get(1).target);
        assertEquals("", config.items.get(2).icon);
        assertEquals("", config.items.get(2).activeIcon);
    }

    @Test
    public void preservesCompiledAssetsButRejectsRawImageData() {
        String asset = "asset:quantum_nav_icons/0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef.png";
        NativeNavigationConfig config = NativeNavigationConfig.parse("App",
                "[{\"label\":\"Custom\",\"url\":\"/custom\",\"icon\":\"" + asset
                        + "\",\"active_icon\":\"data:image/png;base64,iVBORw0KGgo=\"}]"
        );
        assertEquals(asset, config.items.get(0).icon);
        assertEquals("", config.items.get(0).activeIcon);
    }

    @Test
    public void queryAndFragmentTargetsKeepEstablishedJavaResolution() {
        assertEquals("https://example.test/app/?page=2", NativeNavigationConfig.resolveTarget(
                "https://example.test/app/index.html?old=1", "?page=2"));
        assertEquals("https://example.test/app/index.html?old=1#section", NativeNavigationConfig.resolveTarget(
                "https://example.test/app/index.html?old=1", "#section"));
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
