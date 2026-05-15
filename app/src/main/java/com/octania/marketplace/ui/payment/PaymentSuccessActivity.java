package com.octania.marketplace.ui.payment;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.octania.marketplace.databinding.ActivityPaymentSuccessBinding;
import com.octania.marketplace.ui.home.HomeActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PaymentSuccessActivity extends AppCompatActivity {

    private ActivityPaymentSuccessBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentSuccessBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        double amount = getIntent().getDoubleExtra("amount", 0);
        int transactionId = getIntent().getIntExtra("transaction_id", 0);
        String transactionNumber = getIntent().getStringExtra("transaction_number");

        // Tampilkan nomor transaksi yang aman (bukan raw ID)
        String displayRef = (transactionNumber != null && !transactionNumber.isEmpty())
                ? transactionNumber
                : "#" + transactionId;

        binding.tvSuccessAmount.setText(String.format("Rp %,.0f", amount));
        binding.tvSuccessOrderId.setText(displayRef);

        String currentTime = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date());
        binding.tvSuccessTime.setText(currentTime);

        binding.btnBackHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        binding.btnPrintInvoice.setOnClickListener(v -> {
            Intent intent = new Intent(this, InvoiceActivity.class);
            intent.putExtra("transaction_id", transactionId);
            intent.putExtra("transaction_number", transactionNumber);
            startActivity(intent);
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
