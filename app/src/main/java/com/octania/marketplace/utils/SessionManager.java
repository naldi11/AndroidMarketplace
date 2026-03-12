package com.octania.marketplace.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;

public class SessionManager {
    private static final String PREF_NAME = "MarketplaceSession";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";

    public static final String KEY_NAME = "userName";
    public static final String KEY_EMAIL = "userEmail";
    public static final String KEY_PHONE = "userPhone";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(String token, int userId) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_TOKEN, token);
        editor.putInt(KEY_USER_ID, userId);
        editor.commit();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getToken() {
        return pref.getString(KEY_TOKEN, null);
    }

    public int getUserId() {
        return pref.getInt(KEY_USER_ID, 0);
    }

    public void logoutUser() {
        editor.clear();
        editor.commit();
    }

    public void saveUser(com.octania.marketplace.data.model.User user) {
        if (user != null) {
            editor.putString(KEY_NAME, user.getName());
            editor.putString(KEY_EMAIL, user.getEmail());
            editor.putString(KEY_PHONE, user.getPhone());
            editor.apply();
        }
    }

    public HashMap<String, String> getUserDetails() {
        HashMap<String, String> user = new HashMap<>();
        user.put(KEY_NAME, pref.getString(KEY_NAME, null));
        user.put(KEY_EMAIL, pref.getString(KEY_EMAIL, null));
        user.put(KEY_PHONE, pref.getString(KEY_PHONE, null));
        return user;
    }
}
