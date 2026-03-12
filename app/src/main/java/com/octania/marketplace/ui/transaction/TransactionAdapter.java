package com.octania.marketplace.ui.transaction;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.octania.marketplace.data.model.Transaction;
import com.octania.marketplace.databinding.ItemTransactionBinding;

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
                binding.btnUploadProof.setVisibility(View.VISIBLE);
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

            // Format date string
            String dateStr = transaction.getCreatedAt();
            if (dateStr != null) {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                            Locale.getDefault());
                    Date date = inputFormat.parse(dateStr);
                    if (date != null) {
                        SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
                        binding.tvTransactionDate.setText(outputFormat.format(date));
                    }
                } catch (ParseException e) {
                    try {
                        // Fallback format
                        SimpleDateFormat altFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",
                                Locale.getDefault());
                        Date date = altFormat.parse(dateStr);
                        if (date != null) {
                            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy HH:mm",
                                    Locale.getDefault());
                            binding.tvTransactionDate.setText(outputFormat.format(date));
                        }
                    } catch (ParseException ex) {
                        binding.tvTransactionDate.setText(dateStr);
                    }
                }
            } else {
                binding.tvTransactionDate.setText("-");
            }
        }
    }
}
