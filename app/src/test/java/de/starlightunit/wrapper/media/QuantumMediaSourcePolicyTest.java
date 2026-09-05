package de.starlightunit.wrapper.media;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class QuantumMediaSourcePolicyTest {
    private QuantumMediaSourcePolicy policy;

    @Before
    public void setUp() {
        policy = new QuantumMediaSourcePolicy(
                "starlight-unit.de",
                "/assets/sounds/campaign/"
        );
    }

    @Test
    public void acceptsExistingAndFutureCampaignTracks() {
        assertEquals(
                "https://game.starlight-unit.de/assets/sounds/campaign/game1.ogg",
                policy.validateAndNormalize(
                        "https://game.starlight-unit.de/assets/sounds/campaign/game1.ogg"
                )
        );
        assertEquals(
                "https://game.starlight-unit.de/assets/sounds/campaign/game12.ogg",
                policy.validateAndNormalize(
                        "https://game.starlight-unit.de/assets/sounds/campaign/game12.ogg"
                )
        );
        assertEquals(
                "https://game.starlight-unit.de/assets/sounds/campaign/future-track.ogg",
                policy.validateAndNormalize(
                        "https://game.starlight-unit.de/assets/sounds/campaign/future-track.ogg"
                )
        );
    }

    @Test
    public void acceptsNestedCampaignMedia() {
        assertEquals(
                "https://game.starlight-unit.de/assets/sounds/campaign/episode-3/scene.ogg",
                policy.validateAndNormalize(
                        "https://game.starlight-unit.de/assets/sounds/campaign/episode-3/scene.ogg"
                )
        );
    }

    @Test
    public void rejectsOtherSoundFoldersAndPrefixLookalikes() {
        assertNull(policy.validateAndNormalize(
                "https://game.starlight-unit.de/assets/sounds/ui/click.ogg"
        ));
        assertNull(policy.validateAndNormalize(
                "https://game.starlight-unit.de/assets/sounds/campaign-evil/game1.ogg"
        ));
    }

    @Test
    public void rejectsUntrustedOriginsAndCleartext() {
        assertNull(policy.validateAndNormalize(
                "https://starlight-unit.de.evil.example/assets/sounds/campaign/game1.ogg"
        ));
        assertNull(policy.validateAndNormalize(
                "http://game.starlight-unit.de/assets/sounds/campaign/game1.ogg"
        ));
    }

    @Test
    public void rejectsTraversalAndEncodedTraversal() {
        assertNull(policy.validateAndNormalize(
                "https://game.starlight-unit.de/assets/sounds/campaign/../ui/click.ogg"
        ));
        assertNull(policy.validateAndNormalize(
                "https://game.starlight-unit.de/assets/sounds/campaign/%2e%2e/ui/click.ogg"
        ));
        assertNull(policy.validateAndNormalize(
                "https://game.starlight-unit.de/assets/sounds/campaign/%2Foutside.ogg"
        ));
    }
}
