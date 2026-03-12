package com.octania.marketplace.ui.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityManageAddressBinding;
import com.octania.marketplace.utils.SessionManager;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageAddressActivity extends AppCompatActivity {

    private ActivityManageAddressBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private AddressAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManageAddressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        setupRecyclerView();

        binding.swipeRefresh.setOnRefreshListener(this::loadAddresses);
        binding.fabAddAddress.setOnClickListener(v -> {
            startActivity(new Intent(this, AddEditAddressActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAddresses();
    }

    private void setupRecyclerView() {
        adapter = new AddressAdapter(this, false, new AddressAdapter.AddressActionCallback() {
            @Override
            public void onAddressSelected(Map<String, Object> address) {
            }

            @Override
            public void onSetDefault(Map<String, Object> address) {
                int id = ((Double) address.get("id")).intValue();
                setDefaultAddress(id);
            }

            @Override
            public void onEdit(Map<String, Object> address) {
                Intent intent = new Intent(ManageAddressActivity.this, AddEditAddressActivity.class);
                intent.putExtra("address_json", new Gson().toJson(address));
                startActivity(intent);
            }

            @Override
            public void onDelete(Map<String, Object> address) {
                int id = ((Double) address.get("id")).intValue();
                new AlertDialog.Builder(ManageAddressActivity.this)
                        .setTitle("Hapus Alamat")
                        .setMessage("Yakin ingin menghapus alamat ini?")
                        .setPositiveButton("Ya", (d, w) -> deleteAddress(id))
                        .setNegativeButton("Tidak", null)
                        .show();
            }
        });
        binding.rvAddresses.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAddresses.setAdapter(adapter);
    }

    private void loadAddresses() {
        binding.swipeRefresh.setRefreshing(true);
        String token = "Bearer " + sessionManager.getToken();
        apiService.getAddresses(token).enqueue(new Callback<ApiResponse<List<Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Object>>> call, Response<ApiResponse<List<Object>>> response) {
                binding.swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Object>> res = response.body();
                    if ("success".equals(res.getStatus()) && res.getData() != null) {
                        try {
                            Gson gson = new Gson();
                            String json = gson.toJson(res.getData());
                            Type listType = new TypeToken<List<Map<String, Object>>>() {
                            }.getType();
                            List<Map<String, Object>> userAddresses = gson.fromJson(json, listType);

                            if (userAddresses.isEmpty()) {
                                binding.tvEmptyAddresses.setVisibility(View.VISIBLE);
                                binding.rvAddresses.setVisibility(View.GONE);
                            } else {
                                binding.tvEmptyAddresses.setVisibility(View.GONE);
                                binding.rvAddresses.setVisibility(View.VISIBLE);
                                adapter.setAddresses(userAddresses);
                            }
                        } catch (Exception e) {
                            Toast.makeText(ManageAddressActivity.this, "Gagal parsing alamat", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    } else {
                        Toast.makeText(ManageAddressActivity.this, "Gagal meload alamat", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Object>>> call, Throwable t) {
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(ManageAddressActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setDefaultAddress(int id) {
        String token = "Bearer " + sessionManager.getToken();
        apiService.setDefaultAddress(token, id).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ManageAddressActivity.this, "Alamat utama diperbarui!", Toast.LENGTH_SHORT).show();
                    loadAddresses();
                } else {
                    Toast.makeText(ManageAddressActivity.this, "Gagal memperbarui", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                Toast.makeText(ManageAddressActivity.this, "Error koneksi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteAddress(int id) {
        String token = "Bearer " + sessionManager.getToken();
        apiService.deleteAddress(token, id).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ManageAddressActivity.this, "Alamat dihapus!", Toast.LENGTH_SHORT).show();
                    loadAddresses();
                } else {
                    Toast.makeText(ManageAddressActivity.this, "Gagal menghapus", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                Toast.makeText(ManageAddressActivity.this, "Error koneksi", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
