package com.octania.marketplace.utils;

import android.content.Context;
import android.widget.Toast;

import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductActionHelper {

    private final ApiService apiService;
    private final Context context;
    private final String token;

    public interface ActionCallback {
        void onSuccess(String message);

        void onError(String errorMessage);
    }

    public ProductActionHelper(Context context, SessionManager sessionManager) {
        this.context = context;
        this.apiService = ApiClient.getClient().create(ApiService.class);
        this.token = sessionManager.isLoggedIn() ? "Bearer " + sessionManager.getToken() : null;
    }

    public void addToCart(int productId, int quantity, ActionCallback callback) {
        if (token == null) {
            callback.onError(context.getString(com.octania.marketplace.R.string.login_first));
            return;
        }

        apiService.addToCart(token, productId, quantity).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> res = response.body();
                    if ("success".equals(res.getStatus())) {
                        callback.onSuccess(res.getMessage() != null ? String.valueOf(res.getMessage())
                                : context.getString(com.octania.marketplace.R.string.add_to_cart_success));
                    } else {
                        callback.onError(res.getMessage() != null ? String.valueOf(res.getMessage())
                                : context.getString(com.octania.marketplace.R.string.add_to_cart_failed));
                    }
                } else {
                    String errorMsg = context.getString(com.octania.marketplace.R.string.add_to_cart_failed);
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception ignored) {
                    }
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                callback.onError(
                        context.getString(com.octania.marketplace.R.string.network_error) + ": " + t.getMessage());
            }
        });
    }

    public void toggleWishlist(int productId, ActionCallback callback) {
        if (token == null) {
            callback.onError(context.getString(com.octania.marketplace.R.string.login_first));
            return;
        }

        apiService.toggleWishlist(token, productId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> res = response.body();
                    if ("success".equals(res.getStatus())) {
                        callback.onSuccess(
                                res.getMessage() != null ? String.valueOf(res.getMessage())
                                        : context.getString(com.octania.marketplace.R.string.wishlist_updated));
                    } else {
                        callback.onError(context.getString(com.octania.marketplace.R.string.wishlist_update_failed));
                    }
                } else {
                    callback.onError(context.getString(com.octania.marketplace.R.string.server_error));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                callback.onError(
                        context.getString(com.octania.marketplace.R.string.network_error) + ": " + t.getMessage());
            }
        });
    }
}
