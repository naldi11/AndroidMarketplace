package com.octania.marketplace.ui.cart;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.octania.marketplace.R;
import com.octania.marketplace.data.model.CartItem;
import com.octania.marketplace.data.model.Product;
import com.octania.marketplace.databinding.ItemCartBinding;

import java.util.ArrayList;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private final Context context;
    private final List<CartItem> cartItems;
    private final OnCartItemListener listener;

    public interface OnCartItemListener {
        void onDeleteClick(CartItem item, int position);

        void onCheckChanged(CartItem item, int position, boolean isChecked);

        void onQuantityChanged(CartItem item, int position, int newQuantity);

        void onItemClick(CartItem item, int position);
    }

    public CartAdapter(Context context, OnCartItemListener listener) {
        this.context = context;
        this.cartItems = new ArrayList<>();
        this.listener = listener;
    }

    public void updateData(List<CartItem> newItems) {
        this.cartItems.clear();
        this.cartItems.addAll(newItems);
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < cartItems.size()) {
            cartItems.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, cartItems.size());
        }
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCartBinding binding = ItemCartBinding.inflate(LayoutInflater.from(context), parent, false);
        return new CartViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    class CartViewHolder extends RecyclerView.ViewHolder {
        private final ItemCartBinding binding;

        public CartViewHolder(ItemCartBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.getRoot().setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onItemClick(cartItems.get(pos), pos);
                }
            });

            binding.btnRemoveCartItem.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(cartItems.get(pos), pos);
                }
            });

            binding.cbCartItem.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    CartItem item = cartItems.get(pos);
                    if (item.isSelected() != isChecked) {
                        item.setSelected(isChecked);
                        listener.onCheckChanged(item, pos, isChecked);
                    }
                }
            });

            binding.btnIncreaseQty.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    CartItem item = cartItems.get(pos);
                    // Ensure the stock isn't exceeded during bind, but let Activity handle API side
                    listener.onQuantityChanged(item, pos, item.getQuantity() + 1);
                }
            });

            binding.btnDecreaseQty.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    CartItem item = cartItems.get(pos);
                    if (item.getQuantity() > 1) {
                        listener.onQuantityChanged(item, pos, item.getQuantity() - 1);
                    } else {
                        new androidx.appcompat.app.AlertDialog.Builder(context)
                                .setTitle("Hapus Produk")
                                .setMessage("Hapus produk ini dari keranjang?")
                                .setPositiveButton("Hapus", (dialog, which) -> {
                                    int p = getAdapterPosition();
                                    if (p != RecyclerView.NO_POSITION)
                                        listener.onDeleteClick(cartItems.get(p), p);
                                })
                                .setNegativeButton("Batal", null)
                                .show();
                    }
                }
            });
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

            // Temporary detach listener to set state without firing events
            binding.cbCartItem.setOnCheckedChangeListener(null);
            binding.cbCartItem.setChecked(item.isSelected());
            binding.cbCartItem.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    CartItem cItem = cartItems.get(pos);
                    if (cItem.isSelected() != isChecked) {
                        cItem.setSelected(isChecked);
                        listener.onCheckChanged(cItem, pos, isChecked);
                    }
                }
            });
        }
    }
}
