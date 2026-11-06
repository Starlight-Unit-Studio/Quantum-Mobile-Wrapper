package de.starlightunit.wrapper.navigation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.view.Gravity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.starlightunit.wrapper.R;

/** Per-navigation-controller cache for bundled, bounded PNGs and original vector icons. */
public final class NativeNavigationIcons {
    private final Context context;
    private final Map<String, Bitmap> bitmaps = new HashMap<>();
    private final Set<String> unavailableAssets = new HashSet<>();

    public NativeNavigationIcons(Context context) {
        this.context = context;
    }

    public Drawable forState(String icon, String activeIcon, boolean selected, int color, int sizePx,
                             boolean reserveSlot) {
        String selectedReference = NativeNavigationIconPolicy.forState(icon, activeIcon, selected);
        Drawable drawable = load(selectedReference, color, sizePx);
        String normalReference = NativeNavigationIconPolicy.normalize(icon);
        if (drawable == null && selected && !selectedReference.equals(normalReference)) {
            drawable = load(normalReference, color, sizePx);
        }
        if (drawable == null && reserveSlot) {
            drawable = new ColorDrawable(Color.TRANSPARENT);
        }
        if (drawable != null) {
            drawable.setBounds(0, 0, sizePx, sizePx);
        }
        return drawable;
    }

    private Drawable load(String reference, int color, int sizePx) {
        int resource = builtinResource(reference);
        if (resource != 0) {
            Drawable drawable = context.getDrawable(resource).mutate();
            drawable.setTint(color);
            return drawable;
        }
        String path = NativeNavigationIconPolicy.assetPath(reference);
        if (path.isEmpty() || unavailableAssets.contains(path)) {
            return null;
        }
        Bitmap bitmap = bitmaps.get(path);
        if (bitmap == null) {
            bitmap = readPng(path);
            if (bitmap == null) {
                unavailableAssets.add(path);
                return null;
            }
            bitmaps.put(path, bitmap);
        }
        BitmapDrawable image = new BitmapDrawable(context.getResources(), bitmap);
        image.setGravity(Gravity.FILL);
        image.setFilterBitmap(true);
        float scale = sizePx / (float) Math.max(bitmap.getWidth(), bitmap.getHeight());
        int width = Math.max(1, Math.round(bitmap.getWidth() * scale));
        int height = Math.max(1, Math.round(bitmap.getHeight() * scale));
        int left = (sizePx - width) / 2;
        int top = (sizePx - height) / 2;
        return new InsetDrawable(image, left, top, sizePx - width - left, sizePx - height - top);
    }

    private Bitmap readPng(String path) {
        try (InputStream input = context.getAssets().open(path);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            int total = 0;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > NativeNavigationIconPolicy.MAX_PNG_BYTES) {
                    return null;
                }
                output.write(buffer, 0, count);
            }
            byte[] bytes = output.toByteArray();
            if (!isPng(bytes)) {
                return null;
            }
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
            if (bounds.outWidth < 1 || bounds.outHeight < 1
                    || bounds.outWidth > NativeNavigationIconPolicy.MAX_PNG_DIMENSION
                    || bounds.outHeight > NativeNavigationIconPolicy.MAX_PNG_DIMENSION) {
                return null;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            if (bitmap != null) {
                bitmap.setDensity(Bitmap.DENSITY_NONE);
            }
            return bitmap;
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private static boolean isPng(byte[] bytes) {
        return bytes.length >= 24 && (bytes[0] & 0xff) == 0x89
                && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10;
    }

    private static int builtinResource(String reference) {
        switch (reference) {
            case "home": return R.drawable.quantum_nav_home;
            case "search": return R.drawable.quantum_nav_search;
            case "grid": return R.drawable.quantum_nav_grid;
            case "star": return R.drawable.quantum_nav_star;
            case "heart": return R.drawable.quantum_nav_heart;
            case "user": return R.drawable.quantum_nav_user;
            case "settings": return R.drawable.quantum_nav_settings;
            case "info": return R.drawable.quantum_nav_info;
            case "mail": return R.drawable.quantum_nav_mail;
            case "cart": return R.drawable.quantum_nav_cart;
            case "calendar": return R.drawable.quantum_nav_calendar;
            case "camera": return R.drawable.quantum_nav_camera;
            case "map": return R.drawable.quantum_nav_map;
            case "phone": return R.drawable.quantum_nav_phone;
            case "download": return R.drawable.quantum_nav_download;
            case "help": return R.drawable.quantum_nav_help;
            case "play": return R.drawable.quantum_nav_play;
            case "folder": return R.drawable.quantum_nav_folder;
            case "globe": return R.drawable.quantum_nav_globe;
            case "link": return R.drawable.quantum_nav_link;
            default: return 0;
        }
    }

    public void clear() {
        // Views may still hold drawables while being detached; never recycle their bitmaps here.
        bitmaps.clear();
        unavailableAssets.clear();
    }
}
