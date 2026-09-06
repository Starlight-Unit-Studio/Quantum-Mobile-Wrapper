# Changelog

## 0.1.0-beta7-diagnostic

Startup-isolation build after beta6 still force-closed on real hardware in both normal Android and Samsung DeX.

- Added a dedicated launcher activity that does not inflate the normal wrapper WebView on startup.
- Added a process-wide uncaught Java exception logger stored in app-private files.
- Added staged tests for raw WebView creation and wrapper component initialization.
- Added a separate action to start the full `MainActivity` only after the diagnostic shell is already open.
- Added selectable/copyable diagnostic output and persisted crash display on the next launch.
- Kept the beta6 non-Media3 Quantum NMP runtime, persistent OGG store and Quantum Asset Store unchanged.
- Android `versionCode` increased to 7.

This build is intentionally diagnostic and should not be merged as the normal product launcher. Its purpose is to identify whether the force-close occurs before WebView creation, during wrapper initialization, in `MainActivity`, or outside the Java exception path.

## 0.1.0-beta6

Emergency startup-crash rollback after beta4/beta5 real-device testing.

- Removed Media3/ExoPlayer runtime dependencies from the wrapper.
- Removed the MediaSessionService and MediaController client layer.
- Restored the beta3 Android framework `MediaPlayer` implementation for Quantum NMP.
- Restored the pre-Media3 manifest without foreground media-service declarations.
- Kept persistent campaign OGG storage, trusted-source validation, enabled/volume preferences and the existing `window.QuantumNMP` bridge contract unchanged.
- Kept Quantum Asset Store unchanged.
- Android `versionCode` increased to 6.

This rollback deliberately prioritizes a reliably starting application. Background/lock-screen Media3 playback will be redesigned separately instead of remaining in the critical wrapper runtime while the startup regression is unresolved.

## 0.1.0-beta5

Startup crash hardening for Quantum NMP Media3 integration.

- Removed eager Media3 controller binding from `MainActivity` startup by creating the session client only when native soundtrack playback is actually requested.
- Added a fail-safe around Media3 session client creation so an initialization failure cannot take down the Game Activity.
- Hardened `QuantumMediaPlaybackService` startup so a Media3/ExoPlayer initialization failure is logged and the service declines the session instead of crashing the application process.
- Existing `window.QuantumNMP` bridge methods, campaign OGG persistence and trusted-source policy remain unchanged.
- Android `versionCode` increased to 5.

## 0.1.0-beta4

Media3 background playback for Quantum NMP.

- Replaced the Activity-owned Android framework `MediaPlayer` with Media3 ExoPlayer hosted by a dedicated `MediaSessionService`.
- Added a `MediaController` client layer so the existing `window.QuantumNMP` bridge contract can control the service without moving playback ownership back into the Activity.
- Active soundtrack playback can continue while the Game Activity is backgrounded or the display is off.
- Media3 now publishes standard Android media-session state for notification, lock-screen, headset/Bluetooth and other system playback controls.
- ExoPlayer uses media audio attributes with audio-focus handling enabled.
- Campaign OGG persistence, trusted-source validation, enabled preference and volume preference remain owned by the existing Quantum NMP pipeline.
- Media notification titles are derived from campaign filenames through a small pure-Java metadata helper with unit tests.
- Added foreground-service and media-playback foreground-service manifest permissions and declared the Media3 session service.
- Added Media3 ExoPlayer/session dependencies.
- Android `versionCode` increased to 4.

## 0.1.0-beta3

Quantum Asset Store for Game resources below `/assets/`.

- Trusted static assets are warmed into app-private persistent `files/quantum_assets` storage after a page has loaded.
- Later WebView requests use native cache hits through `shouldInterceptRequest()` while normal network loading remains the fallback on every miss or error.
- Cache scope is restricted to HTTPS resources on `game.starlight-unit.de` below `/assets/` with a supported static file type.
- API, HTML/PHP traffic, credential URLs, traversal attempts, redirects and HTML error/login responses are excluded from the store.
- Campaign soundtrack files below `/assets/sounds/campaign/` remain owned by Quantum NMP and are not duplicated in the generic Asset Store.
- Cache filenames are SHA-256-derived from the complete normalized asset URL including query strings, so Game cachebuster versions produce distinct native entries.
- Versioned assets use a 30-day native TTL, unversioned assets use a 24-hour TTL.
- Storage is capped at 64 MiB per entry and 256 MiB total with oldest-entry eviction.
- Wrapper request markers and available WebView cookies are reused for native warm-up requests.
- Android `versionCode` increased to 3.

## 0.1.0-beta2

Persistent native campaign audio storage.

- Campaign `.ogg` files are downloaded once by Quantum NMP into the app's private persistent `files` data directory instead of relying on WebView/cache storage.
- Stored files survive normal app restarts and app updates and are removed only when app data is cleared or the app is uninstalled.
- Downloads are written atomically through `.part` files and verified for the `OggS` stream signature before becoming playable persistent media.
- Persistent filenames are SHA-256-derived from the normalized source URL, preventing collisions across nested campaign paths and versioned query strings.
- Downloads are restricted to trusted HTTPS campaign sources, redirects are not followed, and individual files are capped at 64 MiB.
- If persistent download fails, Quantum NMP falls back to direct remote playback instead of breaking music playback.
- Android `versionCode` increased to 2.

## 0.1.0-beta1

Initial beta foundation.

- Java 17 Android WebView wrapper.
- Android 16 / API 36 target.
- Direct load of the Starlight Unit Game URL.
- Persistent cookies and DOM storage.
- File upload through the system document picker.
- Download integration through Android `DownloadManager`.
- Trusted STU-domain navigation in-app; external links handed to Android.
- Native/custom HTML fullscreen support.
- Immersive fullscreen UI.
- Back navigation that respects WebView history.
- Retry overlay for main-frame loading/TLS/HTTP failures.
- WebView Safe Browsing enabled.
- Cleartext HTTP and mixed content disabled.
- CI for unit tests and debug APK generation.
- Quantum NMP beta bridge with native OGG-capable Android media playback that survives ordinary WebView page navigation.
- Persistent native music enabled/volume preferences exposed to the game through `window.QuantumNMP`.
- Starlight wrapper/app/version request markers on trusted top-level GET navigation.
- Trusted-origin checks for side-effecting native media bridge commands.
