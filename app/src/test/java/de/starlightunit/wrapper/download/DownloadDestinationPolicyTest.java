package de.starlightunit.wrapper.download;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class DownloadDestinationPolicyTest {
    @Test
    public void android10PlusUsesPublicDownloadsWhenEnabled() {
        assertTrue(DownloadDestinationPolicy.usePublicDownloads(true, 29, false));
        assertTrue(DownloadDestinationPolicy.usePublicDownloads(true, 36, false));
    }

    @Test
    public void disabledAlwaysUsesAppPrivateDestination() {
        assertFalse(DownloadDestinationPolicy.usePublicDownloads(false, 36, true));
        assertFalse(DownloadDestinationPolicy.usePublicDownloads(false, 28, true));
    }

    @Test
    public void android6Through9RequireLegacyPermission() {
        assertFalse(DownloadDestinationPolicy.usePublicDownloads(true, 23, false));
        assertTrue(DownloadDestinationPolicy.usePublicDownloads(true, 23, true));
        assertFalse(DownloadDestinationPolicy.usePublicDownloads(true, 28, false));
        assertTrue(DownloadDestinationPolicy.usePublicDownloads(true, 28, true));
    }

    @Test
    public void legacyPermissionPromptOnlyNeededOnApi23Through28() {
        assertTrue(DownloadDestinationPolicy.needsLegacyWritePermission(true, 23, false));
        assertTrue(DownloadDestinationPolicy.needsLegacyWritePermission(true, 28, false));
        assertFalse(DownloadDestinationPolicy.needsLegacyWritePermission(true, 29, false));
        assertFalse(DownloadDestinationPolicy.needsLegacyWritePermission(false, 28, false));
        assertFalse(DownloadDestinationPolicy.needsLegacyWritePermission(true, 28, true));
    }
}
