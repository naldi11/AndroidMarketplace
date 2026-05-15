package com.octania.marketplace.ui.payment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.octania.marketplace.R;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityScanQrBinding;
import com.octania.marketplace.utils.SessionManager;
import com.octania.marketplace.utils.ToastManager;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScanQrActivity extends AppCompatActivity {

    private ActivityScanQrBinding binding;
    private ExecutorService cameraExecutor;
    private BarcodeScanner scanner;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private boolean isFlashOn = false;
    private boolean isAnalysisActive = true;
    private boolean isProcessing = false;

    private ApiService apiService;
    private SessionManager sessionManager;

    private static final int PERMISSION_REQUEST_CAMERA = 1001;
    private static final int REQUEST_CODE_GALLERY = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScanQrBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        initScanner();
        startScanAnimation();

        cameraExecutor = Executors.newSingleThreadExecutor();

        binding.btnClose.setOnClickListener(v -> finish());
        binding.btnFlash.setOnClickListener(v -> toggleFlash());
        binding.btnGallery.setOnClickListener(v -> openGallery());

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        }
    }

    private void initScanner() {
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_AZTEC)
                .build();
        scanner = BarcodeScanning.getClient(options);
    }

    private void startScanAnimation() {
        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.95f
        );
        animation.setDuration(2500);
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);
        binding.scanLine.startAnimation(animation);
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e("ScanQrActivity", "Error starting camera", e);
                ToastManager.showToast(this, "Gagal memulai kamera");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) return;

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        try {
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            Log.e("ScanQrActivity", "Use case binding failed", e);
        }
    }

    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (!isAnalysisActive || isProcessing) {
            imageProxy.close();
            return;
        }

        @SuppressWarnings("UnsafeOptInUsageError")
        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        if (!barcodes.isEmpty() && !isProcessing) {
                            processBarcode(barcodes.get(0));
                        }
                    })
                    .addOnFailureListener(e -> Log.e("ScanQrActivity", "QR Analysis failed", e))
                    .addOnCompleteListener(task -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }

    private void processBarcode(Barcode barcode) {
        if (isProcessing) return;
        String rawValue = barcode.getRawValue();
        if (rawValue == null || rawValue.isEmpty()) return;

        isProcessing = true;
        isAnalysisActive = false;

        runOnUiThread(() -> {
            showLoading(true, "Memverifikasi kode QR...");
            verifyQrCode(rawValue);
        });
    }

    /**
     * Kirim kode QR ke server untuk verifikasi & proses pembayaran
     */
    private void verifyQrCode(String qrCode) {
        String token = "Bearer " + sessionManager.getToken();
        apiService.verifyPaymentCode(token, qrCode).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                showLoading(false, null);
                if (response.isSuccessful() && response.body() != null) {
                    onPaymentSuccess(response.body().getData());
                } else {
                    String msg = "QR Code tidak valid atau transaksi tidak ditemukan";
                    try {
                        if (response.errorBody() != null) {
                            ApiResponse<?> err = new Gson().fromJson(response.errorBody().string(), ApiResponse.class);
                            if (err.getMessage() != null) msg = err.getMessage();
                        }
                    } catch (Exception ignored) {}

                    final String finalMsg = msg;
                    runOnUiThread(() -> showErrorAndResume(finalMsg));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                showLoading(false, null);
                runOnUiThread(() -> showErrorAndResume("Gagal terhubung ke server: " + t.getMessage()));
            }
        });
    }

    private void showErrorAndResume(String message) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("QR Tidak Valid ❌")
                .setMessage(message)
                .setPositiveButton("Scan Ulang", (d, w) -> {
                    isProcessing = false;
                    isAnalysisActive = true;
                })
                .setCancelable(false)
                .show();
    }

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

    private void showLoading(boolean show, String message) {
        runOnUiThread(() -> {
            if (binding.loadingOverlay != null) {
                binding.loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
            }
            if (show) binding.scanLine.clearAnimation();
            else if (!isProcessing) startScanAnimation();
        });
    }

    private void toggleFlash() {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            isFlashOn = !isFlashOn;
            camera.getCameraControl().enableTorch(isFlashOn);
            binding.btnFlash.setImageResource(isFlashOn ? R.drawable.ic_flash_on : R.drawable.ic_flash_off);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_GALLERY && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    decodeFromBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                    ToastManager.showToast(this, "Gagal memuat gambar");
                }
            }
        }
    }

    private void decodeFromBitmap(Bitmap bitmap) {
        showLoading(true, "Memproses gambar...");
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    if (!barcodes.isEmpty()) {
                        processBarcode(barcodes.get(0));
                    } else {
                        showLoading(false, null);
                        ToastManager.showToast(this, "Tidak ada QR Code ditemukan pada gambar");
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false, null);
                    ToastManager.showToast(this, "Gagal memproses gambar");
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                ToastManager.showToast(this, "Izin kamera ditolak");
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
