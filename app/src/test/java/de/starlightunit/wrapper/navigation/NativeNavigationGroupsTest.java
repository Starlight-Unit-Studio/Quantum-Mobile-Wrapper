package de.starlightunit.wrapper.navigation;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class NativeNavigationGroupsTest {
    @Test public void flattensOnlyDestinationsInDepthFirstOrder() throws Exception {
        NativeNavigationConfig config = config(
                link("Home", "/"),
                group("library", false,
                        link("News", "/news"),
                        group("account", true,
                                link("Profile", "/profile"),
                                group("settings", false, link("Security", "/security"))),
                        link("Help", "/help")),
                link("About", "/about"));

        assertTrue(config.valid);
        assertEquals(3, config.tree.size());
        List<String> labels = new ArrayList<>();
        for (NativeNavigationConfig.Item item : config.items) {
            assertFalse(item.isGroup);
            labels.add(item.label);
        }
        assertEquals(Arrays.asList("Home", "News", "Profile", "Security", "Help", "About"), labels);
        assertEquals("library", config.tree.get(1).id);
        assertTrue(config.tree.get(1).children.get(1).collapsed);
        assertEquals(5, config.items.subList(0, 5).size());
    }

    @Test public void groupDefaultsIconsAndLegacyLinksShareOnePolicy() throws Exception {
        JSONObject library = group("library", true, link("News", "/news").put("type", "link"));
        library.remove("collapsed");
        library.put("icon", "folder").put("active_icon", "star");
        NativeNavigationConfig config = config(library, link("Home", "/"));

        assertTrue(config.valid);
        NativeNavigationConfig.Item group = config.tree.get(0);
        assertFalse(group.collapsed);
        assertEquals("folder", group.icon);
        assertEquals("star", group.activeIcon);
        assertTrue(NativeNavigationGroups.hasIcons(config.tree));
        assertFalse(NativeNavigationGroups.hasIcons(config.items));
        assertTrue(new NativeNavigationGroups(config, "https://example.test/").isExpanded("library"));
        try {
            group.children.clear();
            fail("Children must remain immutable");
        } catch (UnsupportedOperationException expected) {
            // State changes belong to the controller, never the source tree.
        }
    }

    @Test public void emptyGroupsRemainAvailableAlongsideHomeFallback() throws Exception {
        NativeNavigationConfig config = config(group("empty", false),
                group("parent", true, group("child", false)));
        assertTrue(config.valid);
        assertEquals(2, config.tree.size());
        assertTrue(config.items.isEmpty());
        NativeNavigationGroups state = new NativeNavigationGroups(config, "https://example.test/");
        state.navigate("https://example.test/");
        assertTrue(state.isExpanded("empty"));
        assertFalse(state.isExpanded("parent"));
        assertFalse(state.isActive(config.tree.get(0), "https://example.test/"));
    }

    @Test public void enforcesGlobalCountsAndThreeGroupAncestors() throws Exception {
        JSONArray atLimit = new JSONArray();
        for (int i = 0; i < 12; i++) atLimit.put(group("g" + i, false, link("Link " + i, "/" + i)));
        NativeNavigationConfig accepted = NativeNavigationConfig.parse("", atLimit.toString());
        assertTrue(accepted.valid);
        assertEquals(12, accepted.items.size());
        assertEquals(12, accepted.tree.size());
        atLimit.put(link("Thirteenth", "/13"));
        rejected(NativeNavigationConfig.parse("", atLimit.toString()));

        JSONArray links = new JSONArray();
        JSONArray groups = new JSONArray();
        for (int i = 0; i < 13; i++) {
            links.put(link("Link " + i, "/" + i));
            groups.put(group("g" + i, false));
        }
        rejected(NativeNavigationConfig.parse("", links.toString()));
        rejected(NativeNavigationConfig.parse("", groups.toString()));
        assertTrue(config(group("one", false, group("two", false,
                group("three", false, link("Deep", "/deep"))))).valid);
        rejected(config(group("one", false, group("two", false,
                group("three", false, group("four", false))))));
    }

    @Test public void requiresExactGloballyUniqueGroupIds() throws Exception {
        rejected(config(group("parent", false, group("duplicate", false)), group("duplicate", false)));
        String[] invalidIds = {"", "Uppercase", " leading", "trailing ", "1start", "a.b",
                "abcdefghijklmnopqrstuvwxyzabcdefghijklmno"};
        for (String id : invalidIds) rejected(config(group(id, false)));
        JSONObject missing = group("missing", false);
        missing.remove("id");
        rejected(config(missing));
        rejected(config(group("number", false).put("id", 1)));
        rejected(config(group("null", false).put("id", JSONObject.NULL)));
        assertTrue(config(group("abc_123-def", false)).valid);
        assertTrue(config(group("abcdefghijklmnopqrstuvwxyzabcdefghijklmn", false)).valid);
    }

    @Test public void rejectsMalformedTreesAtomically() throws Exception {
        List<Object> invalid = new ArrayList<>();
        invalid.add(JSONObject.NULL);
        invalid.add(new JSONArray());
        JSONObject noChildren = group("missing", false);
        noChildren.remove("children");
        invalid.add(noChildren);
        invalid.add(group("object", false).put("children", new JSONObject()));
        invalid.add(group("url", false).put("url", ""));
        invalid.add(group("nullurl", false).put("url", JSONObject.NULL));
        invalid.add(group("string", false).put("collapsed", "false"));
        invalid.add(group("nullcollapsed", false).put("collapsed", JSONObject.NULL));
        invalid.add(link("Children", "/children").put("children", new JSONArray()));
        invalid.add(link("Id", "/id").put("id", JSONObject.NULL));
        invalid.add(link("Collapsed", "/collapsed").put("collapsed", false));
        invalid.add(link("Unknown", "/unknown").put("type", "divider"));
        invalid.add(link("Null", "/null").put("type", JSONObject.NULL));
        invalid.add(link(" ", "/blank"));
        invalid.add(new JSONObject().put("label", "Missing"));
        invalid.add(link("Number", "/number").put("url", 123));
        for (Object node : invalid) rejected(config(link("Earlier valid link", "/"), node));
    }

    @Test public void boundsJsonBeforeRecursiveParsingAndRejectsTrailingContent() throws Exception {
        StringBuilder deeplyNested = new StringBuilder();
        for (int i = 0; i < 1000; i++) deeplyNested.append('[');
        for (int i = 0; i < 1000; i++) deeplyNested.append(']');
        rejected(NativeNavigationConfig.parse("", deeplyNested.toString()));
        rejected(NativeNavigationConfig.parse("", "[] []"));
        rejected(NativeNavigationConfig.parse("", "[] extra"));
        rejected(NativeNavigationConfig.parse("", "[{\"label\":\"unterminated}]"));
        rejected(NativeNavigationConfig.parse("", "[{'label':'Home','url':'/'}]"));
        rejected(NativeNavigationConfig.parse("", "[{label:\"Home\",url:\"/\"}]"));
        rejected(NativeNavigationConfig.parse("", "/* \" */[]/* \" */"));
        rejected(NativeNavigationConfig.parse("", "[{\"label\":\"Bad\\xescape\",\"url\":\"/\"}]"));
        rejected(NativeNavigationConfig.parse("", "[{\"label\":\"Bad\ncontrol\",\"url\":\"/\"}]"));
        assertTrue(config(link("Braces [ ] { } and \"quotes\"", "/")).valid);
        StringBuilder oversized = new StringBuilder("[]");
        while (oversized.length() <= 65_535) oversized.append(' ');
        rejected(NativeNavigationConfig.parse("", oversized.toString()));
    }

    @Test public void opensActiveAncestorsAndPreservesManualStateOnSameUrl() throws Exception {
        NativeNavigationConfig config = config(
                group("library", true, group("nested", true, link("News", "/news"))),
                group("account", true, link("Profile", "/profile")));
        NativeNavigationGroups state = new NativeNavigationGroups(config, "https://example.test/");
        assertFalse(state.isExpanded("library"));
        state.navigate("https://example.test/news");
        assertTrue(state.isExpanded("library"));
        assertTrue(state.isExpanded("nested"));
        assertFalse(state.isExpanded("account"));
        assertTrue(state.isActive(config.tree.get(0), "https://example.test/news#top"));
        assertFalse(state.isActive(config.tree.get(1), "https://example.test/news"));
        assertTrue(state.setExpanded("library", false));
        assertTrue(state.setExpanded("nested", false));
        state.navigate("https://example.test/news");
        assertFalse(state.isExpanded("library"));
        assertFalse(state.isExpanded("nested"));
        state.navigate(null);
        assertFalse(state.isExpanded("library"));
        state.navigate("https://example.test/profile");
        assertTrue(state.isExpanded("account"));
        assertFalse(state.isExpanded("library"));
        state.navigate("https://example.test/news#top");
        assertTrue(state.isExpanded("library"));
        assertTrue(state.isExpanded("nested"));
        assertTrue(state.isExpanded("account"));
        assertFalse(state.setExpanded("unknown", true));
        assertFalse(state.isExpanded("unknown"));
    }

    @Test public void activeSelectionPreservesQueryAndPathSemantics() throws Exception {
        NativeNavigationConfig config = config(group("search", false, link("Search", "/search?q=a")));
        NativeNavigationGroups state = new NativeNavigationGroups(config, "https://example.test/app/");
        assertTrue(state.isActive(config.tree.get(0), "https://example.test/search?q=a#results"));
        assertFalse(state.isActive(config.tree.get(0), "https://example.test/search?q=b"));
        assertFalse(state.isActive(config.tree.get(0), "https://example.test/search/?q=a"));
        assertFalse(state.isActive(config.tree.get(0), null));
        assertFalse(NativeNavigationGroups.sameLocation("", ""));
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

    private static NativeNavigationConfig config(Object... nodes) {
        JSONArray items = new JSONArray();
        for (Object node : nodes) items.put(node);
        return NativeNavigationConfig.parse("Navigation", items.toString());
    }

    private static void rejected(NativeNavigationConfig config) {
        assertFalse(config.valid);
        assertTrue(config.tree.isEmpty());
        assertTrue(config.items.isEmpty());
    }
}
