package com.octania.marketplace.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.model.Voucher;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityMyVoucherBinding;
import com.octania.marketplace.ui.cart.VoucherAdapter;
import com.octania.marketplace.ui.home.HomeActivity;
import com.octania.marketplace.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyVoucherActivity extends AppCompatActivity {

    private ActivityMyVoucherBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private VoucherAdapter voucherAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyVoucherBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        setupToolbar();
        setupAdapters();
        loadVouchers();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupAdapters() {
        // Vouchers
        voucherAdapter = new VoucherAdapter();
        voucherAdapter.setSelectionMode(false); // Mode tampilan, bukan seleksi checkout
        binding.rvVouchers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvVouchers.setAdapter(voucherAdapter);
        
        voucherAdapter.setOnVoucherClickListener(voucher -> {
            // Redirect ke Home untuk menggunakan voucher
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            Toast.makeText(this, "Silakan pilih produk untuk menggunakan voucher " + voucher.getCode(), Toast.LENGTH_LONG).show();
        });
    }

    private void loadVouchers() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmpty.setVisibility(View.GONE);

        // Fetch all user vouchers
        apiService.getVouchers("Bearer " + sessionManager.getToken(), 0).enqueue(new Callback<ApiResponse<List<Voucher>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Voucher>>> call, Response<ApiResponse<List<Voucher>>> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<Voucher> vouchers = response.body().getData();
                    if (vouchers == null || vouchers.isEmpty()) {
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                        voucherAdapter.setVouchers(new ArrayList<>());
                    } else {
                        voucherAdapter.setVouchers(vouchers);
                    }
                } else {
                    Toast.makeText(MyVoucherActivity.this, "Gagal memuat voucher", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Voucher>>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(MyVoucherActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
