package com.octania.marketplace.ui.product;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.octania.marketplace.R;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.model.Product;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityEditProductBinding;
import com.octania.marketplace.utils.SessionManager;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProductActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "extra_product_id";

    private ActivityEditProductBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private int productId;
    private Product currentProduct;

    // Category data
    private final List<String> categoryNames = new ArrayList<>();
    private final List<Integer> categoryIds = new ArrayList<>();

    // Condition data
    private static final String[] CONDITIONS = { "like_new", "used" };
    private static final String[] CONDITION_LABELS = { "Seperti Baru", "Bekas" };

    private final String[] weightUnits = { "gr", "kg" };

    private final androidx.activity.result.ActivityResultLauncher<android.content.Intent> mapPickerLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    String address = result.getData().getStringExtra("picked_address");
                    if (address != null) {
                        binding.etLocation.setText(address);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProductBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        productId = getIntent().getIntExtra(EXTRA_PRODUCT_ID, -1);
        if (productId == -1) {
            Toast.makeText(this, getString(R.string.product_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> saveProduct());

        // Condition spinner
        ArrayAdapter<String> conditionAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, CONDITION_LABELS);
        conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spCondition.setAdapter(conditionAdapter);

        // Weight units spinner
        ArrayAdapter<String> weightAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                weightUnits);
        weightAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spWeightUnit.setAdapter(weightAdapter);

        binding.tilLocation.setEndIconOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, com.octania.marketplace.ui.profile.MapPickerActivity.class);
            mapPickerLauncher.launch(intent);
        });

        fetchCategories();
        fetchProductDetail();
    }

    private void fetchCategories() {
        apiService.getCategories().enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null &&
                        "success".equals(response.body().getStatus())) {
                    try {
                        Gson gson = new Gson();
                        String json = gson.toJson(response.body().getData());
                        Type listType = new TypeToken<List<Map<String, Object>>>() {
                        }.getType();
                        List<Map<String, Object>> categories = gson.fromJson(json, listType);

                        categoryNames.clear();
                        categoryIds.clear();
                        for (Map<String, Object> cat : categories) {
                            categoryNames.add(cat.get("name").toString());
                            categoryIds.add(((Double) cat.get("id")).intValue());
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                EditProductActivity.this,
                                android.R.layout.simple_spinner_item, categoryNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        binding.spCategory.setAdapter(adapter);

                        // If product already loaded, set selection
                        if (currentProduct != null) {
                            preselectCategory();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
            }
        });
    }

    private void fetchProductDetail() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getProductDetail(token, productId).enqueue(new Callback<ApiResponse<Product>>() {
            @Override
            public void onResponse(Call<ApiResponse<Product>> call, Response<ApiResponse<Product>> response) {
                if (response.isSuccessful() && response.body() != null &&
                        "success".equals(response.body().getStatus())) {
                    currentProduct = response.body().getData();
                    populateForm(currentProduct);
                } else {
                    Toast.makeText(EditProductActivity.this,
                            getString(R.string.server_error), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Product>> call, Throwable t) {
                Toast.makeText(EditProductActivity.this,
                        getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateForm(Product product) {
        binding.etName.setText(product.getName());
        binding.etPrice.setText(String.valueOf((int) product.getPrice()));

        if (product.isHasDiscount() && product.getEffectivePrice() > 0 &&
                product.getEffectivePrice() < product.getPrice()) {
            binding.etDiscountPrice.setText(String.valueOf((int) product.getEffectivePrice()));
        }

        binding.etStock.setText(String.valueOf(product.getStock()));

        // Handle weight display
        if (product.getWeight() != null) {
            int w = product.getWeight();
            if (w >= 1000 && w % 100 == 0) {
                binding.etWeight.setText(String.valueOf(w / 1000.0));
                binding.spWeightUnit.setSelection(1); // KG
            } else {
                binding.etWeight.setText(String.valueOf(w));
                binding.spWeightUnit.setSelection(0); // GR
            }
        }

        binding.etLocation.setText(product.getLocation() != null ? product.getLocation() : "");
        binding.etDescription.setText(product.getDescription() != null ? product.getDescription() : "");

        // Condition
        if (product.getCondition() != null) {
            for (int i = 0; i < CONDITIONS.length; i++) {
                if (CONDITIONS[i].equals(product.getCondition())) {
                    binding.spCondition.setSelection(i);
                    break;
                }
            }
        }

        preselectCategory();
    }

    private void preselectCategory() {
        if (currentProduct == null || currentProduct.getCategory() == null)
            return;
        int catId = currentProduct.getCategory().getId();
        for (int i = 0; i < categoryIds.size(); i++) {
            if (categoryIds.get(i) == catId) {
                binding.spCategory.setSelection(i);
                break;
            }
        }
    }

    private void saveProduct() {
        String name = binding.etName.getText().toString().trim();
        String priceStr = binding.etPrice.getText().toString().trim();
        String stockStr = binding.etStock.getText().toString().trim();
        String weightStr = binding.etWeight.getText().toString().trim();
        String location = binding.etLocation.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();
        String discountStr = binding.etDiscountPrice.getText().toString().trim();

        if (name.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty()) {
            Toast.makeText(this, "Nama, harga, dan stok wajib diisi", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceStr);
        int stock = Integer.parseInt(stockStr);

        int weight = 1000;
        if (!weightStr.isEmpty()) {
            try {
                double w = Double.parseDouble(weightStr);
                String unit = binding.spWeightUnit.getSelectedItem().toString();
                if ("kg".equals(unit)) {
                    w = w * 1000;
                }
                weight = (int) w;
            } catch (Exception e) {
                weight = 1000;
            }
        }

        Double discountPrice = discountStr.isEmpty() ? null : Double.parseDouble(discountStr);

        int categoryId = categoryIds.isEmpty() ? 1 : categoryIds.get(binding.spCategory.getSelectedItemPosition());
        String condition = CONDITIONS[binding.spCondition.getSelectedItemPosition()];

        binding.btnSave.setEnabled(false);
        binding.btnSave.setText("Menyimpan...");

        String token = "Bearer " + sessionManager.getToken();
        apiService.updateProduct(token, productId, name, price, discountPrice, stock,
                categoryId, condition, weight, location, description)
                .enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                        binding.btnSave.setEnabled(true);
                        binding.btnSave.setText("Simpan Perubahan");

                        if (response.isSuccessful() && response.body() != null &&
                                "success".equals(response.body().getStatus())) {
                            Toast.makeText(EditProductActivity.this,
                                    "Produk berhasil diperbarui!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(EditProductActivity.this,
                                    getString(R.string.server_error), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                        binding.btnSave.setEnabled(true);
                        binding.btnSave.setText("Simpan Perubahan");
                        Toast.makeText(EditProductActivity.this,
                                getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
