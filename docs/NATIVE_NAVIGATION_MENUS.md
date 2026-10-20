# Independent menus and URL-based presentation

Quantum Mobile Wrapper **0.1.0-beta12**, Android 6 / API23 minimum.

`NativeNavigationMenus` extends the existing Standard menu with at most three
additional named menus. Sidebar and Bottom Tabs independently select a menu.
At most sixteen ordered URL rules can choose different menus and override the
visibility of Top Bar, Sidebar, Bottom Tabs and Contextual Toolbar.

## Generated configuration

The existing `NATIVE_NAVIGATION_ITEMS_JSON` remains the Standard menu. The new
`NATIVE_NAVIGATION_PROFILE_JSON` contains:

```json
{
  "menus":[
    {"id":"shop","label":"Shop","items":[
      {"label":"Products","url":"/shop","icon":"grid"},
      {"label":"Basket","url":"/shop/basket","icon":"cart"}
    ]}
  ],
  "sidebar_menu":"default",
  "bottom_tabs_menu":"shop",
  "rules":[
    {"match":"exact","path":"/shop/checkout","visibility":{"bottom_tabs":false}},
    {"match":"prefix","path":"/shop","sidebar_menu":"shop","visibility":{"bottom_tabs":true}}
  ]
}
```

An absent extension is `{}`. Menu IDs follow `[a-z][a-z0-9_-]{0,39}`, with
`default` reserved; they are exact, untrimmed and unique. Menu names contain
1 to 80 UTF-8 bytes after trimming. Items retain the existing link/group/icon
contract. Each menu allows twelve links, twelve groups and three group levels;
all menus together allow at most 24 nodes and 48 icon fields. Group IDs are
unique within one menu and may repeat across menus.

Both JSON constants are length/depth guarded before parsing. Wrong container
types, unknown references, duplicate menu IDs, unknown definition/rule/visibility
fields, invalid paths/hosts, nonboolean visibility and inert rules reject the
whole extension. Fallback uses the validated Standard menu with default
visibility. A malformed Standard tree retains the existing Home fallback. The
compiler checks Java modified-UTF8 constant limits before generating assets.

## Selection and matching

The first matching rule wins as a whole. Unspecified settings inherit profile
defaults rather than later rules. Visibility may explicitly enable a surface
whose default AppConfig flag is false. Rules only affect presentation, never the
trusted navigation boundary.

Each rule requires `match` (`exact` or `prefix`) and an ASCII raw `path` beginning
with one `/`, up to 2,048 bytes. Percent escapes must be valid. Queries, fragments,
spaces and backslashes are rejected in rule paths. Matching ignores a current
URL's query and fragment and uses its raw case-sensitive path without decoding
percent escapes or normalizing dot segments.

Prefix `/shop` matches `/shop` and `/shop/...`, but not `/shopping`. `/shop/`
does not match `/shop`; `/` matches every path. An optional host is an exact
ASCII DNS name inside the trusted domain, normalized to lowercase; no wildcard,
scheme or port is allowed. Matching uses valid trusted HTTPS URLs, ignores their
port for host comparison and defaults safely for invalid/untrusted locations.

Sidebar displays the complete selected tree. Bottom Tabs display the first five
depth-first links of their own selected tree. Empty menus independently use
Home. Selecting a menu never calls `Host.navigate` or resets WebView history.

## Controller and lifecycle

`NativeNavigationMenus.State` owns immutable menu configurations, the current
selection and one `NativeNavigationGroups` instance per menu. All groups observe
actual URL changes, even while their menu is hidden. Returning to a destination
reveals its ancestors; same-URL updates retain manual collapse. Repeated group
IDs in separate menus never share state. Activity recreation resets the controller.

The controller rebuilds bars only when effective menu IDs or visibility change.
Ordinary URL updates refresh selection/history/icon state. Hidden bars release
WebView insets. Hiding Sidebar or changing its menu closes the drawer; a tabs-only
change preserves it. Sidebar retains its floating
launcher if Top Bar is hidden. Contextual Toolbar moves above currently visible
tabs. Existing actions, icons, accessibility and trusted loading stay connected.

The WebView client reports page starts, visited-history changes and completed
page loads to the presentation controller, including reported history updates
without a complete reload. MainActivity passes the reported URL directly so
evaluation does not depend on an older `WebView.getUrl()` value during startup.

## Verification boundary

Local navigation checks compile against Android 6/API23 and run parser, matcher,
menu-selection, bounds and group-state JUnit cases. The original twenty vector
resources still compile with AAPT2. Builder model/browser/PHP/worker evidence is
in [NAVIGATION_MENUS.md](https://github.com/Starlight-Unit-Studio/Quantum-Builder/blob/main/docs/NAVIGATION_MENUS.md).

Recorded local results: 52 navigation JUnit cases (17 menu/rule/state cases),
22 navigation-related sources compiled against API23, and 20 compiled vectors.
All 40 production Java sources also compile together against the official SDK36
and existing AndroidX dependencies, with only generated R/BuildConfig stubs. A
focused callback harness checks started/visited-history/current-finished dispatch.
The Builder request/SQLite/worker/generated-Java fixture agrees with JS on 92
URL decisions across three menus and eight rules, preserving shared PNG assets
and escaped Unicode labels.

These are focused source/policy checks, not APK execution. Full Gradle lint,
APK assembly, actual WebView callback timing, API23/current-device rendering,
large fonts and TalkBack remain acceptance items in `BETA_TEST_CHECKLIST.md`.
No new runtime dependency or GitHub Actions run is required.
