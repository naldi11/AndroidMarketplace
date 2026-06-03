package com.octania.marketplace.ui.transaction;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.octania.marketplace.data.model.Transaction;
import com.octania.marketplace.databinding.ItemTransactionBinding;
import com.octania.marketplace.ui.dispute.DisputeDetailActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private final Context context;
    private final List<Transaction> transactions;
    private final OnTransactionListener listener;

    public interface OnTransactionListener {
        void onUploadProofClick(Transaction transaction);

        void onItemClick(Transaction transaction);
    }

    public TransactionAdapter(Context context, OnTransactionListener listener) {
        this.context = context;
        this.transactions = new ArrayList<>();
        this.listener = listener;
    }

    public void updateData(List<Transaction> newData) {
        this.transactions.clear();
        this.transactions.addAll(newData);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTransactionBinding binding = ItemTransactionBinding.inflate(LayoutInflater.from(context), parent, false);
        return new TransactionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        holder.bind(transactions.get(position));
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final ItemTransactionBinding binding;

        public TransactionViewHolder(ItemTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onItemClick(transactions.get(pos));
                }
            });

            binding.btnUploadProof.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onUploadProofClick(transactions.get(pos));
                }
            });
        }

        public void bind(Transaction transaction) {
            binding.tvTransactionTotal.setText(String.format("Total: Rp %,.0f", transaction.getTotalAmount()));

            // Format status
            String rawStatus = transaction.getStatus() != null ? transaction.getStatus() : "unknown";
            String displayStatus = rawStatus.toUpperCase();

            if ("waiting_payment".equalsIgnoreCase(rawStatus)) {
                displayStatus = "MENUNGGU PEMBAYARAN";
            } else if ("pending".equalsIgnoreCase(rawStatus)) {
                displayStatus = "MENUNGGU VERIFIKASI";
            } else if ("paid_verified".equalsIgnoreCase(rawStatus)) {
                displayStatus = "PEMBAYARAN TERVERIFIKASI";
            } else if ("shipped".equalsIgnoreCase(rawStatus)) {
                displayStatus = "DIKIRIM";
            } else if ("received".equalsIgnoreCase(rawStatus)) {
                displayStatus = "DITERIMA";
            } else if ("completed".equalsIgnoreCase(rawStatus)) {
                displayStatus = "SELESAI";
            } else if ("cancelled".equalsIgnoreCase(rawStatus)) {
                displayStatus = "DIBATALKAN";
            }

            binding.tvTransactionStatus.setText(displayStatus);

            if ("waiting_payment".equalsIgnoreCase(rawStatus)) {
                // Hide upload button for automated payment methods
                String method = transaction.getPaymentMethodCode() != null ? transaction.getPaymentMethodCode() : "";
                if (method.contains("meypay")) {
                    binding.btnUploadProof.setVisibility(View.GONE);
                } else {
                    binding.btnUploadProof.setVisibility(View.VISIBLE);
                }
                
                binding.tvTransactionStatus.setTextColor(android.graphics.Color.parseColor("#E65100"));
                binding.tvTransactionStatus.setBackgroundColor(android.graphics.Color.parseColor("#FFF3E0"));
            } else if ("pending".equalsIgnoreCase(rawStatus)) {
                binding.btnUploadProof.setVisibility(View.GONE);
                binding.tvTransactionStatus.setTextColor(android.graphics.Color.parseColor("#1565C0")); // Blue for
                                                                                                        // verification
                binding.tvTransactionStatus.setBackgroundColor(android.graphics.Color.parseColor("#E3F2FD"));
            } else {
                binding.btnUploadProof.setVisibility(View.GONE);
                binding.tvTransactionStatus.setTextColor(android.graphics.Color.parseColor("#1B5E20"));
                binding.tvTransactionStatus.setBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"));
            }

            // Format date string — parse UTC, tampilkan WIB
            binding.tvTransactionDate.setText(formatWIBDate(transaction.getCreatedAt()));

            MaterialButton btnLaporan = binding.btnLaporanMasalah;
            MaterialButton btnReturn  = binding.btnReturnItem;
            MaterialButton btnRefund  = binding.btnRefund;
            String txStatus = transaction.getStatus();

            // Reset dulu (Laporan Masalah dan Return disembunyikan secara default kecuali ada kondisi khusus)
            btnLaporan.setVisibility(View.GONE);
            btnReturn.setVisibility(View.GONE);
            
            // Refund button always visible
            btnRefund.setVisibility(View.VISIBLE);
            btnRefund.setOnClickListener(v -> {
                Intent i = new Intent(context, DisputeDetailActivity.class);
                i.putExtra(DisputeDetailActivity.EXTRA_TRANSACTION_ID, transaction.getId());
                context.startActivity(i);
            });

            if ("shipped".equals(txStatus) || "received".equals(txStatus)) {
                // Bisa buka laporan masalah
                btnLaporan.setVisibility(View.VISIBLE);
                btnLaporan.setText("⚠️ Buka Laporan Masalah");
                btnLaporan.setOnClickListener(v -> {
                    Intent i = new Intent(context, DisputeDetailActivity.class);
                    i.putExtra(DisputeDetailActivity.EXTRA_TRANSACTION_ID, transaction.getId());
                    context.startActivity(i);
                });
            } else if ("disputed".equals(txStatus)) {
                // Laporan aktif — tampil dua tombol
                btnLaporan.setVisibility(View.VISIBLE);
                btnLaporan.setText("📋 Lihat Status Laporan");
                btnLaporan.setOnClickListener(v -> {
                    Intent i = new Intent(context, DisputeDetailActivity.class);
                    i.putExtra(DisputeDetailActivity.EXTRA_TRANSACTION_ID, transaction.getId());
                    context.startActivity(i);
                });

                btnReturn.setVisibility(View.VISIBLE);
                btnReturn.setText("📦 Kirim Barang Kembali");
                btnReturn.setOnClickListener(v -> {
                    Intent i = new Intent(context, DisputeDetailActivity.class);
                    i.putExtra(DisputeDetailActivity.EXTRA_TRANSACTION_ID, transaction.getId());
                    context.startActivity(i);
                });

            } else if ("disputed_refunded".equals(txStatus)) {
                // Refund selesai
                btnLaporan.setVisibility(View.VISIBLE);
                btnLaporan.setText("✅ Refund Selesai — Lihat Detail");
                btnLaporan.setOnClickListener(v -> {
                    Intent i = new Intent(context, DisputeDetailActivity.class);
                    i.putExtra(DisputeDetailActivity.EXTRA_TRANSACTION_ID, transaction.getId());
                    context.startActivity(i);
                });
            }
        }
    }

    /** Parse timestamp ISO 8601 (UTC) dan tampilkan dalam WIB */
    private static String formatWIBDate(String raw) {
        if (raw == null || raw.isEmpty()) return "-";
        java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault());
        out.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Jakarta"));
        String[] formats = {
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss"
        };
        for (String fmt : formats) {
            try {
                java.text.SimpleDateFormat in = new java.text.SimpleDateFormat(fmt, java.util.Locale.US);
                if (fmt.endsWith("'Z'") || fmt.endsWith("HH:mm:ss")) {
                    in.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                }
                java.util.Date d = in.parse(raw);
                if (d != null) return out.format(d);
            } catch (Exception ignored) {}
        }
        return raw;
    }
}
