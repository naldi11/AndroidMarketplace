package com.octania.marketplace.data.remote;

import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.model.AuthResponse;
import com.octania.marketplace.data.model.Product;
import com.octania.marketplace.data.model.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;
import retrofit2.http.Body;
import retrofit2.http.Part;
import retrofit2.http.Multipart;
import retrofit2.http.Path;
import okhttp3.RequestBody;
import okhttp3.MultipartBody;

public interface ApiService {

        @FormUrlEncoded
        @POST("login")
        Call<AuthResponse> login(
                        @Field("login") String loginId,
                        @Field("password") String password);

        @FormUrlEncoded
        @POST("register")
        Call<AuthResponse> register(
                        @Field("name") String name,
                        @Field("email") String email,
                        @Field("phone") String phone,
                        @Field("password") String password,
                        @Field("password_confirmation") String passwordConfirmation,
                        @Field("role") String role,
                        @Field("shop_name") String shopName,
                        @Field("address") String address,
                        @Field("latitude") Double latitude,
                        @Field("longitude") Double longitude,
                        @Field("device_id") String deviceId);

        @POST("logout")
        Call<ApiResponse<Void>> logout(
                        @Header("Authorization") String token);

        @GET("user")
        Call<User> getUserProfile(
                        @Header("Authorization") String token);

        @FormUrlEncoded
        @retrofit2.http.PUT("profile")
        Call<ApiResponse<User>> updateProfile(
                        @Header("Authorization") String token,
                        @Field("name") String name,
                        @Field("email") String email,
                        @Field("phone") String phone);

        @retrofit2.http.PUT("profile/password")
        Call<ApiResponse<Void>> updatePassword(
                        @Header("Authorization") String token,
                        @Body java.util.Map<String, String> body);

        @retrofit2.http.Multipart
        @POST("profile/avatar")
        Call<ApiResponse<User>> updateAvatar(
                        @Header("Authorization") String token,
                        @Part okhttp3.MultipartBody.Part avatar);

        @GET("products")
        Call<ApiResponse<Object>> getProducts(
                        @Header("Authorization") String token,
                        @Query("search") String search,
                        @Query("category") String category,
                        @Query("latitude") Double latitude,
                        @Query("longitude") Double longitude,
                        @Query("radius") Integer radius,
                        @Query("per_page") Integer perPage,
                        @Query("limit") Integer limit,
                        @Query("all") Integer all,
                        @Query("no_paginate") Integer noPaginate,
                        @Query("page") Integer page);

        @GET("categories")
        Call<ApiResponse<Object>> getCategories();

        @GET("ad-banners")
        Call<com.octania.marketplace.data.model.response.AdBannerResponse> getAdBanners();

        @GET("products/{id}")
        Call<ApiResponse<Product>> getProductDetail(
                        @Header("Authorization") String token,
                        @retrofit2.http.Path("id") int id);

        @GET("cart")
        Call<ApiResponse<Object>> getCart(
                        @Header("Authorization") String token);

        @FormUrlEncoded
        @POST("cart")
        Call<ApiResponse<Object>> addToCart(
                        @Header("Authorization") String token,
                        @Field("product_id") int productId,
                        @Field("quantity") int quantity);

        @FormUrlEncoded
        @retrofit2.http.PUT("cart/{id}")
        Call<ApiResponse<Object>> updateCart(
                        @Header("Authorization") String token,
                        @retrofit2.http.Path("id") int cartId,
                        @Field("quantity") int quantity);

        @retrofit2.http.DELETE("cart/{id}")
        Call<ApiResponse<Void>> deleteCartItem(
                        @Header("Authorization") String token,
                        @retrofit2.http.Path("id") int cartId);

        // ===== New Checkout Flow =====
        @POST("transactions/preview")
        Call<ApiResponse<Object>> previewCheckout(
                        @Header("Authorization") String token,
                        @Body java.util.Map<String, Object> body);

        @POST("transactions/confirm")
        Call<ApiResponse<Object>> confirmCheckout(
                        @Header("Authorization") String token,
                        @Body java.util.Map<String, Object> body);

        @GET("transactions/check-status/{id}")
        Call<ApiResponse<Object>> checkPaymentStatus(
                        @Header("Authorization") String token,
                        @retrofit2.http.Path("id") int transactionId);

        @POST("transactions/pay-wallet/{id}")
        Call<ApiResponse<Object>> payWithWallet(
                        @Header("Authorization") String token,
                        @retrofit2.http.Path("id") int transactionId);

