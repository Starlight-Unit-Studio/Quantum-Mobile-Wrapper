package de.starlightunit.wrapper.navigation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Per-controller expansion state for the bounded sidebar tree; no storage or network access. */
public final class NativeNavigationGroups {
    private final List<NativeNavigationConfig.Item> tree;
    private final String startUrl;
    private final Map<String, Boolean> expanded = new HashMap<>();
    private String currentUrl;

    public NativeNavigationGroups(NativeNavigationConfig config, String startUrl) {
        this.tree = config.tree;
        this.startUrl = startUrl;
        initialize(tree);
    }

    private void initialize(List<NativeNavigationConfig.Item> source) {
        for (NativeNavigationConfig.Item item : source) {
            if (item.isGroup) {
                expanded.put(item.id, !item.collapsed);
                initialize(item.children);
            }
        }
    }

    public boolean isExpanded(String id) {
        return Boolean.TRUE.equals(expanded.get(id));
    }

    public boolean setExpanded(String id, boolean value) {
        if (!expanded.containsKey(id)) return false;
        expanded.put(id, value);
        return true;
    }

    public void navigate(String url) {
        if (url == null || url.equals(currentUrl)) return;
        currentUrl = url;
        openActiveAncestors(tree, url);
    }

    private boolean openActiveAncestors(List<NativeNavigationConfig.Item> source, String url) {
        boolean active = false;
        for (NativeNavigationConfig.Item item : source) {
            if (item.isGroup) {
                boolean childActive = openActiveAncestors(item.children, url);
                if (childActive) expanded.put(item.id, true);
                active |= childActive;
            } else {
                active |= sameLocation(NativeNavigationConfig.resolveTarget(startUrl, item.target), url);
            }
        }
        return active;
    }

    public boolean isActive(NativeNavigationConfig.Item item, String url) {
        if (url == null || url.isEmpty()) return false;
        if (!item.isGroup) {
            return sameLocation(NativeNavigationConfig.resolveTarget(startUrl, item.target), url);
        }
        for (NativeNavigationConfig.Item child : item.children) {
            if (isActive(child, url)) return true;
        }
        return false;
    }

    public static boolean hasIcons(List<NativeNavigationConfig.Item> source) {
        for (NativeNavigationConfig.Item item : source) {
            if (NativeNavigationIconPolicy.hasIcon(item.icon, item.activeIcon)
                    || (item.isGroup && hasIcons(item.children))) return true;
        }
        return false;
    }

    public static boolean sameLocation(String left, String right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) return false;
        int leftHash = left.indexOf('#');
        int rightHash = right.indexOf('#');
        return (leftHash >= 0 ? left.substring(0, leftHash) : left)
                .equals(rightHash >= 0 ? right.substring(0, rightHash) : right);
    }
}
