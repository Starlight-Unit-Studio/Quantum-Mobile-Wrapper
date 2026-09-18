package de.starlightunit.wrapper.navigation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NativeNavigationConfig {
    public static final int MAX_ITEMS = 12;

    public static final class Item {
        public final String label;
        public final String target;

        Item(String label, String target) {
            this.label = label;
            this.target = target;
        }
    }

    public final String title;
    public final List<Item> items;

    private NativeNavigationConfig(String title, List<Item> items) {
        this.title = title;
        this.items = Collections.unmodifiableList(items);
    }

    public static NativeNavigationConfig parse(String title, String itemsJson) {
        String safeTitle = title == null ? "" : title.trim();
        List<Item> items = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(itemsJson == null || itemsJson.trim().isEmpty() ? "[]" : itemsJson);
            for (int i = 0; i < array.length() && items.size() < MAX_ITEMS; i++) {
                JSONObject raw = array.optJSONObject(i);
                if (raw == null) {
                    continue;
                }
                String label = raw.optString("label", "").trim();
                String target = raw.optString("url", "").trim();
                if (label.isEmpty() || target.isEmpty()) {
                    continue;
                }
                if (label.length() > 40) {
                    label = label.substring(0, 40);
                }
                items.add(new Item(label, target));
            }
        } catch (Exception ignored) {
            items.clear();
        }
        return new NativeNavigationConfig(safeTitle, items);
    }

    public static String resolveTarget(String startUrl, String target) {
        if (startUrl == null || target == null || target.trim().isEmpty()) {
            return "";
        }
        try {
            URI base = new URI(startUrl);
            URI resolved = base.resolve(target.trim());
            return resolved.toString();
        } catch (URISyntaxException | IllegalArgumentException ignored) {
            return "";
        }
    }
}
