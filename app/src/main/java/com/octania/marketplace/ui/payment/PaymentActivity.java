package com.octania.marketplace.ui.payment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Toast;
import android.database.Cursor;

import androidx.appcompat.app.AppCompatActivity;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityPaymentBinding;
import com.octania.marketplace.ui.home.HomeActivity;
import com.octania.marketplace.utils.SessionManager;

import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.graphics.Bitmap;
import com.octania.marketplace.utils.QrCodeUtils;
import java.io.OutputStream;
import android.content.ContentValues;
import android.provider.MediaStore;
import android.os.CountDownTimer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class PaymentActivity extends AppCompatActivity {
    private ActivityPaymentBinding binding;
    private SessionManager sessionManager;
    private ApiService apiService;
    private int transactionId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Pembayaran");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        transactionId = getIntent().getIntExtra("transaction_id", -1);
        if (transactionId == -1) {
            Toast.makeText(this, "ID Transaksi Tidak Valid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadTransactionData();
        startPaymentStatusPolling();
    }

    private void initViews() {
        binding.btnCheckStatus.setOnClickListener(v -> {
            loadTransactionData();
            checkStatus();
        });
        binding.btnDownloadQr.setOnClickListener(v -> downloadQrCode());
        binding.btnPayWithWallet.setOnClickListener(v -> openWalletPayment());
        
        binding.cardVa.setOnClickListener(v -> {
            String va = binding.tvMeyPayVA.getText().toString();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("MeyPay VA", va);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Nomor VA berhasil disalin", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Bitmap currentQrBitmap;
    private double currentAmount = 0;
    private CountDownTimer countDownTimer;

    private void loadTransactionData() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getTransactionDetail(token, transactionId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    java.util.Map<String, Object> data = (java.util.Map<String, Object>) response.body().getData();
                    if (data != null) {
                        currentAmount = ((Number) data.get("total_amount")).doubleValue();
                        binding.tvAmount.setText(String.format("Rp %,.0f", currentAmount));

                        // Tampilkan nomor transaksi yang aman
                        String txRef = (data.containsKey("transaction_number") && data.get("transaction_number") != null)
                                ? String.valueOf(data.get("transaction_number"))
                                : "#" + transactionId;
                        binding.tvOrderNumber.setText("No. Pesanan: " + txRef);

                        // Ambil expires_at untuk hitung sisa waktu nyata
                        String expiresAt = (data.containsKey("expires_at") && data.get("expires_at") != null)
                                ? String.valueOf(data.get("expires_at")) : null;
                        
                        if (data.containsKey("meypay_va") && data.get("meypay_va") != null) {
                            binding.tvMeyPayVA.setText(String.valueOf(data.get("meypay_va")));
                        }

                        if (data.containsKey("meypay_qr_content") && data.get("meypay_qr_content") != null) {
                            String qrContent = String.valueOf(data.get("meypay_qr_content"));
                            currentQrBitmap = QrCodeUtils.generateQrCode(qrContent);
                            if (currentQrBitmap != null) {
                                binding.ivQrCode.setImageBitmap(currentQrBitmap);
                            }
                        }

                        startCountdown(expiresAt);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {}
        });
    }

    private void startCountdown(String expiresAtStr) {
        if (countDownTimer != null) countDownTimer.cancel();

        long millisRemaining = 0;

        if (expiresAtStr != null && !expiresAtStr.isEmpty() && !"null".equals(expiresAtStr)) {
            // Format dari Laravel: "2026-05-12T18:21:11.000000Z" atau "2026-05-12 18:21:11"
            String[] formats = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss"
            };
            Date expireDate = null;
            for (String fmt : formats) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.getDefault());
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    expireDate = sdf.parse(expiresAtStr);
                    break;
                } catch (ParseException ignored) {}
            }
            if (expireDate != null) {
                millisRemaining = expireDate.getTime() - System.currentTimeMillis();
            }
        }

        // Fallback: jika tidak bisa parse, pakai 15 menit dari sekarang
        if (millisRemaining <= 0 && expiresAtStr == null) {
            millisRemaining = 15L * 60 * 1000;
        } else if (millisRemaining <= 0) {
            // Waktu sudah habis
            binding.tvCountdown.setText("EXPIRED");
            binding.tvCountdown.setTextColor(0xFFD32F2F);
            binding.btnPayWithWallet.setEnabled(false);
            binding.btnPayWithWallet.setText("WAKTU HABIS");
            Toast.makeText(this, "Waktu pembayaran telah habis. Pesanan akan dibatalkan otomatis.", Toast.LENGTH_LONG).show();
            return;
        }

        final long duration = millisRemaining;
        countDownTimer = new CountDownTimer(duration, 1000) {
            public void onTick(long millisUntilFinished) {
                long hours   = millisUntilFinished / (1000 * 60 * 60);
                long minutes = (millisUntilFinished / (1000 * 60)) % 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                String timeStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
                binding.tvCountdown.setText(timeStr);

                // Warna merah jika kurang dari 1 jam
                if (millisUntilFinished < 60 * 60 * 1000) {
                    binding.tvCountdown.setTextColor(0xFFD32F2F); // merah
                } else if (millisUntilFinished < 3 * 60 * 60 * 1000) {
                    binding.tvCountdown.setTextColor(0xFFFF6F00); // oranye
                } else {
                    binding.tvCountdown.setTextColor(0xFF4CAF50); // hijau
                }
            }
            public void onFinish() {
                binding.tvCountdown.setText("EXPIRED");
                binding.tvCountdown.setTextColor(0xFFD32F2F);
                binding.btnPayWithWallet.setEnabled(false);
                binding.btnPayWithWallet.setText("WAKTU HABIS");
                Toast.makeText(PaymentActivity.this, "Waktu pembayaran telah habis", Toast.LENGTH_LONG).show();
            }
        }.start();
    }

    /**
     * Arahkan ke layar konfirmasi pembayaran MeyPay
     * Bukan langsung proses — user harus konfirmasi dulu di WalletPaymentActivity
     */
    private void openWalletPayment() {
        Intent intent = new Intent(this, WalletPaymentActivity.class);
        intent.putExtra("transaction_id", transactionId);
        intent.putExtra("amount", currentAmount);
        startActivity(intent);
    }

    private void payWithWallet() {
        binding.btnPayWithWallet.setEnabled(false);
        binding.btnPayWithWallet.setText("MEMPROSES...");

        String token = "Bearer " + sessionManager.getToken();
        apiService.payWithWallet(token, transactionId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(PaymentActivity.this, "Pembayaran Berhasil!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(PaymentActivity.this, PaymentSuccessActivity.class);
                    intent.putExtra("transaction_id", transactionId);
                    startActivity(intent);
                    finish();
                } else {
                    binding.btnPayWithWallet.setEnabled(true);
                    binding.btnPayWithWallet.setText("BAYAR DENGAN SALDO MEYPAY");
                    
                    String message = "Gagal memproses pembayaran";
                    try {
                        if (response.errorBody() != null) {
                            String errStr = response.errorBody().string();
                            ApiResponse<?> err = new Gson().fromJson(errStr, ApiResponse.class);
                            if (err != null && err.getMessage() != null) message = err.getMessage();
                        }
                    } catch (Exception ignored) {}
                    
                    Toast.makeText(PaymentActivity.this, message, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                binding.btnPayWithWallet.setEnabled(true);
                binding.btnPayWithWallet.setText("BAYAR DENGAN SALDO MEYPAY");
                Toast.makeText(PaymentActivity.this, "Error Jaringan: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkStatus() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.checkPaymentStatus(token, transactionId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    java.util.Map<String, Object> data = (java.util.Map<String, Object>) response.body().getData();
                    String status = String.valueOf(data.get("payment_status"));
                    if ("paid_verified".equals(status) || "processing".equals(status) || "packed".equals(status) || "shipped".equals(status)) {
                        onPaymentSuccess();
                    } else {
                        Toast.makeText(PaymentActivity.this, "Status: " + status, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {}
        });
    }

    private void onPaymentSuccess() {
        stopPolling();
        Intent intent = new Intent(this, PaymentSuccessActivity.class);
        intent.putExtra("amount", currentAmount);
        intent.putExtra("transaction_id", transactionId);
        startActivity(intent);
        finish();
    }

    private void downloadQrCode() {
        if (currentQrBitmap == null) return;

        try {
            String filename = "QRIS_MeyPay_" + transactionId + ".png";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MeyPay");

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                OutputStream out = getContentResolver().openOutputStream(uri);
                currentQrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();
                Toast.makeText(this, "QRIS berhasil diunduh ke Galeri", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal mengunduh QRIS", Toast.LENGTH_SHORT).show();
        }
    }

    // Polling logic
    private android.os.Handler pollingHandler = new android.os.Handler();
    private Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            checkStatusSilent();
            pollingHandler.postDelayed(this, 5000); // Poll every 5 seconds
        }
    };

    private void startPaymentStatusPolling() {
        pollingHandler.postDelayed(pollingRunnable, 5000);
    }

    private void stopPolling() {
        pollingHandler.removeCallbacks(pollingRunnable);
    }

    private void checkStatusSilent() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.checkPaymentStatus(token, transactionId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    java.util.Map<String, Object> data = (java.util.Map<String, Object>) response.body().getData();
                    String status = String.valueOf(data.get("payment_status"));
                    if ("paid_verified".equals(status) || "processing".equals(status) || "packed".equals(status) || "shipped".equals(status)) {
                        onPaymentSuccess();
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
    }
}
