package com.octania.marketplace.ui.cart;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.model.CartItem;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityCheckoutBinding;
import com.octania.marketplace.ui.home.HomeActivity;
import com.octania.marketplace.ui.profile.MapPickerActivity;
import com.octania.marketplace.utils.SessionManager;
import com.octania.marketplace.R;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CheckoutActivity extends AppCompatActivity implements CheckoutAdapter.OnQuantityChangeListener {

    private ActivityCheckoutBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private CheckoutAdapter checkoutAdapter;
    private FusedLocationProviderClient fusedLocationClient;

    private List<Integer> cartIds = new ArrayList<>();
    private List<CartItem> cartItems = new ArrayList<>();
    private int selectedAddressId = -1;
    private int selectedPaymentMethodId = -1;
    private List<Map<String, Object>> userAddresses = new ArrayList<>();
    private List<Map<String, Object>> paymentMethodsList = new ArrayList<>();

    private int selectedUserVoucherId = -1;
    private String selectedVoucherName = null;
    private String appliedVoucherCode = null;
    private double discountAmount = 0;
    private double serviceFee = 0;
    private double adminFee = 0;
    private double shippingCost = 0;
    private double subtotal = 0;
    private String deliveryType = "courier";
    private String shippingVehicle = "motor";
    private final String[] vehicleOptions = {"Kurir Motor", "Becak (Bentor)", "Mobil Pickup", "Jemput Sendiri"};
    private final String[] vehicleValues = {"motor", "becak", "pickup", "jemput_sendiri"};
    private double lastTotalWeightKg = -1.0;
    private int spinnerUpdateDepth = 0; // Counter to prevent recursive/race-condition spinner events
    private boolean isLoadingAddresses = false;
    private Double deviceLat = null;
    private Double deviceLng = null;
    private Double selectedAddressLat = null;
    private Double selectedAddressLng = null;

    // Direct buy (skip cart)
    private boolean isDirectBuy = false;
    private int directProductId = -1;
    private int directQuantity = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.content.SharedPreferences prefs = getSharedPreferences("osmdroid", 0);
        org.osmdroid.config.Configuration.getInstance().load(getApplicationContext(), prefs);
        
        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.rowServiceFee.setVisibility(View.GONE);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        binding.mapPreview.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
        binding.mapPreview.setMultiTouchControls(false);
        binding.mapPreview.getZoomController().setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER);

        // Capture device GPS for shipping calculation fallback
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    deviceLat = location.getLatitude();
                    deviceLng = location.getLongitude();
                }
            });
        }

        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Silakan login.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupRecyclerView();

        isDirectBuy = getIntent().getBooleanExtra("direct_buy", false);

        if (isDirectBuy) {
            directProductId = getIntent().getIntExtra("product_id", -1);
            directQuantity = getIntent().getIntExtra("quantity", 1);
            if (directProductId == -1) {
                Toast.makeText(this, "Produk tidak valid.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            cartIds = getIntent().getIntegerArrayListExtra("cart_ids");
            if (cartIds == null || cartIds.isEmpty()) {
                Toast.makeText(this, "Tidak ada item untuk di-checkout.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        binding.cardAddress.setOnClickListener(v -> handleAddressClick());
        binding.btnPlaceOrder.setOnClickListener(v -> processCheckout());
        binding.btnSelectVoucher.setOnClickListener(v -> showVoucherSelection());

        // Initialize shipping vehicle spinner
        binding.spinnerShippingVehicle.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (spinnerUpdateDepth > 0) return; // Sedang dalam proses update adapter, abaikan event ini
                String selected = vehicleValues[position];
                
                // Get current total weight
                double totalWeightGrams = 0;
                for (CartItem item : cartItems) {
                    if (item.getProduct() != null && item.getProduct().getWeight() != null) {
                        totalWeightGrams += item.getProduct().getWeight() * item.getQuantity();
                    }
                }
                double totalWeightKg = totalWeightGrams / 1000.0;
                
                boolean isEnabled = true;
                if ("motor".equals(selected) && totalWeightKg > 20) isEnabled = false;
                if ("becak".equals(selected) && totalWeightKg > 100) isEnabled = false;
                if ("pickup".equals(selected) && totalWeightKg > 1000) isEnabled = false;
                
                if (!isEnabled) {
                    // Force select recommended index
                    String recommended = getRecommendedVehicle(totalWeightKg);
                    int recIndex = 0;
                    for (int i = 0; i < vehicleValues.length; i++) {
                        if (vehicleValues[i].equals(recommended)) {
                            recIndex = i;
                            break;
                        }
                    }
                    spinnerUpdateDepth++;
                    binding.spinnerShippingVehicle.setSelection(recIndex);
                    spinnerUpdateDepth--;
                    Toast.makeText(CheckoutActivity.this, "Opsi pengiriman tersebut melebihi kapasitas berat barang!", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (!selected.equals(shippingVehicle)) {
                    shippingVehicle = selected;
                    if ("jemput_sendiri".equals(shippingVehicle)) {
                        deliveryType = "pickup";
                    } else {
                        deliveryType = "courier";
                    }
                    calculateTotal();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });



        loadUserAddresses();
        loadCheckoutCartItems();
        loadPaymentMethods();
        fetchUserProfile();
    }

    private void fetchUserProfile() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getUserProfile(token).enqueue(new Callback<com.octania.marketplace.data.model.User>() {
            @Override
            public void onResponse(Call<com.octania.marketplace.data.model.User> call, Response<com.octania.marketplace.data.model.User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sessionManager.saveUser(response.body());
                    // Refresh current address UI if it's already showing fallback "User"
                    if (binding.tvRecipientName.getText().toString().equals("User") || 
                        binding.tvRecipientName.getText().toString().equals("Memuat alamat...")) {
                        Map<String, String> user = sessionManager.getUserDetails();
                        binding.tvRecipientName.setText(user.get(SessionManager.KEY_NAME));
                        binding.tvPhoneNumber.setText(user.get(SessionManager.KEY_PHONE));
                    }
                }
            }

            @Override
            public void onFailure(Call<com.octania.marketplace.data.model.User> call, Throwable t) {
                // Silent failure, fallback values are already in place
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only reload addresses when returning from address management screen.
        // Initial load is handled in onCreate via loadUserAddresses().
        if (userAddresses.isEmpty()) {
            loadUserAddresses();
        }
    }

    private void setupRecyclerView() {
        checkoutAdapter = new CheckoutAdapter(this, this);
        binding.rvCheckoutItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCheckoutItems.setAdapter(checkoutAdapter);
        binding.rvCheckoutItems.setNestedScrollingEnabled(false);
    }

    private void loadCheckoutCartItems() {
        String token = "Bearer " + sessionManager.getToken();

        if (isDirectBuy) {
            // Load product details directly
            apiService.getProductDetail(token, directProductId)
                    .enqueue(new Callback<ApiResponse<com.octania.marketplace.data.model.Product>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<com.octania.marketplace.data.model.Product>> call,
                                Response<ApiResponse<com.octania.marketplace.data.model.Product>> response) {
                            if (response.isSuccessful() && response.body() != null
                                    && "success".equals(response.body().getStatus())) {
                                com.octania.marketplace.data.model.Product product = response.body().getData();
                                CartItem directItem = new CartItem();
                                directItem.setProduct(product);
                                directItem.setProductId(directProductId);
                                directItem.setQuantity(directQuantity);

                                cartItems.clear();
                                cartItems.add(directItem);
                                setInitialVehicleFromCartItems();
                                checkoutAdapter.updateData(cartItems);
                                calculateTotal();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<com.octania.marketplace.data.model.Product>> call,
                                Throwable t) {
                            Toast.makeText(CheckoutActivity.this, "Gagal meload produk: " + t.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
            return;
        }

        apiService.getCart(token).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> res = response.body();
                    if ("success".equals(res.getStatus())) {
                        try {
                            Map<String, Object> dataMap = (Map<String, Object>) res.getData();
                            Object itemsObj = dataMap.get("items");

                            Gson gson = new Gson();
                            String json = gson.toJson(itemsObj);
                            Type listType = new TypeToken<List<CartItem>>() {
                            }.getType();
                            List<CartItem> allCartItems = gson.fromJson(json, listType);

                            cartItems.clear();
                            if (allCartItems != null) {
                                for (CartItem item : allCartItems) {
                                    if (cartIds.contains(item.getId())) {
                                        cartItems.add(item);
                                    }
                                }
                            }

                            setInitialVehicleFromCartItems();
                            checkoutAdapter.updateData(cartItems);
                            calculateTotal();
                        } catch (Exception e) {
                            Toast.makeText(CheckoutActivity.this, "Gagal memproses detail item", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                Toast.makeText(CheckoutActivity.this, "Gagal meload keranjang: " + t.getMessage(), Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    private void calculateTotal() {
        Map<String, Object> body = new java.util.HashMap<>();

        if (isDirectBuy) {
            body.put("product_id", directProductId);
            body.put("quantity", directQuantity);
        } else {
            if (cartIds == null || cartIds.isEmpty())
                return;
            body.put("cart_ids", cartIds);
        }

        body.put("delivery_type", deliveryType);
        body.put("shipping_vehicle", shippingVehicle);
        if (selectedAddressId != -1) {
            body.put("user_address_id", selectedAddressId);
        }
        // Send buyer coordinates for shipping calculation
        // Priority: selected address coordinates > device GPS
        Double buyerLat = (selectedAddressLat != null) ? selectedAddressLat : deviceLat;
        Double buyerLng = (selectedAddressLng != null) ? selectedAddressLng : deviceLng;
        if (buyerLat != null && buyerLng != null) {
            body.put("buyer_latitude", buyerLat);
            body.put("buyer_longitude", buyerLng);
        }
        if (selectedUserVoucherId != -1) {
            body.put("user_voucher_id", selectedUserVoucherId);
        }

        // Calculate and display weight warning
        double totalWeightGrams = 0;
        for (CartItem item : cartItems) {
            if (item.getProduct() != null && item.getProduct().getWeight() != null) {
                totalWeightGrams += item.getProduct().getWeight() * item.getQuantity();
            }
        }
        double totalWeightKg = totalWeightGrams / 1000.0;
        
        // Only update spinner adapter if weight actually changed
        if (totalWeightKg != lastTotalWeightKg) {
            lastTotalWeightKg = totalWeightKg;
            updateVehicleSpinnerAdapter(totalWeightKg);
        }

        String recommendedLabel = "";
        String recommendedVal = getRecommendedVehicle(totalWeightKg);
        if ("motor".equals(recommendedVal)) {
            recommendedLabel = "Kurir Motor";
        } else if ("becak".equals(recommendedVal)) {
            recommendedLabel = "Becak (Bentor)";
        } else if ("pickup".equals(recommendedVal)) {
            recommendedLabel = "Mobil Pickup";
        } else {
            recommendedLabel = "Jemput Sendiri";
        }

        if (totalWeightKg > 20) {
            binding.tvShippingWarning.setVisibility(View.VISIBLE);
            binding.tvShippingWarning.setTextColor(Color.RED);
            String warningMsg = String.format(Locale.getDefault(),
                    "Total berat barang: %.1f kg.\nOpsi Kurir Motor dinonaktifkan (kapasitas maks 20 kg). Disarankan menggunakan %s.",
                    totalWeightKg, recommendedLabel);
            if (totalWeightKg > 100) {
                warningMsg = String.format(Locale.getDefault(),
                        "Total berat barang: %.1f kg.\nOpsi Kurir Motor & Becak dinonaktifkan (kapasitas maks 100 kg). Disarankan menggunakan %s.",
                        totalWeightKg, recommendedLabel);
            }
            if (totalWeightKg > 1000) {
                warningMsg = String.format(Locale.getDefault(),
                        "Total berat barang: %.1f kg.\nOpsi Kurir Motor, Becak & Pickup dinonaktifkan (kapasitas maks 1000 kg). Wajib menggunakan %s.",
                        totalWeightKg, recommendedLabel);
            }
            binding.tvShippingWarning.setText(warningMsg);
        } else {
            binding.tvShippingWarning.setVisibility(View.VISIBLE);
            binding.tvShippingWarning.setTextColor(Color.parseColor("#4CAF50")); // Green color
            binding.tvShippingWarning.setText(String.format(Locale.getDefault(),
                    "Total berat barang: %.1f kg. (Kapasitas Kurir Motor aman - di bawah 20 kg). Pilihan disarankan: %s.",
                    totalWeightKg, recommendedLabel));
        }

        binding.btnPlaceOrder.setEnabled(false);
        String token = "Bearer " + sessionManager.getToken();
        
        // Include payment_method_id for adminFee calculation
        if (selectedPaymentMethodId != -1) {
            body.put("payment_method_id", selectedPaymentMethodId);
        }

        apiService.previewCheckout(token, body).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                binding.btnPlaceOrder.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> res = response.body();
                    if ("success".equals(res.getStatus())) {
                        try {
                            Map<String, Object> data = (Map<String, Object>) res.getData();
                            subtotal = ((Number) data.get("subtotal_product")).doubleValue();
                            serviceFee = ((Number) data.get("service_fee")).doubleValue();
                            
                            if (data.containsKey("admin_fee") && data.get("admin_fee") != null) {
                                adminFee = ((Number) data.get("admin_fee")).doubleValue();
                            } else {
                                adminFee = 0;
                            }
                            
                            shippingCost = ((Number) data.get("shipping_cost")).doubleValue();
                            discountAmount = ((Number) data.get("discount")).doubleValue();
                            double grandTotal = ((Number) data.get("grand_total")).doubleValue();

                            binding.tvSubtotal.setText(String.format("Rp %,.0f", subtotal));
                            binding.tvServiceFee.setText(String.format("Rp %,.0f", serviceFee));
                            binding.tvShippingCost.setText(String.format("Rp %,.0f", shippingCost));
                            binding.tvShippingCostDetail.setText(String.format("Estimasi Ongkir: Rp %,.0f", shippingCost));

                            // suggested_vehicle logic
                            // suggested_vehicle text logic (display suggestion only, do not force-select)
                            if (data.containsKey("suggested_vehicle") && data.get("suggested_vehicle") != null) {
                                String suggested = String.valueOf(data.get("suggested_vehicle"));
                                if (!suggested.isEmpty() && !"null".equals(suggested)) {
                                    String readableSuggested = "";
                                    for (int i = 0; i < vehicleValues.length; i++) {
                                        if (vehicleValues[i].equals(suggested)) {
                                            readableSuggested = vehicleOptions[i];
                                            break;
                                        }
                                    }
                                    if (!readableSuggested.isEmpty()) {
                                        binding.tvSuggestedVehicle.setVisibility(View.VISIBLE);
                                        binding.tvSuggestedVehicle.setText("Penjual menyarankan menggunakan: " + readableSuggested + " (Saran)");
                                    } else {
                                        binding.tvSuggestedVehicle.setVisibility(View.GONE);
                                    }
                                } else {
                                    binding.tvSuggestedVehicle.setVisibility(View.GONE);
                                }
                            } else {
                                binding.tvSuggestedVehicle.setVisibility(View.GONE);
                            }
                            binding.spinnerShippingVehicle.setEnabled(true);

                            if (data.containsKey("distance_km") && data.get("distance_km") != null &&
                                    data.containsKey("buyer_latitude") && data.get("buyer_latitude") != null &&
                                    data.containsKey("buyer_longitude") && data.get("buyer_longitude") != null &&
                                    data.containsKey("seller_latitude") && data.get("seller_latitude") != null &&
                                    data.containsKey("seller_longitude") && data.get("seller_longitude") != null) {
                                
                                double dist = ((Number) data.get("distance_km")).doubleValue();
                                double buyerLat = ((Number) data.get("buyer_latitude")).doubleValue();
                                double buyerLng = ((Number) data.get("buyer_longitude")).doubleValue();
                                double sellerLat = ((Number) data.get("seller_latitude")).doubleValue();
                                double sellerLng = ((Number) data.get("seller_longitude")).doubleValue();

                                binding.cardDistancePreview.setVisibility(View.VISIBLE);
                                
                                int durationMinutes = 0;
                                if (data.containsKey("duration_seconds") && data.get("duration_seconds") != null) {
                                    double seconds = ((Number) data.get("duration_seconds")).doubleValue();
                                    durationMinutes = (int) Math.round(seconds / 60.0);
                                } else {
                                    durationMinutes = (int) Math.round(dist * 2.0);
                                }
                                if (durationMinutes < 1) durationMinutes = 1;
                                
                                binding.tvDurationPreview.setText(durationMinutes + " mnt");
                                java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols(new Locale("in", "ID"));
                                java.text.DecimalFormat df = new java.text.DecimalFormat("0.###", symbols);
                                binding.tvDistancePreview.setText("(" + df.format(dist) + " km)");

                                binding.mapPreview.getOverlays().clear();

                                org.osmdroid.util.GeoPoint buyerPoint = new org.osmdroid.util.GeoPoint(buyerLat, buyerLng);
                                org.osmdroid.views.overlay.Marker buyerMarker = new org.osmdroid.views.overlay.Marker(binding.mapPreview);
                                buyerMarker.setPosition(buyerPoint);
                                buyerMarker.setTitle("Lokasi Anda");
                                buyerMarker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
                                binding.mapPreview.getOverlays().add(buyerMarker);

                                org.osmdroid.util.GeoPoint sellerPoint = new org.osmdroid.util.GeoPoint(sellerLat, sellerLng);
                                org.osmdroid.views.overlay.Marker sellerMarker = new org.osmdroid.views.overlay.Marker(binding.mapPreview);
                                sellerMarker.setPosition(sellerPoint);
                                sellerMarker.setTitle("Lokasi Penjual");
                                sellerMarker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
                                binding.mapPreview.getOverlays().add(sellerMarker);

                                java.util.List<org.osmdroid.util.GeoPoint> pts = new java.util.ArrayList<>();
                                List<Map<String, Object>> shapeList = null;
                                if (data.containsKey("route_shape") && data.get("route_shape") instanceof List) {
                                    shapeList = (List<Map<String, Object>>) data.get("route_shape");
                                }

                                if (shapeList != null && !shapeList.isEmpty()) {
                                    for (Map<String, Object> pt : shapeList) {
                                        double pLat = ((Number) pt.get("lat")).doubleValue();
                                        double pLng = ((Number) pt.get("lng")).doubleValue();
                                        pts.add(new org.osmdroid.util.GeoPoint(pLat, pLng));
                                    }
                                } else {
                                    pts.add(buyerPoint);
                                    pts.add(sellerPoint);
                                }

                                org.osmdroid.views.overlay.Polyline line = new org.osmdroid.views.overlay.Polyline();
                                line.setPoints(pts);
                                line.setColor(android.graphics.Color.BLUE);
                                line.setWidth(6.0f);
                                binding.mapPreview.getOverlays().add(line);

                                binding.mapPreview.invalidate();

                                binding.mapPreview.post(() -> {
                                    try {
                                        org.osmdroid.util.BoundingBox box = org.osmdroid.util.BoundingBox.fromGeoPoints(pts);
                                        binding.mapPreview.zoomToBoundingBox(box, true, 80);
                                    } catch (Exception ignored) {}
                                });
                            } else {
                                binding.cardDistancePreview.setVisibility(View.GONE);
                            }
                            
                            // UI for Admin Fee and Shipping Cost removed from layout as requested.
                            // Values are still used for total calculation.
                            
                            if (discountAmount > 0) {
                                binding.layoutDiscount.setVisibility(View.VISIBLE);
                                binding.tvDiscount.setText(String.format("- Rp %,.0f", discountAmount));
                                
                                // Display terms if available
                                Map<String, Object> voucherData = (Map<String, Object>) data.get("voucher");
                                if (voucherData != null && voucherData.containsKey("terms")) {
                                    String terms = String.valueOf(voucherData.get("terms"));
                                    if (terms != null && !terms.isEmpty() && !"null".equals(terms)) {
                                        binding.tvVoucherTerms.setVisibility(View.VISIBLE);
                                        binding.tvVoucherTerms.setText("S&K: " + terms);
                                    } else {
                                        binding.tvVoucherTerms.setVisibility(View.GONE);
                                    }
                                } else {
                                    binding.tvVoucherTerms.setVisibility(View.GONE);
                                }
                                
                                Toast.makeText(CheckoutActivity.this, "Voucher berhasil digunakan!", Toast.LENGTH_SHORT)
                                        .show();
                            } else {
                                binding.layoutDiscount.setVisibility(View.GONE);
                                binding.tvVoucherTerms.setVisibility(View.GONE);
                            }

                            binding.tvCheckoutTotal.setText(String.format("Rp %,.0f", grandTotal));

                        } catch (Exception e) {
                            Toast.makeText(CheckoutActivity.this, "Gagal memproses data preview", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    } else {
                        Toast.makeText(CheckoutActivity.this, res.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                binding.btnPlaceOrder.setEnabled(true);
                Toast.makeText(CheckoutActivity.this, "Gagal kalkulasi total: " + t.getMessage(), Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    private void loadPaymentMethods() {
        apiService.getPaymentMethods().enqueue(new Callback<ApiResponse<List<Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Object>>> call, Response<ApiResponse<List<Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Object>> res = response.body();
                    if ("success".equals(res.getStatus()) && res.getData() != null) {
                        Gson gson = new Gson();
                        String json = gson.toJson(res.getData());
                        Type listType = new TypeToken<List<Map<String, Object>>>() {
                        }.getType();
                        paymentMethodsList = gson.fromJson(json, listType);

                        if (!paymentMethodsList.isEmpty()) {
                            List<String> bankNames = new ArrayList<>();
                            for (Map<String, Object> method : paymentMethodsList) {
                                String name = method.containsKey("name") ? String.valueOf(method.get("name")) : "-";
                                bankNames.add(name);
                            }
                            
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(CheckoutActivity.this,
                                    android.R.layout.simple_spinner_item, bankNames);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            binding.spinnerPaymentMethod.setAdapter(adapter);
                            
                            binding.spinnerPaymentMethod.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                                    Map<String, Object> selectedMethod = paymentMethodsList.get(position);
                                    selectedPaymentMethodId = ((Double) selectedMethod.get("id")).intValue();
                                    calculateTotal();
                                }

                                @Override
                                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                            });
                            
                            // Set initial selected ID
                            selectedPaymentMethodId = ((Double) paymentMethodsList.get(0).get("id")).intValue();
                            calculateTotal();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Object>>> call, Throwable t) {
                Toast.makeText(CheckoutActivity.this, "Gagal memuat metode pembayaran", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // checkVoucher is now integrated into calculateTotal() API call

    private void handleAddressClick() {
        if (!userAddresses.isEmpty()) {
            showAddressSelectionDialog();
        } else {
            Toast.makeText(this, "Belum ada alamat, melacak lokasi terkini...", Toast.LENGTH_SHORT).show();
            checkLocationPermissionAndTrack();
        }
    }

    private void loadUserAddresses() {
        if (isLoadingAddresses)
            return;
        isLoadingAddresses = true;

        binding.tvRecipientName.setText("Memuat alamat...");
        binding.tvFullAddress.setText("...");
        binding.btnPlaceOrder.setEnabled(false);

        String token = "Bearer " + sessionManager.getToken();
        apiService.getAddresses(token).enqueue(new Callback<ApiResponse<List<Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Object>>> call, Response<ApiResponse<List<Object>>> response) {
                isLoadingAddresses = false;
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Object>> res = response.body();
                    if ("success".equals(res.getStatus()) && res.getData() != null) {
                        try {
                            Gson gson = new Gson();
                            String json = gson.toJson(res.getData());
                            Type listType = new TypeToken<List<Map<String, Object>>>() {
                            }.getType();
                            userAddresses = gson.fromJson(json, listType);

                            boolean hasDefault = false;
                            Map<String, Object> defaultAddr = null;

                            if (!userAddresses.isEmpty()) {
                                defaultAddr = userAddresses.get(0);
                                for (Map<String, Object> addr : userAddresses) {
                                    boolean isDef = addr.containsKey("is_default")
                                            && (Boolean.parseBoolean(addr.get("is_default").toString()) || "1"
                                                    .equals(addr.get("is_default").toString()));
                                    if (isDef) {
                                        defaultAddr = addr;
                                        hasDefault = true;
                                        break;
                                    }
                                }
                            }

                            if (userAddresses.isEmpty()) {
                                checkLocationPermissionAndTrack();
                            } else {
                                // If we found a default, defaultAddr is already that.
                                // If not, defaultAddr is the first address in the list.
                                applySelectedAddress(defaultAddr);
                            }
                        } catch (Exception e) {
                            Toast.makeText(CheckoutActivity.this, "Gagal memproses alamat", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(CheckoutActivity.this, "Gagal meload alamat", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Object>>> call, Throwable t) {
                isLoadingAddresses = false;
                Toast.makeText(CheckoutActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddressSelectionDialog() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(
                this);
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_address_selection, null);
        bottomSheetDialog.setContentView(view);

        androidx.recyclerview.widget.RecyclerView rvAddresses = view.findViewById(R.id.rvAddresses);
        com.google.android.material.button.MaterialButton btnUseGps = view.findViewById(R.id.btnUseGps);
        com.google.android.material.button.MaterialButton btnAddAddress = view.findViewById(R.id.btnAddAddress);

        rvAddresses.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        com.octania.marketplace.ui.profile.AddressAdapter adapter = new com.octania.marketplace.ui.profile.AddressAdapter(
                this, true, new com.octania.marketplace.ui.profile.AddressAdapter.AddressActionCallback() {
                    @Override
                    public void onAddressSelected(Map<String, Object> address) {
                        applySelectedAddress(address);
                        bottomSheetDialog.dismiss();
                    }

                    @Override
                    public void onSetDefault(Map<String, Object> address) {
                    }

                    @Override
                    public void onEdit(Map<String, Object> address) {
                    }

                    @Override
                    public void onDelete(Map<String, Object> address) {
                    }
                });
        rvAddresses.setAdapter(adapter);
        adapter.setAddresses(userAddresses);

        btnUseGps.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            checkLocationPermissionAndTrack();
        });

        btnAddAddress.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            startActivity(new android.content.Intent(CheckoutActivity.this,
                    com.octania.marketplace.ui.profile.ManageAddressActivity.class));
        });

        bottomSheetDialog.show();
    }

    private void applySelectedAddress(Map<String, Object> addr) {
        selectedAddressId = ((Double) addr.get("id")).intValue();
        
        // STRICT FIX: Always take Name and Phone from SessionManager (Account Data)
        // This ignores whatever name/phone is stored in the specific address database entry.
        Map<String, String> userDetails = sessionManager.getUserDetails();
        String name = userDetails.get(SessionManager.KEY_NAME);
        String phone = userDetails.get(SessionManager.KEY_PHONE);
        
        // Fallback only if session is literally empty
        if (name == null || name.isEmpty() || "null".equals(name)) {
            name = addr.containsKey("recipient_name") ? String.valueOf(addr.get("recipient_name")) : "User";
        }
        if (phone == null || phone.isEmpty() || "null".equals(phone)) {
            phone = addr.containsKey("phone") ? String.valueOf(addr.get("phone")) : "-";
        }
        
        String full = addr.containsKey("full_address") ? addr.get("full_address").toString() : "";

        // Extract lat/lng from the selected address
        selectedAddressLat = null;
        selectedAddressLng = null;
        if (addr.containsKey("latitude") && addr.get("latitude") != null) {
            try {
                selectedAddressLat = ((Number) addr.get("latitude")).doubleValue();
            } catch (Exception ignored) {}
        }
        if (addr.containsKey("longitude") && addr.get("longitude") != null) {
            try {
                selectedAddressLng = ((Number) addr.get("longitude")).doubleValue();
            } catch (Exception ignored) {}
        }

        binding.tvRecipientName.setText(name);
        binding.tvPhoneNumber.setText(phone);
        binding.tvFullAddress.setText(full);
        binding.btnPlaceOrder.setEnabled(true);
        calculateTotal();
    }

    private void checkLocationPermissionAndTrack() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 101);
        } else {
            getCurrentLocationForAddress();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocationForAddress();
        } else {
            Toast.makeText(this, "Izin lokasi diperlukan untuk deteksi alamat otomatis.", Toast.LENGTH_SHORT).show();
            binding.tvRecipientName.setText("Alamat Kosong");
            binding.tvFullAddress.setText("Tolong tambahkan alamat di Profil");
        }
    }

    private void getCurrentLocationForAddress() {
        binding.tvRecipientName.setText("Mendeteksi lokasi...");
        binding.tvFullAddress.setText("Mencari sinyal GPS...");

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    new Thread(() -> {
                        try {
                            Geocoder geocoder = new Geocoder(CheckoutActivity.this, Locale.getDefault());
                            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(),
                                    location.getLongitude(), 1);
                            if (addresses != null && !addresses.isEmpty()) {
                                Address addr = addresses.get(0);
                                String fullAddress = addr.getAddressLine(0);
                                if (fullAddress == null) {
                                    String street = addr.getThoroughfare() != null ? addr.getThoroughfare() : "";
                                    String sub = addr.getSubLocality() != null ? addr.getSubLocality() : "";
                                    String city = addr.getLocality() != null ? addr.getLocality() : "";
                                    fullAddress = street + " " + sub + ", " + city;
                                }
                                final String finalAddress = fullAddress;
                                runOnUiThread(() -> {
                                    if (userAddresses != null) {
                                        for (Map<String, Object> existing : userAddresses) {
                                            String existingAddr = existing.containsKey("full_address")
                                                    ? String.valueOf(existing.get("full_address"))
                                                    : "";
                                            if (existingAddr.trim().equalsIgnoreCase(finalAddress.trim())) {
                                                applySelectedAddress(existing);
                                                Toast.makeText(CheckoutActivity.this,
                                                        "Alamat sudah ada, memilih alamat tersebut", Toast.LENGTH_SHORT)
                                                        .show();
                                                return;
                                            }
                                        }
                                    }
                                    saveAutoAddressAndSelect(finalAddress, location.getLatitude(),
                                            location.getLongitude());
                                });
                            } else {
                                runOnUiThread(() -> {
                                    Toast.makeText(CheckoutActivity.this, "Gagal mendapatkan nama jalan",
                                            Toast.LENGTH_SHORT).show();
                                    binding.tvRecipientName.setText("Alamat Tidak Ditemukan");
                                    binding.tvFullAddress.setText("Ketuk untuk mencoba lagi");
                                });
                            }
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                Toast.makeText(CheckoutActivity.this, "Geocoder error: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                binding.tvRecipientName.setText("Alamat Tidak Ditemukan");
                                binding.tvFullAddress.setText("Ketuk untuk mencoba lagi");
                            });
                        }
                    }).start();
                } else {
                    Toast.makeText(this, "Lokasi tidak ditemukan. Pastikan GPS aktif.", Toast.LENGTH_SHORT).show();
                    binding.tvRecipientName.setText("Alamat Tidak Ditemukan");
                    binding.tvFullAddress.setText("Ketuk untuk mencoba lagi");
                }
            });
        }
    }

    private void saveAutoAddressAndSelect(String addressText, double lat, double lon) {
        String token = "Bearer " + sessionManager.getToken();
        String name = sessionManager.getUserDetails().get(SessionManager.KEY_NAME);
        String phone = sessionManager.getUserDetails().get(SessionManager.KEY_PHONE);
        if (name == null || name.isEmpty())
            name = "User";
        if (phone == null || phone.isEmpty())
            phone = "-";

        apiService.addAddress(token, name, phone, addressText, lat, lon, true)
                .enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(CheckoutActivity.this, "Alamat otomatis berhasil dibuat dan dipilih",
                                    Toast.LENGTH_SHORT).show();
                            loadUserAddresses(); // Reload to pick it up properly
                        } else {
                            Toast.makeText(CheckoutActivity.this, "Gagal membuat alamat otomatis", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                        Toast.makeText(CheckoutActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void processCheckout() {
        if (selectedAddressId == -1) {
            Toast.makeText(this, "Pilih alamat pengiriman terlebih dahulu.", Toast.LENGTH_SHORT).show();
            return;
        }

        int paymentMethodId = selectedPaymentMethodId != -1 ? selectedPaymentMethodId : 1;

        binding.btnPlaceOrder.setEnabled(false);
        binding.btnPlaceOrder.setText("MEMPROSES...");

        String token = "Bearer " + sessionManager.getToken();

        Map<String, Object> body = new java.util.HashMap<>();

        if (isDirectBuy) {
            body.put("product_id", directProductId);
            body.put("quantity", directQuantity);
        } else {
            body.put("cart_ids", cartIds);
        }

        body.put("user_address_id", selectedAddressId);
        body.put("payment_method_id", paymentMethodId);
        body.put("delivery_type", deliveryType);
        body.put("shipping_vehicle", shippingVehicle);
        // Send buyer coordinates for shipping calculation
        // Priority: selected address coordinates > device GPS
        Double cBuyerLat = (selectedAddressLat != null) ? selectedAddressLat : deviceLat;
        Double cBuyerLng = (selectedAddressLng != null) ? selectedAddressLng : deviceLng;
        if (cBuyerLat != null && cBuyerLng != null) {
            body.put("buyer_latitude", cBuyerLat);
            body.put("buyer_longitude", cBuyerLng);
        }
        if (selectedUserVoucherId != -1) {
            body.put("user_voucher_id", selectedUserVoucherId);
        }
        if (appliedVoucherCode != null && !appliedVoucherCode.isEmpty()) {
            body.put("voucher_code", appliedVoucherCode);
        }

        apiService.confirmCheckout(token, body)
                .enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Object> res = response.body();
                            if ("success".equals(res.getStatus())) {
                                Toast.makeText(CheckoutActivity.this, "Pesanan Dibuat!", Toast.LENGTH_SHORT).show();
                                try {
                                    Map<String, Object> data = (Map<String, Object>) res.getData();
                                    int transactionId = ((Double) data.get("id")).intValue();

                                    // Navigate to Payment Activity
                                    Intent intent = new Intent(CheckoutActivity.this,
                                            com.octania.marketplace.ui.payment.PaymentActivity.class);
                                    intent.putExtra("transaction_id", transactionId);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                    finish();
                                } catch (Exception e) {
                                    Intent intent = new Intent(CheckoutActivity.this, HomeActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                    finish();
                                }
                            } else {
                                binding.btnPlaceOrder.setEnabled(true);
                                binding.btnPlaceOrder.setText("BUAT PESANAN");
                                Toast.makeText(CheckoutActivity.this, res.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            binding.btnPlaceOrder.setEnabled(true);
                            binding.btnPlaceOrder.setText("BUAT PESANAN");
                            Toast.makeText(CheckoutActivity.this, "Gagal checkout", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                        binding.btnPlaceOrder.setEnabled(true);
                        binding.btnPlaceOrder.setText("BUAT PESANAN");
                        Toast.makeText(CheckoutActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onQuantityChanged(CartItem item, int newQuantity) {
        if (isDirectBuy) {
            directQuantity = newQuantity;
        }
        calculateTotal();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showVoucherSelection() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_voucher_selection, null);
        bottomSheet.setContentView(view);

        androidx.recyclerview.widget.RecyclerView rvVouchers = view.findViewById(R.id.rvVouchers);
        android.widget.ProgressBar progressBar = view.findViewById(R.id.progressBar);
        android.widget.TextView tvNoVoucher = view.findViewById(R.id.tvNoVoucher);

        rvVouchers.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        VoucherAdapter adapter = new VoucherAdapter();
        adapter.setSelectedUserVoucherId(selectedUserVoucherId);
        rvVouchers.setAdapter(adapter);

        adapter.setOnVoucherClickListener(voucher -> {
            selectedUserVoucherId = voucher.getUserVoucherId();
            adapter.setSelectedUserVoucherId(selectedUserVoucherId);
            appliedVoucherCode = voucher.getCode();
            selectedVoucherName = voucher.getName();
            binding.tvSelectedVoucherName.setText(selectedVoucherName);
            binding.tvSelectedVoucherName.setTextColor(Color.parseColor("#333333"));
            calculateTotal();
            bottomSheet.dismiss();
        });

        String token = "Bearer " + sessionManager.getToken();
        apiService.getVouchers(token, subtotal).enqueue(new Callback<ApiResponse<List<com.octania.marketplace.data.model.Voucher>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<com.octania.marketplace.data.model.Voucher>>> call, Response<ApiResponse<List<com.octania.marketplace.data.model.Voucher>>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<com.octania.marketplace.data.model.Voucher> vouchers = response.body().getData();
                    if (vouchers != null && !vouchers.isEmpty()) {
                        adapter.setVouchers(vouchers);
                        tvNoVoucher.setVisibility(View.GONE);
                    } else {
                        tvNoVoucher.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<com.octania.marketplace.data.model.Voucher>>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CheckoutActivity.this, "Gagal memuat voucher", Toast.LENGTH_SHORT).show();
            }
        });

        bottomSheet.show();
    }

    private String getRecommendedVehicle(double weightKg) {
        if (weightKg <= 20) {
            return "motor";
        } else if (weightKg <= 100) {
            return "becak";
        } else if (weightKg <= 1000) {
            return "pickup";
        } else {
            return "jemput_sendiri";
        }
    }

    private void setInitialVehicleFromCartItems() {
        String suggested = null;
        int highestPriority = 0;
        for (CartItem item : cartItems) {
            if (item.getProduct() != null && item.getProduct().getShippingSuggestion() != null) {
                String sug = item.getProduct().getShippingSuggestion();
                int priority = 0;
                if ("jemput_sendiri".equals(sug)) priority = 1;
                else if ("motor".equals(sug)) priority = 2;
                else if ("becak".equals(sug)) priority = 3;
                else if ("pickup".equals(sug)) priority = 4;
                
                if (priority > highestPriority) {
                    highestPriority = priority;
                    suggested = sug;
                }
            }
        }
        
        if (suggested != null) {
            shippingVehicle = suggested;
            if ("jemput_sendiri".equals(shippingVehicle)) {
                deliveryType = "pickup";
            } else {
                deliveryType = "courier";
            }
            
            int index = -1;
            for (int i = 0; i < vehicleValues.length; i++) {
                if (vehicleValues[i].equals(shippingVehicle)) {
                    index = i;
                    break;
                }
            }
            if (index != -1) {
                spinnerUpdateDepth++;
                binding.spinnerShippingVehicle.setSelection(index);
                spinnerUpdateDepth--;
            }
        }
    }

    private void updateVehicleSpinnerAdapter(double totalWeightKg) {
        String recommendedValue = getRecommendedVehicle(totalWeightKg);
        String[] dynamicOptions = new String[vehicleOptions.length];
        
        for (int i = 0; i < vehicleOptions.length; i++) {
            String val = vehicleValues[i];
            String name = vehicleOptions[i];
            
            if ("motor".equals(val)) {
                if (totalWeightKg > 20) {
                    name += " (Maks 20kg - Dinonaktifkan)";
                } else if ("motor".equals(recommendedValue)) {
                    name += " (Disarankan)";
                }
            } else if ("becak".equals(val)) {
                if (totalWeightKg > 100) {
                    name += " (Maks 100kg - Dinonaktifkan)";
                } else if ("becak".equals(recommendedValue)) {
                    name += " (Disarankan)";
                }
            } else if ("pickup".equals(val)) {
                if (totalWeightKg > 1000) {
                    name += " (Maks 1000kg - Dinonaktifkan)";
                } else if ("pickup".equals(recommendedValue)) {
                    name += " (Disarankan)";
                }
            } else if ("jemput_sendiri".equals(val)) {
                if ("jemput_sendiri".equals(recommendedValue)) {
                    name += " (Disarankan)";
                }
            }
            dynamicOptions[i] = name;
        }
        
        // Temukan indeks dari pilihan PEMBELI saat ini (shippingVehicle)
        // Jika pilihan pembeli tidak valid (melebihi kapasitas), otomatis pilih yang direkomendasikan
        int targetPosition = -1;
        for (int i = 0; i < vehicleValues.length; i++) {
            if (vehicleValues[i].equals(shippingVehicle)) {
                targetPosition = i;
                break;
            }
        }
        
        // Cek apakah pilihan saat ini masih valid setelah perubahan berat
        boolean currentSelectionDisabled = false;
        if (targetPosition >= 0) {
            String currentVal = vehicleValues[targetPosition];
            if (totalWeightKg > 20 && "motor".equals(currentVal)) currentSelectionDisabled = true;
            if (totalWeightKg > 100 && "becak".equals(currentVal)) currentSelectionDisabled = true;
            if (totalWeightKg > 1000 && "pickup".equals(currentVal)) currentSelectionDisabled = true;
        }
        
        if (currentSelectionDisabled || targetPosition == -1) {
            // Pilihan saat ini tidak valid, fallback ke rekomendasi
            targetPosition = -1;
            for (int i = 0; i < vehicleValues.length; i++) {
                if (vehicleValues[i].equals(recommendedValue)) {
                    targetPosition = i;
                    shippingVehicle = recommendedValue;
                    deliveryType = "jemput_sendiri".equals(shippingVehicle) ? "pickup" : "courier";
                    break;
                }
            }
        }
        
        // Set adapter baru dengan counter untuk mencegah event onItemSelected terpicu
        spinnerUpdateDepth++;
        VehicleSpinnerAdapter adapter = new VehicleSpinnerAdapter(this, dynamicOptions, totalWeightKg);
        binding.spinnerShippingVehicle.setAdapter(adapter);
        
        if (targetPosition != -1) {
            binding.spinnerShippingVehicle.setSelection(targetPosition);
        }
        
        // Turunkan counter setelah semua operasi selesai
        binding.spinnerShippingVehicle.post(() -> spinnerUpdateDepth--);
    }

    private class VehicleSpinnerAdapter extends ArrayAdapter<String> {
        private final double totalWeightKg;

        public VehicleSpinnerAdapter(android.content.Context context, String[] objects, double totalWeightKg) {
            super(context, android.R.layout.simple_spinner_item, objects);
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            this.totalWeightKg = totalWeightKg;
        }

        @Override
        public boolean isEnabled(int position) {
            String val = vehicleValues[position];
            if ("motor".equals(val) && totalWeightKg > 20) {
                return false;
            }
            if ("becak".equals(val) && totalWeightKg > 100) {
                return false;
            }
            if ("pickup".equals(val) && totalWeightKg > 1000) {
                return false;
            }
            return true;
        }

        @Override
        public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
            View view = super.getDropDownView(position, convertView, parent);
            android.widget.TextView tv = (android.widget.TextView) view.findViewById(android.R.id.text1);
            if (!isEnabled(position)) {
                tv.setTextColor(Color.GRAY);
            } else {
                tv.setTextColor(Color.BLACK);
            }
            return view;
        }
    }

    private String formatRupiah(double amount) {
        java.text.NumberFormat nf = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("id", "ID"));
        return nf.format(amount);
    }
}