        @GET("payment-methods")
        Call<ApiResponse<List<Object>>> getPaymentMethods();

        @GET("transactions")
        Call<ApiResponse<java.util.List<Object>>> getTransactions(
                        @Header("Authorization") String token);

        @GET("transactions/{id}")
        Call<ApiResponse<Object>> getTransactionDetail(
                        @Header("Authorization") String token,
                        @retrofit2.http.Path("id") int transactionId);

        @retrofit2.http.Multipart
        @POST("transactions/{id}/proof")
        Call<ApiResponse<Object>> uploadProof(
                        @Header("Authorization") String token,
                        @retrofit2.http.Path("id") int transactionId,
                        @retrofit2.http.Part okhttp3.MultipartBody.Part file);

        @POST("products/{id}/wishlist")
        Call<ApiResponse<Object>> toggleWishlist(
                        @Header("Authorization") String token,
                        @retrofit2.http.Path("id") int productId);

        @GET("profile/addresses")
        Call<ApiResponse<List<Object>>> getAddresses(@Header("Authorization") String token);

        @retrofit2.http.FormUrlEncoded
        @POST("profile/addresses")
        Call<ApiResponse<Object>> addAddress(
                        @Header("Authorization") String token,
                        @retrofit2.http.Field("recipient_name") String recipientName,
                        @retrofit2.http.Field("phone") String phone,
                        @retrofit2.http.Field("full_address") String fullAddress,
                        @retrofit2.http.Field("latitude") Double latitude,
                        @retrofit2.http.Field("longitude") Double longitude,
                        @retrofit2.http.Field("is_default") boolean isDefault);

        @retrofit2.http.FormUrlEncoded
        @PUT("profile/addresses/{id}")
        Call<ApiResponse<Object>> updateAddress(
                        @Header("Authorization") String token,
                        @retrofit2.http.Path("id") int addressId,
                        @retrofit2.http.Field("recipient_name") String recipientName,
                        @retrofit2.http.Field("phone") String phone,
                        @retrofit2.http.Field("full_address") String fullAddress,
                        @retrofit2.http.Field("latitude") Double latitude,
                        @retrofit2.http.Field("longitude") Double longitude);

        @retrofit2.http.DELETE("profile/addresses/{id}")
        Call<ApiResponse<Object>> deleteAddress(
                        @Header("Authorization") String token,
                        @retrofit2.http.Path("id") int addressId);

        @PUT("profile/addresses/{id}/default")
        Call<ApiResponse<Object>> setDefaultAddress(
                        @Header("Authorization") String token,
                        @retrofit2.http.Path("id") int addressId);

        @GET("profile/wishlists")
        Call<ApiResponse<Object>> getWishlists(
                        @Header("Authorization") String token);

        @retrofit2.http.Multipart
        @retrofit2.http.POST("products")
        Call<ApiResponse<Object>> addProduct(
                        @Header("Authorization") String token,
                        @retrofit2.http.Part("name") okhttp3.RequestBody name,
                        @retrofit2.http.Part("description") okhttp3.RequestBody description,
                        @retrofit2.http.Part("price") okhttp3.RequestBody price,
                        @retrofit2.http.Part("discount_price") okhttp3.RequestBody discountPrice,
                        @retrofit2.http.Part("stock") okhttp3.RequestBody stock,
                        @retrofit2.http.Part("category_id") okhttp3.RequestBody categoryId,
                        @retrofit2.http.Part("condition") okhttp3.RequestBody condition,
                        @retrofit2.http.Part("weight") okhttp3.RequestBody weight,
                        @retrofit2.http.Part("location") okhttp3.RequestBody location,
                        @retrofit2.http.Part("latitude") okhttp3.RequestBody latitude,
                        @retrofit2.http.Part("longitude") okhttp3.RequestBody longitude,
                        @retrofit2.http.Part java.util.List<okhttp3.MultipartBody.Part> images);

        // ===== My Products =====
        @GET("my-products")
        Call<ApiResponse<Object>> getMyProducts(
                        @Header("Authorization") String token);

        @GET("user/counts")
        Call<ApiResponse<Object>> getUserCounts(
                        @Header("Authorization") String token);

