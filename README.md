# STU Mobile Wrapper

Native Android shell for Starlight Unit web applications.

## Beta target

The first app target is:

`https://game.starlight-unit.de/index_01.html`

Package: `de.starlightunit.game`

Version: `0.1.0-beta1`

## Included in the first beta shell

- Secure HTTPS-only WebView configuration
- JavaScript and DOM storage for the existing game client
- Cookies and third-party cookies for web compatibility
- Internal navigation for `starlight-unit.de` and its subdomains
- External links handled by Android instead of silently loading inside the game
- File picker support through HTML file inputs
- DownloadManager integration with session cookies
- HTML fullscreen support for media and game content
- Immersive Android fullscreen mode
- Back button integration with WebView history
- Offline and main-frame error overlay with retry
- Release builds with WebView debugging disabled
- Unit-tested domain navigation policy
- GitHub Actions build for API 36

## Configuration

Runtime product values are centralized in:

`app/src/main/java/de/starlightunit/wrapper/config/AppConfig.java`

This keeps the shell reusable without scattering URLs and host rules through Activity code.

## Build

Requirements:

- JDK 17 or newer
- Android SDK 36
- Android Build Tools 36.0.0

Linux/macOS:

```bash
./gradlew test assembleDebug
```

Windows:

```bat
gradlew.bat test assembleDebug
```

The wrapper JAR is bootstrapped once from the official Gradle repository and verified against the Gradle 9.6.0 wrapper SHA-256 checksum.

Debug APK output:

`app/build/outputs/apk/debug/app-debug.apk`

## Signing

No release keystore is stored in this repository. Release signing should be supplied by CI secrets or a local `keystore.properties` setup before public distribution.

## Branding note

The current launcher star is a temporary neutral placeholder. Replace it with the approved Starlight Unit app artwork before the public store release.

## License

STU Mobile Wrapper is source-available under the **Starlight Unit Studios Mobile Wrapper Community Source License 1.0**.

Important practical distinction:

- You may build and commercially distribute your own generated application.
- Your generated application may use entirely independent branding.
- Using an unmodified wrapper build does not require you to publish unrelated app, website or backend source code.
- Publicly distributed modifications to the wrapper itself must make the corresponding modified wrapper source available under the same license.
- The wrapper itself may not be resold or operated as a paid web-to-app, APK/AAB-generation or white-label wrapper service without individual written permission.

The legally controlling license text is `LICENSE.de.md`. See `LICENSE.md`, `COMMUNITY_POLICY.md`, `NOTICE.md`, `COPYRIGHT.md`, `TRADEMARKS.md` and `LICENSE_HISTORY.md` for the complete project notices.
