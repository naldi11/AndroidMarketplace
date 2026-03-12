package com.octania.marketplace.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityAddEditAddressBinding;
import com.octania.marketplace.utils.SessionManager;

import java.lang.reflect.Type;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddEditAddressActivity extends AppCompatActivity {

    private ActivityAddEditAddressBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;

    private int addressIdToEdit = -1;

    private final ActivityResultLauncher<Intent> mapLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String addr = result.getData().getStringExtra("picked_address");
                    if (addr != null && !addr.isEmpty()) {
                        binding.etFullAddress.setText(addr);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEditAddressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        setSupportActionBar(binding.toolbar);

        if (getIntent().hasExtra("address_json")) {
            getSupportActionBar().setTitle("Ubah Alamat");
            try {
                String json = getIntent().getStringExtra("address_json");
                Type mapType = new TypeToken<Map<String, Object>>() {
                }.getType();
                Map<String, Object> addr = new Gson().fromJson(json, mapType);

                addressIdToEdit = ((Double) addr.get("id")).intValue();
                binding.etRecipientName.setText(String.valueOf(addr.get("recipient_name")));
                binding.etPhone.setText(String.valueOf(addr.get("phone")));
                binding.etFullAddress.setText(String.valueOf(addr.get("full_address")));

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            getSupportActionBar().setTitle("Tambah Alamat");
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.btnSave.setOnClickListener(v -> saveAddress());
        binding.btnUseMap.setOnClickListener(v -> mapLauncher.launch(new Intent(this, MapPickerActivity.class)));
    }

    private void saveAddress() {
        String name = binding.etRecipientName.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String address = binding.etFullAddress.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Harap lengkapi semua data", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSave.setEnabled(false);
        binding.btnSave.setText("Menyimpan...");

        String token = "Bearer " + sessionManager.getToken();

        Callback<ApiResponse<Object>> callback = new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                binding.btnSave.setEnabled(true);
                binding.btnSave.setText("Simpan Alamat");
                if (response.isSuccessful()) {
                    Toast.makeText(AddEditAddressActivity.this, "Alamat berhasil disimpan!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AddEditAddressActivity.this, "Gagal menyimpan alamat", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                binding.btnSave.setEnabled(true);
                binding.btnSave.setText("Simpan Alamat");
                Toast.makeText(AddEditAddressActivity.this, "Error koneksi", Toast.LENGTH_SHORT).show();
            }
        };

        if (addressIdToEdit == -1) {
            // Create
            apiService.addAddress(token, name, phone, address, null, null, false).enqueue(callback);
        } else {
            // Update
            apiService.updateAddress(token, addressIdToEdit, name, phone, address).enqueue(callback);
        }
    }
}
