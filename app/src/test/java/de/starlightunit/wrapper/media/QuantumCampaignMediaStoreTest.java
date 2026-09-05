package de.starlightunit.wrapper.media;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class QuantumCampaignMediaStoreTest {

    @Test
    public void recognizesHttpsOggSources() {
        assertTrue(QuantumCampaignMediaStore.isPersistentOggSource(
                "https://game.starlight-unit.de/assets/sounds/campaign/game3.ogg"
        ));
        assertTrue(QuantumCampaignMediaStore.isPersistentOggSource(
                "https://game.starlight-unit.de/assets/sounds/campaign/episode/game12.OGG?v=2"
        ));
    }

    @Test
    public void rejectsNonOggAndCleartextSources() {
        assertFalse(QuantumCampaignMediaStore.isPersistentOggSource(
                "https://game.starlight-unit.de/assets/sounds/campaign/game3.mp3"
        ));
        assertFalse(QuantumCampaignMediaStore.isPersistentOggSource(
                "http://game.starlight-unit.de/assets/sounds/campaign/game3.ogg"
        ));
        assertFalse(QuantumCampaignMediaStore.isPersistentOggSource(null));
    }

    @Test
    public void generatesStableCollisionResistantOggFileNames() {
        String source = "https://game.starlight-unit.de/assets/sounds/campaign/game3.ogg";
        String same = QuantumCampaignMediaStore.fileNameForSource(source);
        String versioned = QuantumCampaignMediaStore.fileNameForSource(source + "?v=2");

        assertEquals(same, QuantumCampaignMediaStore.fileNameForSource(source));
        assertTrue(same.matches("^[0-9a-f]{64}\\.ogg$"));
        assertNotEquals(same, versioned);
    }
}
