package com.octania.marketplace.ui.seller;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.octania.marketplace.R;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.utils.SessionManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailActivity extends AppCompatActivity {

    private String getBaseStorageUrl() {
        return ApiClient.BASE_URL.replace("api/", "storage/");
    }

    private SessionManager sessionManager;
    private ApiService apiService;
    private int transactionId;
    private boolean isSeller = false;

    // Views
    private TextView tvShopName, tvOrderId, tvStatus, tvOrderDate, tvRejectionReason;
    private TextView tvSubtotal, tvDiscount, tvServiceFee, tvTotal;
    private TextView tvPaymentMethod, tvShippingAddress, tvCourier, tvTrackingNumber;
    private LinearLayout llItems, rowDiscount;
    private MaterialButton btnAction, btnCancel;
    private MaterialCardView cardProof;
    private ImageView ivProofImage;
    private TextView tvProofTitle;
    private LinearLayout llReceiptProofs;
    private MaterialCardView cardReceiptProofs;

    private int selectedTxId = -1;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ActivityResultLauncher<Intent> paymentProofLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);
        transactionId = getIntent().getIntExtra("transaction_id", -1);
        isSeller = getIntent().getBooleanExtra("is_seller", false);

        setupToolbar();
        bindViews();
        setupFilePicker();
        setupPaymentProofPicker();

        if (transactionId == -1) {
            Toast.makeText(this, "ID Pesanan tidak valid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadDetail();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void bindViews() {
        tvShopName = findViewById(R.id.tvShopName);
        tvOrderId = findViewById(R.id.tvOrderId);
        tvStatus = findViewById(R.id.tvStatus);
        tvOrderDate = findViewById(R.id.tvOrderDate);
        tvRejectionReason = findViewById(R.id.tvRejectionReason);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvDiscount = findViewById(R.id.tvDiscount);
        tvServiceFee = findViewById(R.id.tvServiceFee);
        tvTotal = findViewById(R.id.tvTotal);
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod);
        tvShippingAddress = findViewById(R.id.tvShippingAddress);
        tvCourier = findViewById(R.id.tvCourier);
        tvTrackingNumber = findViewById(R.id.tvTrackingNumber);
        llItems = findViewById(R.id.llItems);
        rowDiscount = findViewById(R.id.rowDiscount);
        btnAction = findViewById(R.id.btnAction);
        btnCancel = findViewById(R.id.btnCancel);
        cardProof = findViewById(R.id.cardProof);
        tvProofTitle = findViewById(R.id.tvProofTitle);
        ivProofImage = findViewById(R.id.ivProofImage);
        cardReceiptProofs = findViewById(R.id.cardReceiptProofs);
        llReceiptProofs = findViewById(R.id.llReceiptProofs);
    }

    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        List<Uri> uris = new ArrayList<>();

                        if (data.getClipData() != null) {
                            int count = data.getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                uris.add(data.getClipData().getItemAt(i).getUri());
                            }
                        } else if (data.getData() != null) {
                            uris.add(data.getData());
                        }

                        if (!uris.isEmpty()) {
                            uploadMultimediaProof(selectedTxId, uris);
                        }
                    }
                });
    }

    private void setupPaymentProofPicker() {
        paymentProofLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            uploadProof(transactionId, uri);
                        }
                    }
                });
    }

    private void loadDetail() {
        String token = "Bearer " + sessionManager.getToken();
        android.util.Log.d("OrderDetail", "Loading detail for ID: " + transactionId);

        Call<ApiResponse<Object>> call = isSeller ? apiService.getSellerTransactionDetail(token, transactionId)
                : apiService.getTransactionDetail(token, transactionId);

        call.enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object data = response.body().getData();
                    android.util.Log.d("OrderDetail", "Raw Response Data: " + new Gson().toJson(data));
                    if (data instanceof Map) {
                        try {
                            renderDetail((Map<String, Object>) data);
                        } catch (Exception e) {
                            android.util.Log.e("OrderDetail", "Render Error: " + e.getMessage());
                            Toast.makeText(OrderDetailActivity.this, "Gagal menampilkan detail: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    String error = "Gagal memuat detail pesanan";
                    try {
                        if (response.errorBody() != null) {
                            error += ": " + response.errorBody().string();
                        }
                    } catch (Exception ignored) {
                    }
                    android.util.Log.e("OrderDetail", error);
                    Toast.makeText(OrderDetailActivity.this, error, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                android.util.Log.e("OrderDetail", "Network Error: " + t.getMessage());
                Toast.makeText(OrderDetailActivity.this, "Jaringan error: " + t.getMessage(), Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    private void renderDetail(Map<String, Object> order) {
        // IDs and header
        int txId = (int) Double.parseDouble(String.valueOf(order.get("id")));
        selectedTxId = txId;

        if (isSeller) {
            Map<String, Object> buyer = (Map<String, Object>) order.get("buyer");
            tvShopName.setText(buyer != null ? String.valueOf(buyer.get("name")) : "Pembeli");
        } else {
            Map<String, Object> seller = (Map<String, Object>) order.get("seller");
            tvShopName.setText(seller != null ? String.valueOf(seller.get("name")) : "Toko Penjual");
        }
        tvOrderId.setText("#INV-" + String.format("%05d", txId));

        String status = String.valueOf(order.get("status"));
        tvStatus.setText(statusLabel(status));
        tvStatus.setTextColor(statusColor(status));

        String createdAt = String.valueOf(order.get("created_at"));
        tvOrderDate.setText(createdAt.replace("T", " · ").substring(0, Math.min(createdAt.length(), 19)));

        // Rejection reason
        String rejection = safeString(order.get("seller_notes"), "");
        if ("payment_rejected".equals(status) && !rejection.isEmpty()) {
            tvRejectionReason.setVisibility(View.VISIBLE);
            tvRejectionReason.setText("Alasan Penolakan: " + rejection);
        } else {
            tvRejectionReason.setVisibility(View.GONE);
        }

        // Items
        llItems.removeAllViews();
        List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");
        double subtotal = 0;
        if (items != null) {
            for (Map<String, Object> item : items) {
                int qty = (int) Double.parseDouble(String.valueOf(item.get("quantity")));
                double price = Double.parseDouble(String.valueOf(item.get("price")));
                Map<String, Object> prod = (Map<String, Object>) item.get("product");
                String name = prod != null ? String.valueOf(prod.get("name")) : "Produk";
                subtotal += qty * price;

                View row = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
                ((TextView) row.findViewById(android.R.id.text1)).setText(name);
                ((TextView) row.findViewById(android.R.id.text2)).setText(qty + "x  ·  " + formatRp(price));
                ((TextView) row.findViewById(android.R.id.text2)).setTextColor(0xFF888888);
                llItems.addView(row);
            }
        }

        // Pricing
        double discount = parseDouble(order.get("discount_total"));
        double serviceFee = parseDouble(order.get("service_fee"));
        double total = parseDouble(order.get("total_amount"));

        tvSubtotal.setText(formatRp(subtotal));
        tvServiceFee.setText(formatRp(serviceFee));
        tvTotal.setText(formatRp(total));

        if (discount > 0) {
            rowDiscount.setVisibility(View.VISIBLE);
            tvDiscount.setText("- " + formatRp(discount));
        } else {
            rowDiscount.setVisibility(View.GONE);
        }

        // Payment + shipping info
        tvPaymentMethod.setText(safeString(order.get("payment_method"), "-"));
        tvShippingAddress.setText(safeString(order.get("shipping_address"), "-"));
        tvCourier.setText(safeString(order.get("courier"), "-") + " / " + safeString(order.get("delivery_type"), "-"));
        tvTrackingNumber.setText(safeString(order.get("tracking_number"), "-"));

        // Proof image
        if ("shipped".equals(status) || "received".equals(status) || "completed".equals(status)) {
            String shippingProof = safeString(order.get("shipping_proof"), null);
            if (shippingProof != null && !shippingProof.isEmpty()) {
                cardProof.setVisibility(View.VISIBLE);
                if (tvProofTitle != null)
                    tvProofTitle.setText("Bukti Pengiriman");
                Glide.with(this).load(getBaseStorageUrl() + shippingProof).into(ivProofImage);
                String finalImgUrl = getBaseStorageUrl() + shippingProof;
                ivProofImage.setOnClickListener(v -> showFullScreenImage(finalImgUrl));
            } else {
                cardProof.setVisibility(View.GONE);
            }
        } else {
            String proof = safeString(order.get("payment_proof"), null);
            if (proof != null && !proof.isEmpty()) {
                cardProof.setVisibility(View.VISIBLE);
                if (tvProofTitle != null)
                    tvProofTitle.setText("Bukti Pembayaran");
                Glide.with(this).load(getBaseStorageUrl() + proof).into(ivProofImage);
                String finalImgUrl = getBaseStorageUrl() + proof;
                ivProofImage.setOnClickListener(v -> showFullScreenImage(finalImgUrl));
            } else {
                cardProof.setVisibility(View.GONE);
            }
        }

        // Receipt Proofs (Multimedia from Buyer)
        if ("received".equals(status) || "completed".equals(status)) {
            List<String> receiptPhotos = (List<String>) order.get("receipt_photos");
            if (receiptPhotos != null && !receiptPhotos.isEmpty()) {
                cardReceiptProofs.setVisibility(View.VISIBLE);
                llReceiptProofs.removeAllViews();
                for (String path : receiptPhotos) {
                    ImageView iv = new ImageView(this);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            (int) (120 * getResources().getDisplayMetrics().density),
                            (int) (120 * getResources().getDisplayMetrics().density));
                    lp.setMargins(0, 0, 16, 0);
                    iv.setLayoutParams(lp);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    Glide.with(this).load(getBaseStorageUrl() + path).into(iv);
                    String finalPath = getBaseStorageUrl() + path;
                    iv.setOnClickListener(v -> showFullScreenImage(finalPath));
                    llReceiptProofs.addView(iv);

                    // Click to enlarge (optional)
                    iv.setOnClickListener(v -> {
                        // Open full screen or similar
                    });
                }
            } else {
                cardReceiptProofs.setVisibility(View.GONE);
            }
        } else {
            cardReceiptProofs.setVisibility(View.GONE);
        }

        // Action button
        Map<String, Object> existingReview = (Map<String, Object>) order.get("review");
        setupActionButton(status, txId, existingReview);
    }

    private void setupActionButton(String status, int txId, Map<String, Object> existingReview) {
        switch (status) {
            case "waiting_payment":
                btnAction.setText("Bayar Sekarang");
                btnAction.setEnabled(true);
                btnAction.setAlpha(1f);
                btnAction.setOnClickListener(v -> {
                    Intent intent = new Intent(OrderDetailActivity.this,
                            com.octania.marketplace.ui.payment.PaymentActivity.class);
                    intent.putExtra("transaction_id", txId);
                    startActivity(intent);
                });
                if (!isSeller) {
                    btnCancel.setVisibility(View.VISIBLE);
                    btnCancel.setText("Batalkan");
                    btnCancel.setOnClickListener(v -> cancelOrder(txId));
                } else {
                    btnCancel.setVisibility(View.GONE);
                }
                break;

            case "pending":
                if (!isSeller) {
                    btnAction.setText("Ganti Bukti Bayar");
                    btnAction.setEnabled(true);
                    btnAction.setAlpha(1f);
                    btnAction.setOnClickListener(v -> {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        paymentProofLauncher.launch(Intent.createChooser(intent, "Pilih Bukti Baru"));
                    });
                    btnCancel.setVisibility(View.VISIBLE);
                    btnCancel.setText("Batalkan");
                    btnCancel.setOnClickListener(v -> cancelOrder(txId));
                } else {
                    btnAction.setText("Terima Pembayaran");
                    btnAction.setEnabled(true);
                    btnAction.setAlpha(1f);
                    btnAction.setOnClickListener(v -> updateStatus(txId, "processing", "Pembayaran Diterima"));

                    btnCancel.setVisibility(View.VISIBLE);
                    btnCancel.setText("Tolak");
                    btnCancel.setOnClickListener(v -> showRejectDialog(txId));
                }
                break;

            case "payment_rejected":
                if (!isSeller) {
                    btnAction.setText("Unggah Bukti Baru");
                    btnAction.setEnabled(true);
                    btnAction.setAlpha(1f);
                    btnAction.setOnClickListener(v -> {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        paymentProofLauncher.launch(Intent.createChooser(intent, "Pilih Bukti Pembayaran"));
                    });
                    btnCancel.setVisibility(View.VISIBLE);
                    btnCancel.setText("Batalkan");
                    btnCancel.setOnClickListener(v -> cancelOrder(txId));
                } else {
                    btnAction.setText("Menunggu Pembeli");
                    btnAction.setEnabled(false);
                    btnAction.setAlpha(0.6f);
                    btnCancel.setVisibility(View.GONE);
                }
                break;

            case "processing":
            case "packed":
                if (isSeller) {
                    btnAction.setText("Kirim Pesanan");
                    btnAction.setEnabled(true);
                    btnAction.setAlpha(1f);
                    btnAction.setOnClickListener(v -> {
                        // Logic for shipping already exists in shipOrder (usually separate screen or
                        // dialog)
                        // For now let's just use statusLabel or manual Trigger
                        Toast.makeText(this, "Silakan masukkan nomor resi", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    btnAction.setText("Pesanan Diproses");
                    btnAction.setEnabled(false);
                    btnAction.setAlpha(0.6f);
                }
                btnCancel.setVisibility(View.GONE);
                break;

            case "shipped":
                btnCancel.setVisibility(View.GONE);
                if (!isSeller) {
                    btnAction.setText("Konfirmasi Pesanan Diterima");
                    btnAction.setEnabled(true);
                    btnAction.setAlpha(1f);
                    btnAction.setOnClickListener(v -> {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("*/*");
                        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] { "image/*", "video/*" });
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                        filePickerLauncher.launch(Intent.createChooser(intent, "Pilih Foto/Video Bukti Penerimaan"));
                    });
                } else {
                    btnAction.setText("Pesanan Dikirim");
                    btnAction.setEnabled(false);
                    btnAction.setAlpha(0.6f);
                }
                break;

            case "received":
            case "completed":
                btnCancel.setVisibility(View.GONE);
                if (!isSeller) {
                    if (existingReview != null) {
                        btnAction.setText("Lihat Penilaian");
                        btnAction.setEnabled(true);
                        btnAction.setAlpha(1f);
                        btnAction.setOnClickListener(v -> showReviewDialog(existingReview));
                    } else {
                        btnAction.setText("Beri Penilaian");
                        btnAction.setEnabled(true);
                        btnAction.setAlpha(1f);
                        btnAction.setOnClickListener(v -> showReviewDialog(null));
                    }
                } else {
                    btnAction.setText("Pesanan Selesai");
                    btnAction.setEnabled(false);
                    btnAction.setAlpha(0.6f);
                }
                break;

            case "cancelled":
                btnCancel.setVisibility(View.GONE);
                btnAction.setText("Pesanan Dibatalkan");
                btnAction.setEnabled(false);
                btnAction.setAlpha(0.5f);
                break;

            default:
                btnCancel.setVisibility(View.GONE);
                btnAction.setText(statusLabel(status));
                btnAction.setEnabled(false);
                btnAction.setAlpha(0.6f);
                break;
        }
    }

    private void uploadProof(int txId, Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) != -1)
                bos.write(buf, 0, len);
            byte[] bytes = bos.toByteArray();

            String fileName = "bukti_" + System.currentTimeMillis() + ".jpg";
            Cursor c = getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx != -1)
                    fileName = c.getString(idx);
                c.close();
            }

            RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), bytes);
            MultipartBody.Part body = MultipartBody.Part.createFormData("proof_of_payment", fileName, reqFile);

            String token = "Bearer " + sessionManager.getToken();
            btnAction.setEnabled(false);
            btnAction.setText("Mengunggah...");

            apiService.uploadProof(token, txId, body).enqueue(new Callback<ApiResponse<Object>>() {
                @Override
                public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(OrderDetailActivity.this, "Bukti pembayaran berhasil diunggah!",
                                Toast.LENGTH_SHORT).show();
                        loadDetail(); // Reload to reflect new status
                    } else {
                        btnAction.setEnabled(true);
                        String msg = "Gagal mengunggah bukti bayar";
                        try {
                            msg = response.errorBody().string();
                        } catch (Exception ignored) {
                        }
                        Toast.makeText(OrderDetailActivity.this, msg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                    btnAction.setEnabled(true);
                    Toast.makeText(OrderDetailActivity.this, "Jaringan error: " + t.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error membaca file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadMultimediaProof(int txId, List<Uri> uris) {
        String token = "Bearer " + sessionManager.getToken();
        btnAction.setEnabled(false);
        btnAction.setText("Mengunggah Bukti...");

        List<MultipartBody.Part> parts = new ArrayList<>();

        boolean hasLargeFile = false;
        for (Uri uri : uris) {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int len;
                while ((len = is.read(buf)) != -1)
                    bos.write(buf, 0, len);
                byte[] bytes = bos.toByteArray();

                if (bytes.length > 2 * 1024 * 1024) {
                    hasLargeFile = true;
                }

                String mimeType = getContentResolver().getType(uri);
                if (mimeType == null)
                    mimeType = "image/jpeg";

                String fileName = "proof_" + System.currentTimeMillis() + "_" + uris.indexOf(uri);

                // Attempt to get real filename
                Cursor c = getContentResolver().query(uri, null, null, null, null);
                if (c != null && c.moveToFirst()) {
                    int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx != -1)
                        fileName = c.getString(idx);
                    c.close();
                }

                // Ensure it has an extension matching the MIME type if missing
                if (!fileName.contains(".")) {
                    String ext = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                    if (ext != null)
                        fileName += "." + ext;
                    else if (mimeType.contains("video"))
                        fileName += ".mp4";
                    else
                        fileName += ".jpg";
                }

                RequestBody reqFile = RequestBody.create(MediaType.parse(mimeType), bytes);
                parts.add(MultipartBody.Part.createFormData("files[]", fileName, reqFile));
            } catch (Exception e) {
                Log.e("OrderDetail", "Error reading file: " + e.getMessage());
            }
        }

        if (hasLargeFile) {
            Toast.makeText(this,
                    "file terlalu besar (> 2MB). Silakan unggah file yang lebih kecil",
                    Toast.LENGTH_LONG).show();
        }

        if (parts.isEmpty()) {
            btnAction.setEnabled(true);
            btnAction.setText("Konfirmasi Pesanan Diterima");
            Toast.makeText(this, "Tidak ada file valid yang dipilih", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.confirmReceived(token, txId, parts).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(OrderDetailActivity.this, "Pesanan diterima! Dana segera dilepaskan.",
                            Toast.LENGTH_LONG).show();
                    loadDetail();
                } else {
                    btnAction.setEnabled(true);
                    btnAction.setText("Konfirmasi Pesanan Diterima");
                    String errorMsg = "Gagal mengunggah bukti";
                    try {
                        if (response.errorBody() != null) {
                            String errJson = response.errorBody().string();
                            ApiResponse<?> errResponse = new Gson().fromJson(errJson, ApiResponse.class);
                            if (errResponse != null && errResponse.getMessage() != null) {
                                errorMsg = errResponse.getMessage();
                            } else {
                                errorMsg += ": " + errJson;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                    Toast.makeText(OrderDetailActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                btnAction.setEnabled(true);
                btnAction.setText("Konfirmasi Pesanan Diterima");
                Toast.makeText(OrderDetailActivity.this, "Koneksi Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showReviewDialog(Map<String, Object> existingReview) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_review_product, null);
        dialog.setContentView(view);

        android.widget.RatingBar rb = view.findViewById(R.id.ratingBar);
        com.google.android.material.textfield.TextInputEditText etComment = view.findViewById(R.id.etComment);
        MaterialButton btnSubmit = view.findViewById(R.id.btnSubmitReview);

        if (existingReview != null) {
            // Read-only mode
            int rating = (int) parseDouble(existingReview.get("rating"));
            String comment = safeString(existingReview.get("comment"), "");

            rb.setRating(rating);
            rb.setIsIndicator(true); // Read-only for RatingBar
            etComment.setText(comment);
            etComment.setEnabled(false);

            btnSubmit.setVisibility(View.GONE); // Hide submit button
        } else {
            btnSubmit.setOnClickListener(v -> {
                int rating = (int) rb.getRating();
                String comment = etComment.getText() != null ? etComment.getText().toString().trim() : "";

                if (rating == 0) {
                    Toast.makeText(this, "Silakan pilih bintang", Toast.LENGTH_SHORT).show();
                    return;
                }

                submitReview(rating, comment, dialog);
            });
        }

        dialog.show();
    }

    private void showRejectDialog(int txId) {
        android.widget.EditText etReason = new android.widget.EditText(this);
        etReason.setHint("Masukkan alasan penolakan...");
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Tolak Pembayaran")
                .setView(etReason)
                .setPositiveButton("Tolak", (dialog, which) -> {
                    String reason = etReason.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(this, "Alasan harus diisi", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateStatus(txId, "payment_rejected", reason);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void updateStatus(int txId, String status, String note) {
        String token = "Bearer " + sessionManager.getToken();
        btnAction.setEnabled(false);
        btnAction.setText("Memproses...");

        apiService.updateOrderStatus(token, txId, status, note).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(OrderDetailActivity.this, "Status diperbarui", Toast.LENGTH_SHORT).show();
                    loadDetail();
                } else {
                    btnAction.setEnabled(true);
                    Toast.makeText(OrderDetailActivity.this, "Gagal memperbarui status", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                btnAction.setEnabled(true);
                Toast.makeText(OrderDetailActivity.this, "Koneksi Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancelOrder(int txId) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Batalkan Pesanan")
                .setMessage("Apakah Anda yakin ingin membatalkan pesanan ini?")
                .setPositiveButton("Ya, Batalkan", (dialog, which) -> {
                    String token = "Bearer " + sessionManager.getToken();
                    btnCancel.setEnabled(false);
                    btnCancel.setText("Membatalkan...");

                    apiService.cancelOrder(token, txId).enqueue(new Callback<ApiResponse<Object>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(OrderDetailActivity.this, "Pesanan berhasil dibatalkan",
                                        Toast.LENGTH_SHORT).show();
                                loadDetail();
                            } else {
                                btnCancel.setEnabled(true);
                                btnCancel.setText("Batalkan");
                                String msg = "Gagal membatalkan pesanan";
                                try {
                                    msg = response.errorBody().string();
                                } catch (Exception ignored) {
                                }
                                Toast.makeText(OrderDetailActivity.this, msg, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                            btnCancel.setEnabled(true);
                            btnCancel.setText("Batalkan");
                            Toast.makeText(OrderDetailActivity.this, "Jaringan error: " + t.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Tidak", null)
                .show();
    }

    private void submitReview(int rating, String comment, BottomSheetDialog dialog) {
        String token = "Bearer " + sessionManager.getToken();
        apiService.submitReview(token, transactionId, rating, comment).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(OrderDetailActivity.this, "Terima kasih atas penilaian Anda! ⭐", Toast.LENGTH_SHORT)
                            .show();
                    dialog.dismiss();
                    loadDetail();
                } else {
                    Toast.makeText(OrderDetailActivity.this, "Gagal mengirim penilaian", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                Toast.makeText(OrderDetailActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ===== Helpers =====

    private String formatRp(double amount) {
        return "Rp " + NumberFormat.getNumberInstance(new Locale("id", "ID")).format((long) amount);
    }

    private double parseDouble(Object val) {
        if (val == null)
            return 0;
        try {
            return Double.parseDouble(String.valueOf(val));
        } catch (Exception e) {
            return 0;
        }
    }

    private String safeString(Object val, String fallback) {
        if (val == null || val.toString().equals("null"))
            return fallback != null ? fallback : "";
        return val.toString();
    }

    private String statusLabel(String status) {
        switch (status) {
            case "waiting_payment":
                return "Belum Bayar";
            case "pending":
                return "Verifikasi Pembayaran";
            case "processing":
                return "Diproses";
            case "packed":
                return "Dikemas";
            case "shipped":
                return "Dikirim";
            case "received":
                return "Diterima";
            case "completed":
                return "Selesai";
            case "cancelled":
                return "Dibatalkan";
            case "payment_rejected":
                return "Pembayaran Ditolak";
            default:
                return status;
        }
    }

    private int statusColor(String status) {
        switch (status) {
            case "waiting_payment":
            case "pending":
                return 0xFFE65100;
            case "processing":
            case "packed":
                return 0xFF1565C0;
            case "shipped":
                return 0xFF1976D2;
            case "received":
            case "completed":
                return 0xFF2E7D32;
            case "cancelled":
            case "payment_rejected":
                return 0xFFC62828;
            default:
                return 0xFF888888;
        }
    }

    private void showFullScreenImage(String imageUrl) {
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_full_screen_image);

        ImageView iv = dialog.findViewById(R.id.ivFullScreenImage);
        Glide.with(this).load(imageUrl).into(iv);

        View btnClose = dialog.findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
        dialog.show();
    }
}
