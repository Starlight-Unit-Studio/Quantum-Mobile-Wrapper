# Quantum Mobile Wrapper

[![DOI](https://zenodo.org/badge/1358596056.svg)](https://doi.org/10.5281/zenodo.22420025)

Native Android shell for Starlight web applications.

## Beta target

The first app target is:

`https://game.starlight-unit.de/index_01.html`

Package: `de.starlightunit.game`

Version: `0.1.0-beta3`

## Included in the current beta shell

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
- Quantum NMP native music bridge for persistent in-app soundtrack playback
- Native handling for the complete `/assets/sounds/campaign/` media tree
- Campaign `.ogg` files are downloaded once into app-private persistent data storage instead of WebView/cache storage
- Persisted campaign audio survives normal app restarts and application updates
- Quantum Asset Store for trusted static Game resources below `/assets/`
- Native asset warm-up after page load and cache-hit interception on later navigations
- URL-versioned cache keys so Game cachebuster query strings produce separate persistent entries
- Asset Store safety limits of 64 MiB per entry and 256 MiB total
- Starlight wrapper/app/version request markers on trusted top-level GET navigation

## Configuration

Runtime product values are centralized in:

`app/src/main/java/de/starlightunit/wrapper/config/AppConfig.java`

This keeps the shell reusable without scattering URLs and host rules through Activity code.

Quantum NMP is documented in `docs/QUANTUM_NMP.md`. Its bridge is exposed to trusted game pages as `window.QuantumNMP`.

Quantum Asset Store is documented in `docs/QUANTUM_ASSET_STORE.md`. Generic Game assets are stored under app-private `files/quantum_assets`. Campaign soundtrack files are excluded from this generic store so Quantum NMP remains the single owner of persistent campaign audio.

Campaign OGG files managed by Quantum NMP are stored under the app-private Android files directory in `quantum_nmp/campaign`. This is persistent application data, not cache. Android removes it when the user clears app storage or uninstalls the application.

The custom request headers identify the wrapper to the web application, but they are intentionally documented as client markers rather than authentication because arbitrary HTTP clients can spoof static headers.

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

The current launcher star is a temporary neutral placeholder. Replace it with the approved Starlight Unit game artwork before the public store release.

## License

Quantum Mobile Wrapper is source-available under the **Starlight Unit Studios Mobile Wrapper Community Source License 1.0**.

Important practical distinction:

- You may build and commercially distribute your own generated application.
- Your generated application may use entirely independent branding.
- Using an unmodified wrapper build does not require you to publish unrelated app, website or backend source code.
- Publicly distributed modifications to the wrapper itself must make the corresponding modified wrapper source available under the same license.
- The wrapper itself may not be resold or operated as a paid web-to-app, APK/AAB-generation or white-label wrapper service without individual written permission.

The legally controlling license text is `LICENSE.de.md`. See `LICENSE.md`, `COMMUNITY_POLICY.md`, `NOTICE.md`, `COPYRIGHT.md`, `TRADEMARKS.md` and `LICENSE_HISTORY.md` for the complete project notices.
