package de.starlightunit.wrapper.navigation;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Bounded menu catalogue and ordered, presentation-only URL rules. */
public final class NativeNavigationMenus {
    public static final String DEFAULT_MENU = "default";
    public static final int MAX_MENUS = 4;
    public static final int MAX_RULES = 16;
    public static final int MAX_TOTAL_NODES = 24;
    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_-]{0,39}");
    private static final Pattern HOST = Pattern.compile(
            "[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)*");
    private static final Pattern AUTHORITY = Pattern.compile("[a-zA-Z0-9.-]+(?::[0-9]+)?");
    private static final Pattern PATH = Pattern.compile("/(?:[a-zA-Z0-9._~!$&'()*+,;=:@/-]|%[a-fA-F0-9]{2})*");
    private static final Set<String> SURFACES = names("top_bar", "sidebar", "bottom_tabs", "contextual_toolbar");

    public final boolean valid;
    private final Map<String, NativeNavigationConfig> menus;
    private final List<Rule> rules;
    private final String sidebarMenu;
    private final String tabsMenu;
    private final String trustedDomain;

    private NativeNavigationMenus(Map<String, NativeNavigationConfig> menus, List<Rule> rules,
                                  String sidebarMenu, String tabsMenu, String trustedDomain, boolean valid) {
        this.menus = Collections.unmodifiableMap(new LinkedHashMap<>(menus));
        this.rules = Collections.unmodifiableList(new ArrayList<>(rules));
        this.sidebarMenu = sidebarMenu;
        this.tabsMenu = tabsMenu;
        this.trustedDomain = trustedDomain;
        this.valid = valid;
    }

    /** Invalid extensions are discarded together; the validated legacy menu remains usable. */
    public static NativeNavigationMenus parse(String title, String itemsJson, String profileJson,
                                              String trustedDomain) {
        NativeNavigationConfig legacy = NativeNavigationConfig.parse(title, itemsJson);
        Map<String, NativeNavigationConfig> fallback = new LinkedHashMap<>();
        fallback.put(DEFAULT_MENU, legacy);
        String trusted = trustedDomain == null ? "" : trustedDomain.toLowerCase(Locale.ROOT);
        try {
            if (!legacy.valid) throw new IllegalArgumentException("Invalid default menu");
            String source = profileJson == null || profileJson.trim().isEmpty() ? "{}" : profileJson;
            NativeNavigationConfig.requireBoundedJson(source);
            JSONTokener tokens = new JSONTokener(source);
            JSONObject profile = new JSONObject(tokens);
            if (tokens.nextClean() != 0) throw new IllegalArgumentException("Trailing profile JSON");
            requireKeys(profile, names("menus", "sidebar_menu", "bottom_tabs_menu", "rules"));
            Map<String, NativeNavigationConfig> menus = new LinkedHashMap<>(fallback);
            int totalNodes = countNodes(legacy.tree);
            JSONArray definitions = optionalArray(profile, "menus");
            if (definitions.length() >= MAX_MENUS) throw new IllegalArgumentException("Too many menus");
            for (int i = 0; i < definitions.length(); i++) {
                JSONObject menu = definitions.optJSONObject(i);
                if (menu == null) throw new IllegalArgumentException("Menu must be an object");
                requireKeys(menu, names("id", "label", "items"));
                String id = exactText(menu, "id");
                if (!ID.matcher(id).matches() || menus.containsKey(id)) {
                    throw new IllegalArgumentException("Invalid or duplicate menu id");
                }
                String label = exactText(menu, "label").trim();
                if (label.isEmpty() || label.getBytes(StandardCharsets.UTF_8).length > 80) {
                    throw new IllegalArgumentException("Invalid menu label");
                }
                JSONArray items = menu.optJSONArray("items");
                if (items == null) throw new IllegalArgumentException("Menu items must be an array");
                NativeNavigationConfig config = NativeNavigationConfig.parse(title, items.toString());
                if (!config.valid || (totalNodes += countNodes(config.tree)) > MAX_TOTAL_NODES) {
                    throw new IllegalArgumentException("Invalid or oversized menu catalogue");
                }
                menus.put(id, config);
            }
            String sidebar = menuReference(profile, "sidebar_menu", DEFAULT_MENU, menus);
            String tabs = menuReference(profile, "bottom_tabs_menu", DEFAULT_MENU, menus);
            JSONArray rawRules = optionalArray(profile, "rules");
            if (rawRules.length() > MAX_RULES) throw new IllegalArgumentException("Too many navigation rules");
            List<Rule> rules = new ArrayList<>();
            for (int i = 0; i < rawRules.length(); i++) {
                JSONObject raw = rawRules.optJSONObject(i);
                if (raw == null) throw new IllegalArgumentException("Rule must be an object");
                requireKeys(raw, names("match", "path", "host", "sidebar_menu", "bottom_tabs_menu", "visibility"));
                String match = exactText(raw, "match");
                if (!"exact".equals(match) && !"prefix".equals(match)) {
                    throw new IllegalArgumentException("Invalid path match mode");
                }
                String path = exactText(raw, "path");
                if (path.length() > 2048 || path.startsWith("//") || !PATH.matcher(path).matches()) {
                    throw new IllegalArgumentException("Invalid raw path");
                }
                String originalHost = raw.has("host") ? exactText(raw, "host") : "";
                String host = originalHost.toLowerCase(Locale.ROOT);
                if (raw.has("host") && (!isAscii(originalHost) || !validHost(host) || !trustedHost(host, trusted))) {
                    throw new IllegalArgumentException("Invalid navigation rule host");
                }
                String selectedSidebar = menuReference(raw, "sidebar_menu", null, menus);
                String selectedTabs = menuReference(raw, "bottom_tabs_menu", null, menus);
                Map<String, Boolean> visibility = new LinkedHashMap<>();
                if (raw.has("visibility")) {
                    JSONObject values = raw.optJSONObject("visibility");
                    if (values == null || values.length() == 0) {
                        throw new IllegalArgumentException("Visibility must be a nonempty object");
                    }
                    requireKeys(values, SURFACES);
                    Iterator<String> keys = values.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        Object value = values.opt(key);
                        if (!(value instanceof Boolean)) throw new IllegalArgumentException("Visibility must be boolean");
                        visibility.put(key, (Boolean) value);
                    }
                }
                if (selectedSidebar == null && selectedTabs == null && visibility.isEmpty()) {
                    throw new IllegalArgumentException("Rule requires an override");
                }
                rules.add(new Rule(match, path, host, selectedSidebar, selectedTabs, visibility));
            }
            return new NativeNavigationMenus(menus, rules, sidebar, tabs, trusted, true);
        } catch (Exception ignored) {
            return new NativeNavigationMenus(fallback, Collections.emptyList(), DEFAULT_MENU,
                    DEFAULT_MENU, trusted, false);
        }
    }

    public NativeNavigationConfig menu(String id) {
        return menus.get(id);
    }

    public Set<String> menuIds() {
        return menus.keySet();
    }

    public Selection select(String currentUrl, boolean top, boolean sidebar, boolean tabs, boolean contextual) {
        Rule matched = null;
        int index = -1;
        URI route = trustedRoute(currentUrl);
        if (route != null) {
            String path = route.getRawPath();
            if (path == null || path.isEmpty()) path = "/";
            String host = route.getHost().toLowerCase(Locale.ROOT);
            for (int i = 0; i < rules.size(); i++) {
                Rule rule = rules.get(i);
                if (rule.matches(host, path)) {
                    matched = rule;
                    index = i;
                    break;
                }
            }
        }
        String selectedSidebar = matched != null && matched.sidebarMenu != null ? matched.sidebarMenu : sidebarMenu;
        String selectedTabs = matched != null && matched.tabsMenu != null ? matched.tabsMenu : tabsMenu;
        return new Selection(selectedSidebar, selectedTabs, menus.get(selectedSidebar), menus.get(selectedTabs),
                visible(matched, "top_bar", top), visible(matched, "sidebar", sidebar),
                visible(matched, "bottom_tabs", tabs), visible(matched, "contextual_toolbar", contextual), index);
    }

    private URI trustedRoute(String url) {
        if (url == null || hasForbiddenRouteCharacters(url)) return null;
        try {
            URI route = new URI(url);
            String host = route.getHost();
            if (!"https".equalsIgnoreCase(route.getScheme()) || host == null
                    || route.getRawUserInfo() != null || route.getPort() > 65535
                    || route.getRawAuthority() == null || !AUTHORITY.matcher(route.getRawAuthority()).matches()
                    || !validHost(host.toLowerCase(Locale.ROOT))
                    || !trustedHost(host.toLowerCase(Locale.ROOT), trustedDomain)) return null;
            return route;
        } catch (Exception ignored) {
            return null;
        }
    }

    // Share this explicit set with the preview matcher. Java URI and WHATWG
    // URL otherwise differ for brackets in queries, backticks and whitespace.
    // Encoded characters remain encoded and do not take part in this check.
    private static boolean hasForbiddenRouteCharacters(String url) {
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c <= 0x20 || (c >= 0x7f && c <= 0x9f)
                    || c == 0xa0 || c == 0x1680 || (c >= 0x2000 && c <= 0x200a)
                    || c == 0x2028 || c == 0x2029 || c == 0x202f || c == 0x205f
                    || c == 0x3000 || c == 0xfeff || "\\<>\"{}|^[]`".indexOf(c) >= 0) return true;
        }
        return false;
    }

    private static boolean visible(Rule rule, String key, boolean fallback) {
        return rule != null && rule.visibility.containsKey(key) ? rule.visibility.get(key) : fallback;
    }

    private static boolean validHost(String host) {
        return host.length() <= 253 && HOST.matcher(host).matches();
    }

    private static boolean isAscii(String value) {
        for (int i = 0; i < value.length(); i++) if (value.charAt(i) > 127) return false;
        return true;
    }

    private static boolean trustedHost(String host, String trusted) {
        return validHost(trusted) && (host.equals(trusted) || host.endsWith("." + trusted));
    }

    private static int countNodes(List<NativeNavigationConfig.Item> items) {
        int count = items.size();
        for (NativeNavigationConfig.Item item : items) if (item.isGroup) count += countNodes(item.children);
        return count;
    }

    private static Set<String> names(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }

    private static void requireKeys(JSONObject object, Set<String> allowed) {
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) if (!allowed.contains(keys.next())) throw new IllegalArgumentException("Unknown navigation field");
    }

    private static JSONArray optionalArray(JSONObject object, String key) {
        if (!object.has(key)) return new JSONArray();
        JSONArray value = object.optJSONArray(key);
        if (value == null) throw new IllegalArgumentException("Expected navigation array");
        return value;
    }

    private static String exactText(JSONObject object, String key) {
        Object value = object.opt(key);
        if (!(value instanceof String)) throw new IllegalArgumentException("Expected navigation text");
        return (String) value;
    }

    private static String menuReference(JSONObject object, String key, String fallback,
                                        Map<String, NativeNavigationConfig> menus) {
        if (!object.has(key)) return fallback;
        String id = exactText(object, key);
        if (!menus.containsKey(id)) throw new IllegalArgumentException("Unknown menu reference");
        return id;
    }

    private static final class Rule {
        final String match;
        final String path;
        final String host;
        final String sidebarMenu;
        final String tabsMenu;
        final Map<String, Boolean> visibility;

        Rule(String match, String path, String host, String sidebarMenu, String tabsMenu,
             Map<String, Boolean> visibility) {
            this.match = match;
            this.path = path;
            this.host = host;
            this.sidebarMenu = sidebarMenu;
            this.tabsMenu = tabsMenu;
            this.visibility = Collections.unmodifiableMap(new LinkedHashMap<>(visibility));
        }

        boolean matches(String currentHost, String currentPath) {
            if (!host.isEmpty() && !host.equals(currentHost)) return false;
            if (path.equals(currentPath)) return true;
            return "prefix".equals(match) && currentPath.startsWith(path.endsWith("/") ? path : path + "/");
        }
    }

    public static final class Selection {
        public final String sidebarMenu;
        public final String tabsMenu;
        public final NativeNavigationConfig sidebar;
        public final NativeNavigationConfig tabs;
        public final boolean topEnabled;
        public final boolean sidebarEnabled;
        public final boolean bottomEnabled;
        public final boolean contextualEnabled;
        public final int matchedRuleIndex;

        private Selection(String sidebarMenu, String tabsMenu, NativeNavigationConfig sidebar,
                          NativeNavigationConfig tabs, boolean top, boolean sidebarVisible,
                          boolean bottom, boolean contextual, int matchedRuleIndex) {
            this.sidebarMenu = sidebarMenu;
            this.tabsMenu = tabsMenu;
            this.sidebar = sidebar;
            this.tabs = tabs;
            this.topEnabled = top;
            this.sidebarEnabled = sidebarVisible;
            this.bottomEnabled = bottom;
            this.contextualEnabled = contextual;
            this.matchedRuleIndex = matchedRuleIndex;
        }

        boolean sameLayout(Selection other) {
            return other != null && sidebarMenu.equals(other.sidebarMenu) && tabsMenu.equals(other.tabsMenu)
                    && topEnabled == other.topEnabled && sidebarEnabled == other.sidebarEnabled
                    && bottomEnabled == other.bottomEnabled && contextualEnabled == other.contextualEnabled;
        }
    }

    /** Per-controller state, including separate expansion state for each menu ID. */
    public static final class State {
        private final NativeNavigationMenus menus;
        private final boolean top;
        private final boolean sidebar;
        private final boolean bottom;
        private final boolean contextual;
        private final Map<String, NativeNavigationGroups> groups = new LinkedHashMap<>();
        private Selection selection;

        public State(NativeNavigationMenus menus, String startUrl, boolean top, boolean sidebar,
                     boolean bottom, boolean contextual) {
            this.menus = menus;
            this.top = top;
            this.sidebar = sidebar;
            this.bottom = bottom;
            this.contextual = contextual;
            for (String id : menus.menuIds()) groups.put(id, new NativeNavigationGroups(menus.menu(id), startUrl));
            update(startUrl == null ? "" : startUrl);
        }

        public boolean update(String currentUrl) {
            if (currentUrl == null) return false;
            Selection next = menus.select(currentUrl, top, sidebar, bottom, contextual);
            boolean changed = !next.sameLayout(selection);
            selection = next;
            // Every menu observes navigation, even while hidden. Returning to a
            // previously visited destination still reveals its active ancestors.
            for (NativeNavigationGroups state : groups.values()) state.navigate(currentUrl);
            return changed;
        }

        public Selection selection() {
            return selection;
        }

        public NativeNavigationGroups sidebarGroups() {
            return groups.get(selection.sidebarMenu);
        }
    }
}
