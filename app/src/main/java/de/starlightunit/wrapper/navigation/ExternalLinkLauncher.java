package de.starlightunit.wrapper.navigation;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public final class ExternalLinkLauncher {
    private ExternalLinkLauncher() {
    }

    public static boolean open(Context context, String url) {
        if (!LinkRoutingPolicy.isAllowedExternalScheme(url)) {
            return false;
        }
        try {
            Intent intent;
            String scheme = Uri.parse(url).getScheme();
            if ("intent".equalsIgnoreCase(scheme)) {
                intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                intent.setComponent(null);
                intent.setSelector(null);
            } else {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            }
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException | java.net.URISyntaxException ignored) {
            return false;
        }
    }
}
