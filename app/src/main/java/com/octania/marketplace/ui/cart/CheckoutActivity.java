package com.octania.marketplace.ui.cart;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
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
    private List<Map<String, Object>> userAddresses = new ArrayList<>();
    private List<Map<String, Object>> paymentMethodsList = new ArrayList<>();

    private String appliedVoucherCode = null;
    private double discountAmount = 0;
    private double serviceFee = 0;
    private double adminFee = 0;
    private double shippingCost = 0;
    private double subtotal = 0;
    private String deliveryType = "courier";
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
        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

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
        binding.btnApplyVoucher.setOnClickListener(v -> {
            appliedVoucherCode = binding.etVoucherCode.getText().toString().trim();
            calculateTotal();
        });

        binding.rgShippingOption.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == binding.rbShippingPickup.getId()) {
                deliveryType = "pickup";
            } else {
                deliveryType = "courier";
            }
            calculateTotal();
        });

        binding.spinnerPaymentMethod.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                calculateTotal();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        loadUserAddresses();
        loadCheckoutCartItems();
        loadPaymentMethods();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserAddresses();
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
        if (appliedVoucherCode != null && !appliedVoucherCode.isEmpty()) {
            body.put("voucher_code", appliedVoucherCode);
        }

        binding.btnPlaceOrder.setEnabled(false);
        String token = "Bearer " + sessionManager.getToken();
        
        // Include payment_method_id for adminFee calculation
        if (!paymentMethodsList.isEmpty() && binding.spinnerPaymentMethod.getSelectedItemPosition() >= 0) {
            int selectedPos = binding.spinnerPaymentMethod.getSelectedItemPosition();
            if (selectedPos < paymentMethodsList.size()) {
                int paymentMethodId = ((Double) paymentMethodsList.get(selectedPos).get("id")).intValue();
                body.put("payment_method_id", paymentMethodId);
            }
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

                            if (adminFee > 0) {
                                binding.layoutAdminFee.setVisibility(View.VISIBLE);
                                binding.tvAdminFee.setText(String.format("Rp %,.0f", adminFee));
                            } else {
                                binding.layoutAdminFee.setVisibility(View.GONE);
                            }

                            binding.layoutShippingCost.setVisibility(View.VISIBLE);
                            binding.tvShippingCost.setText(String.format("Rp %,.0f", shippingCost));

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
                                if (appliedVoucherCode != null && !appliedVoucherCode.isEmpty()) {
                                    Toast.makeText(CheckoutActivity.this,
                                            "Voucher tidak valid atau syarat tidak terpenuhi", Toast.LENGTH_SHORT)
                                            .show();
                                    appliedVoucherCode = null; // Reset invalid voucher
                                    binding.etVoucherCode.setText("");
                                }
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

                        List<String> names = new ArrayList<>();
                        for (Map<String, Object> method : paymentMethodsList) {
                            names.add(method.get("name").toString());
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(CheckoutActivity.this,
                                android.R.layout.simple_spinner_dropdown_item, names);
                        binding.spinnerPaymentMethod.setAdapter(adapter);
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
        String name = addr.containsKey("recipient_name") ? addr.get("recipient_name").toString() : "";
        String phone = addr.containsKey("phone") ? addr.get("phone").toString() : "";
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

        if (paymentMethodsList.isEmpty()) {
            Toast.makeText(this, "Metode pembayaran belum dimuat.", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedPos = binding.spinnerPaymentMethod.getSelectedItemPosition();
        if (selectedPos < 0 || selectedPos >= paymentMethodsList.size()) {
            Toast.makeText(this, "Pilih metode pembayaran terlebih dahulu.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnPlaceOrder.setEnabled(false);
        binding.btnPlaceOrder.setText("MEMPROSES...");

        String token = "Bearer " + sessionManager.getToken();
        int paymentMethodId = ((Double) paymentMethodsList.get(selectedPos).get("id")).intValue();

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
        // Send buyer coordinates for shipping calculation
        // Priority: selected address coordinates > device GPS
        Double cBuyerLat = (selectedAddressLat != null) ? selectedAddressLat : deviceLat;
        Double cBuyerLng = (selectedAddressLng != null) ? selectedAddressLng : deviceLng;
        if (cBuyerLat != null && cBuyerLng != null) {
            body.put("buyer_latitude", cBuyerLat);
            body.put("buyer_longitude", cBuyerLng);
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
}
