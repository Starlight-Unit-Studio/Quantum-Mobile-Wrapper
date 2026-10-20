# Native navigation icons

Available in Quantum Mobile Wrapper `0.1.0-beta10`, with Android 6 / API 23 as the minimum runtime.

Sidebar and Bottom Tabs share the ordered `NATIVE_NAVIGATION_ITEMS_JSON` array. Existing entries with only `label` and `url` remain valid. Each entry may additionally contain `icon` and `active_icon` string fields.

```json
[
  {"label":"Home","url":"/","icon":"home"},
  {"label":"Favorites","url":"/favorites","icon":"heart","active_icon":"star"}
]
```

## Built-in catalogue

The supported identifiers are `home`, `search`, `grid`, `star`, `heart`, `user`, `settings`, `info`, `mail`, `cart`, `calendar`, `camera`, `map`, `phone`, `download`, `help`, `play`, `folder`, `globe` and `link`.

These 20 original outline vectors use the same 24-unit paths as Quantum Builder's `public/assets/navigation-icons.js`. Each icon renders within a 22 dp square. Built-in icons use the navigation foreground color when inactive and the accent color when selected.

## Custom PNGs

Builder accepts a custom PNG in the profile and bundles it into the generated app. The compiler replaces its data URL with a content-addressed reference such as:

```json
{
  "label":"Brand",
  "url":"/brand",
  "icon":"asset:quantum_nav_icons/0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef.png"
}
```

The reference must name a PNG under `app/src/main/assets/quantum_nav_icons/` with exactly 64 lowercase hexadecimal characters in its filename. Builder generates that filename from the PNG's SHA-256 digest and deduplicates repeated images. When editing this repository manually, package the matching PNG asset as well as configuring its reference.

Each PNG is limited to 65,536 bytes and dimensions from 1 through 256 pixels on each side. The native loader enforces the byte limit before decoding and checks the decoded dimensions. Images retain their own colors, transparency and aspect ratio inside the 22 dp icon square. Runtime icon loading never requests a network URL. Remote URLs, data URLs, arbitrary asset paths and traversal sequences are rejected.

## Selection and layout

`active_icon` overrides `icon` only while that destination is selected. An omitted or empty `active_icon` inherits the normal icon. A missing or undecodable active PNG also falls back to the normal icon. Invalid or unavailable normal icons leave the label and navigation action usable.

The selection rule compares the current WebView URL with the resolved menu target, ignoring the fragment. It preserves the existing exact URL and query behavior. Both Sidebar and Bottom Tabs update their selected text style and icon, and expose the selected state to Android accessibility. The visible label remains the accessibility name.

Sidebar icons appear beside their labels. Tab icons appear above their labels. A menu surface reserves equal icon slots whenever any of its displayed entries has an icon, including an active-only icon. This keeps labels aligned when iconed and iconless entries are mixed. Profiles without any icons keep the existing text-only layout.

There are still at most 12 menu entries, with the first five available as Bottom Tabs. Navigation continues through the existing trusted-domain loader. The fixed Top Bar and Contextual Toolbar actions do not gain per-item icon settings in this release.

## Local verification

`NativeNavigationIconPolicyTest` covers the catalogue, exact compiled asset references, unsafe source rejection and selected-state fallback. `NativeNavigationConfigTest` covers legacy profiles, icon field preservation, invalid field isolation and existing relative URL behavior. Run the project's normal local checks with an installed JDK and Android SDK:

```bash
./gradlew lintDebug test assembleDebug
```

Device checks should include a mixed Sidebar/Bottom Tab menu, both PNG variants, selected state after Back/Forward, custom image aspect ratio, missing assets, Android 6 and a recent Android version. A Java source compilation or browser preview alone does not verify the final Android layout.
