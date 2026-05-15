package com.octania.marketplace.ui.payment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityWalletPaymentBinding;
import com.octania.marketplace.utils.SessionManager;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletPaymentActivity extends AppCompatActivity {

    private ActivityWalletPaymentBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private int transactionId;
    private double amount;
    private double walletBalance = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWalletPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Konfirmasi Pembayaran");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        transactionId = getIntent().getIntExtra("transaction_id", -1);
        amount = getIntent().getDoubleExtra("amount", 0);

        if (transactionId == -1) {
            Toast.makeText(this, "ID Transaksi tidak valid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Tampilkan jumlah tagihan
        binding.tvPayAmount.setText(String.format("Rp %,.0f", amount));
        binding.tvTransactionId.setText("ID Pesanan: " + transactionId);

        // Muat saldo wallet
        loadWalletBalance();

        binding.btnConfirmPay.setOnClickListener(v -> showConfirmDialog());
        binding.btnCancel.setOnClickListener(v -> finish());
    }

    private void loadWalletBalance() {
        binding.loadingBalance.setVisibility(View.VISIBLE);
        binding.tvWalletBalance.setVisibility(View.GONE);

        String token = "Bearer " + sessionManager.getToken();
        apiService.getWalletInfo(token).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                binding.loadingBalance.setVisibility(View.GONE);
                binding.tvWalletBalance.setVisibility(View.VISIBLE);

                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Map<String, Object> data = (Map<String, Object>) response.body().getData();
                        Object balObj = data.get("balance");
                        walletBalance = balObj instanceof Number
                                ? ((Number) balObj).doubleValue()
                                : Double.parseDouble(String.valueOf(balObj));

                        binding.tvWalletBalance.setText(String.format("Rp %,.0f", walletBalance));

                        // Disable tombol jika saldo tidak cukup
                        if (walletBalance < amount) {
                            binding.tvBalanceStatus.setText("⚠️ Saldo tidak mencukupi");
                            binding.tvBalanceStatus.setTextColor(0xFFD32F2F);
                            binding.btnConfirmPay.setEnabled(false);
                            binding.btnConfirmPay.setAlpha(0.5f);
                        } else {
                            binding.tvBalanceStatus.setText("✅ Saldo mencukupi");
                            binding.tvBalanceStatus.setTextColor(0xFF4CAF50);
                        }
                    } catch (Exception e) {
                        binding.tvWalletBalance.setText("Gagal memuat saldo");
                    }
                } else {
                    binding.tvWalletBalance.setText("Gagal memuat saldo");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                binding.loadingBalance.setVisibility(View.GONE);
                binding.tvWalletBalance.setText("Error: " + t.getMessage());
            }
        });
    }

    private void showConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Konfirmasi Pembayaran")
                .setMessage(String.format(
                        "Bayar sebesar Rp %,.0f menggunakan saldo MeyPay?\n\nSaldo setelah bayar: Rp %,.0f",
                        amount, walletBalance - amount))
                .setPositiveButton("Ya, Bayar Sekarang", (d, w) -> processPayment())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void processPayment() {
        binding.btnConfirmPay.setEnabled(false);
        binding.btnConfirmPay.setText("MEMPROSES...");
        binding.loadingOverlay.setVisibility(View.VISIBLE);

        String token = "Bearer " + sessionManager.getToken();
        apiService.payWithWallet(token, transactionId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                binding.loadingOverlay.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    // Ambil transaction_number dari response jika ada
                    String txNumber = "";
                    try {
                        Map<String, Object> data = (Map<String, Object>) response.body().getData();
                        if (data != null && data.get("transaction_number") != null) {
                            txNumber = String.valueOf(data.get("transaction_number"));
                        }
                    } catch (Exception ignored) {}

                    Intent intent = new Intent(WalletPaymentActivity.this, PaymentSuccessActivity.class);
                    intent.putExtra("transaction_id", transactionId);
                    intent.putExtra("transaction_number", txNumber);
                    intent.putExtra("amount", amount);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    binding.btnConfirmPay.setEnabled(true);
                    binding.btnConfirmPay.setText("BAYAR SEKARANG");

                    String msg = "Gagal memproses pembayaran";
                    try {
                        if (response.errorBody() != null) {
                            ApiResponse<?> err = new Gson().fromJson(
                                    response.errorBody().string(), ApiResponse.class);
                            if (err != null && err.getMessage() != null) msg = err.getMessage();
                        }
                    } catch (Exception ignored) {}
                    Toast.makeText(WalletPaymentActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                binding.loadingOverlay.setVisibility(View.GONE);
                binding.btnConfirmPay.setEnabled(true);
                binding.btnConfirmPay.setText("BAYAR SEKARANG");
                Toast.makeText(WalletPaymentActivity.this,
                        "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
