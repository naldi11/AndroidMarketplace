package com.octania.marketplace.ui.dispute;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.content.Intent;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.database.Cursor;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.octania.marketplace.R;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.model.Dispute;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.bumptech.glide.Glide;

import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DisputeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TRANSACTION_ID = "transaction_id";

    private ApiService apiService;
    private String token;
    private int transactionId;
    private Dispute currentDispute;

    private ProgressBar progressBar;
    private MaterialCardView cardShipBack, cardWaitingSeller, cardRefunded, cardRefundTransferred;
    private TextView tvDisputeId, tvStatus, tvStatusDesc, tvAdminNotes;
    private TextView tvReason, tvDescription, tvTrackingInfo;
    private TextInputEditText etReturnCourier, etReturnTracking;
    private MaterialButton btnShipBack, btnSellerConfirmReturn, btnConfirmRefundReceived;
    private android.widget.ImageView ivAdminRefundProof;

    private android.widget.ImageView ivReturnProofPreview;
    private MaterialButton btnSelectReturnProof;
    private Uri selectedReturnProofUri = null;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedReturnProofUri = result.getData().getData();
                    if (selectedReturnProofUri != null) {
                        ivReturnProofPreview.setImageURI(selectedReturnProofUri);
                        ivReturnProofPreview.setVisibility(View.VISIBLE);
                    }
                }
            });

    private void openReturnImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "Pilih Foto Bukti Kirim Balik"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispute_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        transactionId = getIntent().getIntExtra(EXTRA_TRANSACTION_ID, -1);
        token = "Bearer " + new com.octania.marketplace.utils.SessionManager(this).getToken();
        apiService = ApiClient.getClient().create(ApiService.class);

        progressBar       = findViewById(R.id.progressBar);
        cardShipBack      = findViewById(R.id.cardShipBack);
        cardWaitingSeller = findViewById(R.id.cardWaitingSeller);
        cardRefunded      = findViewById(R.id.cardRefunded);
        cardRefundTransferred = findViewById(R.id.cardRefundTransferred);
        tvDisputeId       = findViewById(R.id.tvDisputeId);
        tvStatus          = findViewById(R.id.tvStatus);
        tvStatusDesc      = findViewById(R.id.tvStatusDesc);
        tvAdminNotes      = findViewById(R.id.tvAdminNotes);
        tvReason          = findViewById(R.id.tvReason);
        tvDescription     = findViewById(R.id.tvDescription);
        tvTrackingInfo    = findViewById(R.id.tvTrackingInfo);
        etReturnCourier   = findViewById(R.id.etReturnCourier);
        etReturnTracking  = findViewById(R.id.etReturnTracking);
        btnShipBack       = findViewById(R.id.btnShipBack);
        btnSellerConfirmReturn = findViewById(R.id.btnSellerConfirmReturn);
        btnConfirmRefundReceived = findViewById(R.id.btnConfirmRefundReceived);
        ivAdminRefundProof    = findViewById(R.id.ivAdminRefundProof);

        ivReturnProofPreview = findViewById(R.id.ivReturnProofPreview);
        btnSelectReturnProof = findViewById(R.id.btnSelectReturnProof);

        btnSelectReturnProof.setOnClickListener(v -> openReturnImagePicker());
        btnShipBack.setOnClickListener(v -> submitShipBack());
        btnSellerConfirmReturn.setOnClickListener(v -> submitSellerConfirmReturn());
        btnConfirmRefundReceived.setOnClickListener(v -> submitConfirmRefundReceived());

        if (transactionId == -1) {
            Toast.makeText(this, "ID transaksi tidak valid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        loadDispute();
    }

    private void loadDispute() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getDispute(token, transactionId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Object>> call,
                                   @NonNull Response<ApiResponse<Object>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.code() == 404) {
                    // Belum ada laporan masalah untuk transaksi ini
                    showNoDisputeState();
                    return;
                }

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    Gson gson = new Gson();
                    Type type = new TypeToken<Dispute>(){}.getType();
                    currentDispute = gson.fromJson(gson.toJson(response.body().getData()), type);
                    renderDispute(currentDispute);
                } else {
                    Toast.makeText(DisputeDetailActivity.this,
                        "Gagal memuat data laporan (kode: " + response.code() + ")",
                        Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Object>> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(DisputeDetailActivity.this, "Koneksi gagal: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void showNoDisputeState() {
        if (tvDisputeId != null) tvDisputeId.setText("Pesanan #" + transactionId);
        if (tvStatus != null) {
            tvStatus.setText("Belum Ada Laporan Masalah");
            tvStatus.setTextColor(0xFF64748B);
        }
        if (tvStatusDesc != null) {
            tvStatusDesc.setText("Pesanan ini belum memiliki laporan masalah.\nTap tombol di bawah untuk mengajukan laporan ke admin.");
        }
        if (cardShipBack != null)      cardShipBack.setVisibility(android.view.View.GONE);
        if (cardWaitingSeller != null)  cardWaitingSeller.setVisibility(android.view.View.GONE);
        if (cardRefunded != null)       cardRefunded.setVisibility(android.view.View.GONE);

        // Tampilkan form ajukan laporan baru
        showOpenDisputeForm();
    }

    private void showOpenDisputeForm() {
        // Buat dialog input alasan laporan
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Ajukan Laporan Masalah");
        builder.setMessage("Jelaskan masalah dengan pesanan #" + transactionId);

        final android.widget.EditText etReason = new android.widget.EditText(this);
        etReason.setHint("Contoh: Barang rusak, tidak sesuai, dll.");
        etReason.setPadding(48, 24, 48, 8);
        builder.setView(etReason);

        builder.setPositiveButton("Ajukan", (dialog, which) -> {
            String reason = etReason.getText().toString().trim();
            if (reason.isEmpty()) {
                Toast.makeText(this, "Alasan tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }
            submitOpenDispute(reason);
        });

        builder.setNegativeButton("Batal", (dialog, which) -> finish());
        builder.setCancelable(false);
        builder.show();
    }

    private void submitOpenDispute(String reason) {
        progressBar.setVisibility(android.view.View.VISIBLE);

        apiService.openDispute(token, transactionId, reason, "").enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Object>> call,
                                   @NonNull Response<ApiResponse<Object>> response) {
                progressBar.setVisibility(android.view.View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(DisputeDetailActivity.this,
                        "Laporan berhasil diajukan! Admin akan segera meninjau.",
                        Toast.LENGTH_LONG).show();
                    // Reload untuk tampilkan status dispute yang baru dibuat
                    loadDispute();
                } else {
                    String msg = "Gagal mengajukan laporan.";
                    try {
                        if (response.errorBody() != null) {
                            msg = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    Toast.makeText(DisputeDetailActivity.this, msg, Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Object>> call, @NonNull Throwable t) {
                progressBar.setVisibility(android.view.View.GONE);
                Toast.makeText(DisputeDetailActivity.this, "Koneksi gagal", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void renderDispute(Dispute d) {
        tvDisputeId.setText("Laporan #D" + d.getId());
        tvReason.setText(d.getReason() != null ? d.getReason() : "-");
        tvDescription.setText(d.getDescription() != null ? d.getDescription() : "");

        if (d.getAdminNotes() != null && !d.getAdminNotes().isEmpty()) {
            tvAdminNotes.setVisibility(View.VISIBLE);
            tvAdminNotes.setText("📋 Catatan Admin: " + d.getAdminNotes());
        }

        cardShipBack.setVisibility(View.GONE);
        cardWaitingSeller.setVisibility(View.GONE);
        cardRefunded.setVisibility(View.GONE);
        cardRefundTransferred.setVisibility(View.GONE);

        String role = new com.octania.marketplace.utils.SessionManager(this).getActiveRole();
        String status = d.getStatus() != null ? d.getStatus() : "";
        switch (status) {
            case "open":
                tvStatus.setText("⚖️ Menunggu Tinjauan Admin");
                tvStatus.setTextColor(0xFFF97316);
                tvStatusDesc.setText("Laporan sedang ditinjau oleh admin.");
                break;
            case "admin_reviewing":
                tvStatus.setText("🔍 Sedang Ditinjau");
                tvStatus.setTextColor(0xFF6366F1);
                tvStatusDesc.setText("Admin sedang meninjau laporan.");
                break;
            case "buyer_won":
                if ("seller".equals(role)) {
                    tvStatus.setText("❌ Pembeli Menang");
                    tvStatus.setTextColor(0xFFEF4444);
                    tvStatusDesc.setText("Admin memutuskan pembeli berhak mendapat refund. Harap menunggu pembeli mengirim barang kembali beserta resinya.");
                    cardShipBack.setVisibility(View.GONE);
                } else {
                    tvStatus.setText("✅ Kamu Menang");
                    tvStatus.setTextColor(0xFF10B981);
                    tvStatusDesc.setText("Admin memutuskan kamu berhak mendapat refund. Kirim barang kembali ke penjual.");
                    cardShipBack.setVisibility(View.VISIBLE);
                }
                break;
            case "buyer_shipping_back":
                tvStatus.setText("📦 Barang Dikirim Kembali");
                tvStatus.setTextColor(0xFFF59E0B);
                tvStatusDesc.setText("Barang sedang dalam pengiriman kembali ke penjual.");
                cardWaitingSeller.setVisibility(View.VISIBLE);
                tvTrackingInfo.setText("Kurir: " + (d.getReturnCourier() != null ? d.getReturnCourier() : "-")
                        + "\nResi: " + (d.getReturnTrackingNumber() != null ? d.getReturnTrackingNumber() : "-"));
                
                if ("seller".equals(role)) {
                    btnSellerConfirmReturn.setVisibility(View.VISIBLE);
                    tvStatusDesc.setText("Pembeli telah mengirim barang kembali. Silakan konfirmasi jika sudah menerima barang agar dana diteruskan ke pembeli.");
                } else {
                    btnSellerConfirmReturn.setVisibility(View.GONE);
                }
                break;
            case "seller_received_back":
                tvStatus.setText("⏳ Penjual Sudah Terima Barang");
                tvStatus.setTextColor(0xFF6366F1);
                tvStatusDesc.setText("Dana sedang diproses untuk ditransfer manual oleh Admin.");
                break;
            case "refund_transferred":
                tvStatus.setText("💸 Dana Telah Ditransfer");
                tvStatus.setTextColor(0xFF8B5CF6);
                tvStatusDesc.setText("Admin telah mentransfer dana ke rekening kamu secara manual.");
                cardRefundTransferred.setVisibility(View.VISIBLE);

                if (d.getAdminRefundProof() != null && !d.getAdminRefundProof().isEmpty()) {
                    ivAdminRefundProof.setVisibility(View.VISIBLE);
                    String proofUrl = getBaseStorageUrl() + d.getAdminRefundProof();
                    Glide.with(this).load(proofUrl).into(ivAdminRefundProof);
                    ivAdminRefundProof.setOnClickListener(v -> showFullScreenImage(proofUrl));
                } else {
                    ivAdminRefundProof.setVisibility(View.GONE);
                }

                if ("buyer".equals(role)) {
                    btnConfirmRefundReceived.setVisibility(View.VISIBLE);
                    btnConfirmRefundReceived.setEnabled(true);
                } else {
                    btnConfirmRefundReceived.setVisibility(View.GONE);
                }
                break;
            case "refunded":
                tvStatus.setText("💰 Dana Dikembalikan");
                tvStatus.setTextColor(0xFF10B981);
                tvStatusDesc.setText("Refund selesai! Dana telah diterima oleh pembeli.");
                cardRefunded.setVisibility(View.VISIBLE);
                cardRefundTransferred.setVisibility(View.VISIBLE);
                btnConfirmRefundReceived.setVisibility(View.GONE);

                if (d.getAdminRefundProof() != null && !d.getAdminRefundProof().isEmpty()) {
                    ivAdminRefundProof.setVisibility(View.VISIBLE);
                    String proofUrl = getBaseStorageUrl() + d.getAdminRefundProof();
                    Glide.with(this).load(proofUrl).into(ivAdminRefundProof);
                    ivAdminRefundProof.setOnClickListener(v -> showFullScreenImage(proofUrl));
                } else {
                    ivAdminRefundProof.setVisibility(View.GONE);
                }
                break;
            case "seller_won":
            case "closed":
                tvStatus.setText("❌ Laporan Ditutup");
                tvStatus.setTextColor(0xFF64748B);
                tvStatusDesc.setText("seller".equals(d.getWinner())
                        ? "Admin memutuskan penjual memenangkan laporan ini."
                        : "Laporan telah ditutup.");
                break;
            default:
                tvStatus.setText(status);
                tvStatus.setTextColor(0xFF64748B);
        }
    }

    private void submitShipBack() {
        if (currentDispute == null) return;

        String courier  = etReturnCourier.getText() != null ? etReturnCourier.getText().toString().trim() : "";
        String tracking = etReturnTracking.getText() != null ? etReturnTracking.getText().toString().trim() : "";

        if (courier.isEmpty())  { etReturnCourier.setError("Nama driver/kurir wajib diisi"); return; }
        if (tracking.isEmpty()) { etReturnTracking.setError("Plat nomor/resi wajib diisi"); return; }
        if (selectedReturnProofUri == null) {
            Toast.makeText(this, "Foto bukti pengiriman balik wajib diunggah", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnShipBack.setEnabled(false);

        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedReturnProofUri);
            byte[] bytes = getBytes(inputStream);

            String filename = "return_proof_" + System.currentTimeMillis() + ".jpg";
            Cursor returnCursor = getContentResolver().query(selectedReturnProofUri, null, null, null, null);
            if (returnCursor != null) {
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();
                filename = returnCursor.getString(nameIndex);
                returnCursor.close();
            }

            RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(selectedReturnProofUri)), bytes);
            MultipartBody.Part body = MultipartBody.Part.createFormData("return_shipping_proof", filename, requestFile);

            RequestBody courierBody = RequestBody.create(MediaType.parse("text/plain"), courier);
            RequestBody trackingBody = RequestBody.create(MediaType.parse("text/plain"), tracking);

            apiService.buyerShipBack(token, currentDispute.getId(), courierBody, trackingBody, body)
                    .enqueue(new Callback<ApiResponse<Object>>() {
                        @Override
                        public void onResponse(@NonNull Call<ApiResponse<Object>> call,
                                               @NonNull Response<ApiResponse<Object>> response) {
                            progressBar.setVisibility(View.GONE);
                            btnShipBack.setEnabled(true);
                            if (response.isSuccessful()) {
                                Toast.makeText(DisputeDetailActivity.this,
                                        "Pengiriman balik berhasil dikonfirmasi!", Toast.LENGTH_LONG).show();
                                loadDispute();
                            } else {
                                Toast.makeText(DisputeDetailActivity.this, "Gagal mengirim konfirmasi", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<ApiResponse<Object>> call, @NonNull Throwable t) {
                            progressBar.setVisibility(View.GONE);
                            btnShipBack.setEnabled(true);
                            Toast.makeText(DisputeDetailActivity.this, "Koneksi gagal", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            btnShipBack.setEnabled(true);
            e.printStackTrace();
            Toast.makeText(this, "Gagal memproses gambar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void submitSellerConfirmReturn() {
        if (currentDispute == null) return;
        
        progressBar.setVisibility(View.VISIBLE);
        btnSellerConfirmReturn.setEnabled(false);

        apiService.sellerConfirmReturn(token, currentDispute.getId())
                .enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Object>> call,
                                           @NonNull Response<ApiResponse<Object>> response) {
                        progressBar.setVisibility(View.GONE);
                        btnSellerConfirmReturn.setEnabled(true);
                        if (response.isSuccessful()) {
                            Toast.makeText(DisputeDetailActivity.this,
                                    "Konfirmasi penerimaan barang berhasil!", Toast.LENGTH_LONG).show();
                            loadDispute();
                        } else {
                            Toast.makeText(DisputeDetailActivity.this, "Gagal mengirim konfirmasi", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Object>> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnSellerConfirmReturn.setEnabled(true);
                        Toast.makeText(DisputeDetailActivity.this, "Koneksi gagal", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void submitConfirmRefundReceived() {
        if (currentDispute == null) return;

        progressBar.setVisibility(View.VISIBLE);
        btnConfirmRefundReceived.setEnabled(false);

        apiService.buyerConfirmRefund(token, currentDispute.getId())
                .enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Object>> call,
                                           @NonNull Response<ApiResponse<Object>> response) {
                        progressBar.setVisibility(View.GONE);
                        btnConfirmRefundReceived.setEnabled(true);
                        if (response.isSuccessful()) {
                            Toast.makeText(DisputeDetailActivity.this,
                                    "Konfirmasi penerimaan dana berhasil!", Toast.LENGTH_LONG).show();
                            loadDispute();
                        } else {
                            Toast.makeText(DisputeDetailActivity.this, "Gagal mengirim konfirmasi", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Object>> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnConfirmRefundReceived.setEnabled(true);
                        Toast.makeText(DisputeDetailActivity.this, "Koneksi gagal", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getBaseStorageUrl() {
        return ApiClient.BASE_URL.replace("api/", "storage/");
    }

    private void showFullScreenImage(String imageUrl) {
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_full_screen_image);

        android.widget.ImageView iv = dialog.findViewById(R.id.ivFullScreenImage);
        Glide.with(this).load(imageUrl).into(iv);

        View btnClose = dialog.findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
