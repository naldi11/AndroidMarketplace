package com.octania.marketplace.ui.payment;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.octania.marketplace.R;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivityInvoiceBinding;
import com.octania.marketplace.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InvoiceActivity extends AppCompatActivity {

    private ActivityInvoiceBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInvoiceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Invoice Pembayaran");
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        int transactionId = getIntent().getIntExtra("transaction_id", 0);
        String transactionNumber = getIntent().getStringExtra("transaction_number");

        if (transactionId <= 0) {
            Toast.makeText(this, "ID Transaksi tidak valid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadInvoiceData(transactionId, transactionNumber);

        binding.btnShare.setOnClickListener(v -> shareInvoice());
        binding.btnBackHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.octania.marketplace.ui.home.HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void loadInvoiceData(int transactionId, String transactionNumber) {
        binding.loadingView.setVisibility(View.VISIBLE);
        binding.invoiceContent.setVisibility(View.GONE);

        String token = "Bearer " + sessionManager.getToken();
        apiService.getTransactionDetail(token, transactionId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                binding.loadingView.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    bindInvoiceData(response.body().getData(), transactionNumber);
                } else {
                    Toast.makeText(InvoiceActivity.this, "Gagal memuat data invoice", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                binding.loadingView.setVisibility(View.GONE);
                Toast.makeText(InvoiceActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void bindInvoiceData(Object rawData, String transactionNumber) {
        try {
            Gson gson = new Gson();
            Map<String, Object> data = (Map<String, Object>) rawData;

            // Nomor transaksi
            String txNumber = transactionNumber;
            if (txNumber == null || txNumber.isEmpty()) {
                txNumber = data.containsKey("transaction_number")
                        ? String.valueOf(data.get("transaction_number"))
                        : "#" + ((Number) data.get("id")).intValue();
            }
            binding.tvInvoiceNumber.setText(txNumber);

            // Tanggal
            String paidAt = data.containsKey("paid_at") && data.get("paid_at") != null
                    ? String.valueOf(data.get("paid_at"))
                    : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            binding.tvInvoiceDate.setText(formatDate(paidAt));

            // Status
            String status = data.containsKey("status") ? String.valueOf(data.get("status")) : "paid_verified";
            binding.tvInvoiceStatus.setText(statusLabel(status));

            // Info Pembeli
            String buyerName = "-";
            String buyerPhone = "-";
            if (data.containsKey("buyer") && data.get("buyer") != null) {
                Map<String, Object> buyer = (Map<String, Object>) data.get("buyer");
                if (buyer.containsKey("name")) buyerName = String.valueOf(buyer.get("name"));
                if (buyer.containsKey("phone")) buyerPhone = String.valueOf(buyer.get("phone"));
            }
            binding.tvBuyerName.setText(buyerName);
            binding.tvBuyerPhone.setText(buyerPhone);

            // Metode Pembayaran
            String payMethod = data.containsKey("payment_method")
                    ? String.valueOf(data.get("payment_method")) : "MeyPay Wallet";
            binding.tvPaymentMethod.setText(payMethod);

            // Alamat pengiriman
            String address = data.containsKey("shipping_address")
                    ? String.valueOf(data.get("shipping_address")) : "-";
            binding.tvShippingAddress.setText(address);

            // Items
            if (data.containsKey("items")) {
                List<Object> items = (List<Object>) data.get("items");
                binding.llInvoiceItems.removeAllViews();
                double subtotal = 0;
                for (Object item : items) {
                    Map<String, Object> itemMap = (Map<String, Object>) item;
                    String productName = "-";
                    double price = 0;
                    int qty = 1;

                    if (itemMap.containsKey("product") && itemMap.get("product") != null) {
                        Map<String, Object> product = (Map<String, Object>) itemMap.get("product");
                        productName = product.containsKey("name") ? String.valueOf(product.get("name")) : "-";
                    }
                    if (itemMap.containsKey("price")) price = parseDouble(itemMap.get("price"));
                    if (itemMap.containsKey("quantity")) qty = (int) parseDouble(itemMap.get("quantity"));

                    subtotal += price * qty;
                    addItemRow(productName, qty, price);
                }
                binding.tvSubtotal.setText(String.format("Rp %,.0f", subtotal));
            }

            double serviceFee = parseDouble(data.get("service_fee"));
            double discount    = parseDouble(data.get("discount_total"));
            double total       = parseDouble(data.get("total_amount"));

            binding.tvServiceFee.setText(String.format("Rp %,.0f", serviceFee));
            binding.tvDiscount.setText(discount > 0 ? String.format("- Rp %,.0f", discount) : "Rp 0");
            binding.tvTotal.setText(String.format("Rp %,.0f", total));

            binding.invoiceContent.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            // Jangan langsung finish — tampilkan error tapi tetap buka halaman
            binding.invoiceContent.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Beberapa data tidak dapat dimuat", Toast.LENGTH_SHORT).show();
        }
    }

    /** Helper: parse nilai JSON yang bisa berupa String "0.00" atau Number */
    private double parseDouble(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(String.valueOf(val)); } catch (Exception e) { return 0; }
    }

    private void addItemRow(String name, int qty, double price) {
        View row = getLayoutInflater().inflate(R.layout.item_invoice_row, binding.llInvoiceItems, false);
        TextView tvName = row.findViewById(R.id.tvItemName);
        TextView tvQtyPrice = row.findViewById(R.id.tvItemQtyPrice);
        TextView tvItemTotal = row.findViewById(R.id.tvItemTotal);

        tvName.setText(name);
        tvQtyPrice.setText(qty + " x Rp " + String.format("%,.0f", price));
        tvItemTotal.setText(String.format("Rp %,.0f", qty * price));

        binding.llInvoiceItems.addView(row);
    }

    private void shareInvoice() {
        StringBuilder sb = new StringBuilder();
        sb.append("🧾 INVOICE MEYPAY\n");
        sb.append("═══════════════════\n");
        sb.append("No. Pesanan: ").append(binding.tvInvoiceNumber.getText()).append("\n");
        sb.append("Tanggal: ").append(binding.tvInvoiceDate.getText()).append("\n");
        sb.append("Status: ").append(binding.tvInvoiceStatus.getText()).append("\n");
        sb.append("Metode: ").append(binding.tvPaymentMethod.getText()).append("\n");
        sb.append("───────────────────\n");
        sb.append("Total: ").append(binding.tvTotal.getText()).append("\n");
        sb.append("═══════════════════\n");
        sb.append("Terima kasih telah berbelanja!\n");
        sb.append("MeyPay - Fast, Simple & Secure");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Invoice MeyPay - " + binding.tvInvoiceNumber.getText());
        startActivity(Intent.createChooser(shareIntent, "Bagikan Invoice"));
    }

    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("dd MMMM yyyy, HH:mm", new Locale("id"));
            Date date = input.parse(dateStr);
            return output.format(date);
        } catch (Exception e) {
            try {
                SimpleDateFormat input2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat output = new SimpleDateFormat("dd MMMM yyyy, HH:mm", new Locale("id"));
                Date date = input2.parse(dateStr);
                return output.format(date);
            } catch (Exception e2) {
                return dateStr;
            }
        }
    }

    private String statusLabel(String status) {
        switch (status) {
            case "paid_verified": return "✅ Pembayaran Berhasil";
            case "processing":    return "🔄 Sedang Diproses";
            case "shipped":       return "🚚 Dalam Pengiriman";
            case "received":      return "✅ Diterima";
            default:              return status;
        }
    }
}