        @FormUrlEncoded
        @PUT("products/{id}")
        Call<ApiResponse<Object>> updateProduct(
                        @Header("Authorization") String token,
                        @retrofit2.http.Path("id") int productId,
                        @Field("name") String name,
                        @Field("price") double price,
                        @Field("discount_price") Double discountPrice,
                        @Field("stock") int stock,
                        @Field("category_id") int categoryId,
                        @Field("condition") String condition,
                        @Field("weight") int weight,
                        @Field("location") String location,
                        @Field("description") String description);

        @retrofit2.http.DELETE("products/{id}")
        Call<ApiResponse<Void>> deleteProduct(
                        @Header("Authorization") String token,
                        @retrofit2.http.Path("id") int productId);

        // ===== Seller Dashboard =====
        @GET("seller/transactions")
        Call<ApiResponse<Object>> getSellerTransactions(
                        @Header("Authorization") String token);

        @GET("seller/transactions/{id}")
        Call<ApiResponse<Object>> getSellerTransactionDetail(
                        @Header("Authorization") String token,
                        @retrofit2.http.Path("id") int transactionId);

        @retrofit2.http.DELETE("seller/transactions/{id}")
        Call<ApiResponse<Object>> deleteSellerTransaction(
                        @Header("Authorization") String token,
                        @retrofit2.http.Path("id") int transactionId);

        @GET("seller/balance")
        Call<ApiResponse<Object>> getSellerBalance(
                        @Header("Authorization") String token);

        @FormUrlEncoded
        @POST("seller/withdraw")
        Call<ApiResponse<Object>> requestWithdraw(
                        @Header("Authorization") String token,
                        @Field("amount") double amount,
                        @Field("bank_name") String bankName,
                        @Field("account_number") String accountNumber,
                        @Field("account_name") String accountName);

        // ===== Order Management =====
        @Multipart
        @POST("transactions/{id}/ship")
        Call<ApiResponse<Object>> shipOrder(
                        @Header("Authorization") String token,
                        @retrofit2.http.Path("id") int transactionId,
                        @Part("courier") RequestBody courier,
                        @Part("tracking_number") RequestBody trackingNumber,
                        @Part MultipartBody.Part shippingProof);

        @Multipart
        @POST("transactions/{id}/confirm")
        Call<ApiResponse<Object>> confirmReceived(
                        @Header("Authorization") String token,
                        @retrofit2.http.Path("id") int transactionId,
                        @Part List<MultipartBody.Part> files);

        @retrofit2.http.DELETE("transactions/{id}")
        Call<ApiResponse<Object>> deleteTransaction(
                        @Header("Authorization") String token,
                        @retrofit2.http.Path("id") int transactionId);

        @POST("transactions/{id}/cancel")
        Call<ApiResponse<Object>> cancelOrder(@Header("Authorization") String token, @retrofit2.http.Path("id") int id);

        // ===== Dispute / Laporan Masalah =====
        @retrofit2.http.FormUrlEncoded
        @POST("disputes/{transactionId}")
        Call<ApiResponse<Object>> openDispute(
                @Header("Authorization") String token,
                @retrofit2.http.Path("transactionId") int transactionId,
                @retrofit2.http.Field("reason") String reason,
                @retrofit2.http.Field("description") String description);

        @GET("disputes/{transactionId}")
        Call<ApiResponse<Object>> getDispute(
                @Header("Authorization") String token,
                @retrofit2.http.Path("transactionId") int transactionId);

        @retrofit2.http.FormUrlEncoded
        @POST("disputes/{id}/buyer-ship-back")
        Call<ApiResponse<Object>> buyerShipBack(
                @Header("Authorization") String token,
                @retrofit2.http.Path("id") int disputeId,
                @retrofit2.http.Field("return_courier") String courier,
                @retrofit2.http.Field("return_tracking_number") String trackingNumber);

        @POST("disputes/{id}/seller-confirm-return")
        Call<ApiResponse<Object>> sellerConfirmReturn(
                @Header("Authorization") String token,
                @retrofit2.http.Path("id") int disputeId);

        @GET("seller/disputes")
        Call<ApiResponse<java.util.List<com.octania.marketplace.data.model.Dispute>>> getSellerDisputes(
                @Header("Authorization") String token);


        @FormUrlEncoded
        @POST("transactions/{id}/status")
        Call<ApiResponse<Object>> updateOrderStatus(
                        @Header("Authorization") String token,
                        @retrofit2.http.Path("id") int id,
                        @Field("status") String status,
                        @Field("note") String note);

