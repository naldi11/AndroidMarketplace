package com.octania.marketplace.ui.home;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.octania.marketplace.R;
import com.octania.marketplace.data.model.Product;
import com.octania.marketplace.data.remote.ApiClient;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_PRODUCT = 0;
    private static final int VIEW_TYPE_AD = 1;

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
        System.out.println("=== PRODUCT_ADAPTER: updateData called with " + (newProducts != null ? newProducts.size() : 0) + " items");
        this.productList.clear();
        this.productList.addAll(newProducts);
        System.out.println("=== PRODUCT_ADAPTER: productList size after update: " + this.productList.size());
        notifyDataSetChanged();
    }

    public void clearData() {
        this.productList.clear();
        notifyDataSetChanged();
    }

    public List<Product> getProductList() {
        return productList;
    }

    @Override
    public int getItemViewType(int position) {
        Product product = productList.get(position);
        if (product.getId() == -1) {
            return VIEW_TYPE_AD;
        }
        return VIEW_TYPE_PRODUCT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_AD) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_ad_banner, parent, false);
            return new AdViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
            return new ProductViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ProductViewHolder) {
            ((ProductViewHolder) holder).bind(productList.get(position));
        } else if (holder instanceof AdViewHolder) {
            // Nothing to bind for statis ad banner currently
        }
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    class AdViewHolder extends RecyclerView.ViewHolder {
        AdViewHolder(View itemView) {
            super(itemView);
        }
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductImage, btnWishlist;
        TextView tvProductName, tvProductPrice, tvOriginalPrice, tvProductRating, tvProductDistance, tvDiscountBadge;
        MaterialButton btnAddToCart;

        ProductViewHolder(View itemView) {
            super(itemView);
            
            // Manual findViewById with null checks
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            btnWishlist = itemView.findViewById(R.id.btnWishlist);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            tvOriginalPrice = itemView.findViewById(R.id.tvOriginalPrice);
            tvProductRating = itemView.findViewById(R.id.tvProductRating);
            tvProductDistance = itemView.findViewById(R.id.tvProductDistance);
            tvDiscountBadge = itemView.findViewById(R.id.tvDiscountBadge);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);

            System.out.println("=== PRODUCT_VIEWHOLDER: Views initialized");
            System.out.println("=== PRODUCT_VIEWHOLDER: ivProductImage = " + (ivProductImage != null ? "OK" : "NULL"));
            System.out.println("=== PRODUCT_VIEWHOLDER: btnWishlist = " + (btnWishlist != null ? "OK" : "NULL"));
            System.out.println("=== PRODUCT_VIEWHOLDER: tvProductName = " + (tvProductName != null ? "OK" : "NULL"));
            System.out.println("=== PRODUCT_VIEWHOLDER: tvProductPrice = " + (tvProductPrice != null ? "OK" : "NULL"));
            System.out.println("=== PRODUCT_VIEWHOLDER: btnAddToCart = " + (btnAddToCart != null ? "OK" : "NULL"));

            // Card click → detail
            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onItemClick(productList.get(pos));
                }
            });

            // Cart button click (only if button exists)
            if (btnAddToCart != null) {
                btnAddToCart.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onAddToCartClick(productList.get(pos));
                    }
                });
            }

            // Wishlist button click (only if button exists)
            if (btnWishlist != null) {
                btnWishlist.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onWishlistClick(productList.get(pos));
                    }
                });
            }
        }

        void bind(Product product) {
            System.out.println("=== PRODUCT_BIND: Binding product: " + product.getName());
            
            // Safe text setting
            if (tvProductName != null) {
                tvProductName.setText(product.getName());
            }

            // Price
            if (tvProductPrice != null) {
                if (product.isHasDiscount() && product.getOriginalPrice() > 0) {
                    // Formatting original price with strikethrough
                    String oriPriceStr = String.format("Rp %,.0f", product.getOriginalPrice());
                    if (tvOriginalPrice != null) {
                        tvOriginalPrice.setText(oriPriceStr);
                        tvOriginalPrice.setPaintFlags(
                                tvOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                        tvOriginalPrice.setVisibility(View.VISIBLE);
                    }

                    String effPriceStr = String.format("Rp %,.0f", product.getEffectivePrice());
                    tvProductPrice.setText(effPriceStr);
                } else {
                    // No discount
                    if (tvOriginalPrice != null) {
                        tvOriginalPrice.setVisibility(View.GONE);
                    }
                    String priceStr = String.format("Rp %,.0f", product.getPrice());
                    tvProductPrice.setText(priceStr);
                }
            }

            // Product Rating
            if (tvProductRating != null) {
                tvProductRating.setText(String.format(java.util.Locale.US, "%.1f", product.getAvgRating()));
            }

            // Distance — always show when available
            if (tvProductDistance != null) {
                if (product.getDistanceKm() != null) {
                    if (product.getDistanceKm() < 1.0) {
                        tvProductDistance.setText("< 1 km");
                    } else {
                        tvProductDistance.setText(
                                String.format("%.1f km", product.getDistanceKm()));
                    }
                    tvProductDistance.setVisibility(View.VISIBLE);
                } else {
                    tvProductDistance.setVisibility(View.GONE);
                }
            }

            // Wishlist
            if (btnWishlist != null) {
                btnWishlist.setColorFilter(ContextCompat.getColor(context,
                        product.isWishlisted() ? R.color.wishlist_red : R.color.grey_inactive));

                if (product.isMine()) {
                    btnWishlist.setVisibility(View.GONE);
                    if (btnAddToCart != null) {
                        btnAddToCart.setVisibility(View.GONE);
                    }
                } else {
                    btnWishlist.setVisibility(View.VISIBLE);
                    if (btnAddToCart != null) {
                        btnAddToCart.setVisibility(View.VISIBLE);
                    }
                }
            }

            // Discount badge
            if (tvDiscountBadge != null) {
                if (product.isHasDiscount() && product.getDiscountPercent() != null) {
                    tvDiscountBadge.setText(
                            String.format("-%.0f%%", product.getDiscountPercent()));
                    tvDiscountBadge.setVisibility(View.VISIBLE);
                } else {
                    tvDiscountBadge.setVisibility(View.GONE);
                }
            }

            // Image
            if (ivProductImage != null) {
                String imageUrl = product.getImage();
                
                // For test product, use a placeholder image
                if (imageUrl == null || imageUrl.isEmpty()) {
                    // Use app icon as placeholder for test
                    ivProductImage.setImageResource(R.mipmap.ic_launcher);
                    ivProductImage.setBackgroundColor(ContextCompat.getColor(context, R.color.grey_bg));
                    System.out.println("=== PRODUCT_BIND: Using placeholder image");
                } else {
                    // Real product image
                    if (!imageUrl.startsWith("http")) {
                        String baseStorage = ApiClient.BASE_URL.replace("/api/", "/storage/");
                        imageUrl = baseStorage + imageUrl;
                    }

                    System.out.println("=== PRODUCT_BIND: Loading image: " + imageUrl);

                    Glide.with(context)
                            .load(imageUrl)
                            .placeholder(R.mipmap.ic_launcher)
                            .error(R.mipmap.ic_launcher)
                            .centerCrop()
                            .into(ivProductImage);
                }
            }
                    
            System.out.println("=== PRODUCT_BIND: Product binding complete");
        }
    }
}
