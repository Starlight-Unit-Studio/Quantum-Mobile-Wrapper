package de.starlightunit.wrapper.navigation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class DeepLinkResolverTest {

    private final NavigationPolicy navigation = new NavigationPolicy("example.test");

    @Test
    public void resolvesRelativeCustomSchemePath() {
        assertEquals(
                "https://example.test/news/42",
                DeepLinkResolver.resolve(
                        "quantum:///news/42",
                        "quantum",
                        "https://example.test/index.html",
                        navigation
                )
        );
    }

    @Test
    public void resolvesTrustedUrlQueryTarget() {
        assertEquals(
                "https://example.test/account?tab=2",
                DeepLinkResolver.resolve(
                        "quantum://open?url=https%3A%2F%2Fexample.test%2Faccount%3Ftab%3D2",
                        "quantum",
                        "https://example.test/",
                        navigation
                )
        );
    }

    @Test
    public void rejectsUntrustedQueryTargetAndWrongScheme() {
        assertEquals(
                "",
                DeepLinkResolver.resolve(
                        "quantum://open?url=https%3A%2F%2Fevil.test%2F",
                        "quantum",
                        "https://example.test/",
                        navigation
                )
        );
        assertEquals(
                "",
                DeepLinkResolver.resolve(
                        "other:///news",
                        "quantum",
                        "https://example.test/",
                        navigation
                )
        );
    }
}
