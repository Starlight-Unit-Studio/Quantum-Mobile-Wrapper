package de.starlightunit.wrapper.media;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Small transport codec for playlists crossing the WebView JavaScript bridge.
 *
 * JavaScriptInterface does not expose Java collections directly in a useful,
 * portable way. The web client therefore sends one source per line. This
 * class owns only transport parsing and basic size limits; trust validation
 * remains in QuantumNativeMediaBridge.
 */
public final class QuantumPlaylistCodec {

    static final int MAX_TRACKS = 64;
    static final int MAX_SERIALIZED_LENGTH = 16 * 1024;

    private QuantumPlaylistCodec() {
    }

    public static List<String> decode(String serializedSources) {
        if (serializedSources == null) {
            return Collections.emptyList();
        }

        String input = serializedSources.trim();
        if (input.isEmpty() || input.length() > MAX_SERIALIZED_LENGTH) {
            return Collections.emptyList();
        }

        String[] lines = input.split("\\r?\\n");
        List<String> sources = new ArrayList<>();

        for (String line : lines) {
            String source = line == null ? "" : line.trim();
            if (source.isEmpty()) {
                continue;
            }

            if (sources.size() >= MAX_TRACKS) {
                return Collections.emptyList();
            }

            sources.add(source);
        }

        if (sources.isEmpty()) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(sources);
    }
}
