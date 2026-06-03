package com.octania.marketplace.ui.transaction;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.model.Transaction;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityTransactionsBinding;
import com.octania.marketplace.utils.SessionManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransactionActivity extends AppCompatActivity {

    private ActivityTransactionsBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private TransactionAdapter adapter;

    private int selectedTransactionId = -1;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null && selectedTransactionId != -1) {
                        uploadProofOfPayment(imageUri, selectedTransactionId);
                        selectedTransactionId = -1; // reset
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTransactionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        setupRecyclerView();
        loadTransactions();
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(this, new TransactionAdapter.OnTransactionListener() {
            @Override
            public void onUploadProofClick(Transaction transaction) {
                selectedTransactionId = transaction.getId();
                openImagePicker();
            }

            @Override
            public void onItemClick(Transaction transaction) {
                Intent intent = new Intent(TransactionActivity.this,
                        com.octania.marketplace.ui.seller.OrderDetailActivity.class);
                intent.putExtra("transaction_id", transaction.getId());
                intent.putExtra("is_seller", false);
                startActivity(intent);
            }
        });
        binding.rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTransactions.setAdapter(adapter);
    }

    private void loadTransactions() {
        String token = "Bearer " + sessionManager.getToken();
        apiService.getTransactions(token).enqueue(new Callback<ApiResponse<List<Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Object>>> call, Response<ApiResponse<List<Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Object>> res = response.body();
                    if ("success".equals(res.getStatus())) {
                        try {
                            Gson gson = new Gson();
                            String json = gson.toJson(res.getData());
                            Type listType = new TypeToken<List<Transaction>>() {
                            }.getType();
                            List<Transaction> transactions = gson.fromJson(json, listType);

                            if (transactions == null || transactions.isEmpty()) {
                                binding.tvEmptyTransactions.setVisibility(View.VISIBLE);
                                binding.rvTransactions.setVisibility(View.GONE);
                            } else {
                                binding.tvEmptyTransactions.setVisibility(View.GONE);
                                binding.rvTransactions.setVisibility(View.VISIBLE);
                                adapter.updateData(transactions);
                                checkAndNotifyDisputeAction(transactions);
                            }
                        } catch (Exception e) {
                            Toast.makeText(TransactionActivity.this, "Gagal memproses parsing transaksi.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(TransactionActivity.this, res.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(TransactionActivity.this, "Gagal memuat riwayat", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Object>>> call, Throwable t) {
                Toast.makeText(TransactionActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "Pilih Bukti Pembayaran"));
    }

    private void uploadProofOfPayment(Uri uri, int transactionId) {
        String token = "Bearer " + sessionManager.getToken();

        try {
            // Read bytes from URI
            InputStream inputStream = getContentResolver().openInputStream(uri);
            byte[] bytes = getBytes(inputStream);

            // Get filename
            String filename = "proof_" + System.currentTimeMillis() + ".jpg";
            Cursor returnCursor = getContentResolver().query(uri, null, null, null, null);
            if (returnCursor != null) {
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();
                filename = returnCursor.getString(nameIndex);
                returnCursor.close();
            }

            RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(uri)), bytes);
            MultipartBody.Part body = MultipartBody.Part.createFormData("proof_of_payment", filename, requestFile);

            Toast.makeText(this, "Mengupload bukti pembayaran...", Toast.LENGTH_SHORT).show();

            apiService.uploadProof(token, transactionId, body).enqueue(new Callback<ApiResponse<Object>>() {
                @Override
                public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(TransactionActivity.this, "Upload Berhasil!", Toast.LENGTH_SHORT).show();
                        loadTransactions(); // Refresh
                    } else {
                        Toast.makeText(TransactionActivity.this, "Upload Gagal.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                    Log.e("Upload", "Error: ", t);
                    Toast.makeText(TransactionActivity.this, "Error jaringan saat upload.", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
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

    /** Cek apakah ada dispute yang butuh aksi pembeli, kirim notifikasi lokal */
    private void checkAndNotifyDisputeAction(List<Transaction> transactions) {
        for (Transaction t : transactions) {
            String status = t.getStatus();
            if ("buyer_won".equals(status)) {
                sendLocalNotification(
                    "Laporan Masalah — Tindakan Diperlukan",
                    "Kamu memenangkan laporan. Segera kirim barang kembali ke penjual.",
                    t.getId()
                );
                return;
            }
        }
    }

    private static final String NOTIF_CHANNEL_ID = "dispute_channel";

    private void sendLocalNotification(String title, String body, int transactionId) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                NOTIF_CHANNEL_ID, "Laporan Masalah", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Notifikasi update laporan masalah");
            nm.createNotificationChannel(ch);
        }

        Intent intent = new Intent(this, com.octania.marketplace.ui.dispute.DisputeDetailActivity.class);
        intent.putExtra(com.octania.marketplace.ui.dispute.DisputeDetailActivity.EXTRA_TRANSACTION_ID, transactionId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(this, transactionId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setVibrate(new long[]{0, 300, 100, 300});

        nm.notify(transactionId, builder.build());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
