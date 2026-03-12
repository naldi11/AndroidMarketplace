package com.octania.marketplace.ui.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.octania.marketplace.R;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityAddressBinding;
import com.octania.marketplace.utils.SessionManager;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddressActivity extends AppCompatActivity {

    private ActivityAddressBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private EditText currentAddressInput;

    private final ActivityResultLauncher<Intent> mapPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String pickedAddress = result.getData().getStringExtra("picked_address");
                    if (pickedAddress != null && currentAddressInput != null) {
                        currentAddressInput.setText(pickedAddress);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        binding.fabAddAddress.setOnClickListener(v -> showAddDialog());

        fetchAddresses();
    }

    private void fetchAddresses() {
        binding.progressBar.setVisibility(View.VISIBLE);
        String token = "Bearer " + sessionManager.getToken();
        apiService.getAddresses(token).enqueue(new Callback<ApiResponse<List<Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Object>>> call, Response<ApiResponse<List<Object>>> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Object>> res = response.body();
                    if ("success".equals(res.getStatus()) && res.getData() != null) {
                        renderAddresses(res.getData());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Object>>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(AddressActivity.this, "Error memuat alamat", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderAddresses(List<Object> data) {
        binding.llAddressContainer.removeAllViews();
        try {
            Gson gson = new Gson();
            String json = gson.toJson(data);
            Type listType = new TypeToken<List<Map<String, Object>>>() {
            }.getType();
            List<Map<String, Object>> addresses = gson.fromJson(json, listType);

            if (addresses == null || addresses.isEmpty()) {
                binding.tvEmpty.setVisibility(View.VISIBLE);
                return;
            }
            binding.tvEmpty.setVisibility(View.GONE);

            for (Map<String, Object> addr : addresses) {
                View card = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
                TextView tvLine1 = card.findViewById(android.R.id.text1);
                TextView tvLine2 = card.findViewById(android.R.id.text2);

                String name = addr.containsKey("recipient_name") ? addr.get("recipient_name").toString() : "";
                String full = addr.containsKey("full_address") ? addr.get("full_address").toString() : "";
                boolean isDef = addr.containsKey("is_default")
                        && Boolean.parseBoolean(addr.get("is_default").toString());
                int id = addr.containsKey("id") ? ((Double) addr.get("id")).intValue() : 0;

                tvLine1.setText(name + (isDef ? " (DEFAULT)" : ""));
                tvLine2.setText(full);
                tvLine1.setTextSize(16);
                if (isDef) {
                    tvLine1.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                }

                card.setPadding(32, 32, 32, 32);
                card.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
                card.setOnClickListener(v -> setDefault(id));

                binding.llAddressContainer.addView(card);
            }
        } catch (Exception e) {
        }
    }

    private void setDefault(int id) {
        String token = "Bearer " + sessionManager.getToken();
        apiService.setDefaultAddress(token, id).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddressActivity.this, "Alamat utama diperbarui!", Toast.LENGTH_SHORT).show();
                    fetchAddresses();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
            }
        });
    }

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tambah Alamat");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        final EditText nameInput = new EditText(this);
        nameInput.setHint("Nama Penerima");
        layout.addView(nameInput);

        final EditText phoneInput = new EditText(this);
        phoneInput.setHint("Nomor HP");
        layout.addView(phoneInput);

        final EditText addressInput = new EditText(this);
        addressInput.setHint("Alamat Lengkap");
        currentAddressInput = addressInput;

        Button btnPickMap = new Button(this);
        btnPickMap.setText("PILIH LOKASI DARI MAP");
        btnPickMap.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        btnPickMap.setTextColor(getResources().getColor(android.R.color.white));
        layout.addView(btnPickMap);

        btnPickMap.setOnClickListener(v -> {
            Intent intent = new Intent(AddressActivity.this, MapPickerActivity.class);
            mapPickerLauncher.launch(intent);
        });

        layout.addView(addressInput);

        builder.setView(layout);

        builder.setPositiveButton("Simpan", (dialog, which) -> {
            String name = nameInput.getText().toString();
            String phone = phoneInput.getText().toString();
            String address = addressInput.getText().toString();

            if (name.isEmpty() || address.isEmpty()) {
                Toast.makeText(this, "Nama dan alamat tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }

            apiService.addAddress("Bearer " + sessionManager.getToken(), name, phone, address, null, null, false)
                    .enqueue(new Callback<ApiResponse<Object>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                            Toast.makeText(AddressActivity.this, "Alamat Disimpan", Toast.LENGTH_SHORT).show();
                            fetchAddresses();
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                            Toast.makeText(AddressActivity.this, "Gagal koneksi", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        builder.setNegativeButton("Batal", (dialog, which) -> dialog.cancel());
        builder.show();
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
