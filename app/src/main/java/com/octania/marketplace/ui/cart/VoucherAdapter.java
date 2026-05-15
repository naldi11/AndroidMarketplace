package com.octania.marketplace.ui.cart;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.octania.marketplace.R;
import com.octania.marketplace.data.model.Voucher;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.VoucherViewHolder> {

    private List<Voucher> vouchers = new ArrayList<>();
    private OnVoucherClickListener listener;
    private int selectedUserVoucherId = -1;
    private boolean isSelectionMode = true;

    public interface OnVoucherClickListener {
        void onVoucherClick(Voucher voucher);
    }

    public void setVouchers(List<Voucher> vouchers) {
        this.vouchers = vouchers;
        notifyDataSetChanged();
    }

    public void setOnVoucherClickListener(OnVoucherClickListener listener) {
        this.listener = listener;
    }

    public void setSelectedUserVoucherId(int id) {
        this.selectedUserVoucherId = id;
        notifyDataSetChanged();
    }

    public void setSelectionMode(boolean selectionMode) {
        isSelectionMode = selectionMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_voucher, parent, false);
        return new VoucherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
        Voucher voucher = vouchers.get(position);
        holder.bind(voucher);
    }

    @Override
    public int getItemCount() {
        return vouchers.size();
    }

    class VoucherViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDescription, tvMinPurchase, tvStatus, tvValue, tvLimitedTag, btnAction, tvUsedLabel;
        com.google.android.material.card.MaterialCardView cardVoucher;

        public VoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            cardVoucher = itemView.findViewById(R.id.cardVoucher);
            tvName = itemView.findViewById(R.id.tvVoucherName);
            tvDescription = itemView.findViewById(R.id.tvVoucherDescription);
            tvMinPurchase = itemView.findViewById(R.id.tvMinPurchase);
            tvStatus = itemView.findViewById(R.id.tvVoucherStatus);
            tvValue = itemView.findViewById(R.id.tvVoucherValue);
            tvLimitedTag = itemView.findViewById(R.id.tvLimitedTag);
            btnAction = itemView.findViewById(R.id.btnClaim);
            tvUsedLabel = itemView.findViewById(R.id.tvUsedLabel);
        }

        public void bind(Voucher voucher) {
            tvName.setText(voucher.getName());
            tvDescription.setText(voucher.getDescription() != null ? voucher.getDescription() : voucher.getTerms());
            
            NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            tvMinPurchase.setText("Min. Belanja " + nf.format(voucher.getMinPurchase()));

            if ("percent".equals(voucher.getDiscountType())) {
                tvValue.setText((int) voucher.getDiscountAmount() + "%");
            } else {
                // For fixed amount, show in simplified format if possible, or just the amount
                if (voucher.getDiscountAmount() >= 1000) {
                    tvValue.setText((int) (voucher.getDiscountAmount() / 1000) + "RB");
                } else {
                    tvValue.setText(String.valueOf((int) voucher.getDiscountAmount()));
                }
            }

            tvLimitedTag.setVisibility(View.GONE); // Selection doesn't need LIMITED tag
            
            if (!isSelectionMode) {
                // My Vouchers page mode
                tvStatus.setVisibility(View.GONE);
                btnAction.setVisibility(View.VISIBLE);
                btnAction.setText("Gunakan");
                btnAction.setBackgroundResource(R.drawable.bg_btn_pill);
                tvUsedLabel.setVisibility(View.GONE);
                cardVoucher.setStrokeWidth(0);
                itemView.setAlpha(1.0f);
                
                View.OnClickListener clickListener = v -> {
                    if (listener != null) listener.onVoucherClick(voucher);
                };
                itemView.setOnClickListener(clickListener);
                btnAction.setOnClickListener(clickListener);
                return;
            }

            boolean isCurrentlySelected = voucher.getUserVoucherId() != 0 && voucher.getUserVoucherId() == selectedUserVoucherId;

            if (voucher.isValid()) {
                if (isCurrentlySelected) {
                    tvStatus.setText("Voucher sedang digunakan");
                    tvStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
                    btnAction.setVisibility(View.GONE);
                    tvUsedLabel.setVisibility(View.VISIBLE);
                    cardVoucher.setStrokeWidth(4); // 4px or 2dp roughly
                    itemView.setAlpha(1.0f);
                } else {
                    tvStatus.setText("Voucher tersedia");
                    tvStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
                    btnAction.setVisibility(View.VISIBLE);
                    btnAction.setText("Pakai");
                    btnAction.setBackgroundResource(R.drawable.bg_btn_pill);
                    tvUsedLabel.setVisibility(View.GONE);
                    cardVoucher.setStrokeWidth(0);
                    itemView.setAlpha(1.0f);
                }
                
                View.OnClickListener clickListener = v -> {
                    if (listener != null) listener.onVoucherClick(voucher);
                };
                itemView.setOnClickListener(clickListener);
                btnAction.setOnClickListener(clickListener);
            } else {
                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText(voucher.getInvalidReason());
                tvStatus.setTextColor(Color.RED);
                btnAction.setVisibility(View.VISIBLE);
                btnAction.setText("Pilih");
                btnAction.setBackgroundResource(R.drawable.bg_btn_pill_grey);
                tvUsedLabel.setVisibility(View.GONE);
                cardVoucher.setStrokeWidth(0);
                itemView.setAlpha(0.6f);
                itemView.setOnClickListener(null);
                btnAction.setOnClickListener(null);
            }
        }
    }
}
