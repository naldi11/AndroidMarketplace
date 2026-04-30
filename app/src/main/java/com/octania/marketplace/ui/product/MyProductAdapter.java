package com.octania.marketplace.ui.product;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.button.MaterialButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import android.widget.ImageView;
import com.octania.marketplace.R;
import com.octania.marketplace.data.model.Product;

import java.util.ArrayList;
import java.util.List;

public class MyProductAdapter extends RecyclerView.Adapter<MyProductAdapter.ViewHolder> {

    public interface OnActionListener {
        void onEdit(Product product);

        void onDelete(Product product);
    }

    private final Context context;
    private final List<Product> products = new ArrayList<>();
    private final OnActionListener listener;

    public MyProductAdapter(Context context, OnActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void updateData(List<Product> newProducts) {
        products.clear();
        products.addAll(newProducts);
        notifyDataSetChanged();
    }

    public void removeItem(Product product) {
        int pos = products.indexOf(product);
        if (pos != -1) {
            products.remove(pos);
            notifyItemRemoved(pos);
        }
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_my_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(products.get(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivThumb;
        final TextView tvName, tvPrice, tvStock, tvCategory;
        final TextView tvOriginalPrice, tvDiscountBadge;
        final View layoutDiscount;
        final MaterialButton btnEdit, btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.ivProductThumb);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvStock = itemView.findViewById(R.id.tvStockBadge);
            tvCategory = itemView.findViewById(R.id.tvCategoryBadge);
            tvOriginalPrice = itemView.findViewById(R.id.tvOriginalPrice);
            tvDiscountBadge = itemView.findViewById(R.id.tvDiscountBadge);
            layoutDiscount = itemView.findViewById(R.id.layoutDiscount);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(Product product) {
            tvName.setText(product.getName());

            double effectivePrice = product.getEffectivePrice() > 0
                    ? product.getEffectivePrice()
                    : product.getPrice();
            tvPrice.setText(String.format("Rp %,.0f", effectivePrice));

            // Discount display
            if (product.isHasDiscount() && product.getEffectivePrice() > 0
                    && product.getEffectivePrice() < product.getPrice()) {
                layoutDiscount.setVisibility(View.VISIBLE);
                tvOriginalPrice.setText(String.format("Rp %,.0f", product.getPrice()));
                tvOriginalPrice.setPaintFlags(
                        tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                int percent = (int) ((1 - product.getEffectivePrice() / product.getPrice()) * 100);
                tvDiscountBadge.setText("-" + percent + "%");
            } else {
                layoutDiscount.setVisibility(View.GONE);
            }

            tvStock.setText("Stok: " + product.getStock());

            if (product.getCategory() != null && product.getCategory().getName() != null) {
                tvCategory.setText(product.getCategory().getName());
                tvCategory.setVisibility(View.VISIBLE);
            } else {
                tvCategory.setVisibility(View.GONE);
            }

            Glide.with(context)
                    .load(product.getImage())
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .centerCrop()
                    .into(ivThumb);

            btnEdit.setOnClickListener(v -> listener.onEdit(product));
            btnDelete.setOnClickListener(v -> listener.onDelete(product));
        }
    }
}
