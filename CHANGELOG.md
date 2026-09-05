# Changelog

## 0.1.0-beta1 - 2026-09-05

Initial native Android beta shell for Starlight Unit.

### Added

- Direct launch into `https://game.starlight-unit.de/index_01.html`
- HTTPS-only WebView with Safe Browsing and mixed-content blocking
- Cookie and DOM storage compatibility for the existing web game
- STU-domain navigation policy with lookalike-domain protection
- External browser and app handoff for non-STU links and supported schemes
- HTML file chooser support
- Authenticated DownloadManager integration
- HTML fullscreen and immersive Android fullscreen handling
- WebView-aware system back navigation including Android 13+ back callbacks
- Offline/main-frame failure screen with retry
- Debug-only WebView inspection
- Unit tests for domain navigation policy
- GitHub Actions build targeting Android 16 / API 36

### Known limits

- Launcher artwork is a temporary neutral placeholder
- Camera capture from HTML file inputs is not wired yet
- Blob/data downloads are not exported through DownloadManager
- Release signing is intentionally not configured until the permanent package ID and signing key are confirmed
