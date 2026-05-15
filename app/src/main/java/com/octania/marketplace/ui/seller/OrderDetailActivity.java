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
import com.octania.marketplace.utils.ToastManager;

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
    private int currentSellerId = -1;
    private String currentSellerName = "";
    private String currentSellerAvatar = "";

    // Views
    private TextView tvOrderId, tvStatus, tvOrderDate, tvRejectionReason;
    private TextView tvSubtotal, tvDiscount, tvServiceFee, tvTotal;
    private TextView tvShippingAddress, tvTrackingNumber, tvRecipientName, tvRecipientPhone;
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
    private ActivityResultLauncher<Intent> shipProofPickerLauncher;
    private Uri selectedShipProofUri = null;
    private android.widget.ImageView dialogShipProofPreview = null;

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
        setupShipProofPicker();

        if (transactionId == -1) {
            ToastManager.showToast(this, "ID Pesanan tidak valid");
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
        tvOrderId = findViewById(R.id.tvOrderId);
        tvStatus = findViewById(R.id.tvStatus);
        tvOrderDate = findViewById(R.id.tvOrderDate);
        tvRejectionReason = findViewById(R.id.tvRejectionReason);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvDiscount = findViewById(R.id.tvDiscount);
        tvServiceFee = findViewById(R.id.tvServiceFee);
        tvTotal = findViewById(R.id.tvTotal);
        tvShippingAddress = findViewById(R.id.tvShippingAddress);
        tvTrackingNumber = findViewById(R.id.tvTrackingNumber);
        tvRecipientName = findViewById(R.id.tvRecipientName);
        tvRecipientPhone = findViewById(R.id.tvRecipientPhone);
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

    private void setupShipProofPicker() {
        shipProofPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedShipProofUri = result.getData().getData();
                        if (dialogShipProofPreview != null && selectedShipProofUri != null) {
                            dialogShipProofPreview.setImageURI(selectedShipProofUri);
                            dialogShipProofPreview.setVisibility(android.view.View.VISIBLE);
                        }
                    }
                });
    }

    private void showShipDialog(int txId) {
        selectedShipProofUri = null;
        dialogShipProofPreview = null;

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 8);

        android.widget.EditText etCourier = new android.widget.EditText(this);
        etCourier.setHint("Nama Kurir (JNE, JNT, dll)");
        layout.addView(etCourier);

        android.widget.EditText etTracking = new android.widget.EditText(this);
        etTracking.setHint("Nomor Resi");
        layout.addView(etTracking);

        com.google.android.material.button.MaterialButton btnPickImage =
                new com.google.android.material.button.MaterialButton(this);
        btnPickImage.setText("Pilih Foto Bukti Kirim");
        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            shipProofPickerLauncher.launch(intent);
        });
        layout.addView(btnPickImage);

        dialogShipProofPreview = new android.widget.ImageView(this);
        dialogShipProofPreview.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 400));
        dialogShipProofPreview.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        dialogShipProofPreview.setVisibility(android.view.View.GONE);
        layout.addView(dialogShipProofPreview);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Kirim Pesanan")
                .setView(layout)
                .setPositiveButton("Kirim", (d, w) -> {
                    String courier = etCourier.getText().toString().trim();
                    String tracking = etTracking.getText().toString().trim();
                    if (courier.isEmpty() || tracking.isEmpty()) {
                        ToastManager.showToast(this, "Isi nama kurir dan nomor resi");
                        return;
                    }
                    if (selectedShipProofUri == null) {
                        ToastManager.showToast(this, "Wajib upload foto bukti pengiriman!");
                        return;
                    }
                    shipOrder(txId, courier, tracking, selectedShipProofUri);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void shipOrder(int txId, String courier, String tracking, Uri proofUri) {
        String token = "Bearer " + sessionManager.getToken();
        btnAction.setEnabled(false);
        btnAction.setText("Mengirim...");
        try {
            java.io.InputStream is = getContentResolver().openInputStream(proofUri);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) != -1) bos.write(buf, 0, len);
            byte[] imageBytes = bos.toByteArray();
            is.close();

            okhttp3.RequestBody reqFile = okhttp3.RequestBody.create(
                    okhttp3.MediaType.parse("image/*"), imageBytes);
            okhttp3.MultipartBody.Part imagePart = okhttp3.MultipartBody.Part
                    .createFormData("shipping_proof", "proof.jpg", reqFile);
            okhttp3.RequestBody courierReq = okhttp3.RequestBody.create(
                    okhttp3.MediaType.parse("text/plain"), courier);
            okhttp3.RequestBody trackingReq = okhttp3.RequestBody.create(
                    okhttp3.MediaType.parse("text/plain"), tracking);

            apiService.shipOrder(token, txId, courierReq, trackingReq, imagePart)
                    .enqueue(new Callback<ApiResponse<Object>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Object>> call,
                                Response<ApiResponse<Object>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                ToastManager.showToast(OrderDetailActivity.this,
                                        response.body().getMessage() != null
                                                ? response.body().getMessage()
                                                : "Pesanan berhasil dikirim!");
                                loadDetail();
                            } else {
                                btnAction.setEnabled(true);
                                btnAction.setText("Kirim Pesanan");
                                ToastManager.showToast(OrderDetailActivity.this,
                                        "Gagal mengirim pesanan, coba lagi.");
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                            btnAction.setEnabled(true);
                            btnAction.setText("Kirim Pesanan");
                            ToastManager.showToast(OrderDetailActivity.this,
                                    "Koneksi Error: " + t.getMessage());
                        }
                    });
        } catch (Exception e) {
            btnAction.setEnabled(true);
            btnAction.setText("Kirim Pesanan");
            ToastManager.showToast(this, "Gagal memproses gambar: " + e.getMessage());
        }
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
                            ToastManager.showToast(OrderDetailActivity.this, "Gagal menampilkan detail: " + e.getMessage());
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
                    ToastManager.showLongToast(OrderDetailActivity.this, error);
                }
            }

            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                android.util.Log.e("OrderDetail", "Network Error: " + t.getMessage());
                ToastManager.showToast(OrderDetailActivity.this, "Jaringan error: " + t.getMessage());
                finish();
            }
        });
    }

    private void renderDetail(Map<String, Object> order) {
        // IDs and header
        int txId = (int) Double.parseDouble(String.valueOf(order.get("id")));
        selectedTxId = txId;

        // Removed shop name/buyer name display from header as requested
        // Store seller info for chat navigation
        if (!isSeller) {
            // Try nested seller object first
            Map<String, Object> sellerMap = (Map<String, Object>) order.get("seller");
            if (sellerMap != null && sellerMap.get("id") != null) {
                try {
                    currentSellerId = (int) Double.parseDouble(String.valueOf(sellerMap.get("id")));
                    currentSellerName = safeString(sellerMap.get("name"), "Penjual");
                    currentSellerAvatar = safeString(sellerMap.get("avatar"), "");
                } catch (Exception ignored) {}
            }
            // Fallback to top-level seller_id field
            if (currentSellerId == -1 && order.get("seller_id") != null) {
                try {
                    currentSellerId = (int) Double.parseDouble(String.valueOf(order.get("seller_id")));
                } catch (Exception ignored) {}
            }
            android.util.Log.d("Dispute", "currentSellerId = " + currentSellerId + ", name = " + currentSellerName);
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

        // Shipping Cost UI removed from layout as requested.
        // Payment + shipping info
        Map<String, Object> buyer = (Map<String, Object>) order.get("buyer");
        if (buyer != null) {
            tvRecipientName.setText(String.valueOf(buyer.get("name")));
            tvRecipientPhone.setText(String.valueOf(buyer.get("phone")));
        } else {
            tvRecipientName.setText(safeString(order.get("buyer_name"), "-"));
            tvRecipientPhone.setText(safeString(order.get("buyer_phone"), "-"));
        }

        tvShippingAddress.setText(safeString(order.get("shipping_address"), "-"));
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
            // Payment proof can be: plain string, JSON array string, or List
            String proof = null;
            Object proofObj = order.get("payment_proof");
            android.util.Log.d("ProofDebug", "payment_proof raw type: " + (proofObj != null ? proofObj.getClass().getName() : "null"));
            android.util.Log.d("ProofDebug", "payment_proof raw value: " + proofObj);
            
            if (proofObj instanceof java.util.List) {
                java.util.List<?> proofList = (java.util.List<?>) proofObj;
                if (!proofList.isEmpty()) {
                    proof = String.valueOf(proofList.get(0));
                }
            } else if (proofObj instanceof String) {
                String proofStr = (String) proofObj;
                if (!proofStr.isEmpty() && !proofStr.equals("null")) {
                    // Check if it's a JSON array string like '["path1","path2"]'
                    if (proofStr.startsWith("[")) {
                        try {
                            org.json.JSONArray arr = new org.json.JSONArray(proofStr);
                            if (arr.length() > 0) {
                                proof = arr.getString(0);
                            }
                        } catch (org.json.JSONException e) {
                            proof = proofStr; // fallback to raw string
                        }
                    } else {
                        proof = proofStr;
                    }
                }
            }
            
            android.util.Log.d("ProofDebug", "proof resolved: " + proof);
            
            // Hide proof for MeyPay automation
            String methodCode = safeString(order.get("payment_method_code"), "");
            if (methodCode.contains("meypay")) {
                cardProof.setVisibility(View.GONE);
            } else if (proof != null && !proof.isEmpty()) {
                cardProof.setVisibility(View.VISIBLE);
                if (tvProofTitle != null)
                    tvProofTitle.setText("Bukti Pembayaran");
                String finalImgUrl = getBaseStorageUrl() + proof;
                android.util.Log.d("ProofDebug", "Loading URL: " + finalImgUrl);
                
                Glide.with(this)
                    .load(finalImgUrl)
                    .into(ivProofImage);
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
                    btnAction.setText("Menunggu Verifikasi Admin");
                    btnAction.setEnabled(false);
                    btnAction.setAlpha(0.6f);
                    btnCancel.setVisibility(View.GONE);
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
                    btnAction.setOnClickListener(v -> showShipDialog(txId));
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
                    // Tombol Laporkan Masalah
                    btnCancel.setVisibility(View.VISIBLE);
                    btnCancel.setText("⚠ Laporkan Masalah");
                    btnCancel.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFFE53935));
                    btnCancel.setTextColor(0xFFFFFFFF);
                    btnCancel.setOnClickListener(v -> showReportDisputeDialog(txId));
                } else {
                    btnAction.setText("Pesanan Dikirim");
                    btnAction.setEnabled(false);
                    btnAction.setAlpha(0.6f);
                }
                break;

            case "disputed":
                btnCancel.setVisibility(View.GONE);
                btnAction.setText("⚖ Sedang Dalam Proses Dispute");
                btnAction.setEnabled(false);
                btnAction.setAlpha(0.7f);
                btnAction.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFFFF6F00));
                break;

            case "refunded":
                btnCancel.setVisibility(View.GONE);
                btnAction.setText("✅ Refund Berhasil");
                btnAction.setEnabled(false);
                btnAction.setAlpha(0.8f);
                btnAction.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF43A047));
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

    private void showReportDisputeDialog(int txId) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 8);

        android.widget.TextView tvLabel = new android.widget.TextView(this);
        tvLabel.setText("Alasan Laporan:");
        tvLabel.setPadding(0, 0, 0, 8);
        layout.addView(tvLabel);

        String[] reasons = {"Barang tidak sesuai deskripsi", "Barang rusak/cacat", "Barang tidak sampai", "Barang palsu", "Lainnya"};
        android.widget.Spinner spinnerReason = new android.widget.Spinner(this);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, reasons);
        spinnerReason.setAdapter(adapter);
        layout.addView(spinnerReason);

        android.widget.EditText etDesc = new android.widget.EditText(this);
        etDesc.setHint("Deskripsikan masalah Anda...");
        etDesc.setMinLines(3);
        etDesc.setPadding(0, 16, 0, 0);
        layout.addView(etDesc);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("⚠ Laporkan Masalah")
                .setMessage("Laporan akan dikirimkan ke penjual dan dipantau oleh admin.\nAnda bisa melanjutkan diskusi di room chat.")
                .setView(layout)
                .setPositiveButton("Kirim Laporan", (d, w) -> {
                    String reason = reasons[spinnerReason.getSelectedItemPosition()];
                    String desc   = etDesc.getText().toString().trim();
                    submitDispute(txId, reason, desc);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void submitDispute(int txId, String reason, String description) {
        String token = "Bearer " + sessionManager.getToken();
        btnCancel.setEnabled(false);
        btnCancel.setText("Mengirim...");

        apiService.openDispute(token, txId, reason, description)
                .enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Object>> call,
                                           Response<ApiResponse<Object>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && "success".equals(response.body().getStatus())) {
                            ToastManager.showToast(OrderDetailActivity.this,
                                    "Laporan berhasil dikirim! Admin akan meninjau kasus ini.");
                            // Reload detail first, then open chat after detail loaded
                            loadDetailThenChat();
                        } else {
                            btnCancel.setEnabled(true);
                            btnCancel.setText("⚠ Laporkan Masalah");
                            String msg = "Gagal mengirim laporan";
                            try {
                                if (response.errorBody() != null)
                                    msg = response.errorBody().string();
                            } catch (Exception ignored) {}
                            ToastManager.showToast(OrderDetailActivity.this, msg);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                        btnCancel.setEnabled(true);
                        btnCancel.setText("⚠ Laporkan Masalah");
                        ToastManager.showToast(OrderDetailActivity.this, "Jaringan error: " + t.getMessage());
                    }
                });
    }

    private void loadDetailThenChat() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getTransactionDetail(token, transactionId)
                .enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Object>> call,
                                           Response<ApiResponse<Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Object data = response.body().getData();
                            if (data instanceof java.util.Map) {
                                try {
                                    renderDetail((java.util.Map<String, Object>) data);
                                } catch (Exception ignored) {}
                            }
                        }
                        // Open chat regardless — seller info should now be set
                        openChatWithSeller();
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                        // Still try to open chat with cached seller info
                        openChatWithSeller();
                    }
                });
    }

    private void openChatWithSeller() {
        if (currentSellerId == -1) {
            // Fallback: buka daftar percakapan jika seller_id tidak tersedia
            startActivity(new Intent(this, com.octania.marketplace.ui.chat.ConversationsActivity.class));
            return;
        }
        // Langsung buka ChatActivity dengan seller terkait
        Intent chatIntent = new Intent(this, com.octania.marketplace.ui.chat.ChatActivity.class);
        chatIntent.putExtra("user_id", currentSellerId);
        chatIntent.putExtra("user_name", currentSellerName.isEmpty() ? "Penjual" : currentSellerName);
        chatIntent.putExtra("user_avatar", currentSellerAvatar);
        startActivity(chatIntent);
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
            btnAction.setEnabled(true);
            btnAction.setText("Konfirmasi Pesanan Diterima");
            Toast.makeText(this,
                    "Salah satu file terlalu besar (> 2MB). Pilih file yang lebih kecil.",
                    Toast.LENGTH_LONG).show();
            return;
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
            case "waiting_payment":    return "Belum Bayar";
            case "pending":            return "Verifikasi Pembayaran";
            case "paid_verified":      return "Pembayaran Terverifikasi";
            case "processing":         return "Diproses";
            case "packed":             return "Dikemas";
            case "ready_to_ship":      return "Siap Dikirim";
            case "shipped":            return "Dikirim";
            case "received":           return "Diterima";
            case "completed":          return "Selesai";
            case "cancelled":          return "Dibatalkan";
            case "payment_rejected":   return "Pembayaran Ditolak";
            default:                   return status;
        }
    }

    private int statusColor(String status) {
        switch (status) {
            case "waiting_payment":
            case "pending":
                return 0xFFE65100;
            case "paid_verified":
            case "processing":
            case "packed":
            case "ready_to_ship":
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
