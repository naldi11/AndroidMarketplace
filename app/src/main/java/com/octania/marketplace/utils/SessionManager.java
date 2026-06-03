package com.octania.marketplace.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;

public class SessionManager {
    private static final String PREF_NAME = "MarketplaceSession";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_ACTIVE_ROLE = "activeRole";
    private static final String KEY_WISHLIST_COUNT = "wishlistCount";
    private static final String KEY_CART_COUNT = "cartCount";
    private static final String KEY_DISPUTE_COUNT = "disputeCount";

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

    public void createLoginSession(String token, int userId, String role) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_TOKEN, token);
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_ACTIVE_ROLE, role);
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

    public String getActiveRole() {
        return pref.getString(KEY_ACTIVE_ROLE, "buyer"); // default buyer
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

    /** Simpan badge count agar bisa dibaca dari semua activity */
    public void saveBadgeCounts(int wishlistCount, int cartCount) {
        editor.putInt(KEY_WISHLIST_COUNT, wishlistCount);
        editor.putInt(KEY_CART_COUNT, cartCount);
        editor.apply();
    }

    public int getWishlistCount() {
        return pref.getInt(KEY_WISHLIST_COUNT, 0);
    }

    public int getCartCount() {
        return pref.getInt(KEY_CART_COUNT, 0);
    }

    public void saveDisputeCount(int disputeCount) {
        editor.putInt(KEY_DISPUTE_COUNT, disputeCount);
        editor.apply();
    }

    public int getDisputeCount() {
        return pref.getInt(KEY_DISPUTE_COUNT, 0);
    }
}
