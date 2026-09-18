package de.starlightunit.wrapper.download;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.widget.Toast;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import de.starlightunit.wrapper.R;

public final class AppDownloadListener implements DownloadListener {
    private final Context context;
    private final Map<String, String> requestHeaders;

    public AppDownloadListener(Context context) {
        this(context, Collections.emptyMap());
    }

    public AppDownloadListener(Context context, Map<String, String> requestHeaders) {
        this.context = context;
        this.requestHeaders = requestHeaders == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(requestHeaders));
    }

    @Override
    public void onDownloadStart(
            String url,
            String userAgent,
            String contentDisposition,
            String mimeType,
            long contentLength
    ) {
        if (url == null || (!url.startsWith("https://") && !url.startsWith("http://"))) {
            Toast.makeText(context, R.string.download_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(fileName);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            if (mimeType != null && !mimeType.trim().isEmpty()) {
                request.setMimeType(mimeType);
            }
            if (userAgent != null && !userAgent.trim().isEmpty()) {
                request.addRequestHeader("User-Agent", userAgent);
            }

            for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
                if (isDownloadSafeHeader(header.getKey(), header.getValue())) {
                    request.addRequestHeader(header.getKey(), header.getValue());
                }
            }

            String cookies = CookieManager.getInstance().getCookie(url);
            if (cookies != null && !cookies.trim().isEmpty()) {
                request.addRequestHeader("Cookie", cookies);
            }

            if (canWritePublicDownloads()) {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            } else {
                request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName);
            }

            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager == null) {
                throw new IllegalStateException("DownloadManager unavailable");
            }
            manager.enqueue(request);
            Toast.makeText(context, R.string.download_started, Toast.LENGTH_SHORT).show();
        } catch (RuntimeException ignored) {
            Toast.makeText(context, R.string.download_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private static boolean isDownloadSafeHeader(String name, String value) {
        if (name == null || value == null || name.trim().isEmpty() || value.trim().isEmpty()) {
            return false;
        }
        if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            return false;
        }
        String normalized = name.trim().toLowerCase(java.util.Locale.ROOT);
        return !"cookie".equals(normalized)
                && !"user-agent".equals(normalized)
                && !"host".equals(normalized)
                && !"content-length".equals(normalized)
                && !"connection".equals(normalized);
    }

    private boolean canWritePublicDownloads() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true;
        }
        return context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }
}
