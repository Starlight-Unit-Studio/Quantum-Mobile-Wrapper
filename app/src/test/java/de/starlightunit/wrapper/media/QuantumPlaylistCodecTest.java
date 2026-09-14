package de.starlightunit.wrapper.media;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public final class QuantumPlaylistCodecTest {

    @Test
    public void decodeKeepsOrderedNonBlankSources() {
        List<String> sources = QuantumPlaylistCodec.decode(
                " /assets/sounds/campaign/game1.ogg\n\n/assets/sounds/campaign/game2.ogg \r\n"
        );

        assertEquals(2, sources.size());
        assertEquals("/assets/sounds/campaign/game1.ogg", sources.get(0));
        assertEquals("/assets/sounds/campaign/game2.ogg", sources.get(1));
    }

    @Test
    public void decodeRejectsTooManyTracks() {
        StringBuilder input = new StringBuilder();
        for (int i = 0; i <= QuantumPlaylistCodec.MAX_TRACKS; i++) {
            input.append("/assets/sounds/campaign/game").append(i).append(".ogg\n");
        }

        assertTrue(QuantumPlaylistCodec.decode(input.toString()).isEmpty());
    }

    @Test
    public void decodeRejectsOversizedTransport() {
        String oversized = "x".repeat(QuantumPlaylistCodec.MAX_SERIALIZED_LENGTH + 1);
        assertTrue(QuantumPlaylistCodec.decode(oversized).isEmpty());
    }
}
