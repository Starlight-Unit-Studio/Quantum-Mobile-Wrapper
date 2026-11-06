package de.starlightunit.wrapper.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.webkit.CookieManager;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import de.starlightunit.wrapper.config.AppConfig;

/**
 * Persists cookies for the trusted game origin across WebView process restarts.
 *
 * PHP deliberately emits a session cookie (lifetime=0), which is normally lost
 * when Android destroys the WebView process. Quantum stores the trusted-origin
 * cookie header encrypted with an Android Keystore AES/GCM key and restores it
 * before the first navigation. No cookie value is written to disk in plaintext.
 */
public final class QuantumSessionCookieStore {
    private static final String PREFS = "quantum_session_cookie_store";
    private static final String PREF_PAYLOAD = "trusted_cookie_payload";
    private static final String KEY_ALIAS = "quantum_trusted_session_cookie_key_v1";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;

    private final SharedPreferences preferences;
    private final CookieManager cookieManager;

    public QuantumSessionCookieStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        cookieManager = CookieManager.getInstance();
    }

    public void restore() {
        String encoded = preferences.getString(PREF_PAYLOAD, null);
        if (encoded == null || encoded.isEmpty()) {
            return;
        }

        try {
            String cookieHeader = decrypt(encoded);
            List<String> cookies = splitCookieHeader(cookieHeader);
            for (String cookie : cookies) {
                cookieManager.setCookie(
                        AppConfig.START_URL,
                        cookie + "; Path=/; Secure; HttpOnly; SameSite=Lax"
                );
            }
            cookieManager.flush();
        } catch (Exception ignored) {
            // A rotated/invalid keystore entry must never block app startup.
            clear();
        }
    }

    public void capture() {
        String cookieHeader = cookieManager.getCookie(AppConfig.START_URL);
        if (cookieHeader == null || cookieHeader.trim().isEmpty()) {
            clear();
            cookieManager.flush();
            return;
        }

        try {
            preferences.edit().putString(PREF_PAYLOAD, encrypt(cookieHeader)).apply();
            cookieManager.flush();
        } catch (Exception ignored) {
            // Persistence is best-effort; the active in-memory WebView session remains usable.
        }
    }

    public void clear() {
        preferences.edit().remove(PREF_PAYLOAD).apply();
    }

    private String encrypt(String plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();

        byte[] payload = new byte[1 + iv.length + ciphertext.length];
        payload[0] = (byte) iv.length;
        System.arraycopy(iv, 0, payload, 1, iv.length);
        System.arraycopy(ciphertext, 0, payload, 1 + iv.length, ciphertext.length);
        return Base64.encodeToString(payload, Base64.NO_WRAP);
    }

    private String decrypt(String encoded) throws Exception {
        byte[] payload = Base64.decode(encoded, Base64.NO_WRAP);
        if (payload.length < 2) {
            throw new IllegalArgumentException("Invalid encrypted cookie payload");
        }

        int ivLength = payload[0] & 0xff;
        if (ivLength < 1 || payload.length <= 1 + ivLength) {
            throw new IllegalArgumentException("Invalid encrypted cookie IV");
        }

        byte[] iv = new byte[ivLength];
        byte[] ciphertext = new byte[payload.length - 1 - ivLength];
        System.arraycopy(payload, 1, iv, 0, ivLength);
        System.arraycopy(payload, 1 + ivLength, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        java.security.Key existing = keyStore.getKey(KEY_ALIAS, null);
        if (existing instanceof SecretKey) {
            return (SecretKey) existing;
        }

        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return generator.generateKey();
    }

    private static List<String> splitCookieHeader(String header) {
        List<String> cookies = new ArrayList<>();
        for (String part : header.split(";")) {
            String cookie = part.trim();
            int equals = cookie.indexOf('=');
            if (equals > 0) {
                cookies.add(cookie);
            }
        }
        return cookies;
    }
}
