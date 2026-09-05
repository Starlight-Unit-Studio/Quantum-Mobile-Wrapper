# Beta test checklist

Run this checklist before replacing the current Web2App Pro build.

## Startup and session

- Fresh install opens `https://game.starlight-unit.de/index_01.html`
- Existing login flow completes
- Login session survives app restart
- Closing and reopening the app does not create a broken duplicate session
- App resumes correctly after being backgrounded for several minutes

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

## Files and downloads

- Standard HTML file upload opens Android's document picker
- Single file upload works
- Multiple file upload works when requested by the page
- HTTP(S) downloads begin through DownloadManager
- Logged-in downloads receive the active cookie session

## Failure behavior

- Airplane mode shows the retry screen instead of a blank WebView
- Retry succeeds after connectivity returns
- Invalid TLS certificates are rejected
- Cleartext HTTP content is never loaded inside the game WebView

## Device matrix

Minimum useful smoke matrix for the public beta:

- Samsung Galaxy / current Android
- Android 16 / API 36 emulator or device
- Android 14 or 15 device
- One older Android 8-10 device if the beta audience includes them

## Release identity

Before public distribution, confirm:

- Final Android application ID
- Permanent signing/upload key and backup
- Final launcher icon and store artwork
- Version code strategy
