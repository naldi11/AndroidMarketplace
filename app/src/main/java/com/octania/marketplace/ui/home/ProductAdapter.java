package com.octania.marketplace.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.octania.marketplace.R;
import com.octania.marketplace.data.model.Product;
import com.octania.marketplace.data.remote.ApiClient;
import com.octania.marketplace.databinding.ItemProductBinding;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private final Context context;
    private final List<Product> productList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Product product);

        void onAddToCartClick(Product product);

        void onWishlistClick(Product product);
    }

    public ProductAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.productList = new ArrayList<>();
        this.listener = listener;
    }

    public void updateData(List<Product> newProducts) {
        this.productList.clear();
        this.productList.addAll(newProducts);
        notifyDataSetChanged();
    }

    public List<Product> getProductList() {
        return productList;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductBinding binding = ItemProductBinding.inflate(
                LayoutInflater.from(context), parent, false);
        return new ProductViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        holder.bind(productList.get(position));
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        private final ItemProductBinding binding;

        ProductViewHolder(ItemProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // Card click → detail
            binding.getRoot().setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onItemClick(productList.get(pos));
                }
            });

            // Cart button click
            binding.btnAddToCart.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onAddToCartClick(productList.get(pos));
                }
            });

            // Wishlist button click
            binding.btnWishlist.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onWishlistClick(productList.get(pos));
                }
            });
        }

        void bind(Product product) {
            binding.tvProductName.setText(product.getName());

            // Category
            // (Category view removed per design)

            // Price
            if (product.isHasDiscount() && product.getOriginalPrice() > 0) {
                // Formatting original price with strikethrough
                String oriPriceStr = String.format("Rp %,.0f", product.getOriginalPrice());
                if (binding.tvOriginalPrice != null) {
                    binding.tvOriginalPrice.setText(oriPriceStr);
                    binding.tvOriginalPrice.setPaintFlags(
                            binding.tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                    binding.tvOriginalPrice.setVisibility(View.VISIBLE);
                }

                String effPriceStr = String.format("Rp %,.0f", product.getEffectivePrice());
                binding.tvProductPrice.setText(effPriceStr);
            } else {
                // No discount
                if (binding.tvOriginalPrice != null) {
                    binding.tvOriginalPrice.setVisibility(View.GONE);
                }
                String priceStr = String.format("Rp %,.0f", product.getPrice());
                binding.tvProductPrice.setText(priceStr);
            }
            binding.tvPriceTag.setVisibility(View.GONE); // Hide the old glass tag to match sleek design

            // Product Rating
            if (binding.tvProductRating != null) {
                binding.tvProductRating.setText(String.format(java.util.Locale.US, "%.1f", product.getAvgRating()));
            }

            // Distance
            if (binding.tvProductDistance != null) {
                if (product.getDistanceKm() != null) {
                    binding.tvProductDistance.setText(
                            String.format("%.1f km", product.getDistanceKm()));
                    binding.tvProductDistance.setVisibility(View.VISIBLE);
                } else {
                    binding.tvProductDistance.setVisibility(View.GONE);
                }
            }

            // Wishlist (still in top-right of image)
            if (binding.btnWishlist != null) {
                binding.btnWishlist.setColorFilter(ContextCompat.getColor(context,
                        product.isWishlisted() ? R.color.wishlist_red : R.color.grey_inactive));

                if (product.isMine()) {
                    binding.btnWishlist.setVisibility(View.GONE);
                    binding.btnAddToCart.setVisibility(View.GONE);
                } else {
                    binding.btnWishlist.setVisibility(View.VISIBLE);
                    binding.btnAddToCart.setVisibility(View.VISIBLE);
                }
            }

            // Discount badge
            if (product.isHasDiscount() && product.getDiscountPercent() != null) {
                binding.tvDiscountBadge.setText(
                        String.format("-%.0f%%", product.getDiscountPercent()));
                binding.tvDiscountBadge.setVisibility(View.VISIBLE);
            } else {
                binding.tvDiscountBadge.setVisibility(View.GONE);
            }

            // Image — handle both full URLs and relative storage paths
            String imageUrl = product.getImage();
            if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.startsWith("http")) {
                String baseStorage = ApiClient.BASE_URL.replace("/api/", "/storage/");
                imageUrl = baseStorage + imageUrl;
            }

            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .centerCrop()
                    .into(binding.ivProductImage);
        }
    }
}
