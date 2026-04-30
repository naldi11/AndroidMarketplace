package com.octania.marketplace.ui.home;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
        public String code;
        public double discount_amount;
        public double min_purchase;
        public String terms;
    }

    public interface OnVoucherClickListener {
        void onVoucherClick(VoucherModel voucher);
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

        // Show discount type as subtitle
        if (voucher.discount_amount > 100) {
            holder.tvPromoSubtitle.setText("POTONGAN HARGA");
        } else {
            holder.tvPromoSubtitle.setText("DISKON PERSEN");
        }

        // Show voucher code directly as title (no "Promo" prefix)
        if (voucher.code != null && !voucher.code.isEmpty()) {
            holder.tvPromoTitle.setText(voucher.code);
        } else {
            holder.tvPromoTitle.setText("VOUCHER");
        }

        if (voucher.discount_amount > 100) {
            holder.tvPromoDiscount.setText("Potongan Rp" + (int) (voucher.discount_amount / 1000) + "Ribu");
        } else {
            holder.tvPromoDiscount.setText("Diskon hingga " + (int) voucher.discount_amount + "%");
        }

        // Min. belanja
        if (voucher.min_purchase > 0) {
            holder.tvMinBelanja.setText("Min. Belanja\nRp" + String.format("%,.0f", voucher.min_purchase));
        } else {
            holder.tvMinBelanja.setText("Tanpa min. belanja");
        }

        // "Pakai" button copies voucher code to clipboard
        holder.btnShopNow.setOnClickListener(v -> {
            if (voucher.code != null && !voucher.code.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Voucher Code", voucher.code);
                clipboard.setPrimaryClip(clip);
                ToastManager.showToast(context, "Kode \"" + voucher.code + "\" berhasil disalin!");
            }
        });

        // Card click shows terms if present
        holder.itemView.setOnClickListener(v -> showTermsDialog(voucher));

        // Info icon shows terms
        holder.ivInfo.setOnClickListener(v -> showTermsDialog(voucher));
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
        final TextView tvPromoSubtitle, tvPromoTitle, tvPromoDiscount, tvMinBelanja;
        final ImageView ivInfo;
        final View btnShopNow;

        ViewHolder(View itemView) {
            super(itemView);
            tvPromoSubtitle = itemView.findViewById(R.id.tvPromoSubtitle);
            tvPromoTitle = itemView.findViewById(R.id.tvPromoTitle);
            tvPromoDiscount = itemView.findViewById(R.id.tvPromoDiscount);
            tvMinBelanja = itemView.findViewById(R.id.tvMinBelanja);
            ivInfo = itemView.findViewById(R.id.ivInfo);
            btnShopNow = itemView.findViewById(R.id.btnShopNow);
        }
    }
}
