package de.starlightunit.wrapper.media;

import java.net.URI;
import java.net.URISyntaxException;

public final class QuantumMediaMetadata {

    private static final String FALLBACK_TITLE = "Starlight Unit";

    private QuantumMediaMetadata() {
    }

    public static String titleFromSource(String source) {
        if (source == null || source.trim().isEmpty()) {
            return FALLBACK_TITLE;
        }

        try {
            URI uri = new URI(source.trim());
            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                return FALLBACK_TITLE;
            }

            int slash = path.lastIndexOf('/');
            String fileName = slash >= 0 ? path.substring(slash + 1) : path;
            if (fileName.isEmpty()) {
                return FALLBACK_TITLE;
            }

            int dot = fileName.lastIndexOf('.');
            String stem = dot > 0 ? fileName.substring(0, dot) : fileName;
            String title = stem.replace('_', ' ').replace('-', ' ').trim();
            title = title.replaceAll("\\s+", " ");
            return title.isEmpty() ? FALLBACK_TITLE : title;
        } catch (URISyntaxException ignored) {
            return FALLBACK_TITLE;
        }
    }
}
