# Changelog

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
