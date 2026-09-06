# Beta test checklist

Run this checklist before replacing the current Web2App Pro build.

## Startup and session

- Fresh install opens `https://game.starlight-unit.de/index_01.html`
- Existing login flow completes
- Login session survives app restart
- Closing and reopening the app does not create a broken duplicate session
- App resumes correctly after being backgrounded for several minutes
- Android 16 cold start does not regress the beta8 immersive-mode lifecycle fix

## Navigation

- Internal Starlight Unit links stay inside the app
- External HTTPS links open in the system browser
- Android back navigates web history before closing the app
- Back exits HTML fullscreen before navigating away
- Unsupported custom schemes do not load inside the WebView

## Game rendering

- Game fills the screen without unwanted browser chrome
- Rotation does not reload or reset the active game state
- HTML audio starts as expected
- HTML fullscreen content enters and exits cleanly
- Keyboard input works without permanently exposing system bars

## Quantum NMP

- Campaign music starts through the existing `window.QuantumNMP` bridge
- Full-page Game navigation does not restart or duplicate the currently playing track
- `pause()`, `resume()`, `stop()`, enable/disable and volume behave consistently
- Looping tracks continue to loop while the wrapper process remains active
- Campaign OGG persistence survives normal app restarts and application updates
- Clearing Android app data removes persisted campaign media and preferences
- A failed persistent OGG download falls back safely instead of crashing the wrapper

Background notification, lock-screen and guaranteed screen-off/service-owned playback are not beta9 acceptance criteria. Those MediaSession capabilities remain deferred until the service-based NMP redesign is reintroduced as its own test scope.

## Quantum Asset Store

- First visit to a page loads `/assets/` resources normally from the live Game
- Revisit after warm-up renders the same images, sounds, fonts and other cached static assets without visual differences
- Versioned URLs such as `?v=1.1.2.18` do not reuse the cache entry of an older version
- Unversioned assets refresh after the native TTL instead of remaining pinned indefinitely
- Campaign music continues through Quantum NMP and is not duplicated by the generic Asset Store
- API/PHP/HTML requests are never served by the Asset Store
- A cache miss or failed warm-up never produces a blank page or blocks navigation
- Clearing Android app data removes the native Asset Store and the next launch repopulates it normally

## Files and downloads

- Standard HTML file upload opens Android's document picker
- Single file upload works
- Multiple file upload works when requested by the page
- HTTP(S) downloads begin through DownloadManager
- Logged-in downloads receive the active cookie session
- Android 6/API 23 falls back to app-owned external download storage when public storage permission is unavailable

## Failure behavior

- Airplane mode shows the retry screen instead of a blank WebView
- Retry succeeds after connectivity returns
- Invalid TLS certificates are rejected
- Cleartext HTTP content is never loaded inside the game WebView

## Device matrix

Minimum smoke matrix for beta9:

- Android 6.0 / API 23 emulator or device: cold start, login page, navigation, file picker, one download, one campaign OGG, one Asset Store revisit
- Android 8-10 device or emulator: legacy immersive UI path
- Android 11-12 device or emulator: `WindowInsetsController` path
- Android 13-15 device or emulator: modern back dispatcher + current WebView behavior
- Samsung Galaxy / Android 16: beta8 startup regression test
- Samsung DeX: startup only; desktop/freeform sizing remains best-effort

## CI gate

Before merge:

- `lintDebug` passes with `minSdk 23`
- unit tests pass
- debug APK assembles successfully
- no new unsupported-platform API call is suppressed merely to make lint green; version-specific APIs must be guarded or isolated

## Release identity

Before public distribution, confirm:

- Final Android application ID
- Permanent signing/upload key and backup
- Final launcher icon and store artwork
- Version code strategy
