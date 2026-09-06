package de.starlightunit.wrapper.assets;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class QuantumAssetStoreTest {

    @Test
    public void cacheKeyIsStableForSameUrl() {
        String source = "https://game.starlight-unit.de/assets/bg/alliance.webp?v=1.1.2.18";
        assertEquals(
                QuantumAssetStore.fileNameForSource(source),
                QuantumAssetStore.fileNameForSource(source)
        );
        assertTrue(QuantumAssetStore.fileNameForSource(source).endsWith(".asset"));
    }

    @Test
    public void cacheKeyChangesWithAssetVersion() {
        String oldSource = "https://game.starlight-unit.de/assets/bg/alliance.webp?v=1.1.2.17";
        String newSource = "https://game.starlight-unit.de/assets/bg/alliance.webp?v=1.1.2.18";

        assertNotEquals(
                QuantumAssetStore.fileNameForSource(oldSource),
                QuantumAssetStore.fileNameForSource(newSource)
        );
    }
}
