package de.starlightunit.wrapper.navigation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class NativeNavigationIconPolicyTest {
    private static final String HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String ASSET = "asset:quantum_nav_icons/" + HASH + ".png";

    @Test
    public void acceptsEveryPublishedBuiltinAndAnExactCompiledAsset() {
        assertEquals(20, NativeNavigationIconPolicy.BUILTIN_IDS.size());
        for (String id : NativeNavigationIconPolicy.BUILTIN_IDS) {
            assertEquals(id, NativeNavigationIconPolicy.normalize(id));
            assertTrue(NativeNavigationIconPolicy.hasIcon(id, ""));
        }
        assertEquals(ASSET, NativeNavigationIconPolicy.normalize(ASSET));
        assertEquals("quantum_nav_icons/" + HASH + ".png", NativeNavigationIconPolicy.assetPath(ASSET));
    }

    @Test
    public void rejectsUnknownValuesAndEveryUncompiledImageSource() {
        String[] invalid = {
                null, "", "Home", " home", "home ", "home\n", "unknown", "1",
                "https://example.test/icon.png", "http://example.test/icon.png",
                "data:image/png;base64,iVBORw0KGgo=", "file:///data/icon.png",
                "content://images/1", "@drawable/ic_launcher", "../icon.png"
        };
        for (String value : invalid) {
            assertEquals(String.valueOf(value), "", NativeNavigationIconPolicy.normalize(value));
            assertEquals("", NativeNavigationIconPolicy.assetPath(value));
        }
        assertFalse(NativeNavigationIconPolicy.hasIcon("unknown", "https://example.test/icon.png"));
    }

    @Test
    public void rejectsAssetTraversalOtherNamespacesAndNearMisses() {
        String[] invalid = {
                "asset:../quantum_nav_icons/" + HASH + ".png",
                "asset:quantum_nav_icons/../" + HASH + ".png",
                "asset:quantum_nav_icons/%2e%2e/" + HASH + ".png",
                "asset:quantum_nav_icons\\" + HASH + ".png",
                "asset:other/" + HASH + ".png", "asset:/quantum_nav_icons/" + HASH + ".png",
                "asset:quantum_nav_icons/" + HASH.toUpperCase(java.util.Locale.ROOT) + ".png",
                "asset:quantum_nav_icons/" + HASH.substring(1) + ".png",
                "asset:quantum_nav_icons/" + HASH + "0.png", ASSET + "?v=1", ASSET + "#icon",
                ASSET + "\n", ASSET.replace(".png", ".svg"), ASSET.replace(".png", ".PNG")
        };
        for (String value : invalid) {
            assertEquals(value, "", NativeNavigationIconPolicy.normalize(value));
            assertEquals("", NativeNavigationIconPolicy.assetPath(value));
        }
    }

    @Test
    public void selectedVariantFallsBackToNormalAndSupportsActiveOnlyIcons() {
        assertEquals("home", NativeNavigationIconPolicy.forState("home", "star", false));
        assertEquals("star", NativeNavigationIconPolicy.forState("home", "star", true));
        assertEquals("home", NativeNavigationIconPolicy.forState("home", "", true));
        assertEquals("home", NativeNavigationIconPolicy.forState("home", "unsafe", true));
        assertEquals(ASSET, NativeNavigationIconPolicy.forState(ASSET, null, true));
        assertEquals("", NativeNavigationIconPolicy.forState("", "star", false));
        assertEquals("star", NativeNavigationIconPolicy.forState("", "star", true));
        assertEquals("", NativeNavigationIconPolicy.forState(null, null, true));
        assertTrue(NativeNavigationIconPolicy.hasIcon("", "star"));
        assertFalse(NativeNavigationIconPolicy.hasIcon("", ""));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void callersCannotAlterTheRuntimeAllowlist() {
        NativeNavigationIconPolicy.BUILTIN_IDS.add("unsafe");
    }
}
