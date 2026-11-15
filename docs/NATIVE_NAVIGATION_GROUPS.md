# Native navigation groups

Available in Quantum Mobile Wrapper `0.1.0-beta11`, with Android 6 / API 23 as the minimum runtime.

Sidebar can organize destinations into collapsible groups with up to three group ancestors. Builder compiles the ordered `navigation.items` tree into `AppConfig.NATIVE_NAVIGATION_ITEMS_JSON`. Existing flat arrays of links remain valid.

## Profile contract

```json
[
  {"label":"Home","url":"/","icon":"home"},
  {
    "type":"group",
    "id":"library",
    "label":"Library",
    "icon":"folder",
    "active_icon":"star",
    "collapsed":true,
    "children":[
      {"label":"News","url":"/news","icon":"globe"},
      {
        "type":"group",
        "id":"account",
        "label":"Account",
        "children":[
          {"label":"Profile","url":"/profile","icon":"user"}
        ]
      }
    ]
  }
]
```

Every link requires a nonempty `label` and `url`. Optional `type: "link"` is accepted; Builder normalizes it to the existing link shape. Links reject the group-only properties `id`, `children` and `collapsed`, including empty or null values.

Every group requires `type: "group"`, `label`, `id` and an array of `children`. The ID must match `^[a-z][a-z0-9_-]{0,39}$`, without leading or trailing whitespace, and must be unique across the entire menu. A present `collapsed` value must be a JSON boolean; omission means `false`. A group cannot contain a `url` property, including an empty string or null. Group headers only toggle their children.

Both links and groups accept the same `icon` and `active_icon` fields described in `NATIVE_NAVIGATION_ICONS.md`. The compiled profile contains built-in identifiers or references to bundled PNGs. Icons cannot make the runtime fetch external images. Invalid or unavailable icons retain the existing label-only fallback.

## Limits and failure behavior

The complete menu may contain 12 link destinations, 12 groups and 24 total nodes. A root group counts as level one. A third-level group can contain links but cannot contain another group. Empty groups are valid.

The native parser validates the complete tree before exposing it. It bounds JSON length and nesting before calling Android's JSON parser, then checks IDs, node types, property types and global limits. A malformed tree yields an empty configuration and the Home fallback; no earlier valid entries survive as a partial menu. Invalid icon references are isolated to the affected icon as in beta10.

Labels retain the existing native display limit of 40 characters after validation. Builder accepts at most 80 UTF-8 bytes for a label and 2,048 UTF-8 bytes for a destination. Navigation still passes through the wrapper's trusted-domain loader.

## Sidebar and Bottom Tabs

Sidebar preserves the full hierarchy. A group header contains its label, optional icon and a disclosure arrow. Tapping anywhere in the header expands or collapses the child container. It does not navigate or close the Sidebar. Children become visible only when all their ancestors are expanded.

The existing 280 dp Sidebar uses a 50 dp minimum row height, 14 dp base padding and 12 dp extra indentation per ancestor. Links under a third-level group receive 36 dp of extra indentation. A shared 22 dp icon slot keeps labels aligned across iconed and iconless nodes, including collapsed descendants. Group labels can wrap, and their disclosure arrow retains a separate space.

Bottom Tabs use the first five links in depth-first order. Group headers never become tabs, and collapsed state does not remove any link from that order. With only empty groups, Sidebar keeps the headers and appends a Home link; Bottom Tabs show Home.

## Active state and expansion

The selected destination follows the existing exact resolved-URL comparison, ignoring the fragment while retaining path and query distinctions. A group is selected when any descendant destination is selected, even if that group is manually collapsed. Selected groups use the accent color and their configured active icon.

Each controller starts with the configured collapse defaults. On its first URL update, ancestors of the current destination open automatically. Whenever the complete current URL changes, matching ancestors open again. A fragment change therefore counts as a new URL for expansion, while selection still ignores the fragment.

Manual expansion and collapse survive Sidebar reopening and repeated callbacks for the same URL. Navigating elsewhere opens that destination's ancestors without closing other groups. State remains in the current controller and resets on Activity recreation; it is not saved to preferences.

## Accessibility

Each group header is one focusable button with at least a 50 dp height. Its accessible description includes the label and expanded/collapsed state. It exposes the matching native expand or collapse action and announces manual changes. The label and arrow inside the header do not add duplicate accessibility targets. Collapsed child containers are hidden from interaction and accessibility.

## Verification

The local navigation check compiles the controller and related policy code against Android 6 / API 23 classes. The navigation test suite currently contains 35 passing JUnit cases, including bounded tree parsing, depth-first order, IDs, property types, malformed-input fallback, active descendants and same-URL manual expansion state. AAPT2 also compiles all 20 navigation vectors.

These focused checks do not build or run an APK. Full Android lint, Gradle APK assembly, TalkBack behavior, large-font layout and device tests remain separate acceptance checks. With an installed JDK and Android SDK, run:

```bash
./gradlew lintDebug test assembleDebug
```

The device cases are listed in `BETA_TEST_CHECKLIST.md`. No GitHub Actions run is needed for the local policy check.
