package com.octania.marketplace.ui.seller;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.octania.marketplace.R;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityMyOrdersBinding;
import com.octania.marketplace.utils.SessionManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyOrdersActivity extends AppCompatActivity {

    private ActivityMyOrdersBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private MyOrderAdapter adapter;
    private List<Map<String, Object>> allOrders = new ArrayList<>();

    private int selectedTransactionIdForUpload = -1;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null && selectedTransactionIdForUpload != -1) {
                        uploadPaymentProof(selectedTransactionIdForUpload, uri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        adapter = new MyOrderAdapter();
        binding.rvMyOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMyOrders.setAdapter(adapter);

        binding.swipeRefresh.setColorSchemeResources(R.color.primary_orange);
        binding.swipeRefresh.setOnRefreshListener(this::fetchOrders);

        setupTabs();
        setupBottomNav();

        // If opened from profile shortcuts, jump to the specified tab
        int tabIndex = getIntent().getIntExtra("tab_index", 0);
        if (tabIndex > 0 && binding.tabLayout.getTabAt(tabIndex) != null) {
            binding.tabLayout.selectTab(binding.tabLayout.getTabAt(tabIndex));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null && binding.bottomNav != null) {
            // Just refresh visual effect, selection is handled in setupBottomNav
            com.octania.marketplace.utils.NavigationUtils.applyFloatingEffect(binding.bottomNav);
        }
        fetchOrders();
    }

    private void setupBottomNav() {
        // Set visual state first without listener to avoid trigger on launch
        binding.bottomNav.setSelectedItemId(R.id.nav_orders);
        com.octania.marketplace.utils.NavigationUtils.applyFloatingEffect(binding.bottomNav);

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, com.octania.marketplace.ui.home.HomeActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_orders) {
                return true;
            } else if (id == R.id.nav_wishlist) {
                startActivity(new Intent(this, com.octania.marketplace.ui.profile.WishlistActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, com.octania.marketplace.ui.profile.ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterOrders(tab.getPosition());
                markAsSeen(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void fetchOrders() {
        binding.swipeRefresh.setRefreshing(true);
        String token = "Bearer " + sessionManager.getToken();

        apiService.getTransactions(token).enqueue(new Callback<ApiResponse<List<Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Object>>> call, Response<ApiResponse<List<Object>>> response) {
                binding.swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null
                        && "success".equals(response.body().getStatus())) {
                    try {
                        Gson gson = new Gson();
                        String json = gson.toJson(response.body().getData());
                        Type type = new TypeToken<List<Map<String, Object>>>() {
                        }.getType();
                        allOrders = gson.fromJson(json, type);

                        // Update badges
                        updateTabBadges(response.body().getCounts());

                        filterOrders(binding.tabLayout.getSelectedTabPosition());
                    } catch (Exception e) {
                        Toast.makeText(MyOrdersActivity.this, "Data pesanan error", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Object>>> call, Throwable t) {
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(MyOrdersActivity.this, "Gagal mengambil data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTabBadges(Map<String, Integer> counts) {
        if (counts == null)
            return;

        // Tabs: 0: Semua (no badge), 1: Belum Bayar, 2: Dikemas, 3: Dikirim, 4:
        // Selesai, 5: Dibatalkan
        int[] tabIndices = { 1, 2, 3, 4, 5 };
        String[] countKeys = { "waiting_payment", "processing", "shipped", "received", "cancelled" };

        for (int i = 0; i < tabIndices.length; i++) {
            TabLayout.Tab tab = binding.tabLayout.getTabAt(tabIndices[i]);
            if (tab != null) {
                Integer count = counts.get(countKeys[i]);
                if (count != null && count > 0) {
                    com.google.android.material.badge.BadgeDrawable badge = tab.getOrCreateBadge();
                    badge.setNumber(count);
                    badge.setVisible(true);
                    badge.setBackgroundColor(getResources().getColor(R.color.primary_orange));
                } else {
                    tab.removeBadge();
                }
            }
        }
    }

    private void markAsSeen(int position) {
        String status = null;
        switch (position) {
            case 1:
                status = "waiting_payment";
                break;
            case 2:
                status = "processing";
                break;
            case 3:
                status = "shipped";
                break;
            case 4:
                status = "received";
                break;
            case 5:
                status = "cancelled";
                break;
        }

        if (status == null)
            return;

        String token = "Bearer " + sessionManager.getToken();
        apiService.markBuyerOrdersSeen(token, status).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    TabLayout.Tab tab = binding.tabLayout.getTabAt(position);
                    if (tab != null)
                        tab.removeBadge();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
            }
        });
    }

    private void filterOrders(int tabPosition) {
        List<Map<String, Object>> filtered = new ArrayList<>();
        // Tabs: 0: Semua, 1: Belum Bayar, 2: Dikemas, 3: Dikirim, 4: Selesai, 5:
        // Dibatalkan
        for (Map<String, Object> order : allOrders) {
            String status = String.valueOf(order.get("status"));
            boolean add = false;
            switch (tabPosition) {
                case 0:
                    add = true;
                    break;
                case 1:
                    add = status.equals("waiting_payment");
                    break;
                case 2:
                    add = status.equals("pending") || status.equals("processing")
                            || status.equals("packed");
                    break;
                case 3:
                    add = status.equals("shipped");
                    break;
                case 4:
                    add = status.equals("received") || status.equals("completed");
                    break;
                case 5:
                    add = status.equals("cancelled");
                    break;
            }
            if (add) {
                filtered.add(order);
            }
        }
        adapter.setData(filtered);
    }

    private void deleteOrder(int txId) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Pesanan")
                .setMessage("Apakah Anda ingin menghapus pesanan yang dibatalkan ini dari riwayat?")
                .setPositiveButton("Hapus", (d, w) -> {
                    String token = "Bearer " + sessionManager.getToken();
                    apiService.deleteTransaction(token, txId).enqueue(new Callback<ApiResponse<Object>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(MyOrdersActivity.this, "Pesanan dihapus", Toast.LENGTH_SHORT).show();
                                fetchOrders();
                            } else {
                                Toast.makeText(MyOrdersActivity.this, "Gagal menghapus", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                            Toast.makeText(MyOrdersActivity.this, "Koneksi Error", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void cancelOrder(int txId) {
        new AlertDialog.Builder(this)
                .setTitle("Batalkan Pesanan")
                .setMessage("Anda yakin ingin membatalkan pesanan ini?")
                .setPositiveButton("Ya", (d, w) -> {
                    String token = "Bearer " + sessionManager.getToken();
                    apiService.cancelOrder(token, txId).enqueue(new Callback<ApiResponse<Object>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                            Toast.makeText(MyOrdersActivity.this, "Pesanan Dibatalkan", Toast.LENGTH_SHORT).show();
                            fetchOrders();
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                            Toast.makeText(MyOrdersActivity.this, "Koneksi Error", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Tidak", null)
                .show();
    }

    private void openFilePickerForProof(int txId) {
        selectedTransactionIdForUpload = txId;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] { "image/*", "video/*" });
        filePickerLauncher.launch(Intent.createChooser(intent, "Pilih Foto/Video Bukti Pembayaran"));
    }

    private void uploadPaymentProof(int txId, Uri imageUri) {
        try {
            InputStream is = getContentResolver().openInputStream(imageUri);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            byte[] bytes = bos.toByteArray();

            String fileName = "proof_" + System.currentTimeMillis() + ".jpg";
            Cursor cursor = getContentResolver().query(imageUri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1)
                    fileName = cursor.getString(nameIndex);
                cursor.close();
            }

            String mimeType = getContentResolver().getType(imageUri);
            if (mimeType == null)
                mimeType = "image/jpeg";

            RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), bytes);
            MultipartBody.Part body = MultipartBody.Part.createFormData("proof_of_payment", fileName, requestFile);

            String token = "Bearer " + sessionManager.getToken();
            apiService.uploadProof(token, txId, body).enqueue(new Callback<ApiResponse<Object>>() {
                @Override
                public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(MyOrdersActivity.this, "Bukti pembayaran diunggah", Toast.LENGTH_SHORT).show();
                        fetchOrders();
                    } else {
                        Toast.makeText(MyOrdersActivity.this, "Gagal mengunggah bukti", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                    Toast.makeText(MyOrdersActivity.this, "Jaringan Error", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error membaca file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ========== INNER ADAPTER ==========
    class MyOrderAdapter extends RecyclerView.Adapter<MyOrderAdapter.VH> {
        private final List<Map<String, Object>> data = new ArrayList<>();

        void setData(List<Map<String, Object>> newData) {
            data.clear();
            data.addAll(newData);
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_order_unified, parent, false);
            return new VH(v);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onBindViewHolder(@NonNull VH h, int position) {
            Map<String, Object> order = data.get(position);

            // Seller / Shop name
            Map<String, Object> seller = (Map<String, Object>) order.get("seller");
            h.tvName.setText(seller != null ? String.valueOf(seller.get("name")) : "Toko Penjual");
            h.ivIcon.setImageResource(R.drawable.ic_baseline_store_24);

            // Status
            String status = String.valueOf(order.get("status"));
            h.tvStatus.setText(statusLabel(status));
            h.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(statusColor(status)));

            // Items summary
            List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");
            StringBuilder sb = new StringBuilder();
            if (items != null) {
                for (Map<String, Object> item : items) {
                    int qty = (int) Double.parseDouble(String.valueOf(item.get("quantity")));
                    Map<String, Object> prod = (Map<String, Object>) item.get("product");
                    String name = prod != null ? String.valueOf(prod.get("name")) : "Produk";
                    sb.append(qty).append("x ").append(name).append("\n");
                }
            }
            h.tvItems.setText(sb.toString().trim());

            // Total
            double total = Double.parseDouble(String.valueOf(order.get("total_amount")));
            h.tvTotal.setText(String.format("Rp %,.0f", total));

            // Navigate to OrderDetailActivity on card click (unless already reviewed and we
            // want to prevent click)
            int txId = (int) Double.parseDouble(String.valueOf(order.get("id")));
            Map<String, Object> review = (Map<String, Object>) order.get("review");
            boolean isReviewed = review != null;

            h.itemView.setOnClickListener(v -> {
                if (isReviewed) {
                    Toast.makeText(MyOrdersActivity.this, "Pesanan ini sudah dinilai (Hanya Baca)", Toast.LENGTH_SHORT)
                            .show();
                    // If user wants "tidak bisa diclick", we can return here.
                    // But if they want "atau hanya read only", we proceed but ensure it's read
                    // only.
                    // Proceeding to detail which is already read-only for reviewed items.
                }
                Intent intent = new Intent(MyOrdersActivity.this, OrderDetailActivity.class);
                intent.putExtra("transaction_id", txId);
                intent.putExtra("is_seller", false);
                startActivity(intent);
            });

            // Action buttons
            h.btnPrimary.setVisibility(View.GONE);
            h.btnSecondary.setVisibility(View.GONE);

            if ("waiting_payment".equals(status)) {
                h.btnPrimary.setVisibility(View.VISIBLE);
                h.btnPrimary.setText("Upload Bukti");
                h.btnPrimary.setOnClickListener(v -> openFilePickerForProof(txId));
            } else if ("shipped".equals(status)) {
                h.btnPrimary.setVisibility(View.VISIBLE);
                h.btnPrimary.setText("Pesanan Diterima");
                h.btnPrimary.setOnClickListener(v -> confirmReceived(txId));
            }

            if ("cancelled".equals(status)) {
                h.btnSecondary.setVisibility(View.VISIBLE);
                h.btnSecondary.setText("Hapus Pesanan");
                h.btnSecondary.setOnClickListener(v -> deleteOrder(txId));
            }
        }

        private void confirmReceived(int txId) {
            new AlertDialog.Builder(MyOrdersActivity.this)
                    .setTitle("Konfirmasi Diterima")
                    .setMessage(
                            "Sudah menerima pesanan ini? Silakan buka Detail Pesanan jika ingin mengunggah bukti foto/video.")
                    .setPositiveButton("Buka Detail", (d, w) -> {
                        Intent intent = new Intent(MyOrdersActivity.this, OrderDetailActivity.class);
                        intent.putExtra("transaction_id", txId);
                        startActivity(intent);
                    })
                    .setNegativeButton("Tutup", null)
                    .show();
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
                    return 0xFFC62828;
                default:
                    return 0xFF888888;
            }
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvStatus, tvItems, tvTotal;
            ImageView ivIcon;
            MaterialButton btnPrimary, btnSecondary;

            VH(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvHeaderName);
                ivIcon = itemView.findViewById(R.id.ivHeaderIcon);
                tvStatus = itemView.findViewById(R.id.tvStatusBadge);
                tvItems = itemView.findViewById(R.id.tvOrderItems);
                tvTotal = itemView.findViewById(R.id.tvTotalAmount);
                btnPrimary = itemView.findViewById(R.id.btnPrimary);
                btnSecondary = itemView.findViewById(R.id.btnSecondary);
            }
        }
    }
}
