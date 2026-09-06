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
- `media/QuantumNativeMediaPlayer`: stable framework `MediaPlayer` playback, soundtrack state and persisted music settings
- `media/QuantumMediaMetadata`: pure-Java title helper retained for future native media surfaces
- `MainActivity`: lifecycle and view orchestration only

Beta6 intentionally removes the beta4/beta5 Media3 `MediaSessionService` and controller layer after repeated real-device cold-start force-closes. Quantum NMP remains native and independent from WebView document lifetime, but the implementation is back on the simpler framework `MediaPlayer` path that was stable before the Media3 migration.

Quantum Asset Store and Quantum NMP are deliberately separate responsibilities. Generic static Game resources below `/assets/` are warmed into `files/quantum_assets` and may be served on later WebView requests. Campaign soundtrack files below `/assets/sounds/campaign/` are excluded from that generic store and remain under the stricter OGG-aware Quantum NMP media pipeline.

Both stores intentionally use Android's persistent app data area (`Context.getFilesDir()`), not `cacheDir` and not WebView cache storage. Files therefore survive normal restarts and application updates while Android still removes them when the user clears application storage or uninstalls the app. No external-storage permission is required.

The generic Asset Store is an optimization, never a network proxy dependency. On a cache miss WebView performs its normal request. Only complete fresh native entries are intercepted, which avoids reproducing browser cookies, redirects, range requests, CORS and content-encoding behavior inside the wrapper.

CoreUI or another Starlight web app can reuse the shell by changing the product configuration or by introducing a second thin application module later. Product-specific logic should not be moved into the shared web layer.
