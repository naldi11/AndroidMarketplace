package com.octania.marketplace.ui.home;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.widget.ImageView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.octania.marketplace.R;
import com.octania.marketplace.utils.ToastManager;

import java.util.ArrayList;
import java.util.List;

public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.ViewHolder> {

    public static class VoucherModel {
        public int id;
        public String code;
        public double discount_amount;
        public double min_purchase;
        public String terms;
        public boolean is_claimed;
    }

    public interface OnVoucherClickListener {
        void onVoucherClick(VoucherModel voucher);
        void onClaimClick(VoucherModel voucher);
    }

    private final Context context;
    private final List<VoucherModel> vouchers = new ArrayList<>();
    private final OnVoucherClickListener listener;

    public VoucherAdapter(Context context, OnVoucherClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void updateData(List<VoucherModel> newVouchers) {
        vouchers.clear();
        vouchers.addAll(newVouchers);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_voucher, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VoucherModel voucher = vouchers.get(position);

        // Show discount value on the left side (Shopee style)
        if (voucher.discount_amount > 100) {
            String val = (int) (voucher.discount_amount / 1000) + "RB";
            holder.tvVoucherValue.setText(val);
        } else {
            holder.tvVoucherValue.setText((int) voucher.discount_amount + "%");
        }

        // Show voucher code as title
        if (voucher.code != null && !voucher.code.isEmpty()) {
            holder.tvVoucherName.setText(voucher.code);
        } else {
            holder.tvVoucherName.setText("PROMO");
        }

        // Description
        holder.tvVoucherDescription.setText(voucher.terms != null && !voucher.terms.isEmpty() ? voucher.terms : "Semua Produk");

        // Min. belanja
        if (voucher.min_purchase > 0) {
            holder.tvMinPurchase.setText("Min. Belanja Rp" + String.format("%,.0f", voucher.min_purchase));
        } else {
            holder.tvMinPurchase.setText("Tanpa Min. Belanja");
        }

        // Status Klaim UI
        if (voucher.is_claimed) {
            holder.btnClaim.setText("Gunakan");
            holder.btnClaim.setBackgroundResource(R.drawable.bg_btn_pill_grey); // We should create this
            holder.tvVoucherStatus.setText("Sudah diklaim! Gunakan saat checkout.");
            holder.tvVoucherStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
            holder.tvVoucherStatus.setVisibility(View.VISIBLE);
        } else {
            holder.btnClaim.setText("Klaim");
            holder.btnClaim.setBackgroundResource(R.drawable.bg_btn_pill);
            holder.tvVoucherStatus.setText("Klaim Segera!");
            holder.tvVoucherStatus.setTextColor(Color.parseColor("#9E9E9E")); // Grey
            holder.tvVoucherStatus.setVisibility(View.VISIBLE);
        }

        holder.btnClaim.setOnClickListener(v -> {
            if (listener != null) {
                if (voucher.is_claimed) {
                    listener.onVoucherClick(voucher);
                } else {
                    listener.onClaimClick(voucher);
                }
            }
        });

        // Card click copies voucher code to clipboard and shows terms
        holder.itemView.setOnClickListener(v -> {
            if (voucher.code != null && !voucher.code.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Voucher Code", voucher.code);
                clipboard.setPrimaryClip(clip);
                ToastManager.showToast(context, "Kode \"" + voucher.code + "\" berhasil disalin!");
            }
            showTermsDialog(voucher);
        });
    }

    private void showTermsDialog(VoucherModel voucher) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_voucher_terms, null);
        dialog.setContentView(view);

        TextView tvVoucherCode = view.findViewById(R.id.tvVoucherCode);
        TextView tvTermsContent = view.findViewById(R.id.tvTermsContent);
        Button btnClose = view.findViewById(R.id.btnClose);

        tvVoucherCode.setText("KODE: " + voucher.code);
        
        if (voucher.terms != null && !voucher.terms.trim().isEmpty()) {
            tvTermsContent.setText(voucher.terms);
        } else {
            tvTermsContent.setText("Syarat dan ketentuan berlaku untuk voucher ini. Gunakan kode ini saat checkout untuk mendapatkan potongan harga.");
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return vouchers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvVoucherName, tvVoucherDescription, tvMinPurchase, tvVoucherValue, tvVoucherStatus, btnClaim;

        ViewHolder(View itemView) {
            super(itemView);
            tvVoucherName = itemView.findViewById(R.id.tvVoucherName);
            tvVoucherDescription = itemView.findViewById(R.id.tvVoucherDescription);
            tvMinPurchase = itemView.findViewById(R.id.tvMinPurchase);
            tvVoucherValue = itemView.findViewById(R.id.tvVoucherValue);
            tvVoucherStatus = itemView.findViewById(R.id.tvVoucherStatus);
            btnClaim = itemView.findViewById(R.id.btnClaim);
        }
    }
}
