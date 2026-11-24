package com.example.myapplication.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import com.example.myapplication.repository.SessionStore;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class KeystoreSession implements SessionStore {
    private static final String ALIAS = "findit-session-v1";
    private static final String TOKEN = "session";
    private final SharedPreferences preferences;

    public KeystoreSession(Context context) {
        preferences = context.getSharedPreferences("findit_session", Context.MODE_PRIVATE);
    }

    @Override public long read() {
        String stored = preferences.getString(TOKEN, null);
        if (stored == null) return 0;
        try {
            String[] parts = stored.split(":");
            Base64.Decoder decoder = Base64.getDecoder();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, decoder.decode(parts[0])));
            ByteBuffer payload = ByteBuffer.wrap(cipher.doFinal(decoder.decode(parts[1])));
            long userId = payload.getLong();
            if (payload.getLong() > System.currentTimeMillis() && userId > 0) return userId;
        } catch (GeneralSecurityException | IllegalArgumentException | java.nio.BufferUnderflowException ignored) {
            // Treat invalid or inaccessible session data as signed out.
        }
        clear();
        return 0;
    }

    @Override public void save(long userId) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key());
            byte[] payload = ByteBuffer.allocate(16).putLong(userId)
                    .putLong(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)).array();
            Base64.Encoder encoder = Base64.getEncoder();
            String stored = encoder.encodeToString(cipher.getIV()) + ":"
                    + encoder.encodeToString(cipher.doFinal(payload));
            if (!preferences.edit().putString(TOKEN, stored).commit()) {
                throw new IllegalStateException("Sesi tidak dapat disimpan.");
            }
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("Sesi aman tidak dapat dibuat.", failure);
        }
    }

    @Override public void clear() {
        if (!preferences.edit().remove(TOKEN).commit()) {
            throw new IllegalStateException("Sesi tidak dapat diakhiri. Coba lagi.");
        }
    }

    private SecretKey key() throws GeneralSecurityException {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        try {
            store.load(null);
        } catch (java.io.IOException failure) {
            throw new GeneralSecurityException(failure);
        }
        SecretKey existing = (SecretKey) store.getKey(ALIAS, null);
        if (existing != null) return existing;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT
                | KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());
        return generator.generateKey();
    }
}
