# Changelog

## 0.1.0-beta9

Android 6 / API 23 compatibility baseline.

- Lowered `minSdk` from API 26 to API 23 while keeping `compileSdk` and `targetSdk` at API 36.
- Added Android `lintDebug` to CI before unit tests and APK assembly so accidental newer-platform calls are caught against the API 23 minimum.
- Replaced API 24-only `URLConnection.getContentLengthLong()` calls in Quantum Asset Store and Quantum NMP campaign persistence with the API 1-compatible integer content-length accessor; the existing 64 MiB streaming limits remain authoritative.
- Replaced API 24-only `List.sort()` / `Comparator.comparingLong()` cache-eviction code with an API 23-safe `Collections.sort()` comparator.
- Isolated Android 11/API 30 `WindowInsetsController` code in a version-gated helper class while retaining the beta8 Android 16 lifecycle fix.
- Kept the legacy system-UI fullscreen path for Android 6 through Android 10 and the API 33 back-dispatcher path for current Android versions.
- Quantum NMP bridge behavior, persistent OGG ownership, Quantum Asset Store policy, trusted headers and Game URL remain unchanged.
- Android `versionCode` increased to 9.

## 0.1.0-beta8

Android 16 startup crash fix, confirmed on real hardware.

- Root cause confirmed in `MainActivity.enterImmersiveMode()`: immersive mode was requested before the Activity decor view existed, causing Android 16 `PhoneWindow.getInsetsController()` to throw a `NullPointerException` during `onCreate()`.
- `configureWindow()` no longer enters immersive mode before `setContentView()`.
- Immersive mode now obtains `WindowInsetsController` from the actual `DecorView` and posts the system-bar request until that view is ready.
- Restored the normal `MainActivity` launcher after successful beta8 diagnostic testing on a Samsung Galaxy S25+ running Android 16.
- Removed the temporary diagnostic launcher, crash logger and diagnostic `Application` subclass from the product build.
- Confirmed the fixed full wrapper starts in both normal phone mode and Samsung DeX; DeX desktop/freeform sizing remains best-effort.
- Kept the beta6 framework `MediaPlayer` Quantum NMP runtime, persistent campaign OGG store and Quantum Asset Store unchanged while service-based background playback is redesigned separately.
- Android `versionCode` remains 8 for the finalized beta8 build.

## 0.1.0-beta7-diagnostic

Startup-isolation build after beta6 still force-closed on real hardware in both normal Android and Samsung DeX.

- Added a dedicated launcher activity that did not inflate the normal wrapper WebView on startup.
- Added a process-wide uncaught Java exception logger stored in app-private files.
- Added staged tests for raw WebView creation and wrapper component initialization.
- Added a separate action to start the full `MainActivity` only after the diagnostic shell was already open.
- Added selectable/copyable diagnostic output and persisted crash display on the next launch.
- Captured the decisive Android 16 stack trace proving the crash occurred in pre-layout immersive-window setup.
- Android `versionCode` increased to 7.

This diagnostic build was temporary and is not the normal product launcher.

## 0.1.0-beta6

Emergency runtime rollback during startup-crash investigation.

- Removed Media3/ExoPlayer runtime dependencies from the wrapper while isolating the repeated cold-start force-close.
- Removed the MediaSessionService and MediaController client layer.
- Restored the beta3 Android framework `MediaPlayer` implementation for Quantum NMP.
- Restored the pre-Media3 manifest without foreground media-service declarations.
- Kept persistent campaign OGG storage, trusted-source validation, enabled/volume preferences and the existing `window.QuantumNMP` bridge contract unchanged.
- Kept Quantum Asset Store unchanged.
- Android `versionCode` increased to 6.

Later beta7 diagnostics proved the startup crash itself was caused by immersive-mode timing in `MainActivity`, not by Media3 initialization. The simpler `MediaPlayer` runtime is nevertheless retained in beta8 as the known-good playback baseline until background/lock-screen playback is reintroduced behind its own test cycle.

## 0.1.0-beta5

Startup crash hardening attempted during the Media3 investigation.

- Removed eager Media3 controller binding from `MainActivity` startup by creating the session client only when native soundtrack playback was actually requested.
- Added a fail-safe around Media3 session client creation so an initialization failure could not take down the Game Activity.
- Hardened `QuantumMediaPlaybackService` startup so a Media3/ExoPlayer initialization failure was logged and contained.
- Existing `window.QuantumNMP` bridge methods, campaign OGG persistence and trusted-source policy remained unchanged.
- Android `versionCode` increased to 5.

The later beta7 diagnostic trace showed this did not address the real startup fault because the crash happened earlier in immersive-window initialization.

## 0.1.0-beta4

Media3 background playback experiment for Quantum NMP.

- Replaced the Activity-owned Android framework `MediaPlayer` with Media3 ExoPlayer hosted by a dedicated `MediaSessionService`.
- Added a `MediaController` client layer so the existing `window.QuantumNMP` bridge contract could control the service without moving playback ownership back into the Activity.
- Active soundtrack playback was designed to continue while the Game Activity was backgrounded or the display was off.
- Media3 published standard Android media-session state for notification, lock-screen, headset/Bluetooth and other system playback controls.
- ExoPlayer used media audio attributes with audio-focus handling enabled.
- Campaign OGG persistence, trusted-source validation, enabled preference and volume preference remained owned by the existing Quantum NMP pipeline.
- Media notification titles were derived from campaign filenames through a small pure-Java metadata helper with unit tests.
- Added foreground-service and media-playback foreground-service manifest permissions and declared the Media3 session service.
- Added Media3 ExoPlayer/session dependencies.
- Android `versionCode` increased to 4.

The cold-start force-close observed during this phase was later traced to `MainActivity` immersive-mode timing rather than Media3 itself. Media3 is still deferred from beta8 so its background-playback behavior can be reintroduced and device-tested independently.

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
- Immersive Android fullscreen UI.
- Back navigation that respects WebView history.
- Retry overlay for main-frame loading/TLS/HTTP failures.
- WebView Safe Browsing enabled.
- Cleartext HTTP and mixed content disabled.
- CI for unit tests and debug APK generation.
- Quantum NMP beta bridge with native OGG-capable Android media playback that survives ordinary WebView page navigation.
- Persistent native music enabled/volume preferences exposed to the game through `window.QuantumNMP`.
- Starlight wrapper/app/version request markers on trusted top-level GET navigation.
- Trusted-origin checks for side-effecting native media bridge commands.
