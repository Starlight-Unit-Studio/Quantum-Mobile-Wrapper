package de.starlightunit.wrapper.download;

public final class DownloadDestinationPolicy {
    private DownloadDestinationPolicy() {
    }

    public static boolean usePublicDownloads(
            boolean publicDownloadsEnabled,
            int sdkInt,
            boolean legacyWritePermissionGranted
    ) {
        if (!publicDownloadsEnabled) {
            return false;
        }
        if (sdkInt >= 29) {
            return true;
        }
        return legacyWritePermissionGranted;
    }

    public static boolean needsLegacyWritePermission(
            boolean publicDownloadsEnabled,
            int sdkInt,
            boolean legacyWritePermissionGranted
    ) {
        return publicDownloadsEnabled
                && sdkInt >= 23
                && sdkInt <= 28
                && !legacyWritePermissionGranted;
    }
}