        // ===== Reviews =====
        @FormUrlEncoded
        @POST("reviews/{transactionId}")
        Call<ApiResponse<Object>> submitReview(
                        @Header("Authorization") String token,
                        @retrofit2.http.Path("transactionId") int transactionId,
                        @Field("rating") int rating,
                        @Field("comment") String comment);

        // ===== Vouchers =====
        @GET("vouchers")
        Call<ApiResponse<java.util.List<com.octania.marketplace.data.model.Voucher>>> getVouchers(
                        @Header("Authorization") String token,
                        @Query("total_amount") double totalAmount);

        @GET("vouchers/public")
        Call<ApiResponse<java.util.List<com.octania.marketplace.data.model.Voucher>>> getPublicVouchers(
                        @Header("Authorization") String token);

        @GET("vouchers")
        Call<ApiResponse<java.util.List<com.octania.marketplace.data.model.Voucher>>> getVouchersByCategory(
                        @Header("Authorization") String token,
                        @Query("category_id") int categoryId);

        @FormUrlEncoded
        @POST("vouchers/check")
        Call<ApiResponse<com.octania.marketplace.data.model.Voucher>> checkVoucher(
                        @Header("Authorization") String token,
                        @Field("user_voucher_id") int userVoucherId,
                        @Field("total_amount") double totalAmount);

        @POST("vouchers/{id}/claim")
        Call<ApiResponse<Void>> claimVoucher(
                        @Header("Authorization") String token,
                        @retrofit2.http.Path("id") int voucherId);

        // ===== Settings =====
        @GET("settings")
        Call<ApiResponse<java.util.Map<String, String>>> getSettings();

        @GET("settings/{key}")
        Call<ApiResponse<java.util.Map<String, String>>> getSettingByKey(@retrofit2.http.Path("key") String key);

        @FormUrlEncoded
        @POST("seller/transactions/mark-seen")
        Call<ApiResponse<Object>> markSellerOrdersSeen(
                        @Header("Authorization") String token,
                        @Field("status") String status);

@FormUrlEncoded
@POST("transactions/mark-seen")
Call<ApiResponse<Object>> markBuyerOrdersSeen(
        @Header("Authorization") String token,
        @Field("status") String status);

// ===== Seller Dashboard =====
@GET("seller/dashboard")
Call<ApiResponse<Object>> getSellerDashboard(
        @Header("Authorization") String token);

// ===== Reports =====
@FormUrlEncoded
@POST("reports")
Call<ApiResponse<Object>> submitReport(
        @Header("Authorization") String token,
        @Field("transaction_id") int transactionId,
        @Field("type") String type,
        @Field("reason") String reason,
        @Field("description") String description);

// ===== MeyPay Wallet =====
@GET("wallet/info")
Call<ApiResponse<Object>> getWalletInfo(@Header("Authorization") String token);

@GET("wallet/transactions")
Call<ApiResponse<Object>> getWalletTransactions(@Header("Authorization") String token);

@FormUrlEncoded
@POST("wallet/topup")
Call<ApiResponse<Object>> topupWallet(
        @Header("Authorization") String token,
        @Field("amount") double amount);

@FormUrlEncoded
@POST("wallet/verify-payment")
Call<ApiResponse<Object>> verifyPaymentCode(
        @Header("Authorization") String token,
        @Field("code") String code);

@FormUrlEncoded
@POST("wallet/verify-pin")
Call<ApiResponse<Object>> verifyWalletPin(
        @Header("Authorization") String token,
        @Field("pin") String pin);

// ===== Chat System =====
@GET("chat/conversations")
Call<ApiResponse<java.util.List<com.octania.marketplace.data.model.User>>> getChatConversations(
        @Header("Authorization") String token);

@GET("chat/messages/{otherUserId}")
Call<ApiResponse<java.util.List<com.octania.marketplace.data.model.Message>>> getChatMessages(
        @Header("Authorization") String token,
        @retrofit2.http.Path("otherUserId") int otherUserId);

@Multipart
@POST("chat/send")
Call<ApiResponse<com.octania.marketplace.data.model.Message>> sendMessage(
        @Header("Authorization") String token,
        @Part("receiver_id") RequestBody receiverId,
        @Part("message") RequestBody message,
        @Part MultipartBody.Part attachment);

@GET("chat/poll")
Call<ApiResponse<java.util.List<com.octania.marketplace.data.model.Message>>> pollMessages(
        @Header("Authorization") String token);
}
