package com.octania.marketplace.ui.payment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.octania.marketplace.R;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.model.WalletTransaction;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityWalletBinding;
import com.octania.marketplace.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletActivity extends AppCompatActivity {

    private ActivityWalletBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private WalletTransactionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWalletBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        setupRecyclerView();
        setupClickListeners();
        loadData();
    }

    private void setupRecyclerView() {
        adapter = new WalletTransactionAdapter();
        binding.rvWalletTransactions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvWalletTransactions.setAdapter(adapter);
    }

    private void setupClickListeners() {
        binding.btnTopUpAction.setOnClickListener(v -> showTopUpDialog());
        binding.btnScan.setOnClickListener(v -> startActivity(new Intent(this, ScanQrActivity.class)));
        binding.btnPayVA.setOnClickListener(v -> startActivity(new Intent(this, PendingPaymentsActivity.class)));
        binding.btnTransfer.setOnClickListener(v -> Toast.makeText(this, "Fitur Transfer segera hadir!", Toast.LENGTH_SHORT).show());
    }

    private void loadData() {
        fetchWalletInfo();
        fetchWalletTransactions();
    }

    private void fetchWalletInfo() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getWalletInfo(token).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().getData();
                    if (data != null) {
                        double balance = ((Number) data.get("balance")).doubleValue();
                        String walletNumber = (String) data.get("wallet_number");
                        
                        binding.tvBalance.setText(String.format("Rp %,.0f", balance));
                        binding.tvWalletNumber.setText(walletNumber != null ? walletNumber : "—");
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {}
        });
    }

    private void fetchWalletTransactions() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getWalletTransactions(token).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Logic to parse paginated data
                    Map<String, Object> data = (Map<String, Object>) response.body().getData();
                    if (data != null && data.containsKey("data")) {
                        List<Object> listData = (List<Object>) data.get("data");
                        List<WalletTransaction> transactions = new ArrayList<>();
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        for (Object obj : listData) {
                            transactions.add(gson.fromJson(gson.toJson(obj), WalletTransaction.class));
                        }
                        
                        adapter.setList(transactions);
                        binding.llEmptyTransactions.setVisibility(transactions.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {}
        });
    }

    private void showTopUpDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null); // Reusing a simple dialog with EditText
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Isi Saldo MeyPay (Sandbox)");
        
        final EditText etAmount = new EditText(this);
        etAmount.setHint("Masukkan jumlah (Min 10,000)");
        etAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        
        // Add padding to EditText
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        builder.setView(etAmount);
        
        builder.setPositiveButton("Top Up", (dialog, which) -> {
            String amountStr = etAmount.getText().toString();
            if (!amountStr.isEmpty()) {
                double amount = Double.parseDouble(amountStr);
                if (amount >= 10000) {
                    performTopUp(amount);
                } else {
                    Toast.makeText(this, "Minimal Top Up Rp 10.000", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Batal", null);
        builder.show();
    }

    private void performTopUp(double amount) {
        String token = "Bearer " + sessionManager.getToken();
        apiService.topupWallet(token, amount).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(WalletActivity.this, "Top Up Berhasil! ✅", Toast.LENGTH_SHORT).show();
                    loadData();
                } else {
                    Toast.makeText(WalletActivity.this, "Top Up Gagal", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                Toast.makeText(WalletActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
