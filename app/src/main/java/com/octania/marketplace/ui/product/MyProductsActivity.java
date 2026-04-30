package com.octania.marketplace.ui.product;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.octania.marketplace.R;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.model.Product;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityMyProductsBinding;
import com.octania.marketplace.utils.SessionManager;

import java.lang.reflect.Type;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyProductsActivity extends AppCompatActivity {

    private ActivityMyProductsBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private MyProductAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyProductsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        setupRecyclerView();

        binding.btnBack.setOnClickListener(v -> finish());

        binding.fabAddProduct.setOnClickListener(v -> startActivity(new Intent(this, AddProductActivity.class)));

        binding.swipeRefresh.setColorSchemeResources(R.color.primary_orange);
        binding.swipeRefresh.setOnRefreshListener(this::fetchMyProducts);

        setupBottomNav();
    }

    private void setupBottomNav() {
        if (binding.bottomNav == null) return;
        binding.bottomNav.setSelectedItemId(R.id.nav_add);
        com.octania.marketplace.utils.NavigationUtils.applyFloatingEffect(binding.bottomNav);

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, com.octania.marketplace.ui.seller.SellerDashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_add) {
                return true;
            } else if (id == R.id.nav_orders) {
                startActivity(new Intent(this, com.octania.marketplace.ui.seller.SellerOrdersActivity.class));
                finish();
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
        if (binding.bottomNav != null) {
            binding.bottomNav.setSelectedItemId(R.id.nav_add);
            com.octania.marketplace.utils.NavigationUtils.applyFloatingEffect(binding.bottomNav);
        }
        fetchMyProducts();
    }

    private void setupRecyclerView() {
        adapter = new MyProductAdapter(this, new MyProductAdapter.OnActionListener() {
            @Override
            public void onEdit(Product product) {
                Intent intent = new Intent(MyProductsActivity.this, EditProductActivity.class);
                intent.putExtra(EditProductActivity.EXTRA_PRODUCT_ID, product.getId());
                startActivity(intent);
            }

            @Override
            public void onDelete(Product product) {
                showDeleteConfirmation(product);
            }
        });
        binding.rvMyProducts.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));
        binding.rvMyProducts.setAdapter(adapter);
    }

    private void fetchMyProducts() {
        binding.swipeRefresh.setRefreshing(true);
        String token = "Bearer " + sessionManager.getToken();

        apiService.getMyProducts(token).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                binding.swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> apiResponse = response.body();
                    if ("success".equals(apiResponse.getStatus()) && apiResponse.getData() != null) {
                        try {
                            Gson gson = new Gson();
                            String json = gson.toJson(apiResponse.getData());
                            Type listType = new TypeToken<List<Product>>() {
                            }.getType();
                            List<Product> products = gson.fromJson(json, listType);
                            adapter.updateData(products);
                            binding.tvProductCount.setText(products.size() + " produk");
                        } catch (Exception e) {
                            Toast.makeText(MyProductsActivity.this,
                                    getString(R.string.server_error), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(MyProductsActivity.this,
                        getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmation(Product product) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Produk")
                .setMessage("Yakin ingin menghapus \"" + product.getName() + "\"?")
                .setPositiveButton("Hapus", (dialog, which) -> deleteProduct(product))
                .setNegativeButton("Batal", null)
                .show();
    }

    private void deleteProduct(Product product) {
        String token = "Bearer " + sessionManager.getToken();
        apiService.deleteProduct(token, product.getId()).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    adapter.removeItem(product);
                    Toast.makeText(MyProductsActivity.this,
                            "Produk berhasil dihapus", Toast.LENGTH_SHORT).show();
                    // Update count
                    binding.tvProductCount.setText(adapter.getItemCount() + " produk");
                } else {
                    Toast.makeText(MyProductsActivity.this,
                            getString(R.string.server_error), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Toast.makeText(MyProductsActivity.this,
                        getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
