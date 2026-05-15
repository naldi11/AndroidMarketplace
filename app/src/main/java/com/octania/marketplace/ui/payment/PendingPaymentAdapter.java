package com.octania.marketplace.ui.payment;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.octania.marketplace.data.model.Transaction;
import com.octania.marketplace.databinding.ItemPendingPaymentBinding;

import java.util.ArrayList;
import java.util.List;

public class PendingPaymentAdapter extends RecyclerView.Adapter<PendingPaymentAdapter.ViewHolder> {

    private List<Transaction> transactions = new ArrayList<>();
    private OnPaymentClickListener listener;

    public interface OnPaymentClickListener {
        void onPayClick(Transaction transaction);
    }

    public void setOnPaymentClickListener(OnPaymentClickListener listener) {
        this.listener = listener;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPendingPaymentBinding binding = ItemPendingPaymentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        holder.bind(transaction);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ItemPendingPaymentBinding binding;

        public ViewHolder(ItemPendingPaymentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Transaction transaction) {
            // Tampilkan nomor transaksi yang aman, bukan raw ID
            String txRef = transaction.getTransactionNumber() != null && !transaction.getTransactionNumber().isEmpty()
                    ? transaction.getTransactionNumber()
                    : "#" + transaction.getId();
            binding.tvOrderNumber.setText("No. Pesanan: " + txRef);
            binding.tvDate.setText(transaction.getCreatedAt());
            binding.tvTotalAmount.setText(String.format("Rp %,.0f", transaction.getTotalAmount()));

            binding.btnPayNow.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPayClick(transaction);
                }
            });
        }
    }
}
