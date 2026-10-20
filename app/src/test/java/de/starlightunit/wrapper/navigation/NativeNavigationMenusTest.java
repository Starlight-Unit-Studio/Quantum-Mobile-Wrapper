package de.starlightunit.wrapper.navigation;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.*;

public class NativeNavigationMenusTest {
    private static final String START = "https://app.example.test/";
    private static final String LEGACY = "[{\"label\":\"Legacy\",\"url\":\"/\"}]";

    @Test public void legacyProfilesKeepSharedMenuAndVisibilityDefaults() {
        NativeNavigationMenus config = parse("{}");
        assertTrue(config.valid);
        NativeNavigationMenus.Selection selection = config.select(START, true, false, true, false);
        assertEquals("default", selection.sidebarMenu);
        assertEquals("default", selection.tabsMenu);
        assertSame(selection.sidebar, selection.tabs);
        assertEquals("Legacy", selection.sidebar.items.get(0).label);
        assertTrue(selection.topEnabled);
        assertFalse(selection.sidebarEnabled);
        assertTrue(selection.bottomEnabled);
        assertFalse(selection.contextualEnabled);
        assertEquals(-1, selection.matchedRuleIndex);
    }

    @Test public void selectsIndependentMenusAndKeepsDepthFirstTabsImmutable() throws Exception {
        JSONObject definition = menu("shop", group("account", true,
                link("First", "/first"), group("nested", false, link("Second", "/second"))),
                link("Third", "/third"));
        NativeNavigationMenus config = parse(new JSONObject().put("menus", new JSONArray().put(definition))
                .put("bottom_tabs_menu", "shop"));
        assertTrue(config.valid);
        NativeNavigationMenus.Selection selection = selected(config, START);
        assertEquals("default", selection.sidebarMenu);
        assertEquals("shop", selection.tabsMenu);
        assertEquals("Legacy", selection.sidebar.items.get(0).label);
        assertEquals(2, selection.tabs.tree.size());
        assertEquals(3, selection.tabs.items.size());
        assertEquals("First", selection.tabs.items.get(0).label);
        assertEquals("Second", selection.tabs.items.get(1).label);
        assertEquals("Third", selection.tabs.items.get(2).label);
        try {
            config.menuIds().remove("shop");
            fail("Catalogue must be immutable");
        } catch (UnsupportedOperationException expected) {
            // Runtime state cannot change configuration.
        }
    }

    @Test public void firstMatchingRuleWinsWholeAndMissingFieldsUseDefaults() throws Exception {
        JSONObject first = rule("prefix", "/shop").put("sidebar_menu", "shop");
        JSONObject second = rule("exact", "/shop/item").put("bottom_tabs_menu", "shop")
                .put("visibility", new JSONObject().put("top_bar", false));
        NativeNavigationMenus config = parse(profile(menu("shop", link("Shop", "/shop")))
                .put("rules", new JSONArray().put(first).put(second)));
        assertTrue(config.valid);
        NativeNavigationMenus.Selection selected = config.select(START + "shop/item?cart=1#main", true, true, true, true);
        assertEquals(0, selected.matchedRuleIndex);
        assertEquals("shop", selected.sidebarMenu);
        assertEquals("default", selected.tabsMenu);
        assertTrue(selected.topEnabled);
        assertTrue(selected.contextualEnabled);
    }

    @Test public void visibilityCanEnableDefaultOffAndResetOnAnotherPage() throws Exception {
        NativeNavigationMenus config = parse(new JSONObject().put("rules", new JSONArray().put(
                rule("exact", "/checkout").put("visibility", new JSONObject()
                        .put("top_bar", false).put("sidebar", true)
                        .put("bottom_tabs", true).put("contextual_toolbar", false)))));
        NativeNavigationMenus.Selection selected = config.select(START + "checkout", true, false, false, true);
        assertFalse(selected.topEnabled);
        assertTrue(selected.sidebarEnabled);
        assertTrue(selected.bottomEnabled);
        assertFalse(selected.contextualEnabled);
        NativeNavigationMenus.Selection defaults = config.select(START + "elsewhere", true, false, false, true);
        assertTrue(defaults.topEnabled);
        assertFalse(defaults.sidebarEnabled);
        assertFalse(defaults.bottomEnabled);
        assertTrue(defaults.contextualEnabled);
    }

