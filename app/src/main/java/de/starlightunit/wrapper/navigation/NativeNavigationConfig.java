package de.starlightunit.wrapper.navigation;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class NativeNavigationConfig {
    public static final int MAX_ITEMS = 12;
    public static final int MAX_GROUPS = 12;
    public static final int MAX_NODES = 24;
    public static final int MAX_GROUP_DEPTH = 3;
    private static final int MAX_JSON_LENGTH = 65_535;
    private static final Pattern GROUP_ID = Pattern.compile("[a-z][a-z0-9_-]{0,39}");
    private static final Pattern JSON_ATOM = Pattern.compile(
            "true|false|null|-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?");

    public static final class Item {
        public final String label;
        public final String target;
        public final String icon;
        public final String activeIcon;
        public final boolean isGroup;
        public final String id;
        public final boolean collapsed;
        public final List<Item> children;

        Item(String label, String target) {
            this(label, target, "", "");
        }

        Item(String label, String target, String icon, String activeIcon) {
            this(label, target, icon, activeIcon, false, "", false, Collections.emptyList());
        }

        private Item(String label, String target, String icon, String activeIcon,
                     boolean isGroup, String id, boolean collapsed, List<Item> children) {
            this.label = label;
            this.target = target;
            this.icon = NativeNavigationIconPolicy.normalize(icon);
            this.activeIcon = NativeNavigationIconPolicy.normalize(activeIcon);
            this.isGroup = isGroup;
            this.id = id;
            this.collapsed = collapsed;
            this.children = Collections.unmodifiableList(new ArrayList<>(children));
        }
    }

    public final String title;
    /** Depth-first link destinations, retained for Bottom Tabs and legacy callers. */
    public final List<Item> items;
    public final List<Item> tree;
    public final boolean valid;

    private NativeNavigationConfig(String title, List<Item> tree, boolean valid) {
        this.title = title;
        this.tree = Collections.unmodifiableList(new ArrayList<>(tree));
        List<Item> links = new ArrayList<>();
        collectLinks(tree, links);
        this.items = Collections.unmodifiableList(links);
        this.valid = valid;
    }

    public static NativeNavigationConfig parse(String title, String itemsJson) {
        String safeTitle = title == null ? "" : title.trim();
        List<Item> tree = Collections.emptyList();
        boolean valid = false;
        try {
            String source = itemsJson == null || itemsJson.trim().isEmpty() ? "[]" : itemsJson;
            requireBoundedJson(source);
            JSONTokener tokens = new JSONTokener(source);
            JSONArray array = new JSONArray(tokens);
            if (tokens.nextClean() != 0) throw new IllegalArgumentException("Trailing menu JSON");
            tree = parseItems(array, 0, new ParseLimits());
            valid = true;
        } catch (Exception ignored) {
            // Reject the complete menu. Do not retain a partial tree or tab list.
            tree = Collections.emptyList();
        }
        return new NativeNavigationConfig(safeTitle, tree, valid);
    }

    private static final class ParseLimits {
        int nodes;
        int links;
        int groups;
        final Set<String> ids = new HashSet<>();
    }

    private static List<Item> parseItems(JSONArray source, int groupAncestors, ParseLimits limits) {
        if (source.length() > MAX_NODES) throw new IllegalArgumentException("Too many menu nodes");
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < source.length(); i++) {
            JSONObject raw = source.optJSONObject(i);
            if (raw == null || ++limits.nodes > MAX_NODES) throw new IllegalArgumentException("Invalid menu node");
            String label = requiredText(raw, "label", 80);
            if (label.length() > 40) label = label.substring(0, 40);
            String icon = raw.opt("icon") instanceof String ? raw.optString("icon", "") : "";
            String activeIcon = raw.opt("active_icon") instanceof String ? raw.optString("active_icon", "") : "";
            Object type = raw.has("type") ? raw.opt("type") : "link";
            if ("group".equals(type)) {
                if (raw.has("url") || groupAncestors >= MAX_GROUP_DEPTH || ++limits.groups > MAX_GROUPS) {
                    throw new IllegalArgumentException("Invalid menu group");
                }
                Object rawId = raw.opt("id");
                if (!(rawId instanceof String)) throw new IllegalArgumentException("Missing group id");
                String id = (String) rawId;
                if (!GROUP_ID.matcher(id).matches() || !limits.ids.add(id)) {
                    throw new IllegalArgumentException("Invalid or duplicate group id");
                }
                if (raw.has("collapsed") && !(raw.opt("collapsed") instanceof Boolean)) {
                    throw new IllegalArgumentException("Collapsed must be boolean");
                }
                JSONArray children = raw.optJSONArray("children");
                if (children == null) throw new IllegalArgumentException("Group children must be an array");
                items.add(new Item(label, "", icon, activeIcon, true, id,
                        raw.optBoolean("collapsed", false), parseItems(children, groupAncestors + 1, limits)));
            } else if ("link".equals(type)) {
                if (raw.has("children") || raw.has("id") || raw.has("collapsed") || ++limits.links > MAX_ITEMS) {
                    throw new IllegalArgumentException("Invalid menu link");
                }
                items.add(new Item(label, requiredText(raw, "url", 2048), icon, activeIcon));
            } else {
                throw new IllegalArgumentException("Unknown menu node type");
            }
        }
        return items;
    }

    private static String requiredText(JSONObject raw, String field, int maxBytes) {
        Object value = raw.opt(field);
        if (!(value instanceof String)) throw new IllegalArgumentException("Missing menu text");
        String text = ((String) value).trim();
        if (text.isEmpty() || text.getBytes(StandardCharsets.UTF_8).length > maxBytes) {
            throw new IllegalArgumentException("Invalid menu text length");
        }
        return text;
    }

    private static void collectLinks(List<Item> source, List<Item> target) {
        for (Item item : source) {
            if (item.isGroup) collectLinks(item.children, target);
            else target.add(item);
        }
    }

    private static void requireBoundedJson(String source) {
        if (source.length() > MAX_JSON_LENGTH) throw new IllegalArgumentException("Menu JSON is too long");
        int depth = 0;
        boolean quoted = false;
        boolean escaped = false;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (quoted) {
                if (escaped) {
                    if (c == 'u') {
                        for (int hex = 0; hex < 4; hex++) {
                            if (++i >= source.length() || Character.digit(source.charAt(i), 16) < 0) {
                                throw new IllegalArgumentException("Invalid JSON escape");
                            }
                        }
                    } else if ("\"\\/bfnrt".indexOf(c) < 0) {
                        throw new IllegalArgumentException("Invalid JSON escape");
                    }
                    escaped = false;
                }
                else if (c == '\\') escaped = true;
                else if (c == '"') quoted = false;
                else if (c < 32) throw new IllegalArgumentException("Invalid JSON string");
            } else if (c == '"') {
                quoted = true;
            } else if (c == '[' || c == '{') {
                // The supported tree needs at most eight structural levels.
                if (++depth > 16) throw new IllegalArgumentException("Menu JSON is too deeply nested");
            } else if (c == ']' || c == '}') {
                if (--depth < 0) throw new IllegalArgumentException("Invalid menu JSON");
            } else if (" \t\r\n,:".indexOf(c) < 0) {
                // Android's JSONTokener also accepts comments, bare words and single-quoted
                // strings. Reject those before its parser so their quotes cannot hide depth.
                int begin = i;
                while (i + 1 < source.length() && "{}[],: \t\r\n".indexOf(source.charAt(i + 1)) < 0) i++;
                if (!JSON_ATOM.matcher(source.substring(begin, i + 1)).matches()) {
                    throw new IllegalArgumentException("Invalid JSON token");
                }
            }
        }
        if (quoted || depth != 0) throw new IllegalArgumentException("Incomplete menu JSON");
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
