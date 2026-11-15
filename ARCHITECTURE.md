# Architecture

The wrapper stays intentionally small and separates policy from Android UI plumbing.

- `config/AppConfig`: product URL, trusted domains, native cache scopes and compatibility toggles
- `navigation/NavigationPolicy`: pure Java URL trust decision, unit testable without Android
- `navigation/NativeNavigationConfig`: bounded menu tree and depth-first destination list with optional normal/active icons
- `navigation/NativeNavigationGroups`: pure Java group expansion state and active-descendant policy
- `navigation/NativeNavigationMenus`: bounded catalog, ordered raw-path/host rules, independent selections and per-menu group state
- `navigation/NativeNavigationIconPolicy`: pure Java catalogue and strict compiled-asset namespace validation
- `navigation/NativeNavigationIcons`: tinted vector loading and bounded, aspect-preserving bundled PNG loading
- `navigation/NativeNavigationController`: native bars and Sidebar, shared URL selection state and accessible icon/label views
- `web/WebViewConfigurator`: security and browser capability configuration
- `web/GameWebViewClient`: navigation, TLS, load error handling and cache-hit interception
- `web/QuantumAssetWarmup`: discovers static resources already used by a trusted page and schedules native warm-up
- `web/GameWebChromeClient`: HTML file chooser, progress and fullscreen content
- `download/AppDownloadListener`: authenticated downloads through Android DownloadManager
- `assets/QuantumAssetPolicy`: strict pure-Java policy for cacheable HTTPS `/assets/` resources
- `assets/QuantumAssetStore`: persistent app-private static asset cache with URL-versioned keys, TTLs and an LRU-style storage budget
- `media/QuantumMediaSourcePolicy`: validates trusted HTTPS campaign media sources
- `media/QuantumCampaignMediaStore`: downloads campaign OGG files once into the app-private persistent `files/quantum_nmp/campaign` directory, verifies the OGG signature and hands local paths to the native playback layer
- `media/QuantumNativeMediaPlayer`: framework `MediaPlayer` playback, soundtrack state and persisted music settings
- `media/QuantumMediaMetadata`: pure-Java title helper retained for future native media surfaces
- `MainActivity`: lifecycle and view orchestration only

## Navigation menus and URL rules

The Standard tree remains in `NATIVE_NAVIGATION_ITEMS_JSON`; `NATIVE_NAVIGATION_PROFILE_JSON` carries additional menus, default references and ordered presentation rules. `NativeNavigationMenus.State` evaluates the first matching trusted HTTPS rule and retains independent group state per menu. Controller layout changes follow effective menu IDs and visibility without navigating or reloading. Page-start, visited-history and finished-page callbacks update the presentation. See `docs/NATIVE_NAVIGATION_MENUS.md` for strict matching, fallback and validation limits.

## Navigation groups

Builder keeps links and groups in one ordered menu tree. The native parser exposes that tree for Sidebar rendering and a separate depth-first list of link destinations for Bottom Tabs. Existing flat profiles keep their original order. The parser requires unique, exact group IDs and validates the entire tree before exposing any destinations. It caps the menu at 12 links, 12 groups, 24 total nodes and three group ancestors, and bounds JSON length and structural depth before using Android's JSON parser.

Group headers toggle a nested child container without navigating. Their selected style and active icon follow any selected descendant. Expansion state belongs to the navigation controller: configured defaults apply initially, active ancestors open at startup and on a new current URL, and repeated updates for the same URL preserve manual collapse. Activity recreation starts from the configured defaults again. No preferences or external storage are involved.

Sidebar uses 50 dp minimum targets, 12 dp indentation per group ancestor and a separate disclosure arrow. A header exposes one accessible button with expand/collapse actions and an expanded/collapsed description. Empty groups remain visible, and an all-empty menu receives a Home fallback in both Sidebar and Bottom Tabs. See `docs/NATIVE_NAVIGATION_GROUPS.md` for the profile contract and pending device checks.

## Navigation icons

Builder serializes normal and active icon settings with each link or group. Built-in identifiers refer to packaged original vector resources. Custom PNGs are compiled into content-addressed files under `assets/quantum_nav_icons/`, leaving only short asset references in `AppConfig`. The runtime never fetches navigation images over the network. Its per-controller cache is bounded by the menu limit of 24 nodes with at most two icon assets per node, each constrained to 64 KiB and 256 by 256 pixels before decoding.

Both Sidebar and Bottom Tabs use the same current-URL selection rule. The icon loader falls back to the normal icon if the selected variant cannot be decoded, and the label/action survives any icon failure. Icon slots are reserved per surface when needed, while legacy iconless profiles retain their text-only layout. See `docs/NATIVE_NAVIGATION_ICONS.md` for the profile contract.

## Startup lifecycle

Beta7 diagnostics identified the Android 16 cold-start crash in `MainActivity` window setup. The Activity previously requested immersive mode before `setContentView()`, allowing `PhoneWindow.getInsetsController()` to be reached before a usable `DecorView` existed. Beta8 keeps non-content window configuration first, inflates the Activity content, and only then posts the immersive system-bar request through the actual decor view.

This keeps Android UI lifecycle concerns in `MainActivity` and prevents unrelated subsystems such as WebView, Quantum NMP or Quantum Asset Store from being blamed for failures that occur before they initialize.

## Quantum NMP

The beta4/beta5 Media3 playback experiment was rolled back during the crash investigation. The later diagnostic trace showed Media3 was not the source of the cold-start crash; the fault occurred earlier in immersive-window setup. Beta8 nevertheless retains the simpler framework `MediaPlayer` implementation as the current known-good playback baseline. Background notification and lock-screen playback can be reintroduced later as a separate, independently tested service change without altering the JavaScript bridge.

Quantum Asset Store and Quantum NMP are deliberately separate responsibilities. Generic static Game resources below `/assets/` are warmed into `files/quantum_assets` and may be served on later WebView requests. Campaign soundtrack files below `/assets/sounds/campaign/` are excluded from that generic store and remain under the stricter OGG-aware Quantum NMP media pipeline.

Both stores intentionally use Android's persistent app data area (`Context.getFilesDir()`), not `cacheDir` and not WebView cache storage. Files therefore survive normal restarts and application updates while Android still removes them when the user clears application storage or uninstalls the app. No external-storage permission is required.

The generic Asset Store is an optimization, never a network proxy dependency. On a cache miss WebView performs its normal request. Only complete fresh native entries are intercepted, which avoids reproducing browser cookies, redirects, range requests, CORS and content-encoding behavior inside the wrapper.

CoreUI or another Starlight web app can reuse the shell by changing the product configuration or by introducing a second thin application module later. Product-specific logic should not be moved into the shared web layer.