    @Test public void prefixMatchingRespectsSegmentBoundariesAndTrailingSlash() throws Exception {
        NativeNavigationMenus config = withRule(rule("prefix", "/shop"));
        for (String path : new String[]{"/shop", "/shop/", "/shop/a", "/shop?x=1#top"}) matches(config, path, true);
        for (String path : new String[]{"/shopping", "/Shop", "/shop%2Fitem", "/a/../shop"}) matches(config, path, false);
        NativeNavigationMenus trailing = withRule(rule("prefix", "/shop/"));
        matches(trailing, "/shop", false);
        matches(trailing, "/shop/", true);
        matches(trailing, "/shop/a", true);
        NativeNavigationMenus all = withRule(rule("prefix", "/"));
        matches(all, "/anything", true);
        assertEquals(0, selected(all, "https://app.example.test").matchedRuleIndex);
    }

    @Test public void exactMatchingPreservesRawPathAndIgnoresQueryAndFragment() throws Exception {
        NativeNavigationMenus encoded = withRule(rule("exact", "/a%2Fb/%E2%82%AC"));
        matches(encoded, "/a%2Fb/%E2%82%AC?q=1#here", true);
        matches(encoded, "/a%2fb/%E2%82%AC", false);
        matches(encoded, "/a/b/%E2%82%AC", false);
        NativeNavigationMenus dots = withRule(rule("exact", "/a/../shop"));
        matches(dots, "/a/../shop", true);
        matches(dots, "/shop", false);
        matches(withRule(rule("exact", "/shop")), "/shop/", false);
    }

    @Test public void hostsMatchExactlyAndOnlyTrustedValidHttpsRoutesApply() throws Exception {
        NativeNavigationMenus config = withRule(rule("prefix", "/").put("host", "APP.EXAMPLE.TEST"));
        assertTrue(config.valid);
        assertEquals(0, selected(config, "HTTPS://APP.EXAMPLE.TEST:8443/shop").matchedRuleIndex);
        assertEquals(-1, selected(config, "https://child.app.example.test/shop").matchedRuleIndex);
        NativeNavigationMenus anyHost = withRule(rule("prefix", "/"));
        assertEquals(0, selected(anyHost, "https://child.example.test/shop").matchedRuleIndex);
        for (String url : new String[]{"http://app.example.test/", "https://example.test.evil/",
                "https://user@app.example.test/", "https://app.example.test:65536/",
                "https://app.example.test:/", "https://app.example.test/%zz",
                "https://app.example.test\\evil/", "//app.example.test/", "not a URL", "", null}) {
            assertEquals(url, -1, selected(anyHost, url).matchedRuleIndex);
        }
    }

    @Test public void currentUrlCharacterPolicyMatchesPreviewWithoutDecodingEscapes() throws Exception {
        NativeNavigationMenus config = withRule(rule("prefix", "/"));
        int[] forbidden = {0, 9, 10, 13, 31, 32, 0x7f, 0x80, 0x85, 0x9f,
                0xa0, 0x1680, 0x2000, 0x2001, 0x2002, 0x2003, 0x2004, 0x2005,
                0x2006, 0x2007, 0x2008, 0x2009, 0x200a, 0x2028, 0x2029, 0x202f,
                0x205f, 0x3000, 0xfeff, '[', ']', '`', '\\', '<', '>', '"', '{', '}', '|', '^'};
        for (int code : forbidden) {
            String character = String.valueOf((char) code);
            for (String prefix : new String[]{"/shop/", "/shop?q=", "/shop#"}) {
                assertEquals("Raw character " + Integer.toHexString(code) + " in " + prefix,
                        -1, selected(config, "https://app.example.test" + prefix + character).matchedRuleIndex);
            }
        }
        for (String path : new String[]{"/shop?q=%5Bitem%5D", "/shop?q=%60", "/shop?q=%C2%85",
                "/shop?q=%C2%A0", "/shop#%EF%BB%BF", "/shop?q=€", "/shop#Ω", "/shop/😀",
                "/shop#one%23two"}) {
            matches(config, path, true);
        }
        matches(config, "/shop#one#two", false);
    }

