package de.starlightunit.wrapper.media;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class QuantumMediaMetadataTest {

    @Test
    public void derivesReadableTitleFromCampaignUrl() {
        assertEquals(
                "worldboss theme",
                QuantumMediaMetadata.titleFromSource(
                        "https://game.starlight-unit.de/assets/sounds/campaign/worldboss_theme.ogg?v=20"
                )
        );
    }

    @Test
    public void decodesEscapedPathCharacters() {
        assertEquals(
                "Ayal Theme 01",
                QuantumMediaMetadata.titleFromSource(
                        "https://game.starlight-unit.de/assets/sounds/campaign/Ayal%20Theme-01.ogg"
                )
        );
    }

    @Test
    public void fallsBackForInvalidOrEmptySource() {
        assertEquals("Starlight Unit", QuantumMediaMetadata.titleFromSource(""));
        assertEquals("Starlight Unit", QuantumMediaMetadata.titleFromSource("not a valid uri |"));
    }
}
