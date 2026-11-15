package de.starlightunit.wrapper.navigation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class LinkRoutingPolicyTest {

    private final NavigationPolicy navigation = new NavigationPolicy("example.test");

    @Test
    public void orderedRulesUseFirstMatch() {
        LinkRoutingPolicy policy = new LinkRoutingPolicy(
                navigation,
                "[{\"host\":\"example.test\",\"path_prefix\":\"/blocked\",\"action\":\"block\"},"
                        + "{\"host\":\"example.test\",\"action\":\"internal\"}]"
        );
        assertEquals(
                LinkRoutingPolicy.Action.BLOCK,
                policy.actionFor("https://example.test/blocked/page")
        );
        assertEquals(
                LinkRoutingPolicy.Action.INTERNAL,
                policy.actionFor("https://example.test/home")
        );
    }

    @Test
    public void internalRuleCannotForceExternalDomainIntoWebView() {
        LinkRoutingPolicy policy = new LinkRoutingPolicy(
                navigation,
                "[{\"host\":\"outside.test\",\"action\":\"internal\"}]"
        );
        assertEquals(
                LinkRoutingPolicy.Action.BLOCK,
                policy.actionFor("https://outside.test/page")
        );
    }

    @Test
    public void externalRuleStillRequiresAllowedScheme() {
        LinkRoutingPolicy policy = new LinkRoutingPolicy(
                navigation,
                "[{\"scheme\":\"javascript\",\"action\":\"external\"}]"
        );
        assertEquals(
                LinkRoutingPolicy.Action.BLOCK,
                policy.actionFor("javascript:alert(1)")
        );
    }

    @Test
    public void defaultBehaviorKeepsTrustedWebInternalAndExternalHttpsOutside() {
        LinkRoutingPolicy policy = new LinkRoutingPolicy(navigation, "[]");
        assertEquals(
                LinkRoutingPolicy.Action.INTERNAL,
                policy.actionFor("https://sub.example.test/page")
        );
        assertEquals(
                LinkRoutingPolicy.Action.EXTERNAL,
                policy.actionFor("https://outside.test/page")
        );
    }
}