    @Test public void rejectsInvalidIdsReferencesAndDefinitionFieldsAtomically() throws Exception {
        for (String id : new String[]{"", "default", "Upper", "a\n", "1x", " x", "x ",
                "abcdefghijklmnopqrstuvwxyzabcdefghijklmno"}) {
            rejected(profile(menu(id)));
        }
        rejected(profile(menu("shop"), menu("shop")));
        rejected(profile(menu("shop").put("unknown", true)));
        rejected(profile(menu("shop").put("label", 12)));
        rejected(profile(menu("shop").put("label", " ")));
        rejected(profile(menu("shop").put("items", new JSONObject())));
        rejected(profile(menu("shop").put("items", JSONObject.NULL)));
        JSONObject missing = menu("shop");
        missing.remove("items");
        rejected(profile(missing));
        for (Object ref : new Object[]{"missing", "default ", 0, JSONObject.NULL, new JSONArray()}) {
            rejected(new JSONObject().put("sidebar_menu", ref));
            rejected(new JSONObject().put("bottom_tabs_menu", ref));
            rejected(new JSONObject().put("rules", new JSONArray().put(rule("exact", "/").put("sidebar_menu", ref))));
        }
    }

    @Test public void enforcesFourMenusTwentyFourTotalNodesAndPerMenuLimits() throws Exception {
        assertTrue(parse(profile(menu("one"), menu("two"), menu("three"))).valid);
        rejected(profile(menu("one"), menu("two"), menu("three"), menu("four")));
        JSONArray twelve = new JSONArray();
        for (int i = 0; i < 12; i++) twelve.put(link("Link" + i, "/" + i));
        JSONObject extras = profile(menu("shop").put("items", twelve));
        NativeNavigationMenus full = NativeNavigationMenus.parse("Menu", twelve.toString(), extras.toString(), "example.test");
        assertTrue(full.valid);
        JSONArray thirteen = new JSONArray(twelve.toString()).put(link("Thirteenth", "/13"));
        assertFalse(NativeNavigationMenus.parse("Menu", twelve.toString(),
                profile(menu("shop").put("items", thirteen)).toString(), "example.test").valid);
        JSONObject tooMany = profile(menu("shop").put("items", twelve), menu("other", group("empty", false)));
        assertFalse(NativeNavigationMenus.parse("Menu", twelve.toString(), tooMany.toString(), "example.test").valid);
        rejected(profile(menu("shop", group("one", false, group("two", false,
                group("three", false, group("four", false)))))));
    }

    @Test public void rejectsWrongContainersUnknownRuleFieldsAndNonBooleanVisibility() throws Exception {
        for (Object value : new Object[]{JSONObject.NULL, new JSONObject(), 1, "[]"}) {
            rejected(new JSONObject().put("menus", value));
            rejected(new JSONObject().put("rules", value));
        }
        rejected(new JSONObject().put("menus", new JSONArray().put(new JSONArray())));
        rejected(new JSONObject().put("rules", new JSONArray().put(new JSONArray())));
        rejectedRule(rule("prefix", "/").put("typo", true));
        rejectedRule(rule("unknown", "/"));
        rejectedRule(rule("prefix", "/").put("match", JSONObject.NULL));
        rejectedRule(rule("prefix", "/").put("visibility", new JSONObject()));
        rejectedRule(rule("prefix", "/").put("visibility", new JSONArray()));
        rejectedRule(rule("prefix", "/").put("visibility", JSONObject.NULL));
        rejectedRule(rule("prefix", "/").put("visibility", new JSONObject().put("misspelled", true)));
        for (Object value : new Object[]{"false", 0, JSONObject.NULL, new JSONArray()}) {
            rejectedRule(rule("prefix", "/").put("visibility", new JSONObject().put("sidebar", value)));
        }
        rejected(new JSONObject().put("rules", new JSONArray().put(rule("prefix", "/"))));
    }

