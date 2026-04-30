package com.octania.marketplace.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Secure Token Storage using EncryptedSharedPreferences
 * For storing sensitive data like JWT tokens
 */
public class SecureTokenManager {
    
    private static final String PREFS_FILE_NAME = "secure_tokens";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_TOKEN_EXPIRY = "token_expiry";
    
    private final EncryptedSharedPreferences encryptedPrefs;
    
    public SecureTokenManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
            
            encryptedPrefs = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to initialize secure storage", e);
        }
    }
    
    /**
     * Save access token securely
     */
    public void saveAccessToken(String token) {
        encryptedPrefs.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .apply();
    }
    
    /**
     * Get access token
     */
    public String getAccessToken() {
        return encryptedPrefs.getString(KEY_ACCESS_TOKEN, null);
    }
    
    /**
     * Save refresh token securely
     */
    public void saveRefreshToken(String token) {
        encryptedPrefs.edit()
            .putString(KEY_REFRESH_TOKEN, token)
            .apply();
    }
    
    /**
     * Get refresh token
     */
    public String getRefreshToken() {
        return encryptedPrefs.getString(KEY_REFRESH_TOKEN, null);
    }
    
    /**
     * Save token expiry time
     */
    public void saveTokenExpiry(long expiryTimeMillis) {
        encryptedPrefs.edit()
            .putLong(KEY_TOKEN_EXPIRY, expiryTimeMillis)
            .apply();
    }
    
    /**
     * Check if token is expired
     */
    public boolean isTokenExpired() {
        long expiry = encryptedPrefs.getLong(KEY_TOKEN_EXPIRY, 0);
        return System.currentTimeMillis() >= expiry;
    }
    
    /**
     * Clear all tokens (logout)
     */
    public void clearTokens() {
        encryptedPrefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_TOKEN_EXPIRY)
            .apply();
    }
    
    /**
     * Check if tokens exist
     */
    public boolean hasTokens() {
        return getAccessToken() != null;
    }
}
