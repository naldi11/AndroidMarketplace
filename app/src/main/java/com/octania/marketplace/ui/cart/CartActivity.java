package com.octania.marketplace.ui.cart;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.Manifest;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.model.CartItem;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityCartBinding;
import com.octania.marketplace.ui.cart.CheckoutActivity;
import com.octania.marketplace.ui.home.HomeActivity;
import com.octania.marketplace.utils.SessionManager;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartActivity extends AppCompatActivity {

    private ActivityCartBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private CartAdapter cartAdapter;
    private List<CartItem> currentCartItems = new java.util.ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Silakan login.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupRecyclerView();

        binding.btnCheckout.setOnClickListener(v -> attemptCheckout());

        loadCartItems();
    }

    private void setupRecyclerView() {
        cartAdapter = new CartAdapter(this, new CartAdapter.OnCartItemListener() {
            @Override
            public void onDeleteClick(CartItem item, int position) {
                deleteCartItem(item, position);
            }

            @Override
            public void onCheckChanged(CartItem item, int position, boolean isChecked) {
                calculateTotal();
            }

            @Override
            public void onQuantityChanged(CartItem item, int position, int newQuantity) {
                updateCartItemQuantity(item, position, newQuantity);
            }
        });
        binding.rvCartItems.setAdapter(cartAdapter);
    }

    private void loadCartItems() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getCart(token).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> res = response.body();
                    if ("success".equals(res.getStatus())) {
                        try {
                            Map<String, Object> dataMap = (Map<String, Object>) res.getData();
                            Object itemsObj = dataMap.get("items"); // Was "cart_items"
                            Object totalObj = dataMap.get("grand_total"); // Was "total_price"

                            Gson gson = new Gson();
                            String json = gson.toJson(itemsObj);
                            Type listType = new TypeToken<List<CartItem>>() {
                            }.getType();
                            List<CartItem> cartItems = gson.fromJson(json, listType);

                            if (cartItems == null || cartItems.isEmpty()) {
                                binding.tvEmptyCart.setVisibility(View.VISIBLE);
                                binding.rvCartItems.setVisibility(View.GONE);
                                binding.layoutCheckoutBottom.setVisibility(View.GONE);
                            } else {
                                currentCartItems = cartItems;
                                binding.tvEmptyCart.setVisibility(View.GONE);
                                binding.rvCartItems.setVisibility(View.VISIBLE);
                                binding.layoutCheckoutBottom.setVisibility(View.VISIBLE);
                                cartAdapter.updateData(currentCartItems);
                                calculateTotal();
                            }
                        } catch (Exception e) {
                            Toast.makeText(CartActivity.this, "Gagal memproses data keranjang", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                } else {
                    Toast.makeText(CartActivity.this, "Gagal memuat keranjang", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                Toast.makeText(CartActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteCartItem(CartItem item, int position) {
        String token = "Bearer " + sessionManager.getToken();
        apiService.deleteCartItem(token, item.getId()).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CartActivity.this, "Item dihapus", Toast.LENGTH_SHORT).show();
                    loadCartItems(); // Reload full context securely
                } else {
                    Toast.makeText(CartActivity.this, "Gagal menghapus item", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(CartActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateTotal() {
        double total = 0;
        if (currentCartItems != null) {
            for (CartItem item : currentCartItems) {
                if (item.isSelected() && item.getProduct() != null) {
                    double price = item.getProduct().getEffectivePrice() > 0 ? item.getProduct().getEffectivePrice()
                            : item.getProduct().getPrice();
                    total += price * item.getQuantity();
                }
            }
        }
        binding.tvTotalPrice.setText(String.format("Rp %,.0f", total));
    }

    private void updateCartItemQuantity(CartItem item, int position, int newQuantity) {
        int oldQuantity = item.getQuantity();
        item.setQuantity(newQuantity);
        cartAdapter.notifyItemChanged(position);
        calculateTotal();

        String token = "Bearer " + sessionManager.getToken();
        apiService.updateCart(token, item.getId(), newQuantity).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (!response.isSuccessful()) {
                    item.setQuantity(oldQuantity);
                    cartAdapter.notifyItemChanged(position);
                    calculateTotal();
                    Toast.makeText(CartActivity.this, "Gagal update jumlah", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                item.setQuantity(oldQuantity);
                cartAdapter.notifyItemChanged(position);
                calculateTotal();
            }
        });
    }

    private List<Integer> getSelectedCartIds() {
        List<Integer> ids = new java.util.ArrayList<>();
        if (currentCartItems != null) {
            for (CartItem item : currentCartItems) {
                if (item.isSelected()) {
                    ids.add(item.getId());
                }
            }
        }
        return ids;
    }

    private void attemptCheckout() {
        java.util.ArrayList<Integer> cartIds = new java.util.ArrayList<>();
        if (currentCartItems != null) {
            for (CartItem item : currentCartItems) {
                if (item.isSelected()) {
                    cartIds.add(item.getId());
                }
            }
        }

        if (cartIds.isEmpty()) {
            Toast.makeText(this, "Pilih minimal 1 produk.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(CartActivity.this, CheckoutActivity.class);
        intent.putIntegerArrayListExtra("cart_ids", cartIds);
        startActivity(intent);
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
