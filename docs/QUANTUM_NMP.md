# Quantum NMP

Quantum NMP is the native music layer of Quantum Mobile Wrapper.

Its first purpose is the Starlight Unit game soundtrack: music must survive full-page WebView navigation and normal Android backgrounding instead of being recreated by every HTML document or being tied to the Activity lifecycle.

## Beta4 architecture

Quantum NMP keeps the existing JavaScript bridge but moves playback ownership into Media3:

1. A trusted game page calls `window.QuantumNMP`.
2. `QuantumNativeMediaPlayer` stores the requested state and resolves the media source through `QuantumCampaignMediaStore`.
3. Campaign OGG files are reused from app-private persistent storage when available or downloaded and verified once when needed.
4. `QuantumMediaSessionClient` connects asynchronously to `QuantumMediaPlaybackService` with a Media3 `MediaController`.
5. `QuantumMediaPlaybackService` owns ExoPlayer and the `MediaSession`.
6. Android system media controls and the default media notification are driven by that session.

Because the player and session live in a service rather than `MainActivity`, active playback is no longer destroyed merely because the Activity is backgrounded, the display turns off, or WebView navigates between game pages.

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

The web contract is intentionally unchanged from beta1-beta3. The Game does not need to know whether Android playback is backed by framework `MediaPlayer`, ExoPlayer or another later service implementation.

`source` may be absolute or relative to the current page, but playback is accepted only when both the current page and resolved media URL use HTTPS on the configured trusted Starlight domain.

The enabled state and volume are persisted in Android `SharedPreferences`. This gives the game a stable hook for its own settings UI without defining or duplicating the game's settings model inside the wrapper.

## Background and system controls

The service is declared as a `mediaPlayback` foreground service and uses Media3's standard `MediaSessionService` notification handling. While playback is active, Android can expose play/pause controls through the system media UI, notification surface, lock screen, Bluetooth/headset controls and other compatible controllers.

Media items include a title derived from the campaign filename plus the artist `Starlight Unit Studios`. More detailed Game-provided track metadata and artwork can be added later without widening the current playback bridge unless the Game actually needs that control.

The default MediaSessionService task-removal behavior is kept: ongoing playback may remain active when the Activity leaves the recent-task UI, while inactive playback does not need to keep the service alive.

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

## Deliberate beta4 limits

Beta4 does not yet implement playback resumption after the MediaSessionService itself has been terminated or after a device reboot. It also does not expose a browsable Android media library, Cast integration, Game-supplied artwork, queue management or track seek commands through the JavaScript bridge.

Those capabilities can be layered onto the Media3 service later without collapsing the current separation between Game state, trusted media persistence and native playback.

The JavaScript interface remains intentionally narrow. Side-effecting bridge calls are accepted only while the top-level WebView is on a trusted HTTPS Starlight host.