    @Test public void rejectsMalformedRulePathsHostsAndExcessRules() throws Exception {
        for (String path : new String[]{"", "shop", "//shop", "/shop?x", "/shop#x", "/shop space",
                "/shop\\x", "/€", "/%", "/%xy", "/shop\n", "/[test]", "/x\u0000"}) {
            rejectedRule(rule("prefix", path));
        }
        StringBuilder longPath = new StringBuilder("/");
        while (longPath.length() <= 2048) longPath.append('a');
        rejectedRule(rule("prefix", longPath.toString()));
        for (String host : new String[]{"", " example.test", "example.test ", "example.test\n", "*.example.test",
                "https://example.test", "example.test:443", "example.test.", "-bad.example.test",
                "bad-.example.test", "bad..example.test", "evil.test", "example.test.evil", "ümlaut.example.test", "K.example.test"}) {
            rejectedRule(rule("prefix", "/").put("host", host));
        }
        JSONArray rules = new JSONArray();
        for (int i = 0; i < 16; i++) rules.put(rule("exact", "/" + i).put("sidebar_menu", "default"));
        assertTrue(parse(new JSONObject().put("rules", rules)).valid);
        rejected(new JSONObject().put("rules", rules.put(rule("prefix", "/").put("sidebar_menu", "default"))));
    }

    @Test public void boundsRawJsonAndFallsBackToLegacyWithoutPartialRules() {
        for (String source : new String[]{"[]", "null", "{} {}", "{\"menus\":{\"0\":{}}}",
                "{\"rules\":[{\"match\":\"prefix\",\"path\":\"/\",\"sidebar_menu\":\"default\",\"visibility\":[]}]}",
                "{'menus':[]}", "{menus:[]}", "/* comment */{}"}) {
            rejected(parse(source));
        }
        StringBuilder deep = new StringBuilder();
        for (int i = 0; i < 100; i++) deep.append('[');
        for (int i = 0; i < 100; i++) deep.append(']');
        rejected(parse(deep.toString()));
        StringBuilder large = new StringBuilder("{}");
        while (large.length() <= 65535) large.append(' ');
        rejected(parse(large.toString()));
        NativeNavigationMenus badLegacy = NativeNavigationMenus.parse("Title", "{broken", "{}", "example.test");
        assertFalse(badLegacy.valid);
        assertTrue(selected(badLegacy, START).sidebar.items.isEmpty());
    }

    @Test public void emptySelectedMenuRetainsIndependentHomeFallbackInformation() throws Exception {
        NativeNavigationMenus config = parse(profile(menu("empty"))
                .put("sidebar_menu", "empty").put("bottom_tabs_menu", "default"));
        NativeNavigationMenus.Selection selection = selected(config, START);
        assertTrue(config.valid);
        assertTrue(selection.sidebar.tree.isEmpty());
        assertTrue(selection.sidebar.items.isEmpty());
        assertEquals(1, selection.tabs.items.size());
    }

    @Test public void repeatedGroupIdsAreScopedPerMenuAndStateSurvivesSwitches() throws Exception {
        String legacy = new JSONArray().put(group("account", true, link("Home", "/home"))).toString();
        JSONObject profile = profile(menu("shop", group("account", true, link("Shop", "/shop/item"))))
                .put("rules", new JSONArray().put(rule("prefix", "/shop").put("sidebar_menu", "shop")));
        NativeNavigationMenus config = NativeNavigationMenus.parse("Title", legacy, profile.toString(), "example.test");
        assertTrue(config.valid);
        assertEquals(new HashSet<>(Arrays.asList("default", "shop")), config.menuIds());
        NativeNavigationMenus.State state = new NativeNavigationMenus.State(config, START, true, true, true, true);
        NativeNavigationGroups defaultState = state.sidebarGroups();
        assertFalse(defaultState.isExpanded("account"));
        defaultState.setExpanded("account", true);
        assertTrue(state.update(START + "shop"));
        NativeNavigationGroups shopState = state.sidebarGroups();
        assertNotSame(defaultState, shopState);
        assertFalse(shopState.isExpanded("account"));
        shopState.setExpanded("account", true);
        assertTrue(state.update(START));
        assertSame(defaultState, state.sidebarGroups());
        assertTrue(defaultState.isExpanded("account"));
        defaultState.setExpanded("account", false);
        assertTrue(state.update(START + "shop"));
        assertSame(shopState, state.sidebarGroups());
        assertTrue(shopState.isExpanded("account"));
        assertTrue(state.update(START));
        assertFalse(defaultState.isExpanded("account"));
    }

