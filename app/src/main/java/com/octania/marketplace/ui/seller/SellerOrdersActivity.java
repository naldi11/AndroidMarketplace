package com.octania.marketplace.ui.seller;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.octania.marketplace.R;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivitySellerOrdersBinding;
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

@SuppressWarnings("unchecked")
public class SellerOrdersActivity extends AppCompatActivity {

    private ActivitySellerOrdersBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private OrderAdapter adapter;
    private List<Map<String, Object>> allOrders = new ArrayList<>();

    private ActivityResultLauncher<Intent> filePickerLauncher;
    private Uri selectedProofUri = null;
    private ImageView dialogProofPreview = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySellerOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        adapter = new OrderAdapter();
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrders.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.swipeRefresh.setColorSchemeResources(R.color.primary_orange);
        binding.swipeRefresh.setOnRefreshListener(this::fetchOrders);

        setupTabs();
        setupBottomNav();

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedProofUri = result.getData().getData();
                        if (dialogProofPreview != null && selectedProofUri != null) {
                            dialogProofPreview.setImageURI(selectedProofUri);
                            dialogProofPreview.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null && binding.bottomNav != null) {
            binding.bottomNav.post(() -> {
                binding.bottomNav.setSelectedItemId(R.id.nav_orders);
                com.octania.marketplace.utils.NavigationUtils.applyFloatingEffect(binding.bottomNav);
            });
        }
        fetchOrders(); // Call fetchOrders here
    }

    private void setupTabs() {
        String[] titles = { "Semua", "Belum Bayar", "Dikemas", "Dikirim", "Selesai", "Dibatalkan" };
        for (String title : titles) {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(title));
        }

        binding.tabLayout
                .addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                        filterOrders(tab.getPosition());
                        markAsSeen(tab.getPosition());
                    }

                    @Override
                    public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {
                    }

                    @Override
                    public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {
                    }
                });
    }

    private void setupBottomNav() {
        binding.bottomNav.post(() -> {
            binding.bottomNav.setSelectedItemId(R.id.nav_orders);
            com.octania.marketplace.utils.NavigationUtils.applyFloatingEffect(binding.bottomNav);
        });
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, com.octania.marketplace.ui.seller.SellerDashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this, com.octania.marketplace.ui.product.MyProductsActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_orders) {
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, com.octania.marketplace.ui.profile.ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void fetchOrders() {
        binding.swipeRefresh.setRefreshing(true);
        String token = "Bearer " + sessionManager.getToken();

        apiService.getSellerTransactions(token).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                binding.swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null
                        && "success".equals(response.body().getStatus())) {
                    try {
                        Gson gson = new Gson();
                        String json = gson.toJson(response.body().getData());
                        android.util.Log.d("SellerOrders", "Raw Data: " + json);
                        android.util.Log.d("SellerOrders", "Raw Counts: " + gson.toJson(response.body().getCounts()));

                        Type type = new TypeToken<List<Map<String, Object>>>() {
                        }.getType();
                        allOrders = gson.fromJson(json, type);

                        updateTabBadges(response.body().getCounts());
                        filterOrders(binding.tabLayout.getSelectedTabPosition());
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(SellerOrdersActivity.this, getString(R.string.server_error), Toast.LENGTH_SHORT)
                                .show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(SellerOrdersActivity.this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTabBadges(Map<String, Integer> counts) {
        if (counts == null) {
            android.util.Log.d("SellerOrders", "Counts map is NULL");
            return;
        }

        android.util.Log.d("SellerOrders", "Counts Keys: " + counts.keySet().toString());

        // Tab indices: 0:Semua, 1:Belum Bayar, 2:Dikemas, 3:Dikirim, 4:Selesai,
        // 5:Dibatalkan
        setTabBadge(1, getCount(counts, "waiting_payment"));
        setTabBadge(2, getCount(counts, "pending", "processing", "packed", "ready_to_ship"));
        setTabBadge(3, getCount(counts, "shipped"));
        setTabBadge(4, getCount(counts, "received", "completed"));
        setTabBadge(5, getCount(counts, "cancelled"));
    }

    private int getCount(Map<String, Integer> counts, String... keys) {
        int total = 0;
        for (String key : keys) {
            if (counts.containsKey(key)) {
                total += counts.get(key);
            }
        }
        return total;
    }

    private void setTabBadge(int index, int count) {
        com.google.android.material.tabs.TabLayout.Tab tab = binding.tabLayout.getTabAt(index);
        if (tab != null) {
            if (count > 0) {
                tab.getOrCreateBadge().setNumber(count);
                tab.getOrCreateBadge().setBackgroundColor(getResources().getColor(R.color.primary_orange));
                tab.getOrCreateBadge().setVisible(true);
            } else {
                tab.removeBadge();
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
        apiService.markSellerOrdersSeen(token, status).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    // Update badge locally or let the next fetch handle it
                    com.google.android.material.tabs.TabLayout.Tab tab = binding.tabLayout.getTabAt(position);
                    if (tab != null)
                        tab.removeBadge();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
            }
        });
    }

    private void filterOrders(int position) {
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> order : allOrders) {
            String status = String.valueOf(order.get("status"));
            switch (position) {
                case 0: // Semua
                    filtered.add(order);
                    break;
                case 1: // Belum Bayar
                    if (status.equals("waiting_payment"))
                        filtered.add(order);
                    break;
                case 2: // Dikemas
                    if (status.equals("paid_verified") || status.equals("processing") || status.equals("packed")
                            || status.equals("ready_to_ship"))
                        filtered.add(order);
                    break;
                case 3: // Dikirim
                    if (status.equals("shipped"))
                        filtered.add(order);
                    break;
                case 4: // Selesai
                    if (status.equals("received") || status.equals("completed"))
                        filtered.add(order);
                    break;
                case 5: // Dibatalkan
                    if (status.equals("cancelled"))
                        filtered.add(order);
                    break;
            }
        }
        adapter.setData(filtered);
    }

    private void deleteOrder(int transactionId) {
        String token = "Bearer " + sessionManager.getToken();
        apiService.deleteSellerTransaction(token, transactionId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SellerOrdersActivity.this, "Pesanan dihapus", Toast.LENGTH_SHORT).show();
                    fetchOrders();
                } else {
                    String errorMsg = "Gagal menghapus pesanan";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += ": " + response.errorBody().string();
                        }
                    } catch (Exception ignored) {
                    }
                    Toast.makeText(SellerOrdersActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                Toast.makeText(SellerOrdersActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showShipDialog(int transactionId) {
        selectedProofUri = null;
        dialogProofPreview = null;

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 8);

        EditText etCourier = new EditText(this);
        etCourier.setHint("Nama Kurir (JNE, JNT, dll)");
        layout.addView(etCourier);

        EditText etTracking = new EditText(this);
        etTracking.setHint("Nomor Resi");
        layout.addView(etTracking);

        MaterialButton btnPickImage = new MaterialButton(this);
        btnPickImage.setText("Pilih Foto Bukti Kirim");
        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            filePickerLauncher.launch(intent);
        });
        layout.addView(btnPickImage);

        dialogProofPreview = new ImageView(this);
        dialogProofPreview.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 400));
        dialogProofPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        dialogProofPreview.setVisibility(View.GONE);
        layout.addView(dialogProofPreview);

        new AlertDialog.Builder(this)
                .setTitle("Kirim Pesanan")
                .setView(layout)
                .setPositiveButton("Kirim", (d, w) -> {
                    String courier = etCourier.getText().toString().trim();
                    String tracking = etTracking.getText().toString().trim();
                    if (courier.isEmpty() || tracking.isEmpty()) {
                        Toast.makeText(this, "Isi kurir dan resi", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (selectedProofUri == null) {
                        Toast.makeText(this, "Wajib upload Bukti Pengiriman!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    shipOrder(transactionId, courier, tracking, selectedProofUri);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void shipOrder(int transactionId, String courier, String tracking, Uri proofUri) {
        String token = "Bearer " + sessionManager.getToken();
        try {
            InputStream is = getContentResolver().openInputStream(proofUri);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1)
                bos.write(buffer, 0, len);
            byte[] imageBytes = bos.toByteArray();
            is.close();
            bos.close();

            RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), imageBytes);
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData("shipping_proof", "proof.jpg", reqFile);
            RequestBody courierReq = RequestBody.create(MediaType.parse("text/plain"), courier);
            RequestBody trackingReq = RequestBody.create(MediaType.parse("text/plain"), tracking);

            apiService.shipOrder(token, transactionId, courierReq, trackingReq, imagePart)
                    .enqueue(new Callback<ApiResponse<Object>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                Toast.makeText(SellerOrdersActivity.this, response.body().getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                fetchOrders();
                            } else {
                                Toast.makeText(SellerOrdersActivity.this, getString(R.string.server_error),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                            Toast.makeText(SellerOrdersActivity.this, getString(R.string.network_error),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateOrderStatus(int transactionId, String status) {
        String token = "Bearer " + sessionManager.getToken();
        apiService.updateOrderStatus(token, transactionId, status, null)
                .enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(SellerOrdersActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT)
                                    .show();
                            fetchOrders();
                        } else {
                            Toast.makeText(SellerOrdersActivity.this, getString(R.string.server_error),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                        Toast.makeText(SellerOrdersActivity.this, getString(R.string.network_error), Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }

    class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.VH> {
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
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_unified, parent, false);
            return new VH(v);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onBindViewHolder(@NonNull VH h, int position) {
            Map<String, Object> order = data.get(position);
            Map<String, Object> buyer = (Map<String, Object>) order.get("buyer");
            h.tvName.setText(buyer != null ? String.valueOf(buyer.get("name")) : "Pembeli");
            h.ivIcon.setImageResource(android.R.drawable.ic_menu_myplaces);

            String status = String.valueOf(order.get("status"));
            h.tvStatus.setText(statusLabel(status));
            h.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(statusColor(status)));

            List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");
            StringBuilder sb = new StringBuilder();
            if (items != null) {
                for (Map<String, Object> item : items) {
                    int qty = (int) Double.parseDouble(String.valueOf(item.get("quantity")));
                    Map<String, Object> prod = (Map<String, Object>) item.get("product");
                    sb.append(qty).append("x ").append(prod != null ? String.valueOf(prod.get("name")) : "Produk")
                            .append("\n");
                }
            }
            h.tvItems.setText(sb.toString().trim());

            double total = Double.parseDouble(String.valueOf(order.get("total_amount")));
            h.tvTotal.setText(String.format("Rp %,.0f", total));

            int txId = (int) Double.parseDouble(String.valueOf(order.get("id")));

            // Action buttons
            h.btnPrimary.setVisibility(View.GONE);
            h.btnSecondary.setVisibility(View.GONE);

            if ("paid_verified".equals(status)) {
                h.btnPrimary.setVisibility(View.VISIBLE);
                h.btnPrimary.setText("📦 Proses Pesanan");
                h.btnPrimary.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary_orange)));
                h.btnPrimary.setOnClickListener(v -> new AlertDialog.Builder(SellerOrdersActivity.this)
                        .setTitle("Proses")
                        .setMessage("Kemas pesanan?")
                        .setPositiveButton("Ya", (d, w) -> updateOrderStatus(txId, "packed")).show());
            } else if ("packed".equals(status)) {
                h.btnPrimary.setVisibility(View.VISIBLE);
                h.btnPrimary.setText("Kirim Pesanan");
                h.btnPrimary.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50")));
                h.btnPrimary.setOnClickListener(v -> showShipDialog(txId));
            }

            if ("cancelled".equals(status)) {
                h.btnSecondary.setVisibility(View.VISIBLE);
                h.btnSecondary.setText("Hapus");
                h.btnSecondary.setOnClickListener(v -> new AlertDialog.Builder(SellerOrdersActivity.this)
                        .setTitle("Hapus")
                        .setMessage("Hapus pesanan dibatalkan ini?")
                        .setPositiveButton("Hapus", (d, w) -> deleteOrder(txId)).show());
            }

            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(SellerOrdersActivity.this, OrderDetailActivity.class);
                intent.putExtra("transaction_id", txId);
                intent.putExtra("is_seller", true);
                startActivity(intent);
            });
        }

        private String statusLabel(String status) {
            switch (status) {
                case "waiting_payment":
                    return "Menunggu Bayar";
                case "pending":
                    return "Verifikasi Bayar";
                case "paid_verified":
                    return "Siap Dikemas";
                case "packed":
                    return "Siap Dikirim";
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
                case "paid_verified":
                case "packed":
                    return getResources().getColor(R.color.primary_orange);
                case "pending":
                    return getResources().getColor(R.color.grey_icon);
                case "shipped":
                    return android.graphics.Color.parseColor("#2196F3");
                case "completed":
                case "received":
                    return android.graphics.Color.parseColor("#4CAF50");
                case "cancelled":
                    return android.graphics.Color.parseColor("#F44336");
                default:
                    return getResources().getColor(R.color.grey_icon);
            }
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvStatus, tvItems, tvTotal;
            ImageView ivIcon;
            MaterialButton btnPrimary, btnSecondary;

            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvHeaderName);
                ivIcon = v.findViewById(R.id.ivHeaderIcon);
                tvStatus = v.findViewById(R.id.tvStatusBadge);
                tvItems = v.findViewById(R.id.tvOrderItems);
                tvTotal = v.findViewById(R.id.tvTotalAmount);
                btnPrimary = v.findViewById(R.id.btnPrimary);
                btnSecondary = v.findViewById(R.id.btnSecondary);
            }
        }
    }
}
