package com.octania.marketplace.ui.payment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.Gson;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.model.Transaction;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityPendingPaymentsBinding;
import com.octania.marketplace.utils.SessionManager;
import com.octania.marketplace.utils.ToastManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PendingPaymentsActivity extends AppCompatActivity {

    private ActivityPendingPaymentsBinding binding;
    private PendingPaymentAdapter adapter;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPendingPaymentsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        setupRecyclerView();
        loadPendingTransactions();

        binding.swipeRefresh.setOnRefreshListener(this::loadPendingTransactions);

        // Tombol "Lanjutkan" untuk input VA manual
        binding.btnContinueVA.setOnClickListener(v -> {
            String va = binding.etVaNumber.getText().toString().trim();
            if (va.isEmpty()) {
                binding.tilVA.setError("Nomor VA tidak boleh kosong");
                return;
            }
            binding.tilVA.setError(null);
            processViaVerifyAPI(va);
        });
    }

    private void setupRecyclerView() {
        adapter = new PendingPaymentAdapter();
        binding.rvPendingPayments.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPendingPayments.setAdapter(adapter);

        // Ketika "Bayar Sekarang" diklik — langsung proses bayar, jangan buka PaymentActivity
        adapter.setOnPaymentClickListener(transaction -> {
            showPaymentConfirmDialog(transaction);
        });
    }

    /**
     * Konfirmasi sebelum bayar dari daftar tagihan aktif
     */
    private void showPaymentConfirmDialog(Transaction transaction) {
        String txRef = transaction.getTransactionNumber() != null
                ? transaction.getTransactionNumber()
                : "#" + transaction.getId();
        double amount = transaction.getTotalAmount();

        new AlertDialog.Builder(this)
                .setTitle("Konfirmasi Pembayaran")
                .setMessage("Bayar pesanan " + txRef + "\nTotal: Rp " + String.format("%,.0f", amount) + "\n\nSaldo MeyPay akan langsung dipotong.")
                .setPositiveButton("Bayar Sekarang", (d, w) -> directPayWithWallet(transaction.getId()))
                .setNegativeButton("Batal", null)
                .show();
    }

    /**
     * Bayar langsung dengan saldo MeyPay (tanpa input VA)
     */
    private void directPayWithWallet(int transactionId) {
        showLoading(true);
        String token = "Bearer " + sessionManager.getToken();
        apiService.payWithWallet(token, transactionId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    onPaymentSuccess(response.body().getData());
                } else {
                    String msg = "Pembayaran gagal";
                    try {
                        if (response.errorBody() != null) {
                            ApiResponse<?> err = new Gson().fromJson(response.errorBody().string(), ApiResponse.class);
                            if (err.getMessage() != null) msg = err.getMessage();
                        }
                    } catch (Exception ignored) {}
                    ToastManager.showToast(PendingPaymentsActivity.this, msg);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                showLoading(false);
                ToastManager.showToast(PendingPaymentsActivity.this, "Error jaringan: " + t.getMessage());
            }
        });
    }

    /**
     * Verifikasi kode VA yang diinput manual via API
     */
    private void processViaVerifyAPI(String code) {
        showLoading(true);
        String token = "Bearer " + sessionManager.getToken();
        apiService.verifyPaymentCode(token, code).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    onPaymentSuccess(response.body().getData());
                } else {
                    String msg = "Kode VA tidak valid";
                    try {
                        if (response.errorBody() != null) {
                            ApiResponse<?> err = new Gson().fromJson(response.errorBody().string(), ApiResponse.class);
                            if (err.getMessage() != null) msg = err.getMessage();
                        }
                    } catch (Exception ignored) {}
                    binding.tilVA.setError(msg);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                showLoading(false);
                binding.tilVA.setError("Error jaringan: " + t.getMessage());
            }
        });
    }

    /**
     * Navigasi ke PaymentSuccessActivity setelah bayar berhasil
     */
    private void onPaymentSuccess(Object data) {
        double amount = 0;
        int txId = 0;
        String txNumber = "";
        try {
            Map<String, Object> map = (Map<String, Object>) data;
            if (map != null) {
                if (map.get("total_amount") != null)
                    amount = ((Number) map.get("total_amount")).doubleValue();
                if (map.get("transaction_id") != null)
                    txId = ((Number) map.get("transaction_id")).intValue();
                if (map.get("transaction_number") != null)
                    txNumber = String.valueOf(map.get("transaction_number"));
            }
        } catch (Exception ignored) {}

        Intent intent = new Intent(this, PaymentSuccessActivity.class);
        intent.putExtra("amount", amount);
        intent.putExtra("transaction_id", txId);
        intent.putExtra("transaction_number", txNumber);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        binding.loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnContinueVA.setEnabled(!show);
    }

    private void loadPendingTransactions() {
        binding.swipeRefresh.setRefreshing(true);
        String token = "Bearer " + sessionManager.getToken();

        apiService.getTransactions(token).enqueue(new Callback<ApiResponse<List<Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Object>>> call, Response<ApiResponse<List<Object>>> response) {
                binding.swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<Object> rawList = response.body().getData();
                    List<Transaction> pendingList = new ArrayList<>();
                    Gson gson = new Gson();

                    for (Object obj : rawList) {
                        Transaction t = gson.fromJson(gson.toJson(obj), Transaction.class);
                        if ("waiting_payment".equals(t.getStatus()) || "pending".equals(t.getStatus())) {
                            pendingList.add(t);
                        }
                    }

                    adapter.setTransactions(pendingList);
                    binding.llEmpty.setVisibility(pendingList.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    ToastManager.showToast(PendingPaymentsActivity.this, "Gagal memuat data");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Object>>> call, Throwable t) {
                binding.swipeRefresh.setRefreshing(false);
                ToastManager.showToast(PendingPaymentsActivity.this, "Error: " + t.getMessage());
            }
        });
    }
}
