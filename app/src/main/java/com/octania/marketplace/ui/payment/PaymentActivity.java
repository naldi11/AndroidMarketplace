package com.octania.marketplace.ui.payment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Toast;
import android.database.Cursor;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityPaymentBinding;
import com.octania.marketplace.ui.home.HomeActivity;
import com.octania.marketplace.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentActivity extends AppCompatActivity {
    private ActivityPaymentBinding binding;
    private SessionManager sessionManager;
    private ApiService apiService;
    private int transactionId = -1;
    private List<Uri> selectedImageUris = new ArrayList<>();
    private ProofImageAdapter proofImageAdapter;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUris.add(result.getData().getData());
                    proofImageAdapter.notifyDataSetChanged();
                    binding.btnSubmitProof.setEnabled(true);
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

        binding.tvInstruction
                .setText("Silakan transfer ke rekening BCA 1234567890 a.n Octania Market.\n\nSimpan pesanan ini ID #"
                        + transactionId + " sebagai referensi berita acara Anda.");

        // Setup RecyclerView for multiple proof images
        proofImageAdapter = new ProofImageAdapter(this, selectedImageUris);
        binding.rvProofImages.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        binding.rvProofImages.setAdapter(proofImageAdapter);

        binding.btnSelectProof.setOnClickListener(v -> openImageChooser());
        binding.btnSubmitProof.setOnClickListener(v -> showConfirmationDialog());
        binding.btnPayLater.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        filePickerLauncher.launch(intent);
    }

    private void showConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setMessage("Apakah Anda yakin ingin mengunggah bukti pembayaran?");
        builder.setPositiveButton("Ya", (dialog, which) -> submitProof());
        builder.setNegativeButton("Tidak", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void submitProof() {
        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Pilih gambar bukti bayar dulu", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSubmitProof.setEnabled(false);
        binding.btnSubmitProof.setText("Mengunggah...");

        try {
            // For now, upload first image only (can be extended for multiple uploads)
            Uri firstImageUri = selectedImageUris.get(0);
            java.io.File imageFile = createTempFileFromUri(firstImageUri);
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), imageFile);
            MultipartBody.Part body = MultipartBody.Part.createFormData("proof_of_payment", imageFile.getName(),
                    requestFile);

            String token = "Bearer " + sessionManager.getToken();
            apiService.uploadProof(token, transactionId, body).enqueue(new Callback<ApiResponse<Object>>() {
                @Override
                public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(PaymentActivity.this, "Bukti pembayaran berhasil diunggah", Toast.LENGTH_SHORT)
                                .show();
                        Intent intent = new Intent(PaymentActivity.this, HomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        binding.btnSubmitProof.setEnabled(true);
                        binding.btnSubmitProof.setText("Kirim Bukti Pembayaran");
                        try {
                            String errorBody = response.errorBody() != null ? response.errorBody().string()
                                    : "Unknown error";
                            android.util.Log.e("UploadProof", "Error HTTP " + response.code() + ": " + errorBody);
                            Toast.makeText(PaymentActivity.this, "Gagal: " + response.code(), Toast.LENGTH_SHORT)
                                    .show();
                        } catch (Exception e) {
                        }
                        Toast.makeText(PaymentActivity.this, "Gagal mengunggah bukti", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                    binding.btnSubmitProof.setEnabled(true);
                    binding.btnSubmitProof.setText("Kirim Bukti Pembayaran");
                    Toast.makeText(PaymentActivity.this, "Jaringan Error: " + t.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                }
            });
        } catch (Exception e) {
            binding.btnSubmitProof.setEnabled(true);
            binding.btnSubmitProof.setText("Kirim Bukti Pembayaran");
            Toast.makeText(this, "Error membaca file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private java.io.File createTempFileFromUri(Uri uri) throws java.io.IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
        java.io.File tempFile = java.io.File.createTempFile("proof_", ".jpg", getCacheDir());
        java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile);

        if (bitmap != null) {
            int maxResolution = 1000;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width > maxResolution || height > maxResolution) {
                float ratio = Math.min((float) maxResolution / width, (float) maxResolution / height);
                width = Math.round(ratio * width);
                height = Math.round(ratio * height);
            }
            android.graphics.Bitmap resized = android.graphics.Bitmap.createScaledBitmap(bitmap, width, height, true);
            resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, out);
        } else {
            // Fallback
            inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        }

        out.close();
        if (inputStream != null)
            inputStream.close();
        return tempFile;
    }
}
