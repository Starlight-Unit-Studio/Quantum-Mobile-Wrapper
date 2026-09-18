package de.starlightunit.wrapper.navigation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class LinkRoutingPolicy {
    public enum Action {
        INTERNAL,
        EXTERNAL,
        BLOCK
    }

    private static final class Rule {
        final String scheme;
        final String host;
        final String pathPrefix;
        final Action action;

        Rule(String scheme, String host, String pathPrefix, Action action) {
            this.scheme = normalized(scheme);
            this.host = normalized(host);
            this.pathPrefix = pathPrefix == null ? "" : pathPrefix;
            this.action = action;
        }

        boolean matches(URI uri) {
            String actualScheme = normalized(uri.getScheme());
            String actualHost = normalized(uri.getHost());
            String actualPath = uri.getPath() == null ? "/" : uri.getPath();

            if (!scheme.isEmpty() && !scheme.equals(actualScheme)) {
                return false;
            }
            if (!host.isEmpty()) {
                if (host.startsWith("*.")) {
                    String suffix = host.substring(1);
                    if (!actualHost.endsWith(suffix) || actualHost.equals(host.substring(2))) {
                        return false;
                    }
                } else if (!host.equals(actualHost)) {
                    return false;
                }
            }
            return pathPrefix.isEmpty() || actualPath.startsWith(pathPrefix);
        }
    }

    private final NavigationPolicy navigationPolicy;
    private final List<Rule> rules;

    public LinkRoutingPolicy(NavigationPolicy navigationPolicy, String rulesJson) {
        this.navigationPolicy = navigationPolicy;
        this.rules = Collections.unmodifiableList(parseRules(rulesJson));
    }

    public Action actionFor(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return Action.BLOCK;
        }

        final URI uri;
        try {
            uri = new URI(rawUrl.trim());
        } catch (URISyntaxException | IllegalArgumentException ignored) {
            return Action.BLOCK;
        }

        for (Rule rule : rules) {
            if (!rule.matches(uri)) {
                continue;
            }
            if (rule.action == Action.INTERNAL) {
                return navigationPolicy.shouldStayInWebView(rawUrl)
                        ? Action.INTERNAL
                        : Action.BLOCK;
            }
            if (rule.action == Action.EXTERNAL) {
                return isAllowedExternalScheme(rawUrl)
                        ? Action.EXTERNAL
                        : Action.BLOCK;
            }
            return Action.BLOCK;
        }

        if (navigationPolicy.shouldStayInWebView(rawUrl)) {
            return Action.INTERNAL;
        }
        return isAllowedExternalScheme(rawUrl) ? Action.EXTERNAL : Action.BLOCK;
    }

    public static boolean isAllowedExternalScheme(String url) {
        if (url == null) {
            return false;
        }
        try {
            URI uri = new URI(url);
            String scheme = normalized(uri.getScheme());
            return "https".equals(scheme)
                    || "http".equals(scheme)
                    || "mailto".equals(scheme)
                    || "tel".equals(scheme)
                    || "geo".equals(scheme)
                    || "market".equals(scheme)
                    || "intent".equals(scheme);
        } catch (URISyntaxException | IllegalArgumentException ignored) {
            return false;
        }
    }

    private static List<Rule> parseRules(String rulesJson) {
        List<Rule> parsed = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(
                    rulesJson == null || rulesJson.trim().isEmpty() ? "[]" : rulesJson
            );
            for (int i = 0; i < array.length() && parsed.size() < 64; i++) {
                JSONObject raw = array.optJSONObject(i);
                if (raw == null) {
                    continue;
                }
                String actionValue = normalized(raw.optString("action", ""));
                Action action;
                switch (actionValue) {
                    case "internal":
                        action = Action.INTERNAL;
                        break;
                    case "external":
                        action = Action.EXTERNAL;
                        break;
                    case "block":
                        action = Action.BLOCK;
                        break;
                    default:
                        continue;
                }

                String scheme = raw.optString("scheme", "").trim();
                String host = raw.optString("host", "").trim();
                String pathPrefix = raw.optString("path_prefix", "").trim();
                if (scheme.isEmpty() && host.isEmpty() && pathPrefix.isEmpty()) {
                    continue;
                }
                parsed.add(new Rule(scheme, host, pathPrefix, action));
            }
        } catch (Exception ignored) {
            parsed.clear();
        }
        return parsed;
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
