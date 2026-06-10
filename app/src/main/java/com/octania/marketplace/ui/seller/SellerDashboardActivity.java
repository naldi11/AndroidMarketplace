package com.octania.marketplace.ui.seller;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.gson.Gson;
import com.octania.marketplace.R;
import com.octania.marketplace.data.model.ApiResponse;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.data.remote.ApiService;
import com.octania.marketplace.databinding.ActivitySellerDashboardBinding;
import com.octania.marketplace.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Seller Dashboard with Statistics and Charts
 * Uses MPAndroidChart library for data visualization
 */
public class SellerDashboardActivity extends AppCompatActivity {

    private ActivitySellerDashboardBinding binding;
    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySellerDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        setupToolbar();
        setupCharts();
        setupBottomNav();
        setupChat();
        loadDashboardData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding.bottomNav != null) {
            binding.bottomNav.bottomNav.setSelectedItemId(R.id.nav_home);
            com.octania.marketplace.utils.NavigationUtils.applyFloatingEffect(binding.bottomNav.bottomNav);
        }
        loadDashboardData();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setTitle("Dashboard Seller");
        }

        // Setup pull-to-refresh
        SwipeRefreshLayout swipeRefresh = binding.swipeRefreshLayout;
        swipeRefresh.setColorSchemeResources(R.color.primary_orange);
        swipeRefresh.setOnRefreshListener(() -> {
            loadDashboardData();
        });
    }

    private void setupChat() {
        if (binding.btnSellerChat != null) {
            binding.btnSellerChat.setOnClickListener(v -> {
                startActivity(new Intent(this, com.octania.marketplace.ui.chat.ConversationsActivity.class));
            });
        }
    }

    private void setupBottomNav() {
        binding.bottomNav.bottomNav.setSelectedItemId(R.id.nav_home);
        com.octania.marketplace.utils.NavigationUtils.applyFloatingEffect(binding.bottomNav.bottomNav);



        binding.bottomNav.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this,
                        com.octania.marketplace.ui.product.MyProductsActivity.class));
                return true;
            } else if (id == R.id.nav_orders) {
                startActivity(new Intent(this,
                        com.octania.marketplace.ui.seller.SellerOrdersActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this,
                        com.octania.marketplace.ui.profile.ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupCharts() {
        // Setup Line Chart - Sales Trend
        LineChart lineChart = binding.lineChartSales;
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.animateX(1000);

        // Setup Bar Chart - Top Products
        BarChart barChart = binding.barChartProducts;
        barChart.getDescription().setEnabled(false);
        barChart.setTouchEnabled(true);
        barChart.setDragEnabled(true);
        barChart.setScaleEnabled(true);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getAxisRight().setEnabled(false);
        barChart.animateY(1000);

        // Setup Pie Chart - Order Status
        PieChart pieChart = binding.pieChartStatus;
        pieChart.getDescription().setEnabled(false);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);
        pieChart.animateY(1000);
    }

    private void loadDashboardData() {
        String token = "Bearer " + sessionManager.getToken();

        // Show loading
        binding.progressBar.setVisibility(View.VISIBLE);

        // Load dashboard statistics
        apiService.getSellerDashboard(token).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Object> apiResponse = response.body();
                    String status = apiResponse.getStatus();
                    Object data = apiResponse.getData();

                    android.util.Log.d("SELLER_DEBUG", "Status: " + status + ", data: " + (data != null));

                    if ("success".equals(status) && data != null) {
                        try {
                            Gson gson = new Gson();
                            String json = gson.toJson(data);
                            DashboardData dashData = gson.fromJson(json, DashboardData.class);
                            updateUI(dashData);
                            binding.progressBar.setVisibility(View.GONE);
                            android.util.Log.d("SELLER_DEBUG", "Dashboard UI updated");
                            Toast.makeText(SellerDashboardActivity.this, "Dashboard diperbarui", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            android.util.Log.e("SELLER_DEBUG", "Error parsing", e);
                            showDashboardError("Gagal memproses data dashboard.");
                        }
                    } else {
                        showDashboardError("Status API: " + status);
                    }
                } else {
                    String msg = "Gagal memuat dashboard (Error " + response.code() + ")";
                    showDashboardError(msg);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefreshLayout.setRefreshing(false);
                android.util.Log.e("SELLER_DEBUG", "Failure", t);
                showDashboardError("Koneksi gagal: " + t.getMessage());
            }
        });
    }

    private void updateUI(DashboardData data) {
        // Update summary cards
        binding.tvTotalSales.setText(formatRupiah(data.totalSales));
        binding.tvTotalOrders.setText(String.valueOf(data.totalOrders));
        binding.tvTotalProducts.setText(String.valueOf(data.totalProducts));
        binding.tvConversionRate.setText(data.conversionRate + "%");

        // Update charts
        updateSalesChart(data.salesTrend);
        updateProductsChart(data.topProducts);
        updateStatusChart(data.orderStatus);
    }

    private void updateSalesChart(List<SalesData> salesTrend) {
        if (salesTrend == null || salesTrend.isEmpty()) return;
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < salesTrend.size(); i++) {
            entries.add(new Entry(i, (float) salesTrend.get(i).amount));
            labels.add(salesTrend.get(i).date);
        }

        LineDataSet dataSet = new LineDataSet(entries, "Penjualan (Rp)");
        dataSet.setColor(ContextCompat.getColor(this, R.color.primary_orange));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.primary_orange));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.orange_10));

        LineData lineData = new LineData(dataSet);
        binding.lineChartSales.setData(lineData);
        binding.lineChartSales.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.lineChartSales.invalidate();
    }

    private void updateProductsChart(List<ProductSalesData> topProducts) {
        if (topProducts == null || topProducts.isEmpty()) return;
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < topProducts.size(); i++) {
            entries.add(new BarEntry(i, (float) topProducts.get(i).sales));
            labels.add(topProducts.get(i).productName);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Produk Terlaris");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10f);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        binding.barChartProducts.setData(barData);
        binding.barChartProducts.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.barChartProducts.invalidate();
    }

    private void updateStatusChart(OrderStatusData statusData) {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(statusData.pending, "Pending"));
        entries.add(new PieEntry(statusData.processing, "Diproses"));
        entries.add(new PieEntry(statusData.shipped, "Dikirim"));
        entries.add(new PieEntry(statusData.completed, "Selesai"));
        entries.add(new PieEntry(statusData.cancelled, "Dibatalkan"));

        PieDataSet dataSet = new PieDataSet(entries, "Status Pesanan");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData pieData = new PieData(dataSet);
        binding.pieChartStatus.setData(pieData);
        binding.pieChartStatus.invalidate();
    }

    private void showDashboardError(String message) {
        binding.progressBar.setVisibility(View.GONE);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        binding.tvTotalSales.setText("-");
        binding.tvTotalOrders.setText("-");
        binding.tvTotalProducts.setText("-");
        binding.tvConversionRate.setText("-");
    }

    private String formatRupiah(double amount) {
        return "Rp " + String.format("%,.0f", amount).replace(",", ".");
    }

    // Data classes
    public static class DashboardData {
        public double totalSales;
        public int totalOrders;
        public int totalProducts;
        public int conversionRate;
        public List<SalesData> salesTrend;
        public List<ProductSalesData> topProducts;
        public OrderStatusData orderStatus;
    }

    public static class SalesData {
        public String date;
        public double amount;

        public SalesData(String date, double amount) {
            this.date = date;
            this.amount = amount;
        }
    }

    public static class ProductSalesData {
        public String productName;
        public int sales;

        public ProductSalesData(String productName, int sales) {
            this.productName = productName;
            this.sales = sales;
        }
    }

    public static class OrderStatusData {
        public int pending;
        public int processing;
        public int shipped;
        public int completed;
        public int cancelled;
    }
}
