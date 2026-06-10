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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityPaymentBinding;
import com.octania.marketplace.utils.SessionManager;

import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.os.CountDownTimer;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class PaymentActivity extends AppCompatActivity {
    private ActivityPaymentBinding binding;
    private SessionManager sessionManager;
    private ApiService apiService;
    private int transactionId = -1;
    private String transactionNumber = "";
    private double currentAmount = 0;
    private CountDownTimer countDownTimer;
    private Uri selectedProofUri = null;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedProofUri = result.getData().getData();
                    if (selectedProofUri != null) {
                        binding.ivProofPreview.setImageURI(selectedProofUri);
                        binding.ivProofPreview.setVisibility(View.VISIBLE);
                        binding.btnUploadProof.setVisibility(View.VISIBLE);
                    }
                }
            });

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

        binding.btnCopyAccount.setOnClickListener(v -> {
            String accNum = binding.tvAccountNumber.getText().toString();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Nomor Rekening", accNum);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Nomor rekening berhasil disalin", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnSelectProof.setOnClickListener(v -> openImagePicker());

        binding.btnUploadProof.setOnClickListener(v -> uploadProofOfPayment());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "Pilih Bukti Pembayaran"));
    }

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

                        if (data.containsKey("transaction_number") && data.get("transaction_number") != null) {
                            transactionNumber = String.valueOf(data.get("transaction_number"));
                        } else {
                            transactionNumber = "#" + transactionId;
                        }
                        binding.tvOrderNumber.setText("No. Pesanan: " + transactionNumber);

                        String expiresAt = (data.containsKey("expires_at") && data.get("expires_at") != null)
                                ? String.valueOf(data.get("expires_at")) : null;

                        if (data.containsKey("payment_method_detail") && data.get("payment_method_detail") != null) {
                            java.util.Map<String, Object> methodDetail = (java.util.Map<String, Object>) data.get("payment_method_detail");
                            String bankName = methodDetail.containsKey("name") ? String.valueOf(methodDetail.get("name")) : "-";
                            String accNumber = methodDetail.containsKey("account_number") ? String.valueOf(methodDetail.get("account_number")) : "-";
                            String accHolder = methodDetail.containsKey("account_holder_name") ? String.valueOf(methodDetail.get("account_holder_name")) : "Administrasi Market";
                            
                            binding.tvBankName.setText(bankName);
                            binding.tvAccountNumber.setText(accNumber);
                            binding.tvAccountHolder.setText(accHolder);
                        } else {
                            String legacyMethod = data.containsKey("payment_method") ? String.valueOf(data.get("payment_method")) : "Transfer Bank";
                            binding.tvBankName.setText(legacyMethod);
                            binding.tvAccountNumber.setText("-");
                            binding.tvAccountHolder.setText("Administrasi Market");
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

        if (millisRemaining <= 0 && expiresAtStr == null) {
            millisRemaining = 15L * 60 * 1000;
        } else if (millisRemaining <= 0) {
            binding.tvCountdown.setText("EXPIRED");
            binding.tvCountdown.setTextColor(0xFFD32F2F);
            binding.btnSelectProof.setEnabled(false);
            binding.btnSelectProof.setText("WAKTU TRANSFER HABIS");
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

                if (millisUntilFinished < 60 * 60 * 1000) {
                    binding.tvCountdown.setTextColor(0xFFD32F2F);
                } else if (millisUntilFinished < 3 * 60 * 60 * 1000) {
                    binding.tvCountdown.setTextColor(0xFFFF6F00);
                } else {
                    binding.tvCountdown.setTextColor(0xFF4CAF50);
                }
            }
            public void onFinish() {
                binding.tvCountdown.setText("EXPIRED");
                binding.tvCountdown.setTextColor(0xFFD32F2F);
                binding.btnSelectProof.setEnabled(false);
                binding.btnSelectProof.setText("WAKTU TRANSFER HABIS");
                Toast.makeText(PaymentActivity.this, "Waktu pembayaran telah habis", Toast.LENGTH_LONG).show();
            }
        }.start();
    }

    private void uploadProofOfPayment() {
        if (selectedProofUri == null) return;
        showLoading(true, "Mengunggah Bukti Pembayaran...");

        String token = "Bearer " + sessionManager.getToken();
        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedProofUri);
            byte[] bytes = getBytes(inputStream);

            String filename = "proof_" + System.currentTimeMillis() + ".jpg";
            Cursor returnCursor = getContentResolver().query(selectedProofUri, null, null, null, null);
            if (returnCursor != null) {
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();
                filename = returnCursor.getString(nameIndex);
                returnCursor.close();
            }

            RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(selectedProofUri)), bytes);
            MultipartBody.Part body = MultipartBody.Part.createFormData("proof_of_payment", filename, requestFile);

            apiService.uploadProof(token, transactionId, body).enqueue(new Callback<ApiResponse<Object>>() {
                @Override
                public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                    showLoading(false, "");
                    if (response.isSuccessful()) {
                        Toast.makeText(PaymentActivity.this, "Bukti Pembayaran Berhasil Dikirim! Menunggu Verifikasi Admin.", Toast.LENGTH_LONG).show();
                        binding.btnUploadProof.setVisibility(View.GONE);
                        binding.btnSelectProof.setText("Bukti Terkirim (Pilih Ulang)");
                    } else {
                        Toast.makeText(PaymentActivity.this, "Gagal mengunggah bukti.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                    showLoading(false, "");
                    Toast.makeText(PaymentActivity.this, "Error jaringan: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            showLoading(false, "");
            e.printStackTrace();
            Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] getBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
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
                        Toast.makeText(PaymentActivity.this, "Status Pembayaran: " + status, Toast.LENGTH_SHORT).show();
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

    // Polling logic
    private android.os.Handler pollingHandler = new android.os.Handler();
    private Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            checkStatusSilent();
            pollingHandler.postDelayed(this, 7000); // Poll every 7 seconds
        }
    };

    private void startPaymentStatusPolling() {
        pollingHandler.postDelayed(pollingRunnable, 7000);
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

    private void showLoading(boolean show, String message) {
        binding.loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show && message != null && !message.isEmpty()) {
            binding.tvLoadingMessage.setText(message);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
