package com.octania.marketplace.ui.home;

import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.octania.marketplace.R;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.model.Product;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityHomeBinding;
import com.octania.marketplace.ui.auth.LoginActivity;
import com.octania.marketplace.utils.ProductActionHelper;
import com.octania.marketplace.utils.SessionManager;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {
    private ActivityHomeBinding binding;
    private SessionManager sessionManager;
    private ApiService apiService;
    private ProductAdapter productAdapter;
    private CategoryAdapter categoryAdapter;
    private VoucherAdapter voucherAdapter;
    private ProductActionHelper actionHelper;

    private String searchQuery = null;
    private String selectedCategory = null;

    private FusedLocationProviderClient fusedLocationClient;
    private Double currentLat = null;
    private Double currentLng = null;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        actionHelper = new ProductActionHelper(this, sessionManager);

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setupCategoryRecycler();
        setupVoucherRecycler();
        setupProductRecycler();
        setupSearch();
        setupBottomNav();

        binding.swipeRefresh.setColorSchemeResources(R.color.primary_orange);
        binding.swipeRefresh.setOnRefreshListener(this::fetchLocationAndProducts);

        binding.btnNavCart.setOnClickListener(
                v -> startActivity(new Intent(this, com.octania.marketplace.ui.cart.CartActivity.class)));

        fetchCategories();
        fetchVouchers();
        fetchLocationAndProducts();
    }

    // ==================== SETUP ====================

    private void setupCategoryRecycler() {
        categoryAdapter = new CategoryAdapter(this, (categoryName, position) -> {
            selectedCategory = categoryName;
            loadProducts();
        });
        binding.rvCategories.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvCategories.setAdapter(categoryAdapter);
    }

    private void setupVoucherRecycler() {
        voucherAdapter = new VoucherAdapter(this, voucher -> {
            // Just show code when clicked for now (can copy to clipboard in future)
            Toast.makeText(this, "Gunakan kode: " + voucher.code, Toast.LENGTH_SHORT).show();
        });
        binding.rvVouchers.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvVouchers.setAdapter(voucherAdapter);
    }

    private void setupProductRecycler() {
        productAdapter = new ProductAdapter(this, new ProductAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Product product) {
                Intent intent = new Intent(HomeActivity.this,
                        com.octania.marketplace.ui.product.ProductDetailActivity.class);
                intent.putExtra(
                        com.octania.marketplace.ui.product.ProductDetailActivity.EXTRA_PRODUCT_ID,
                        product.getId());
                startActivity(intent);
            }

            @Override
            public void onAddToCartClick(Product product) {
                if (!sessionManager.isLoggedIn()) {
                    Toast.makeText(HomeActivity.this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(HomeActivity.this, LoginActivity.class));
                    return;
                }
                Intent intent = new Intent(HomeActivity.this, com.octania.marketplace.ui.cart.CheckoutActivity.class);
                intent.putExtra("direct_buy", true);
                intent.putExtra("product_id", product.getId());
                intent.putExtra("quantity", 1);
                startActivity(intent);
            }

            @Override
            public void onWishlistClick(Product product) {
                product.setWishlisted(!product.isWishlisted());
                int pos = productAdapter.getProductList().indexOf(product);
                if (pos != -1) {
                    productAdapter.notifyItemChanged(pos);
                } else {
                    productAdapter.notifyDataSetChanged();
                }
                toggleWishlist(product.getId());
            }
        });
        binding.rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        binding.rvProducts.setAdapter(productAdapter);
    }

    private void setupSearch() {
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getAction() == KeyEvent.ACTION_DOWN
                            && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String query = binding.etSearch.getText().toString().trim();
                searchQuery = query.isEmpty() ? null : query;
                loadProducts();
                return true;
            }
            return false;
        });
    }

    private void setupBottomNav() {
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
        com.octania.marketplace.utils.NavigationUtils.applyFloatingEffect(binding.bottomNav);

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_orders) {
                startActivity(new Intent(this,
                        com.octania.marketplace.ui.seller.SellerOrdersActivity.class));
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this,
                        com.octania.marketplace.ui.product.AddProductActivity.class));
                return true;
            } else if (id == R.id.nav_wishlist) {
                startActivity(new Intent(this,
                        com.octania.marketplace.ui.profile.WishlistActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this,
                        com.octania.marketplace.ui.profile.ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    // ==================== DATA FETCHING ====================

    private void fetchCategories() {
        apiService.getCategories().enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> apiResponse = response.body();
                    if ("success".equals(apiResponse.getStatus()) && apiResponse.getData() != null) {
                        try {
                            Gson gson = new Gson();
                            String json = gson.toJson(apiResponse.getData());
                            Type listType = new TypeToken<List<Map<String, Object>>>() {
                            }.getType();
                            List<Map<String, Object>> categories = gson.fromJson(json, listType);
                            categoryAdapter.updateData(categories);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
            }
        });
    }

    private void fetchVouchers() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getVouchers(token).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> apiResponse = response.body();
                    if ("success".equals(apiResponse.getStatus()) && apiResponse.getData() != null) {
                        try {
                            Gson gson = new Gson();
                            String json = gson.toJson(apiResponse.getData());
                            Type listType = new TypeToken<List<VoucherAdapter.VoucherModel>>() {
                            }.getType();
                            List<VoucherAdapter.VoucherModel> vouchers = gson.fromJson(json, listType);

                            voucherAdapter.updateData(vouchers);
                            binding.rvVouchers.setVisibility(vouchers.isEmpty() ? View.GONE : View.VISIBLE);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                // Ignore failure quietly for vouchers
            }
        });
    }

    @SuppressWarnings("MissingPermission")
    private void fetchLocationAndProducts() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        binding.swipeRefresh.setRefreshing(true);
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
            }
            loadProducts();
        }).addOnFailureListener(e -> loadProducts());
    }

    @SuppressWarnings("unchecked")
    private void loadProducts() {
        String token = sessionManager.isLoggedIn() ? "Bearer " + sessionManager.getToken() : null;
        apiService.getProducts(token, searchQuery, selectedCategory, currentLat, currentLng, 1)
                .enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Object>> call,
                            Response<ApiResponse<Object>> response) {
                        binding.swipeRefresh.setRefreshing(false);
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Object> apiResponse = response.body();
                            if ("success".equals(apiResponse.getStatus())) {
                                try {
                                    Map<String, Object> dataMap = (Map<String, Object>) apiResponse.getData();
                                    Object itemsObj = dataMap.get("data");

                                    Gson gson = new Gson();
                                    String json = gson.toJson(itemsObj);
                                    Type listType = new TypeToken<List<Product>>() {
                                    }.getType();
                                    List<Product> products = gson.fromJson(json, listType);

                                    productAdapter.updateData(products);
                                } catch (Exception e) {
                                    Toast.makeText(HomeActivity.this,
                                            getString(R.string.server_error),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                        binding.swipeRefresh.setRefreshing(false);
                        Toast.makeText(HomeActivity.this,
                                getString(R.string.network_error) + ": " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ==================== ACTIONS ====================

    private void addToCart(int productId) {
        actionHelper.addToCart(productId, 1, new ProductActionHelper.ActionCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
                fetchUserCounts(); // INSTANT SYNC
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(HomeActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleWishlist(int productId) {
        actionHelper.toggleWishlist(productId, new ProductActionHelper.ActionCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
                fetchUserCounts(); // INSTANT SYNC
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(HomeActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUserCounts() {
        String token = sessionManager.isLoggedIn() ? "Bearer " + sessionManager.getToken() : null;
        if (token == null)
            return;

        apiService.getUserCounts(token).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> res = response.body();
                    if ("success".equals(res.getStatus())) {
                        try {
                            Map<String, Object> dataMap = (Map<String, Object>) res.getData();

                            int cartCount = 0;
                            if (dataMap.containsKey("cart_count")) {
                                Object c = dataMap.get("cart_count");
                                cartCount = c instanceof Double ? ((Double) c).intValue() : (Integer) c;
                            }

                            int wishlistCount = 0;
                            if (dataMap.containsKey("wishlist_count")) {
                                Object w = dataMap.get("wishlist_count");
                                wishlistCount = w instanceof Double ? ((Double) w).intValue() : (Integer) w;
                            }

                            updateBadges(cartCount, wishlistCount);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                // ignore
            }
        });
    }

    private void updateBadges(int cartCount, int wishlistCount) {
        TextView tvCartBadge = binding.tvCartBadge;
        TextView tvWishlistBadge = binding.tvWishlistBadge;

        if (tvCartBadge != null) {
            if (cartCount > 0) {
                tvCartBadge.setText(cartCount > 99 ? "99+" : String.valueOf(cartCount));
                tvCartBadge.setVisibility(View.VISIBLE);
            } else {
                tvCartBadge.setVisibility(View.GONE);
            }
        }

        if (tvWishlistBadge != null) {
            if (wishlistCount > 0) {
                tvWishlistBadge.setText(wishlistCount > 99 ? "99+" : String.valueOf(wishlistCount));
                tvWishlistBadge.setVisibility(View.VISIBLE);
            } else {
                tvWishlistBadge.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            fetchLocationAndProducts();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reset bottom nav to Home when coming back
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
        com.octania.marketplace.utils.NavigationUtils.applyFloatingEffect(binding.bottomNav);
        fetchUserCounts();

        // Reload products to sync any wishlist state changes made from other screens
        loadProducts();
    }
}
