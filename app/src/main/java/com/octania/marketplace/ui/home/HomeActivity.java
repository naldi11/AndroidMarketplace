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
import com.octania.marketplace.data.model.Voucher;
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
    private com.google.android.gms.location.LocationCallback locationCallback;
    private Double lastLoadedLat = null;
    private Double lastLoadedLng = null;

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

        locationCallback = new com.google.android.gms.location.LocationCallback() {
            @Override
            public void onLocationResult(@NonNull com.google.android.gms.location.LocationResult locationResult) {
                if (locationResult.getLastLocation() == null) return;
                
                double lat = locationResult.getLastLocation().getLatitude();
                double lng = locationResult.getLastLocation().getLongitude();
                
                boolean isFirstLocation = (currentLat == null || currentLng == null);
                
                currentLat = lat;
                currentLng = lng;
                
                if (productAdapter != null) {
                    productAdapter.updateUserLocation(currentLat, currentLng);
                }
                
                if (isFirstLocation || lastLoadedLat == null || lastLoadedLng == null) {
                    lastLoadedLat = lat;
                    lastLoadedLng = lng;
                    loadProducts();
                } else {
                    float[] results = new float[1];
                    android.location.Location.distanceBetween(lastLoadedLat, lastLoadedLng, lat, lng, results);
                    if (results[0] > 200) { // 200 meters
                        lastLoadedLat = lat;
                        lastLoadedLng = lng;
                        loadProducts();
                    }
                }
            }
        };

        // Guest browsing is allowed, so no immediate redirect on startup

        setupDistanceFilter();
        setupCategoryRecycler();
        setupVoucherRecycler();
        setupProductRecycler();
        setupSearch();
        setupBottomNav();

        binding.swipeRefresh.setColorSchemeResources(R.color.primary_orange);
        binding.swipeRefresh.setOnRefreshListener(this::fetchLocationAndProducts);

        binding.btnNavCart.setOnClickListener(v -> {
            if (!sessionManager.isLoggedIn()) {
                Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                return;
            }
            startActivity(new Intent(this, com.octania.marketplace.ui.cart.CartActivity.class));
        });

        binding.btnNavChat.setOnClickListener(v -> {
            if (!sessionManager.isLoggedIn()) {
                Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                return;
            }
            startActivity(new Intent(this, com.octania.marketplace.ui.chat.ConversationsActivity.class));
        });



        fetchCategories();
        fetchVouchers();
        fetchAdBanners();
        fetchLocationAndProducts();
        handleRefreshIntent(getIntent());

        binding.btnFilter.setOnClickListener(v -> binding.drawerLayout.openDrawer(androidx.core.view.GravityCompat.END));
        binding.btnApplyFilter.setOnClickListener(v -> {
            binding.drawerLayout.closeDrawer(androidx.core.view.GravityCompat.END);
            String label = (selectedDistance == null || selectedDistance == 100) ? "semua jarak" : selectedDistance + " KM";
            android.util.Log.d("FILTER_DEBUG", "Applying Filter -> Radius: " + (selectedDistance != null ? selectedDistance : 100) + " KM");
            Toast.makeText(this, "Memfilter produk dalam " + label, Toast.LENGTH_SHORT).show();
            
            // Re-fetch everything with the new filter
            binding.swipeRefresh.setRefreshing(true);
            fetchLocationAndProducts(); 
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
        binding.rvCategories.setNestedScrollingEnabled(false);
        binding.rvCategories.setAdapter(categoryAdapter);
    }

    private void setupVoucherRecycler() {
        voucherAdapter = new VoucherAdapter(this, new VoucherAdapter.OnVoucherClickListener() {
            @Override
            public void onVoucherClick(VoucherAdapter.VoucherModel voucher) {
                // Just show code when clicked for now (can copy to clipboard in future)
                ToastManager.showToast(HomeActivity.this, "Gunakan kode: " + voucher.code);
            }

            @Override
            public void onClaimClick(VoucherAdapter.VoucherModel voucher) {
                claimVoucher(voucher);
            }
        });
        binding.rvVouchers.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvVouchers.setAdapter(voucherAdapter);
    }

    private void setupProductRecycler() {
        productAdapter = new ProductAdapter(this, new ProductAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Product product) {
                if (!sessionManager.isLoggedIn()) {
                    Toast.makeText(HomeActivity.this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(HomeActivity.this, LoginActivity.class));
                    return;
                }
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
        binding.rvProducts.setNestedScrollingEnabled(false);
        binding.rvProducts.setAdapter(productAdapter);
        
        // DEBUG: Add color to see where it is
        binding.rvProducts.setBackgroundColor(android.graphics.Color.parseColor("#10FF0000")); // Very faint red
        
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
        binding.bottomNavInclude.bottomNav.setSelectedItemId(R.id.nav_home);
        com.octania.marketplace.utils.NavigationUtils.applyFloatingEffect(binding.bottomNavInclude.bottomNav);

        binding.bottomNavInclude.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            }

            // Guest guards for all other tabs
            if (!sessionManager.isLoggedIn()) {
                Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                return false;
            }

            if (id == R.id.nav_orders) {
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
                            Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
                            List<Map<String, Object>> rawCategories = gson.fromJson(json, listType);
                            
                            List<Map<String, Object>> categories = new ArrayList<>();
                            for (Map<String, Object> map : rawCategories) {
                                // Safe extraction
                                Map<String, Object> category = new java.util.HashMap<>();
                                Object idObj = map.get("id");
                                int id = (idObj instanceof Double) ? ((Double) idObj).intValue() : 
                                         (idObj instanceof Integer) ? (Integer) idObj : 0;
                                
                                category.put("id", id);
                                category.put("name", map.get("name"));
                                category.put("slug", map.get("slug"));
                                category.put("icon", map.get("icon"));
                                categories.add(category);
                            }
                            
                            Log.d("HOME_DEBUG", "Categories loaded: " + categories.size());
                            categoryAdapter.updateData(categories);
                        } catch (Exception e) {
                            Log.e("HOME_DEBUG", "Error parsing categories", e);
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
        if (!sessionManager.isLoggedIn()) {
            binding.rvVouchers.setVisibility(View.GONE);
            binding.tvVoucherLabel.setVisibility(View.GONE);
            return;
        }
        String token = "Bearer " + sessionManager.getToken();
        apiService.getPublicVouchers(token).enqueue(new Callback<ApiResponse<List<Voucher>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Voucher>>> call, Response<ApiResponse<List<Voucher>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Voucher>> apiResponse = response.body();
                    if ("success".equals(apiResponse.getStatus()) && apiResponse.getData() != null) {
                        List<Voucher> vouchers = apiResponse.getData();
                        List<VoucherAdapter.VoucherModel> models = new ArrayList<>();
                        for (Voucher v : vouchers) {
                            VoucherAdapter.VoucherModel m = new VoucherAdapter.VoucherModel();
                            m.id = v.getId();
                            m.code = v.getCode();
                            m.discount_amount = v.getDiscountAmount();
                            m.min_purchase = v.getMinPurchase();
                            m.terms = v.getTerms();
                            m.is_claimed = v.isClaimed();
                            models.add(m);
                        }
                        voucherAdapter.updateData(models);
                        int visibility = models.isEmpty() ? View.GONE : View.VISIBLE;
                        binding.rvVouchers.setVisibility(visibility);
                        binding.tvVoucherLabel.setVisibility(visibility);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Voucher>>> call, Throwable t) {
                // Ignore failure quietly
            }
        });
    }

    private void claimVoucher(VoucherAdapter.VoucherModel model) {
        String token = "Bearer " + sessionManager.getToken();
        apiService.claimVoucher(token, model.id).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    ToastManager.showToast(HomeActivity.this, "Voucher berhasil diklaim!");
                    fetchVouchers(); // Refresh list to update state
                } else {
                    ToastManager.showToast(HomeActivity.this, "Gagal klaim voucher: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                ToastManager.showToast(HomeActivity.this, "Error: " + t.getMessage());
            }
        });
    }

    @SuppressWarnings("MissingPermission")
    private void fetchLocationAndProducts() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            currentLat = null;
            currentLng = null;
            loadProducts();
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        lastLoadedLat = null;
        lastLoadedLng = null;
        
        startLocationUpdates();
        loadProducts();
    }

    @SuppressWarnings("MissingPermission")
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        com.google.android.gms.location.LocationRequest locationRequest = 
            com.google.android.gms.location.LocationRequest.create()
                .setPriority(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY)
                .setInterval(2000)
                .setFastestInterval(1000);

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, android.os.Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadProducts() {
        String token = sessionManager.isLoggedIn() ? "Bearer " + sessionManager.getToken() : null;
        Integer radius = (selectedDistance != null) ? selectedDistance : (currentLat != null && currentLng != null ? 15 : 100);
        
        Log.d("API_DEBUG", "Fetching Products with: " +
                "Lat=" + currentLat + 
                ", Lng=" + currentLng + 
                ", Radius=" + radius + " KM" +
                ", Query=" + searchQuery);

        apiService.getProducts(token, searchQuery, selectedCategory, currentLat, currentLng, radius, 1000, 1000, 1, 1, 1)
                .enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Object>> call,
                             Response<ApiResponse<Object>> response) {
                        binding.swipeRefresh.setRefreshing(false);
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Object> apiResponse = response.body();
                            String status = apiResponse.getStatus();
                            Object data = apiResponse.getData();
                            
                            Log.d("HOME_DEBUG", "API Response: " + status + ", data present: " + (data != null));
                            
                            if ("success".equals(status) && data != null) {
                                try {
                                    Gson gson = new Gson();
                                    com.google.gson.JsonElement jsonElement = gson.toJsonTree(data);
                                    List<Product> products = new ArrayList<>();
                                    
                                    if (jsonElement.isJsonArray()) {
                                        Type listType = new TypeToken<List<Product>>() {}.getType();
                                        products = gson.fromJson(jsonElement, listType);
                                    } else if (jsonElement.isJsonObject()) {
                                        com.google.gson.JsonObject dataObj = jsonElement.getAsJsonObject();
                                        if (dataObj.has("data") && dataObj.get("data").isJsonArray()) {
                                            Type listType = new TypeToken<List<Product>>() {}.getType();
                                            products = gson.fromJson(dataObj.get("data"), listType);
                                        }
                                    }

                                    int originalCount = products != null ? products.size() : 0;
                                    Log.d("HOME_DEBUG", "Products parsed: " + originalCount);

                                    if (products != null && !products.isEmpty()) {
                                        // Filter by distance on client-side
                                        if (currentLat != null && currentLng != null) {
                                            double maxDist = (selectedDistance != null) ? (double) selectedDistance : 15.0;
                                            List<Product> filtered = new ArrayList<>();
                                            
                                            for (Product p : products) {
                                                Double dist = p.getDistanceKm();
                                                
                                                // Failsafe: if dist is null but product has coordinates, calculate it client-side
                                                if (dist == null && p.getLatitude() != null && p.getLongitude() != null) {
                                                    float[] results = new float[1];
                                                    android.location.Location.distanceBetween(
                                                        currentLat, currentLng,
                                                        p.getLatitude(), p.getLongitude(),
                                                        results
                                                    );
                                                    dist = (double) (results[0] / 1000.0f); // Convert meters to KM
                                                }

                                                if (dist == null) {
                                                    continue;
                                                }

                                                Log.d("DISTANCE_DEBUG", "Product: " + p.getName() + ", Distance: " + dist + " KM, Max: " + maxDist);

                                                if (dist <= maxDist) {
                                                    filtered.add(p);
                                                }
                                            }
                                            products = filtered;
                                            Log.d("DISTANCE_DEBUG", "Products after filter: " + products.size());
                                        }

                                        // Inject Ads
                                        List<Product> productsWithAds = new ArrayList<>();
                                        for (int i = 0; i < products.size(); i++) {
                                            productsWithAds.add(products.get(i));
                                            if ((i + 1) % 4 == 0) {
                                                Product dummyAd = new Product();
                                                dummyAd.setId(-1);
                                                productsWithAds.add(dummyAd);
                                            }
                                        }

                                        productAdapter.updateData(productsWithAds);
                                        binding.rvProducts.setVisibility(View.VISIBLE);
                                    } else {
                                        productAdapter.updateData(new ArrayList<>());
                                    }
                                } catch (Exception e) {
                                    Log.e("HOME_DEBUG", "Parsing error", e);
                                    productAdapter.updateData(new ArrayList<>());
                                    Toast.makeText(HomeActivity.this, "Gagal memproses data server", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                productAdapter.updateData(new ArrayList<>());
                                Toast.makeText(HomeActivity.this, "Status API: " + status, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            productAdapter.updateData(new ArrayList<>());
                            Toast.makeText(HomeActivity.this, "Error " + response.code(), Toast.LENGTH_SHORT).show();
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
                            // Simpan ke cache agar activity lain bisa baca
                            sessionManager.saveBadgeCounts(wishlistCount, cartCount);
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
        // Gunakan BadgeUtils agar konsisten dengan activity lain
        com.octania.marketplace.utils.BadgeUtils.applyBadges(
                binding.bottomNavInclude.bottomNav, wishlistCount, cartCount);
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
    //    //        binding.swipeRefresh.setRefreshing(true);
                loadProducts();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reset bottom nav to Home when coming back
        binding.bottomNavInclude.bottomNav.setSelectedItemId(R.id.nav_home);
        com.octania.marketplace.utils.NavigationUtils.applyFloatingEffect(binding.bottomNavInclude.bottomNav);

        if (sessionManager.isLoggedIn()) {
            fetchUserCounts();
            // Update badge wishlist & cart
            com.octania.marketplace.utils.BadgeUtils.fetchAndApply(
                    this, sessionManager, binding.bottomNavInclude.bottomNav);
            // Update badge dispute aktif di "Pesanan Saya"
            String token = "Bearer " + sessionManager.getToken();
            com.octania.marketplace.utils.BadgeUtils.fetchAndApplyDisputeBadge(
                    this, token, binding.bottomNavInclude.bottomNav);
        } else {
            // Remove badges if guest
            binding.bottomNavInclude.bottomNav.removeBadge(R.id.nav_wishlist);
            binding.bottomNavInclude.bottomNav.removeBadge(R.id.nav_orders);
        }

        // Always refresh products on resume to ensure data is current
        this.fetchLocationAndProducts();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
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
//            binding.swipeRefresh.setRefreshing(true);
//            binding.swipeRefresh.postDelayed(this::fetchLocationAndProducts, 500);
            this.fetchLocationAndProducts();
            intent.removeExtra("refresh_from_add_product");
        }
    }
}
