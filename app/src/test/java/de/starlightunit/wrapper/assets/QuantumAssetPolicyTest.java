package de.starlightunit.wrapper.assets;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class QuantumAssetPolicyTest {

    private final QuantumAssetPolicy policy = new QuantumAssetPolicy(
            "game.starlight-unit.de",
            "/assets/",
            "/assets/sounds/campaign/"
    );

    @Test
    public void acceptsVersionedTrustedAssetAndDropsFragment() {
        QuantumAssetPolicy.AssetSpec spec = policy.inspect(
                "https://game.starlight-unit.de/assets/css/game.css?v=1.1.2.18#ignored"
        );

        assertNotNull(spec);
        assertEquals(
                "https://game.starlight-unit.de/assets/css/game.css?v=1.1.2.18",
                spec.getSource()
        );
        assertEquals("text/css", spec.getMimeType());
        assertEquals("UTF-8", spec.getEncoding());
        assertTrue(spec.isVersioned());
    }

    @Test
    public void treatsUnversionedAssetAsShortLived() {
        QuantumAssetPolicy.AssetSpec spec = policy.inspect(
                "https://game.starlight-unit.de/assets/bg/alliance.webp"
        );

        assertNotNull(spec);
        assertEquals("image/webp", spec.getMimeType());
        assertNull(spec.getEncoding());
        assertFalse(spec.isVersioned());
    }

    @Test
    public void rejectsCleartextAndOtherHosts() {
        assertNull(policy.inspect("http://game.starlight-unit.de/assets/css/game.css"));
        assertNull(policy.inspect("https://evil.example/assets/css/game.css"));
        assertNull(policy.inspect("https://cdn.game.starlight-unit.de/assets/css/game.css"));
    }

    @Test
    public void rejectsNonAssetAndDynamicPaths() {
        assertNull(policy.inspect("https://game.starlight-unit.de/api/player.php"));
        assertNull(policy.inspect("https://game.starlight-unit.de/start.html"));
        assertNull(policy.inspect("https://game.starlight-unit.de/assets/generated.php"));
    }

    @Test
    public void leavesCampaignMusicToQuantumNmpStore() {
        assertNull(policy.inspect(
                "https://game.starlight-unit.de/assets/sounds/campaign/game3.ogg"
        ));
        assertNotNull(policy.inspect(
                "https://game.starlight-unit.de/assets/sounds/ui/click.ogg"
        ));
    }

    @Test
    public void rejectsTraversalAndCredentialUrls() {
        assertNull(policy.inspect(
                "https://game.starlight-unit.de/assets/%2e%2e/api/secret.json"
        ));
        assertNull(policy.inspect(
                "https://user:pass@game.starlight-unit.de/assets/css/game.css"
        ));
    }

    @Test
    public void mapsSupportedStaticTypes() {
        assertEquals("font/woff2", QuantumAssetPolicy.mimeTypeForPath("/assets/fonts/ui.woff2"));
        assertEquals("application/wasm", QuantumAssetPolicy.mimeTypeForPath("/assets/runtime/core.wasm"));
        assertEquals("video/webm", QuantumAssetPolicy.mimeTypeForPath("/assets/video/intro.webm"));
        assertNull(QuantumAssetPolicy.mimeTypeForPath("/assets/archive/data.zip"));
    }
}