    @Test public void activeAncestorsFollowActualNavigationAcrossHiddenMenuAndFragmentChanges() throws Exception {
        JSONObject profile = profile(menu("shop", group("account", true, link("Shop", "/shop/item"))))
                .put("rules", new JSONArray().put(rule("prefix", "/shop").put("sidebar_menu", "shop")));
        NativeNavigationMenus.State state = new NativeNavigationMenus.State(parse(profile), START, true, true, true, true);
        assertTrue(state.update(START + "shop/item"));
        NativeNavigationGroups group = state.sidebarGroups();
        assertTrue(group.isExpanded("account"));
        group.setExpanded("account", false);
        assertFalse(state.update(START + "shop/item"));
        assertFalse(group.isExpanded("account"));
        assertFalse(state.update(null));
        assertFalse(group.isExpanded("account"));
        assertTrue(state.update(START + "elsewhere"));
        assertTrue(state.update(START + "shop/item"));
        assertTrue(group.isExpanded("account"));
        group.setExpanded("account", false);
        assertFalse(state.update(START + "shop/item#next"));
        assertTrue(group.isExpanded("account"));
    }

    @Test public void stateOnlyRebuildsForEffectiveMenuOrVisibilityChanges() throws Exception {
        JSONObject first = rule("exact", "/first").put("visibility", new JSONObject().put("top_bar", false));
        JSONObject second = rule("exact", "/second").put("visibility", new JSONObject().put("top_bar", false));
        NativeNavigationMenus config = parse(new JSONObject().put("rules", new JSONArray().put(first).put(second)));
        NativeNavigationMenus.State state = new NativeNavigationMenus.State(config, START, true, true, false, true);
        assertTrue(state.update(START + "first"));
        assertFalse(state.selection().topEnabled);
        assertTrue(state.selection().sidebarEnabled);
        assertFalse(state.update(START + "second"));
        assertEquals(1, state.selection().matchedRuleIndex);
        assertFalse(state.update(START + "second?q=1"));
        assertTrue(state.update(START));
        assertTrue(state.selection().topEnabled);
    }

    private static NativeNavigationMenus parse(String profile) {
        return NativeNavigationMenus.parse("Menu", LEGACY, profile, "example.test");
    }

    private static NativeNavigationMenus parse(JSONObject profile) {
        return parse(profile.toString());
    }

    private static NativeNavigationMenus.Selection selected(NativeNavigationMenus menus, String url) {
        return menus.select(url, true, true, true, true);
    }

    private static NativeNavigationMenus withRule(JSONObject rule) throws Exception {
        return parse(new JSONObject().put("rules", new JSONArray().put(rule.put("sidebar_menu", "default"))));
    }

    private static void matches(NativeNavigationMenus config, String path, boolean expected) {
        assertTrue(config.valid);
        assertEquals(path, expected ? 0 : -1, selected(config, "https://app.example.test" + path).matchedRuleIndex);
    }

    private static void rejectedRule(JSONObject rule) throws Exception {
        rejected(new JSONObject().put("rules", new JSONArray().put(rule.put("sidebar_menu", "default"))));
    }

    private static void rejected(JSONObject profile) {
        rejected(parse(profile));
    }

    private static void rejected(NativeNavigationMenus config) {
        assertFalse(config.valid);
        assertEquals(1, config.menuIds().size());
        NativeNavigationMenus.Selection selection = selected(config, START);
        assertEquals("default", selection.sidebarMenu);
        assertEquals("default", selection.tabsMenu);
        assertEquals("Legacy", selection.sidebar.items.get(0).label);
        assertEquals(-1, selection.matchedRuleIndex);
    }

    private static JSONObject rule(String match, String path) throws Exception {
        return new JSONObject().put("match", match).put("path", path);
    }

    private static JSONObject profile(JSONObject... menus) throws Exception {
        JSONArray list = new JSONArray();
        for (JSONObject menu : menus) list.put(menu);
        return new JSONObject().put("menus", list);
    }

    private static JSONObject menu(String id, Object... nodes) throws Exception {
        JSONArray items = new JSONArray();
        for (Object node : nodes) items.put(node);
        return new JSONObject().put("id", id).put("label", "Menu " + id).put("items", items);
    }

    private static JSONObject link(String label, String url) throws Exception {
        return new JSONObject().put("label", label).put("url", url);
    }

    private static JSONObject group(String id, boolean collapsed, Object... children) throws Exception {
        JSONArray items = new JSONArray();
        for (Object child : children) items.put(child);
        return new JSONObject().put("type", "group").put("id", id).put("label", id)
                .put("collapsed", collapsed).put("children", items);
    }
}
