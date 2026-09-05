# Quantum NMP

Quantum NMP is the native music layer of Quantum Mobile Wrapper.

Its first purpose is the Starlight Unit game soundtrack: music must survive full-page WebView navigation instead of being recreated by every HTML document.

## Beta architecture

The beta implementation uses Android's framework `MediaPlayer` behind a small JavaScript bridge. The web application chooses when to start, pause, resume or disable music. The player belongs to the Android Activity, so ordinary page navigation does not destroy it.

The bridge is exposed as `window.QuantumNMP` and currently provides:

- `isAvailable()`
- `version()`
- `play(source, loop)`
- `pause()`
- `resume()`
- `stop()`
- `setEnabled(enabled)` / `isEnabled()`
- `setVolume(volume)` / `getVolume()`

`source` may be absolute or relative to the current page, but playback is accepted only when both the current page and resolved media URL use HTTPS on the configured trusted Starlight domain.

The enabled state and volume are persisted in Android `SharedPreferences`. This gives the game a stable hook for its own settings UI without defining or duplicating the game's settings model inside the wrapper.

## Request headers

Trusted top-level GET navigation is loaded with these client markers:

- `X-Starlight-Wrapper: quantum-mobile-wrapper`
- `X-Starlight-Wrapper-Version: <wrapper version>`
- `X-Starlight-App: starlight-unit-game`

The initial URL and retry navigation also carry the same header set.

These headers identify the native client, but they are not authentication. HTTP headers can be spoofed by a desktop client. If the game later needs a security boundary rather than a user-facing browser gate, use a server-issued app session or another cryptographically verifiable bootstrap flow.

## Deliberate beta limits

Quantum NMP currently targets uninterrupted in-app music across game pages. It is not yet a full Android media-session service. Lock-screen controls, notification controls, process-independent background playback, audio-focus policy and Media3/ExoPlayer migration belong to a later module revision behind the same bridge contract.

The JavaScript interface is intentionally narrow and only controls audio state. Side-effecting bridge calls are accepted only while the top-level WebView is on a trusted HTTPS Starlight host.
