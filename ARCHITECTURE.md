# Architecture

The wrapper stays intentionally small and separates policy from Android UI plumbing.

- `config/AppConfig`: product URL, trusted domains, native cache scopes and compatibility toggles
- `navigation/NavigationPolicy`: pure Java URL trust decision, unit testable without Android
- `web/WebViewConfigurator`: security and browser capability configuration
- `web/GameWebViewClient`: navigation, TLS, load error handling and cache-hit interception
- `web/QuantumAssetWarmup`: discovers static resources already used by a trusted page and schedules native warm-up
- `web/GameWebChromeClient`: HTML file chooser, progress and fullscreen content
- `download/AppDownloadListener`: authenticated downloads through Android DownloadManager
- `assets/QuantumAssetPolicy`: strict pure-Java policy for cacheable HTTPS `/assets/` resources
- `assets/QuantumAssetStore`: persistent app-private static asset cache with URL-versioned keys, TTLs and an LRU-style storage budget
- `media/QuantumMediaSourcePolicy`: validates trusted HTTPS campaign media sources
- `media/QuantumCampaignMediaStore`: downloads campaign OGG files once into the app-private persistent `files/quantum_nmp/campaign` directory, verifies the OGG signature and hands local paths to the native playback layer
- `media/QuantumNativeMediaPlayer`: bridge-facing soundtrack state and persisted music settings; it resolves media through the campaign store and delegates playback rather than owning a platform player
- `media/QuantumMediaSessionClient`: Activity-side Media3 controller client that sends playback commands to the service and survives asynchronous session connection
- `media/QuantumMediaPlaybackService`: foreground-capable Media3 `MediaSessionService` that owns ExoPlayer and the media session used by Android system controls
- `media/QuantumMediaMetadata`: pure-Java notification title derivation, unit testable without Android
- `MainActivity`: lifecycle and view orchestration only

Quantum NMP no longer places the actual player inside `MainActivity`. The WebView bridge talks to `QuantumNativeMediaPlayer`, which resolves trusted campaign media and then controls the service through `QuantumMediaSessionClient`. The service owns ExoPlayer and the `MediaSession`, so page navigation, Activity backgrounding and screen-off do not destroy active playback. Media3 supplies the standard Android media notification and system/lock-screen controls from the same session.

Quantum Asset Store and Quantum NMP are deliberately separate responsibilities. Generic static Game resources below `/assets/` are warmed into `files/quantum_assets` and may be served on later WebView requests. Campaign soundtrack files below `/assets/sounds/campaign/` are excluded from that generic store and remain under the stricter OGG-aware Quantum NMP media pipeline.

Both stores intentionally use Android's persistent app data area (`Context.getFilesDir()`), not `cacheDir` and not WebView cache storage. Files therefore survive normal restarts and application updates while Android still removes them when the user clears application storage or uninstalls the app. No external-storage permission is required.

The generic Asset Store is an optimization, never a network proxy dependency. On a cache miss WebView performs its normal request. Only complete fresh native entries are intercepted, which avoids reproducing browser cookies, redirects, range requests, CORS and content-encoding behavior inside the wrapper.

CoreUI or another STU web app can reuse the shell by changing the product configuration or by introducing a second thin application module later. Product-specific logic should not be moved into the shared web layer.
