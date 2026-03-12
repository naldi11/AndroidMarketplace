package com.octania.marketplace.ui.seller;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.octania.marketplace.R;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivitySellerBalanceBinding;
import com.octania.marketplace.utils.SessionManager;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SellerBalanceActivity extends AppCompatActivity {

    private ActivitySellerBalanceBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySellerBalanceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnWithdraw.setOnClickListener(v -> showWithdrawDialog());

        setupRecyclerView();
        fetchBalance();
    }

    private void setupRecyclerView() {
        binding.rvRecentTransactions.setLayoutManager(new LinearLayoutManager(this));
    }

    private void fetchBalance() {
        String token = "Bearer " + sessionManager.getToken();

        apiService.getSellerBalance(token).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null
                        && "success".equals(response.body().getStatus())) {
                    try {
                        Gson gson = new Gson();
                        String json = gson.toJson(response.body().getData());
                        Type type = new TypeToken<Map<String, Object>>() {
                        }.getType();
                        Map<String, Object> data = gson.fromJson(json, type);

                        Map<String, Object> balance = (Map<String, Object>) data.get("balance");
                        Map<String, Object> stats = (Map<String, Object>) data.get("stats");

                        double availBalance = parseDouble(balance != null ? balance.get("available_balance") : null);
                        double totalWithdrawn = parseDouble(balance != null ? balance.get("total_withdrawn") : null);

                        binding.tvBalance.setText(String.format("Rp %,.0f", availBalance));
                        binding.tvTotalWithdrawn.setText(String.format("Rp %,.0f", totalWithdrawn));

                        if (stats != null) {
                            int totalSales = (int) parseDouble(stats.get("total_sales"));
                            double totalEarnings = parseDouble(stats.get("total_earnings"));
                            double avgEarnings = parseDouble(stats.get("avg_earnings"));
                            int pending = (int) parseDouble(stats.get("pending_orders"));

                            binding.tvTotalSales.setText(String.valueOf(totalSales));
                            binding.tvTotalEarnings.setText(String.format("Rp %,.0f", totalEarnings));
                            binding.tvAvgEarnings.setText(String.format("Rp %,.0f", avgEarnings));
                            binding.tvPendingOrders.setText(String.valueOf(pending));
                        }

                        // Recent Transactions
                        List<Map<String, Object>> recentTx = (List<Map<String, Object>>) data
                                .get("recent_transactions");
                        if (recentTx != null && !recentTx.isEmpty()) {
                            binding.rvRecentTransactions.setVisibility(android.view.View.VISIBLE);
                            binding.tvEmptyTransactions.setVisibility(android.view.View.GONE);
                            binding.rvRecentTransactions.setAdapter(new EarningsAdapter(recentTx));
                        } else {
                            binding.rvRecentTransactions.setVisibility(android.view.View.GONE);
                            binding.tvEmptyTransactions.setVisibility(android.view.View.VISIBLE);
                        }
                    } catch (Exception e) {
                        android.util.Log.e("SellerBalance", "Parse Error: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                Toast.makeText(SellerBalanceActivity.this,
                        getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showWithdrawDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 8);

        EditText etAmount = new EditText(this);
        etAmount.setHint("Jumlah (min 50.000)");
        etAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etAmount);

        EditText etBankName = new EditText(this);
        etBankName.setHint("Nama Bank (BCA, BNI, dll)");
        layout.addView(etBankName);

        EditText etAccountNo = new EditText(this);
        etAccountNo.setHint("Nomor Rekening");
        layout.addView(etAccountNo);

        EditText etAccountName = new EditText(this);
        etAccountName.setHint("Nama Pemilik Rekening");
        layout.addView(etAccountName);

        new AlertDialog.Builder(this)
                .setTitle("Tarik Saldo")
                .setView(layout)
                .setPositiveButton("Ajukan", (d, w) -> {
                    String amountStr = etAmount.getText().toString().trim();
                    String bank = etBankName.getText().toString().trim();
                    String accNo = etAccountNo.getText().toString().trim();
                    String accName = etAccountName.getText().toString().trim();

                    if (amountStr.isEmpty() || bank.isEmpty() || accNo.isEmpty() || accName.isEmpty()) {
                        Toast.makeText(this, "Isi semua field", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    requestWithdraw(Double.parseDouble(amountStr), bank, accNo, accName);
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void requestWithdraw(double amount, String bank, String accNo, String accName) {
        if (amount < 50000) {
            Toast.makeText(this, "Minimal penarikan adalah Rp 50.000", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = "Bearer " + sessionManager.getToken();

        apiService.requestWithdraw(token, amount, bank, accNo, accName)
                .enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(SellerBalanceActivity.this,
                                    response.body().getMessage(), Toast.LENGTH_LONG).show();
                            fetchBalance(); // Refresh
                        } else {
                            String message = getString(R.string.server_error);
                            try {
                                if (response.errorBody() != null) {
                                    String errorJson = response.errorBody().string();
                                    Map<String, Object> errorMap = new Gson().fromJson(errorJson,
                                            new TypeToken<Map<String, Object>>() {
                                            }.getType());

                                    if (errorMap.containsKey("message")) {
                                        message = String.valueOf(errorMap.get("message"));
                                    }

                                    // Handle Laravel validation errors specifically
                                    if (errorMap.containsKey("errors")) {
                                        Map<String, List<String>> errors = (Map<String, List<String>>) errorMap
                                                .get("errors");
                                        if (errors != null && !errors.isEmpty()) {
                                            message = errors.values().iterator().next().get(0);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                android.util.Log.e("Withdraw", "Error parsing error body: " + e.getMessage());
                            }
                            Toast.makeText(SellerBalanceActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                        Toast.makeText(SellerBalanceActivity.this,
                                getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private double parseDouble(Object obj) {
        if (obj == null)
            return 0;
        try {
            if (obj instanceof Number)
                return ((Number) obj).doubleValue();
            return Double.parseDouble(String.valueOf(obj));
        } catch (Exception e) {
            return 0;
        }
    }

    private class EarningsAdapter extends RecyclerView.Adapter<EarningsAdapter.ViewHolder> {
        private final List<Map<String, Object>> list;

        EarningsAdapter(List<Map<String, Object>> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> item = list.get(position);
            double amount = parseDouble(item.get("seller_amount"));
            String date = String.valueOf(item.get("updated_at"));
            // Shorten date if it's long
            if (date.length() > 10)
                date = date.substring(0, 10);

            holder.text1.setText(String.format("Transaksi #%s", item.get("id")));
            holder.text2.setText(String.format("Rp %,.0f • %s", amount, date));
            holder.text2.setTextColor(getResources().getColor(R.color.primary_orange));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            android.widget.TextView text1, text2;

            ViewHolder(android.view.View v) {
                super(v);
                text1 = v.findViewById(android.R.id.text1);
                text2 = v.findViewById(android.R.id.text2);
            }
        }
    }
}
