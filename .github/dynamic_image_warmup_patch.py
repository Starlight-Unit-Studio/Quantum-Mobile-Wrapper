from pathlib import Path

ROOT = Path('.')

store_path = ROOT / 'app/src/main/java/de/starlightunit/wrapper/assets/QuantumAssetStore.java'
store = store_path.read_text(encoding='utf-8')
anchor = '''    public File getAssetDirectory() {
        return assetDirectory;
    }
'''
method = '''    /**
     * Opportunistically warms a trusted image request that missed the native
     * cache while leaving the current WebView request untouched. This is used
     * for images assigned dynamically after onPageFinished(), such as game
     * combat portraits that the page-level warm-up could not discover yet.
     */
    public void prefetchImageMiss(String source, Map<String, String> requestHeaders) {
        if (closed) {
            return;
        }
        QuantumAssetPolicy.AssetSpec spec = policy.inspect(source);
        if (spec == null || !spec.getMimeType().startsWith("image/")) {
            return;
        }
        prefetch(spec, requestHeaders);
    }

    public File getAssetDirectory() {
        return assetDirectory;
    }
'''
if anchor not in store:
    raise SystemExit('QuantumAssetStore insertion anchor not found')
store_path.write_text(store.replace(anchor, method, 1), encoding='utf-8')

client_path = ROOT / 'app/src/main/java/de/starlightunit/wrapper/web/GameWebViewClient.java'
client = client_path.read_text(encoding='utf-8')
old = '''        if (!request.isForMainFrame()
                && "GET".equalsIgnoreCase(request.getMethod())
                && !hasHeader(request.getRequestHeaders(), "Range")) {
            WebResourceResponse cached = assetStore.openCachedResponse(request.getUrl().toString());
            if (cached != null) {
                return cached;
            }
        }
'''
new = '''        if (!request.isForMainFrame()
                && "GET".equalsIgnoreCase(request.getMethod())
                && !hasHeader(request.getRequestHeaders(), "Range")) {
            String source = request.getUrl().toString();
            WebResourceResponse cached = assetStore.openCachedResponse(source);
            if (cached != null) {
                return cached;
            }

            // Dynamic images can appear long after onPageFinished(), so they
            // are invisible to QuantumAssetWarmup.capture(). Warm a trusted
            // image miss in parallel while WebView still performs its normal
            // request. A later retry/navigation can then be served natively.
            assetStore.prefetchImageMiss(source, requestHeaders);
        }
'''
if old not in client:
    raise SystemExit('GameWebViewClient intercept anchor not found')
client_path.write_text(client.replace(old, new, 1), encoding='utf-8')

changelog_path = ROOT / 'CHANGELOG.md'
changelog = changelog_path.read_text(encoding='utf-8')
entry = '''# Changelog

## Unreleased

Dynamic image recovery through Quantum Asset Store.

- Trusted image requests below `/assets/` that miss the native store are now warmed immediately in parallel while WebView keeps its ordinary live request path.
- This covers images assigned after `onPageFinished()` (for example rotating combat/opponent portraits), which cannot be discovered by the existing page-finished resource scan.
- Only `image/*` asset types use the miss warm-up path; CSS, JavaScript, audio, video, API and HTML traffic keep their existing behavior.
- Existing host/path policy, cookie/request-header forwarding, file-size limits, TTLs and cache budget remain unchanged.
- The change is additive and API-23 compatible; a failed native warm-up never blocks the WebView request.

'''
if not changelog.startswith('# Changelog\n'):
    raise SystemExit('CHANGELOG heading not found')
changelog_path.write_text(entry + changelog[len('# Changelog\n\n'):], encoding='utf-8')
