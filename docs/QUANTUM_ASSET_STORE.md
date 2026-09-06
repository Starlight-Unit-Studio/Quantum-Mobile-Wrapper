# Quantum Asset Store

Quantum Asset Store is the wrapper-owned persistent cache for trusted static Game resources below `/assets/`.

It is intentionally separate from Quantum NMP. Campaign soundtrack files below `/assets/sounds/campaign/` remain owned by the dedicated native media store because that module verifies OGG streams and hands local files directly to the persistent player.

## Request flow

1. WebView performs the first live request normally.
2. After a trusted page finishes loading, `QuantumAssetWarmup` collects resource URLs already used by the page.
3. `QuantumAssetPolicy` accepts only supported static HTTPS resources on `game.starlight-unit.de` below `/assets/`.
4. `QuantumAssetStore` downloads accepted resources into app-private `files/quantum_assets` storage.
5. On later navigations, `GameWebViewClient.shouldInterceptRequest()` serves a fresh native cache entry before WebView reaches the network.
6. A miss, stale entry, unsupported type or any native cache failure returns control to WebView. The Asset Store must never become a mandatory proxy for Game traffic.

## Security boundaries

The first implementation deliberately rejects:

- cleartext HTTP;
- hosts other than the configured Game host;
- credentials in URLs;
- non-standard HTTPS ports;
- encoded path traversal or encoded separators;
- resources outside `/assets/`;
- PHP, HTML and API traffic;
- unknown file types;
- campaign soundtrack paths owned by Quantum NMP;
- redirects;
- responses that resolve to HTML error/login pages.

Wrapper request markers and the current WebView cookie are reused for native warm-up requests where available.

## Persistence and invalidation

Cache filenames are SHA-256-derived from the complete normalized URL, including its query string. A Game asset such as `ship.webp?v=1.1.2.18` therefore receives a different persistent entry from `ship.webp?v=1.1.2.19`.

Versioned resources are considered fresh for up to 30 days. Unversioned resources are considered fresh for 24 hours so a server-side replacement cannot remain pinned indefinitely. The native cache is capped at 64 MiB per resource and 256 MiB total. Oldest entries are evicted first when the total budget is exceeded.

The store uses the app-private files directory. Entries survive normal restarts and application updates, but Android removes them when application data is cleared or the app is uninstalled.

## Why cache hits are interception-only

The wrapper does not proxy every `/assets/` response through Java. A full proxy would have to reproduce cookies, redirects, content encoding, range requests, CORS behavior and browser caching semantics and would create a much larger failure surface.

Instead, the live WebView request remains authoritative on a cache miss. Native interception is used only when a complete fresh entry is already available. This keeps the cache an optimization rather than a dependency.
