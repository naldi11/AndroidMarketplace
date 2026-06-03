package com.octania.marketplace.ui.product;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.octania.marketplace.R;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.model.Product;
import com.octania.marketplace.data.model.Review;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityProductDetailBinding;
import com.octania.marketplace.ui.auth.LoginActivity;
import com.octania.marketplace.utils.ProductActionHelper;
import com.octania.marketplace.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "extra_product_id";

    private ActivityProductDetailBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private ProductActionHelper actionHelper;
    private int productId;

    // Carousel
    private ImageSliderAdapter sliderAdapter;
    private final List<View> indicatorDots = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);
        actionHelper = new ProductActionHelper(this, sessionManager);

        productId = getIntent().getIntExtra(EXTRA_PRODUCT_ID, -1);
        if (productId == -1) {
            Toast.makeText(this, getString(R.string.product_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnAddToCart.setOnClickListener(v -> addToCart(false));
        binding.btnBuyNow.setOnClickListener(v -> {
            if (!sessionManager.isLoggedIn()) {
                Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                return;
            }
            Intent intent = new Intent(this, com.octania.marketplace.ui.cart.CheckoutActivity.class);
            intent.putExtra("direct_buy", true);
            intent.putExtra("product_id", productId);
            intent.putExtra("quantity", 1);
            startActivity(intent);
        });
        binding.btnWishlistAction.setOnClickListener(v -> toggleWishlist());
        
        binding.btnChatSeller.setOnClickListener(v -> {
            if (!sessionManager.isLoggedIn()) {
                Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                return;
            }
            // Start Chat with seller
            apiService.getProductDetail(sessionManager.isLoggedIn() ? "Bearer " + sessionManager.getToken() : null, productId).enqueue(new Callback<ApiResponse<Product>>() {
                @Override
                public void onResponse(Call<ApiResponse<Product>> call, Response<ApiResponse<Product>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        Product p = response.body().getData();
                        if (p.getUser() != null) {
                            Intent intent = new Intent(ProductDetailActivity.this, com.octania.marketplace.ui.chat.ChatActivity.class);
                            intent.putExtra("user_id", p.getUser().getId());
                            intent.putExtra("user_name", p.getUser().getName());
                            intent.putExtra("user_avatar", p.getUser().getAvatar());
                            startActivity(intent);
                        } else {
                            Toast.makeText(ProductDetailActivity.this, "Data penjual tidak ditemukan", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Product>> call, Throwable t) {}
            });
        });

        fetchProductDetail();
    }

    // ==================== DATA ====================

    private void fetchProductDetail() {
        String token = sessionManager.isLoggedIn() ? "Bearer " + sessionManager.getToken() : null;
        apiService.getProductDetail(token, productId).enqueue(new Callback<ApiResponse<Product>>() {
            @Override
            public void onResponse(Call<ApiResponse<Product>> call, Response<ApiResponse<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Product> res = response.body();
                    if ("success".equals(res.getStatus()) && res.getData() != null) {
                        populateData(res.getData());
                    }
                } else {
                    Toast.makeText(ProductDetailActivity.this,
                            getString(R.string.server_error), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Product>> call, Throwable t) {
                Toast.makeText(ProductDetailActivity.this,
                        getString(R.string.network_error) + ": " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==================== UI ====================

    private void populateData(Product product) {
        // Name & Price
        binding.tvProductName.setText(product.getName());
        double price = product.getEffectivePrice() > 0 ? product.getEffectivePrice() : product.getPrice();
        binding.tvProductPrice.setText(String.format("Rp %,.0f", price));

        // Rating
        binding.tvProductRating.setText(String.format(java.util.Locale.US, "%.1f", product.getAvgRating()));
        binding.tvReviewCount.setText("(" + product.getReviewCount() + " Penilaian)");
        binding.llRating.setVisibility(product.getReviewCount() > 0 ? View.VISIBLE : View.GONE);

        // Discount badge
        if (product.isHasDiscount() && product.getDiscountPercent() != null) {
            binding.tvDiscountBadge.setText(String.format("-%.0f%%", product.getDiscountPercent()));
            binding.tvDiscountBadge.setVisibility(View.VISIBLE);
        }

        // Specs
        binding.tvProductCondition.setText(
                product.getCondition() != null ? product.getCondition() : "Baru");
        binding.tvProductWeight.setText(
                (product.getWeight() != null ? product.getWeight() : 1000) + " gr");
        binding.tvProductStock.setText(String.valueOf(product.getStock()));
        binding.tvProductLocation.setText(
                product.getLocation() != null ? product.getLocation() : "Lokasi Seller");

        // Distance
        if (product.getDistanceKm() != null) {
            Double dist = product.getDistanceKm();
            if (dist < 1.0) {
                int meters = (int) Math.round(dist * 1000);
                binding.tvProductDistance.setText("📍 " + meters + " m dari Anda");
            } else {
                binding.tvProductDistance.setText(String.format(java.util.Locale.US, "📍 %.1f km dari Anda", dist));
            }
            binding.tvProductDistance.setVisibility(View.VISIBLE);
        } else {
            binding.tvProductDistance.setVisibility(View.GONE);
        }

        // Description
        binding.tvProductDesc.setText(
                product.getDescription() != null ? product.getDescription() : "Tidak ada deskripsi.");

        // ---- Image Carousel ----
        String baseUrl = ApiClient.BASE_URL.replace("/api/", "/storage/");
        List<String> imageUrls = product.getImageUrls(baseUrl);
        if (imageUrls.isEmpty()) {
            imageUrls.add(""); // fallback placeholder
        }

        sliderAdapter = new ImageSliderAdapter(this, imageUrls);
        binding.vpProductImages.setAdapter(sliderAdapter);

        // Counter & indicators
        int total = imageUrls.size();
        binding.tvImageCounter.setText("1/" + total);
        setupIndicators(total);
        setActiveIndicator(0);

        binding.vpProductImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                binding.tvImageCounter.setText((position + 1) + "/" + total);
                setActiveIndicator(position);
            }
        });

        // ---- Wishlist state ----
        updateWishlistIcon(product.isWishlisted());

        // ---- Owner visibility ----
        if (product.isMine()) {
            binding.bottomActionBar.setVisibility(View.GONE);
        } else {
            binding.bottomActionBar.setVisibility(View.VISIBLE);
            binding.btnAddToCart.setEnabled(product.getStock() > 0);
            binding.btnBuyNow.setEnabled(product.getStock() > 0);
            if (product.getStock() <= 0) {
                binding.btnBuyNow.setText("Habis");
            }
        }



        // Reviews
        renderReviews(product.getReviews());
    }

    private void renderReviews(List<Review> reviews) {
        binding.llReviewsContainer.removeAllViews();
        if (reviews == null || reviews.isEmpty()) {
            binding.tvNoReviews.setVisibility(View.VISIBLE);
            return;
        }

        binding.tvNoReviews.setVisibility(View.GONE);
        for (Review review : reviews) {
            View itemView = getLayoutInflater().inflate(R.layout.item_review, binding.llReviewsContainer, false);

            android.widget.TextView tvName = itemView.findViewById(R.id.tvReviewerName);
            android.widget.RatingBar rb = itemView.findViewById(R.id.rbReviewRating);
            android.widget.TextView tvComment = itemView.findViewById(R.id.tvReviewComment);
            android.widget.TextView tvDate = itemView.findViewById(R.id.tvReviewDate);
            android.widget.ImageView ivAvatar = itemView.findViewById(R.id.ivReviewerAvatar);

            if (review.getReviewer() != null) {
                tvName.setText(review.getReviewer().getName());
                String avatarUrl = ApiClient.BASE_URL.replace("/api/", "/storage/") + review.getReviewer().getAvatar();
                com.bumptech.glide.Glide.with(this)
                        .load(avatarUrl)
                        .placeholder(R.mipmap.ic_launcher_round)
                        .circleCrop()
                        .into(ivAvatar);
            } else {
                tvName.setText("Pembeli");
            }

            rb.setRating(review.getRating());
            tvComment.setText(review.getComment() != null ? review.getComment() : "");
            tvComment.setVisibility(
                    review.getComment() != null && !review.getComment().isEmpty() ? View.VISIBLE : View.GONE);

            String date = review.getCreatedAt();
            if (date != null && date.length() > 10)
                date = date.substring(0, 10);
            tvDate.setText(date);

            binding.llReviewsContainer.addView(itemView);
        }
    }

    // ==================== INDICATORS ====================

    private void setupIndicators(int count) {
        binding.llIndicators.removeAllViews();
        indicatorDots.clear();
        if (count <= 1) {
            binding.llIndicators.setVisibility(View.GONE);
            return;
        }
        binding.llIndicators.setVisibility(View.VISIBLE);

        for (int i = 0; i < count; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(8, 8);
            params.setMargins(4, 0, 4, 0);
            dot.setLayoutParams(params);

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(ContextCompat.getColor(this, R.color.grey_divider));
            dot.setBackground(shape);
            binding.llIndicators.addView(dot);
            indicatorDots.add(dot);
        }
    }

    private void setActiveIndicator(int position) {
        for (int i = 0; i < indicatorDots.size(); i++) {
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);

            View dot = indicatorDots.get(i);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) dot.getLayoutParams();

            if (i == position) {
                shape.setColor(ContextCompat.getColor(this, R.color.primary_orange));
                params.width = 20;
                params.height = 8;
                shape.setCornerRadius(4);
            } else {
                shape.setColor(ContextCompat.getColor(this, R.color.grey_divider));
                params.width = 8;
                params.height = 8;
            }

            dot.setLayoutParams(params);
            dot.setBackground(shape);
        }
    }

    private void updateWishlistIcon(boolean wishlisted) {
        binding.ivWishlistIcon.setColorFilter(ContextCompat.getColor(this,
                wishlisted ? R.color.wishlist_red : R.color.grey_icon));
    }

    // ==================== ACTIONS ====================



    private void addToCart(boolean navigateToCart) {
        actionHelper.addToCart(productId, 1, new ProductActionHelper.ActionCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(ProductDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                if (navigateToCart) {
                    startActivity(new Intent(ProductDetailActivity.this,
                            com.octania.marketplace.ui.cart.CartActivity.class));
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (errorMessage.equals("Silakan login terlebih dahulu")) {
                    Toast.makeText(ProductDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(ProductDetailActivity.this, LoginActivity.class));
                } else {
                    Toast.makeText(ProductDetailActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void toggleWishlist() {
        actionHelper.toggleWishlist(productId, new ProductActionHelper.ActionCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(ProductDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                fetchProductDetail(); // Refresh to update wishlist icon
            }

            @Override
            public void onError(String errorMessage) {
                if (errorMessage.equals("Silakan login terlebih dahulu")) {
                    Toast.makeText(ProductDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(ProductDetailActivity.this, LoginActivity.class));
                } else {
                    Toast.makeText(ProductDetailActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
