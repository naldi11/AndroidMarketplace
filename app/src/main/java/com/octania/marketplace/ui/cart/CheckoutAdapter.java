package com.octania.marketplace.ui.cart;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.octania.marketplace.R;
import com.octania.marketplace.data.model.CartItem;
import com.octania.marketplace.data.model.Product;
import com.octania.marketplace.databinding.ItemCartBinding;

import java.util.ArrayList;
import java.util.List;

public class CheckoutAdapter extends RecyclerView.Adapter<CheckoutAdapter.CheckoutViewHolder> {

    private final Context context;
    private OnQuantityChangeListener quantityChangeListener;
    private boolean isCartItem;
    private final List<CartItem> cartItems;

    public interface OnQuantityChangeListener {
        void onQuantityChanged(CartItem item, int newQuantity);
    }

    public CheckoutAdapter(Context context, OnQuantityChangeListener listener) {
        this.context = context;
        this.quantityChangeListener = listener;
        this.cartItems = new ArrayList<>();
    }

    public void updateData(List<CartItem> newItems) {
        this.cartItems.clear();
        this.cartItems.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CheckoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCartBinding binding = ItemCartBinding.inflate(LayoutInflater.from(context), parent, false);
        return new CheckoutViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CheckoutViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    class CheckoutViewHolder extends RecyclerView.ViewHolder {
        private final ItemCartBinding binding;

        public CheckoutViewHolder(ItemCartBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // Hide checkbox and remove for checkout view
            binding.cbCartItem.setVisibility(View.GONE);
            binding.btnRemoveCartItem.setVisibility(View.GONE);

            // Show quantity controls
            binding.btnIncreaseQty.setVisibility(View.VISIBLE);
            binding.btnDecreaseQty.setVisibility(View.VISIBLE);
        }

        public void bind(CartItem item) {
            Product product = item.getProduct();
            if (product != null) {
                binding.tvCartItemName.setText(product.getName());

                double price = product.getEffectivePrice() > 0 ? product.getEffectivePrice() : product.getPrice();
                binding.tvCartItemPrice.setText(String.format("Rp %,.0f", price));

                String imageUrl = product.getImage();
                if (imageUrl != null && !imageUrl.startsWith("http")) {
                    imageUrl = com.octania.marketplace.data.remote.ApiClient.BASE_URL.replace("/api/", "/storage/")
                            + imageUrl;
                }
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.mipmap.ic_launcher)
                        .error(R.mipmap.ic_launcher)
                        .centerCrop()
                        .into(binding.ivCartItemImage);
            } else {
                binding.tvCartItemName.setText("Produk Tidak Ditemukan");
                binding.tvCartItemPrice.setText("Rp 0");
            }

            binding.tvCartItemQty.setText(String.valueOf(item.getQuantity()));

            binding.btnIncreaseQty.setOnClickListener(v -> {
                int newQty = item.getQuantity() + 1;
                if (product != null && newQty > product.getStock()) {
                    Toast.makeText(context, "Stok tidak mencukupi", Toast.LENGTH_SHORT).show();
                    return;
                }
                item.setQuantity(newQty);
                binding.tvCartItemQty.setText(String.valueOf(newQty));
                if (quantityChangeListener != null) {
                    quantityChangeListener.onQuantityChanged(item, newQty);
                }
            });

            binding.btnDecreaseQty.setOnClickListener(v -> {
                if (item.getQuantity() > 1) {
                    int newQty = item.getQuantity() - 1;
                    item.setQuantity(newQty);
                    binding.tvCartItemQty.setText(String.valueOf(newQty));
                    if (quantityChangeListener != null) {
                        quantityChangeListener.onQuantityChanged(item, newQty);
                    }
                }
            });
        }
    }
}
