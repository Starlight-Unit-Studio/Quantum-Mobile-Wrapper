# Architecture

The wrapper stays intentionally small and separates policy from Android UI plumbing.

- `config/AppConfig`: product URL, trusted domain and compatibility toggles
- `navigation/NavigationPolicy`: pure Java URL trust decision, unit testable without Android
- `web/WebViewConfigurator`: security and browser capability configuration
- `web/GameWebViewClient`: navigation, TLS and load error handling
- `web/GameWebChromeClient`: HTML file chooser, progress and fullscreen content
- `download/AppDownloadListener`: authenticated downloads through Android DownloadManager
- `media/QuantumMediaSourcePolicy`: validates trusted HTTPS campaign media sources
- `media/QuantumCampaignMediaStore`: downloads campaign OGG files once into the app-private persistent `files/quantum_nmp/campaign` directory, verifies the OGG signature and hands local paths to the native player
- `media/QuantumNativeMediaPlayer`: Android MediaPlayer lifecycle, playback state and persisted music settings
- `MainActivity`: lifecycle and view orchestration only

Campaign media intentionally uses Android's persistent app data area (`Context.getFilesDir()`), not `cacheDir` and not WebView cache storage. That keeps downloaded soundtrack files across normal restarts and application updates while still letting Android remove them when the user clears application storage or uninstalls the app. No external-storage permission is required.

CoreUI or another STU web app can reuse the shell by changing the product configuration or by introducing a second thin application module later. Product-specific logic should not be moved into the shared web layer.
