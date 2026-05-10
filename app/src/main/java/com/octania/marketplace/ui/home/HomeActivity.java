package com.octania.marketplace.ui.home;

import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
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
import android.content.SharedPreferences;
import android.widget.ArrayAdapter;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

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
import com.octania.marketplace.utils.ToastManager;
import com.octania.marketplace.utils.ProductActionHelper;
import com.octania.marketplace.utils.SessionManager;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

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
    private DistanceFilterAdapter distanceFilterAdapter;

    private String searchQuery = null;
    private String selectedCategory = null;
    private Integer selectedDistance = null;

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

        setupDistanceFilter();
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
        fetchAdBanners();
        fetchLocationAndProducts();
        handleRefreshIntent(getIntent());

        binding.btnFilter.setOnClickListener(v -> binding.drawerLayout.openDrawer(androidx.core.view.GravityCompat.END));
        binding.btnApplyFilter.setOnClickListener(v -> {
            binding.drawerLayout.closeDrawer(androidx.core.view.GravityCompat.END);
            fetchLocationAndProducts(); // Ambil lokasi terbaru lalu loadProducts
        });
    }

    // ==================== SETUP ====================

    private void setupDistanceFilter() {
        distanceFilterAdapter = new DistanceFilterAdapter(this, radius -> {
            selectedDistance = radius;
        });
        binding.rvDistanceFilter.setLayoutManager(
                new androidx.recyclerview.widget.GridLayoutManager(this, 2));
        binding.rvDistanceFilter.setAdapter(distanceFilterAdapter);
    }

    private void fetchAdBanners() {
        apiService.getAdBanners().enqueue(new Callback<com.octania.marketplace.data.model.response.AdBannerResponse>() {
            @Override
            public void onResponse(Call<com.octania.marketplace.data.model.response.AdBannerResponse> call, Response<com.octania.marketplace.data.model.response.AdBannerResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    com.octania.marketplace.data.model.response.AdBannerResponse res = response.body();
                    if (res.getData() != null) {
                        Log.d("ADS_DEBUG", "Success fetching ads. Count: " + res.getData().size());
                        productAdapter.setAdBanners(res.getData());
                    }
                } else {
                    Log.e("ADS_DEBUG", "Failed to fetch ads: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<com.octania.marketplace.data.model.response.AdBannerResponse> call, Throwable t) {
                Log.e("ADS_DEBUG", "Error fetching ads: " + t.getMessage());
            }
        });
    }

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
            ToastManager.showToast(this, "Gunakan kode: " + voucher.code);
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
                    ToastManager.showToast(HomeActivity.this, "Silakan login terlebih dahulu");
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
        Log.d("HOME_DEBUG", "Setting up ProductAdapter");
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        binding.rvProducts.setLayoutManager(gridLayoutManager);
        binding.rvProducts.setAdapter(productAdapter);
        
        // Force RecyclerView visibility
        binding.rvProducts.setVisibility(View.VISIBLE);
        binding.rvProducts.requestLayout();
        
        Log.d("HOME_DEBUG", "ProductAdapter setup complete");
    }

    private void setupSearch() {
        SharedPreferences prefs = getSharedPreferences("SearchHistory", MODE_PRIVATE);
        Set<String> historySet = prefs.getStringSet("history", new HashSet<>());
        List<String> historyList = new ArrayList<>(historySet);
        
        // Populate Chips
        populateSearchHistoryChips(historyList);

        binding.etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.layoutSearchHistory.setVisibility(View.VISIBLE);
            } else {
                binding.layoutSearchHistory.setVisibility(View.GONE);
            }
        });

        binding.btnClearHistory.setOnClickListener(v -> {
            prefs.edit().putStringSet("history", new HashSet<>()).apply();
            populateSearchHistoryChips(new ArrayList<>());
        });

        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getAction() == KeyEvent.ACTION_DOWN
                            && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String query = binding.etSearch.getText().toString().trim();
                searchQuery = query.isEmpty() ? null : query;
                
                if (searchQuery != null) {
                    historySet.add(searchQuery);
                    prefs.edit().putStringSet("history", historySet).apply();
                    populateSearchHistoryChips(new ArrayList<>(historySet));
                }

                binding.etSearch.clearFocus();
                binding.layoutSearchHistory.setVisibility(View.GONE);
                loadProducts();

                // Hide keyboard
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(binding.etSearch.getWindowToken(), 0);

                return true;
            }
            return false;
        });
    }

    private void populateSearchHistoryChips(List<String> historyList) {
        binding.chipGroupHistory.removeAllViews();
        if (historyList == null || historyList.isEmpty()) {
            binding.tvNoHistory.setVisibility(View.VISIBLE);
            return;
        }
        binding.tvNoHistory.setVisibility(View.GONE);
        for (String history : historyList) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText(history);
            chip.setClickable(true);
            chip.setCheckable(false);
            chip.setChipBackgroundColorResource(android.R.color.white);
            chip.setChipStrokeColorResource(R.color.grey_inactive);
            chip.setChipStrokeWidth(1f);
            
            chip.setOnClickListener(v -> {
                binding.etSearch.setText(history);
                searchQuery = history;
                binding.etSearch.clearFocus();
                binding.layoutSearchHistory.setVisibility(View.GONE);
                
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(binding.etSearch.getWindowToken(), 0);
                
                loadProducts();
            });
            binding.chipGroupHistory.addView(chip);
        }
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
                        com.octania.marketplace.ui.seller.MyOrdersActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_wishlist) {
                startActivity(new Intent(this,
                        com.octania.marketplace.ui.profile.WishlistActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this,
                        com.octania.marketplace.ui.profile.ProfileActivity.class));
                finish();
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
            // Jika izin lokasi belum diberikan, tetap muat produk tanpa koordinat
            currentLat = null;
            currentLng = null;
            binding.swipeRefresh.setRefreshing(true);
            loadProducts();
            // Opsional: tetap minta izin sekali, tapi tidak bergantung padanya untuk menampilkan produk
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
            } else {
                ToastManager.showToast(this, "Lokasi tidak aktif. Aktifkan GPS untuk filter jarak.");
            }
            loadProducts();
        }).addOnFailureListener(e -> {
            ToastManager.showToast(this, "Gagal mendapatkan lokasi: " + e.getMessage());
            loadProducts();
        });
    }

    @SuppressWarnings("unchecked")
    private void loadProducts() {
        String token = sessionManager.isLoggedIn() ? "Bearer " + sessionManager.getToken() : null;
        apiService.getProducts(token, searchQuery, selectedCategory, currentLat, currentLng, selectedDistance, 1000, 1000, 1, 1, 1)
                .enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Object>> call,
                            Response<ApiResponse<Object>> response) {
                        binding.swipeRefresh.setRefreshing(false);
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Object> apiResponse = response.body();
                            
                            if ("success".equals(apiResponse.getStatus()) && apiResponse.getData() != null) {
                                try {
                                    Gson gson = new Gson();
                                    com.google.gson.JsonElement jsonElement = gson.toJsonTree(apiResponse.getData());
                                    List<Product> products = new ArrayList<>();
                                    
                                    if (jsonElement.isJsonArray()) {
                                        // Case 1: Direct list
                                        Type listType = new TypeToken<List<Product>>() {}.getType();
                                        products = gson.fromJson(jsonElement, listType);
                                    } else if (jsonElement.isJsonObject()) {
                                        // Case 2: Pagination object
                                        com.google.gson.JsonObject dataObj = jsonElement.getAsJsonObject();
                                        if (dataObj.has("data") && dataObj.get("data").isJsonArray()) {
                                            Type listType = new TypeToken<List<Product>>() {}.getType();
                                            products = gson.fromJson(dataObj.get("data"), listType);
                                        }
                                    }

                                    if (products != null && !products.isEmpty()) {
                                        // Strict Range-based distance filtering
                                        if (selectedDistance != null) {
                                            List<Product> filtered = new ArrayList<>();
                                            for (Product p : products) {
                                                Double dist = p.getDistanceKm();
                                                if (dist == null) continue;

                                                boolean match = false;
                                                if (selectedDistance == 1) {
                                                    match = dist <= 1.0;
                                                } else if (selectedDistance == 2) {
                                                    match = dist > 1.0 && dist <= 2.0;
                                                } else if (selectedDistance == 3) {
                                                    match = dist > 2.0 && dist <= 3.0;
                                                } else if (selectedDistance == 4) {
                                                    match = dist > 3.0 && dist <= 4.0;
                                                } else if (selectedDistance == 5) {
                                                    match = dist > 4.0 && dist <= 5.0;
                                                } else if (selectedDistance == 100) {
                                                    match = dist > 5.0;
                                                }

                                                if (match) {
                                                    filtered.add(p);
                                                }
                                            }
                                            products = filtered;
                                        }
                                        // Inject Ad Banners every 4 items
                                        List<Product> productsWithAds = new ArrayList<>();
                                        for (int i = 0; i < products.size(); i++) {
                                            productsWithAds.add(products.get(i));
                                            // Insert ad after every 4th actual product
                                            if ((i + 1) % 4 == 0) {
                                                Product dummyAd = new Product();
                                                dummyAd.setId(-1); // Marker for AD
                                                productsWithAds.add(dummyAd);
                                            }
                                        }

                                        productAdapter.updateData(productsWithAds);
                                    } else {
                                        productAdapter.updateData(new ArrayList<>());
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    productAdapter.updateData(new ArrayList<>());
                                    ToastManager.showToast(HomeActivity.this,
                                            getString(R.string.server_error));
                                }
                            } else {
                                // API tidak sukses atau tidak ada data
                                productAdapter.updateData(new ArrayList<>());
                                ToastManager.showToast(HomeActivity.this,
                                        getString(R.string.server_error));
                            }
                        } else {
                            // Response gagal
                            productAdapter.updateData(new ArrayList<>());
                            ToastManager.showToast(HomeActivity.this,
                                    getString(R.string.server_error));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                        binding.swipeRefresh.setRefreshing(false);
                        productAdapter.updateData(new ArrayList<>());
                        ToastManager.showToast(HomeActivity.this,
                                getString(R.string.network_error) + ": " + t.getMessage());
                    }
                });
    }

    // ==================== ACTIONS ====================

    private void addToCart(int productId) {
        actionHelper.addToCart(productId, 1, new ProductActionHelper.ActionCallback() {
            @Override
            public void onSuccess(String message) {
                ToastManager.showToast(HomeActivity.this, message);
                fetchUserCounts(); // INSTANT SYNC
            }

            @Override
            public void onError(String errorMessage) {
                ToastManager.showToast(HomeActivity.this, errorMessage);
            }
        });
    }

    private void toggleWishlist(int productId) {
        actionHelper.toggleWishlist(productId, new ProductActionHelper.ActionCallback() {
            @Override
            public void onSuccess(String message) {
                ToastManager.showToast(HomeActivity.this, message);
                fetchUserCounts(); // INSTANT SYNC
            }

            @Override
            public void onError(String errorMessage) {
                ToastManager.showToast(HomeActivity.this, errorMessage);
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
            // Hanya refetch lokasi jika izin benar-benar diberikan.
            // Jika ditolak, jangan panggil kembali fetchLocationAndProducts()
            // karena di dalamnya masih ada pemanggilan requestPermissions()
            // yang bisa memicu loop tanpa henti seperti di logcat.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocationAndProducts();
            } else {
                // User menolak izin: muat produk tanpa koordinat lokasi
                currentLat = null;
                currentLng = null;
                binding.swipeRefresh.setRefreshing(true);
                loadProducts();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reset bottom nav to Home when coming back
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
        com.octania.marketplace.utils.NavigationUtils.applyFloatingEffect(binding.bottomNav);
        fetchUserCounts();

        // Always refresh products on resume to ensure data is current
        binding.swipeRefresh.setRefreshing(true);
        binding.swipeRefresh.postDelayed(this::fetchLocationAndProducts, 300);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleRefreshIntent(intent);
    }

    private void handleRefreshIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("refresh_from_add_product", false)) {
            if (productAdapter != null) {
                productAdapter.clearData();
            }
            binding.swipeRefresh.setRefreshing(true);
            binding.swipeRefresh.postDelayed(this::fetchLocationAndProducts, 500);
            intent.removeExtra("refresh_from_add_product");
        }
    }
}
