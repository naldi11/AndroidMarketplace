package com.octania.marketplace.ui.payment;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.octania.marketplace.R;
import com.octania.marketplace.data.model.WalletTransaction;
import com.octania.marketplace.databinding.ItemWalletTransactionBinding;

import java.util.ArrayList;
import java.util.List;

public class WalletTransactionAdapter extends RecyclerView.Adapter<WalletTransactionAdapter.ViewHolder> {

    private List<WalletTransaction> list = new ArrayList<>();

    public void setList(List<WalletTransaction> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemWalletTransactionBinding binding = ItemWalletTransactionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(list.get(position));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ItemWalletTransactionBinding binding;

        public ViewHolder(ItemWalletTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(WalletTransaction item) {
            binding.tvDescription.setText(item.getDescription());
            binding.tvDate.setText(item.getCreatedAt());

            if ("credit".equalsIgnoreCase(item.getType())) {
                binding.tvAmount.setText(String.format("+ Rp %,.0f", item.getAmount()));
                binding.tvAmount.setTextColor(Color.parseColor("#4CAF50")); // Green
                binding.ivType.setImageResource(R.drawable.ic_add);
                binding.ivType.setColorFilter(Color.parseColor("#4CAF50"));
            } else {
                binding.tvAmount.setText(String.format("- Rp %,.0f", item.getAmount()));
                binding.tvAmount.setTextColor(Color.parseColor("#F44336")); // Red
                binding.ivType.setImageResource(R.drawable.ic_remove);
                binding.ivType.setColorFilter(Color.parseColor("#F44336"));
            }
        }
    }
}
