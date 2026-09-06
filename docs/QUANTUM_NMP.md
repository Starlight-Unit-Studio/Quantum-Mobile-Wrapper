# Quantum NMP

Quantum NMP is the native music layer of Quantum Mobile Wrapper.

Its first purpose is the Starlight Unit game soundtrack: music must survive full-page WebView navigation instead of being recreated by every HTML document.

## Beta8 architecture

Beta8 uses the Android framework `MediaPlayer` runtime as the current playback baseline.

1. A trusted game page calls `window.QuantumNMP`.
2. `QuantumNativeMediaPlayer` stores the requested state and resolves the media source through `QuantumCampaignMediaStore`.
3. Campaign OGG files are reused from app-private persistent storage when available or downloaded and verified once when needed.
4. Android framework `MediaPlayer` performs playback inside the wrapper process.
5. Full-page WebView navigation does not recreate the native player, so soundtrack playback remains independent from HTML document lifetime.

The beta4/beta5 Media3 `MediaSessionService` experiment was removed while investigating repeated cold-start force-closes. Beta7 diagnostics later proved that the startup crash itself occurred earlier in `MainActivity` immersive-window setup and was not caused by Media3. Beta8 still keeps `MediaPlayer` so the now-confirmed startup fix and the playback-service redesign remain separate test scopes.

## JavaScript bridge

The bridge remains exposed as `window.QuantumNMP` and provides:

- `isAvailable()`
- `version()`
- `play(source, loop)`
- `pause()`
- `resume()`
- `stop()`
- `setEnabled(enabled)` / `isEnabled()`
- `setVolume(volume)` / `getVolume()`

The web contract is intentionally unchanged. The Game does not need to know which Android playback implementation is behind the bridge.

`source` may be absolute or relative to the current page, but playback is accepted only when both the current page and resolved media URL use HTTPS on the configured trusted Starlight domain.

The enabled state and volume are persisted in Android `SharedPreferences`. This gives the game a stable hook for its own settings UI without defining or duplicating the game's settings model inside the wrapper.

## Persistent campaign media

Campaign OGG files are stored under the app-private Android files directory in `quantum_nmp/campaign`. Files are written through a temporary `.part` file, validated for the `OggS` signature and then renamed into the persistent store.

This storage survives ordinary app restarts and application updates. Android removes it when the user clears app storage or uninstalls the app.

Quantum Asset Store deliberately excludes `/assets/sounds/campaign/`, so campaign music has exactly one native owner and is not duplicated between two persistent stores.

## Request headers

Trusted top-level GET navigation is loaded with these client markers:

- `X-Starlight-Wrapper: quantum-mobile-wrapper`
- `X-Starlight-Wrapper-Version: <wrapper version>`
- `X-Starlight-App: starlight-unit-game`

The initial URL and retry navigation also carry the same header set.

These headers identify the native client, but they are not authentication. HTTP headers can be spoofed by a desktop client. If the game later needs a security boundary rather than a user-facing browser gate, use a server-issued app session or another cryptographically verifiable bootstrap flow.

## Deliberate beta8 limits

Beta8 prioritizes reliable startup and persistent in-app soundtrack playback across WebView navigation. It does not currently provide MediaSession notification controls, lock-screen controls or guaranteed playback after the Activity/process is terminated.

Those capabilities can be reintroduced later behind a separately tested playback service without changing the existing JavaScript bridge.

The JavaScript interface remains intentionally narrow. Side-effecting bridge calls are accepted only while the top-level WebView is on a trusted HTTPS Starlight host.
