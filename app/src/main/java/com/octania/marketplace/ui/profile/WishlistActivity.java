package com.octania.marketplace.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.octania.marketplace.R;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.model.Product;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityWishlistBinding;
import com.octania.marketplace.ui.auth.LoginActivity;
import com.octania.marketplace.ui.home.ProductAdapter;
import com.octania.marketplace.ui.product.ProductDetailActivity;
import com.octania.marketplace.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WishlistActivity extends AppCompatActivity {

    private ActivityWishlistBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private com.octania.marketplace.utils.ProductActionHelper actionHelper;
    private ProductAdapter adapter;
    private List<Product> productList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWishlistBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);
        actionHelper = new com.octania.marketplace.utils.ProductActionHelper(this, sessionManager);

        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Harap login terlebih dahulu.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setupRecyclerView();
        setupBottomNav();
    }

    private void setupBottomNav() {
        binding.bottomNav.setSelectedItemId(R.id.nav_wishlist);
        com.octania.marketplace.utils.NavigationUtils.applyFloatingEffect(binding.bottomNav);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, com.octania.marketplace.ui.home.HomeActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_orders) {
                startActivity(new Intent(this, com.octania.marketplace.ui.seller.MyOrdersActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_wishlist) {
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, com.octania.marketplace.ui.profile.ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.bottomNav.setSelectedItemId(R.id.nav_wishlist);
        com.octania.marketplace.utils.NavigationUtils.applyFloatingEffect(binding.bottomNav);
        fetchWishlists();
    }

    private void setupRecyclerView() {
        adapter = new ProductAdapter(this, new ProductAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Product product) {
                Intent intent = new Intent(WishlistActivity.this, ProductDetailActivity.class);
                intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.getId());
                startActivity(intent);
            }

            @Override
            public void onAddToCartClick(Product product) {
                if (!sessionManager.isLoggedIn()) {
                    Toast.makeText(WishlistActivity.this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(WishlistActivity.this, LoginActivity.class));
                    return;
                }
                Intent intent = new Intent(WishlistActivity.this,
                        com.octania.marketplace.ui.cart.CheckoutActivity.class);
                intent.putExtra("direct_buy", true);
                intent.putExtra("product_id", product.getId());
                intent.putExtra("quantity", 1);
                startActivity(intent);
            }

            @Override
            public void onWishlistClick(Product product) {
                actionHelper.toggleWishlist(product.getId(),
                        new com.octania.marketplace.utils.ProductActionHelper.ActionCallback() {
                            @Override
                            public void onSuccess(String message) {
                                fetchWishlists();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(WishlistActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
        // Based on user feedback: "Cardnya langsung full 1 saja jangan grid 2 karena
        // jelek"
        binding.rvWishlist.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));
        binding.rvWishlist.setAdapter(adapter);
    }

    private void fetchWishlists() {
        binding.progressBar.setVisibility(View.VISIBLE);
        String token = "Bearer " + sessionManager.getToken();

        apiService.getWishlists(token).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call,
                    Response<ApiResponse<Object>> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> res = response.body();
                    if ("success".equals(res.getStatus()) && res.getData() != null) {
                        try {
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            String jsonStr = gson.toJson(res.getData());
                            com.google.gson.JsonArray jsonArray = com.google.gson.JsonParser.parseString(jsonStr)
                                    .getAsJsonArray();

                            productList.clear();
                            for (com.google.gson.JsonElement element : jsonArray) {
                                com.google.gson.JsonObject wishlistObj = element.getAsJsonObject();
                                if (wishlistObj.has("product") && !wishlistObj.get("product").isJsonNull()) {
                                    Product product = gson.fromJson(wishlistObj.get("product"), Product.class);
                                    product.setWishlisted(true);
                                    productList.add(product);
                                }
                            }
                            adapter.updateData(productList);

                            if (productList.isEmpty()) {
                                Toast.makeText(WishlistActivity.this, "Wishlist Anda kosong.", Toast.LENGTH_SHORT)
                                        .show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(WishlistActivity.this, "Gagal memproses data produk.", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    } else {
                        Toast.makeText(WishlistActivity.this, "Gagal memuat wishlist", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(WishlistActivity.this, "Kesalahan Server memuat wishlist", Toast.LENGTH_SHORT)
                            .show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(WishlistActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
