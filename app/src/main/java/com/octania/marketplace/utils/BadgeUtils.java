package com.octania.marketplace.utils;

import android.app.Activity;
import android.content.Context;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.octania.marketplace.R;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Helper untuk menampilkan badge wishlist & cart di BottomNavigationView
 * secara konsisten di semua Activity.
 *
 * Cara pakai di setiap Activity yang punya BottomNav:
 *   BadgeUtils.applyFromCache(sessionManager, bottomNav);
 *   BadgeUtils.fetchAndApply(this, sessionManager, bottomNav); // di onResume
 */
public class BadgeUtils {

    /**
     * Terapkan badge dari cache SharedPreferences (instan, tanpa network).
     * Panggil di onCreate setelah bottomNav siap.
     */
    public static void applyFromCache(SessionManager session, BottomNavigationView bottomNav) {
        int wishlist = session.getWishlistCount();
        int cart     = session.getCartCount();
        applyBadges(bottomNav, wishlist, cart);
    }

    /**
     * Fetch count dari API, simpan ke cache, lalu terapkan badge.
     * Panggil di onResume.
     */
    public static void fetchAndApply(Context context, SessionManager session,
                                     BottomNavigationView bottomNav) {
        if (!session.isLoggedIn()) return;

        // Tampilkan dari cache dulu agar tidak ada jeda
        applyFromCache(session, bottomNav);

        // Fetch fresh dari API
        String token = "Bearer " + session.getToken();
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getUserCounts(token).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call,
                                   Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null &&
                        "success".equals(response.body().getStatus())) {
                    try {
                        Map<String, Object> data = (Map<String, Object>) response.body().getData();
                        int wishlist = parseCount(data.get("wishlist_count"));
                        int cart     = parseCount(data.get("cart_count"));

                        // Simpan ke cache
                        session.saveBadgeCounts(wishlist, cart);

                        // Terapkan ke nav (harus di UI thread)
                        if (context instanceof Activity) {
                            ((Activity) context).runOnUiThread(() ->
                                    applyBadges(bottomNav, wishlist, cart));
                        }
                    } catch (Exception ignored) {}
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                // Tetap tampilkan dari cache jika network gagal
            }
        });
    }

    /** Terapkan badge ke item nav secara langsung */
    public static void applyBadges(BottomNavigationView nav, int wishlist, int cart) {
        applyBadge(nav, R.id.nav_wishlist, wishlist);
    }

    private static void applyBadge(BottomNavigationView nav, int menuItemId, int count) {
        if (count > 0) {
            BadgeDrawable badge = nav.getOrCreateBadge(menuItemId);
            badge.setNumber(count > 99 ? 99 : count);
            badge.setVisible(true);
            badge.setBackgroundColor(0xFFE53935); // merah
            badge.setBadgeTextColor(0xFFFFFFFF);
        } else {
            nav.removeBadge(menuItemId);
        }
    }

    private static int parseCount(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return (int) Double.parseDouble(String.valueOf(val)); } catch (Exception e) { return 0; }
    }
}
