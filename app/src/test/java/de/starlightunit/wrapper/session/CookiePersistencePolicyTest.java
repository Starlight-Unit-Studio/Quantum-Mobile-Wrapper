package de.starlightunit.wrapper.session;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CookiePersistencePolicyTest {

    @Test
    public void persistentIsDefaultAndLegacyDefaultRemainsPersistent() {
        assertEquals("persistent", CookiePersistencePolicy.normalize(null));
        assertEquals("persistent", CookiePersistencePolicy.normalize(""));
        assertEquals("persistent", CookiePersistencePolicy.normalize("default"));
        assertTrue(CookiePersistencePolicy.usesEncryptedPersistence("persistent"));
        assertTrue(CookiePersistencePolicy.usesEncryptedPersistence("default"));
    }

    @Test
    public void serverAndSessionModesRemainDistinct() {
        assertEquals("server", CookiePersistencePolicy.normalize("server"));
        assertEquals("session", CookiePersistencePolicy.normalize("session"));
        assertFalse(CookiePersistencePolicy.usesEncryptedPersistence("server"));
        assertFalse(CookiePersistencePolicy.usesEncryptedPersistence("session"));
        assertTrue(CookiePersistencePolicy.startsFreshSession("session"));
        assertFalse(CookiePersistencePolicy.startsFreshSession("server"));
    }
}
