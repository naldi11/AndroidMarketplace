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
import android.content.SharedPreferences;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.octania.marketplace.R;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.model.Product;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityGuestHomeBinding;
import com.octania.marketplace.ui.auth.LoginActivity;
import com.octania.marketplace.ui.product.ProductDetailActivity;
import com.octania.marketplace.utils.ToastManager;
import com.octania.marketplace.utils.ProductActionHelper;
import com.octania.marketplace.utils.SessionManager;

import java.lang.reflect.Type;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GuestHomeActivity extends AppCompatActivity {
    private ActivityGuestHomeBinding binding;
    private SessionManager sessionManager;
    private ApiService apiService;
    private ProductAdapter productAdapter;
    private CategoryAdapter categoryAdapter;
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
        binding = ActivityGuestHomeBinding.inflate(getLayoutInflater());
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

        setupDistanceFilter();
        setupCategoryRecycler();
        setupProductRecycler();
        setupSearch();

        binding.swipeRefresh.setColorSchemeResources(R.color.primary_orange);
        binding.swipeRefresh.setOnRefreshListener(this::fetchLocationAndProducts);

        binding.btnHeaderLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });

        binding.btnSellerLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });

        fetchCategories();
        fetchAdBanners();
        fetchLocationAndProducts();

        binding.btnFilter.setOnClickListener(v -> binding.drawerLayout.openDrawer(androidx.core.view.GravityCompat.END));
        binding.btnApplyFilter.setOnClickListener(v -> {
            binding.drawerLayout.closeDrawer(androidx.core.view.GravityCompat.END);
            String label = (selectedDistance == null || selectedDistance == 100) ? "semua jarak" : selectedDistance + " KM";
            Toast.makeText(this, "Memfilter produk dalam " + label, Toast.LENGTH_SHORT).show();
            
            binding.swipeRefresh.setRefreshing(true);
            fetchLocationAndProducts(); 
        });
    }

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
                        productAdapter.setAdBanners(res.getData());
                    }
                }
            }

            @Override
            public void onFailure(Call<com.octania.marketplace.data.model.response.AdBannerResponse> call, Throwable t) {
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

    private void setupProductRecycler() {
        productAdapter = new ProductAdapter(this, new ProductAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Product product) {
                Intent intent = new Intent(GuestHomeActivity.this, ProductDetailActivity.class);
                intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
                startActivity(intent);
            }

            @Override
            public void onAddToCartClick(Product product) {
                Toast.makeText(GuestHomeActivity.this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(GuestHomeActivity.this, LoginActivity.class));
            }

            @Override
            public void onWishlistClick(Product product) {
                Toast.makeText(GuestHomeActivity.this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(GuestHomeActivity.this, LoginActivity.class));
            }
        });
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        binding.rvProducts.setLayoutManager(gridLayoutManager);
        binding.rvProducts.setNestedScrollingEnabled(false);
        binding.rvProducts.setAdapter(productAdapter);
        binding.rvProducts.setVisibility(View.VISIBLE);
    }

    private void setupSearch() {
        SharedPreferences prefs = getSharedPreferences("SearchHistory", MODE_PRIVATE);
        Set<String> historySet = prefs.getStringSet("history", new HashSet<>());
        List<String> historyList = new ArrayList<>(historySet);
        
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
                            
                            categoryAdapter.updateData(categories);
                        } catch (Exception e) {
                            Log.e("GUEST_HOME_DEBUG", "Error parsing categories", e);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
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
        Integer radius = (selectedDistance != null) ? selectedDistance : (currentLat != null && currentLng != null ? 15 : 100);
        
        apiService.getProducts(null, searchQuery, selectedCategory, currentLat, currentLng, radius, 1000, 1000, 1, 1, 1)
                .enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Object>> call,
                             Response<ApiResponse<Object>> response) {
                        binding.swipeRefresh.setRefreshing(false);
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Object> apiResponse = response.body();
                            String status = apiResponse.getStatus();
                            Object data = apiResponse.getData();
                            
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

                                                if (dist <= maxDist) {
                                                    filtered.add(p);
                                                }
                                            }
                                            products = filtered;
                                        }

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
                                    productAdapter.updateData(new ArrayList<>());
                                    Toast.makeText(GuestHomeActivity.this, "Gagal memproses data server", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                productAdapter.updateData(new ArrayList<>());
                                Toast.makeText(GuestHomeActivity.this, "Status API: " + status, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            productAdapter.updateData(new ArrayList<>());
                            Toast.makeText(GuestHomeActivity.this, "Error " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                        binding.swipeRefresh.setRefreshing(false);
                        productAdapter.updateData(new ArrayList<>());
                        ToastManager.showToast(GuestHomeActivity.this,
                                getString(R.string.network_error) + ": " + t.getMessage());
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocationAndProducts();
            } else {
                currentLat = null;
                currentLng = null;
                loadProducts();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.fetchLocationAndProducts();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }
}
