package com.octania.marketplace.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.octania.marketplace.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private static String getBaseStorageUrl() {
        return com.octania.marketplace.data.remote.ApiClient.BASE_URL.replace("/api/", "/storage/");
    }

    public interface OnCategoryClickListener {
        void onCategoryClick(String categoryName, int position);
    }

    private final Context context;
    private final List<Map<String, Object>> categories = new ArrayList<>();
    private final OnCategoryClickListener listener;
    private int selectedPosition = 0;

    // Fallback map when backend doesn't provide icon
    private static final java.util.Map<String, Integer> ICON_MAP = new java.util.HashMap<>();

    static {
        ICON_MAP.put("all", android.R.drawable.ic_menu_sort_by_size);
        ICON_MAP.put("makanan", android.R.drawable.ic_menu_today);
        ICON_MAP.put("minuman", android.R.drawable.ic_menu_today);
        ICON_MAP.put("elektronik", android.R.drawable.ic_menu_manage);
        ICON_MAP.put("pakaian", android.R.drawable.ic_menu_gallery);
        ICON_MAP.put("buku", android.R.drawable.ic_menu_agenda);
        ICON_MAP.put("aksesoris", android.R.drawable.ic_menu_compass);
        ICON_MAP.put("olahraga", android.R.drawable.ic_menu_myplaces);
        ICON_MAP.put("kecantikan", android.R.drawable.ic_menu_view);
    }

    public CategoryAdapter(Context context, OnCategoryClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void updateData(List<Map<String, Object>> newCategories) {
        categories.clear();
        // Add "all" category first
        Map<String, Object> allCat = new java.util.HashMap<>();
        allCat.put("name", "Semua");
        allCat.put("slug", "all");
        categories.add(allCat);
        if (newCategories != null) {
            categories.addAll(newCategories);
        }
        selectedPosition = 0;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Map<String, Object> category = categories.get(position);
        String name = category.get("name") != null ? category.get("name").toString() : "?";
        holder.tvCategoryName.setText(name);

        // Try loading icon from backend first, fallback to hardcoded map
        String iconPath = category.get("icon") != null ? category.get("icon").toString() : null;

        boolean isSelected = position == selectedPosition;

        if (iconPath != null && !iconPath.isEmpty() && !iconPath.equals("null")) {
            // Load icon from backend via Glide
            String iconUrl = iconPath.startsWith("http") ? iconPath : getBaseStorageUrl() + iconPath;
            holder.ivCategoryIcon.setImageTintList(null); // Clear tint for actual images
            Glide.with(context)
                    .load(iconUrl)
                    .placeholder(android.R.drawable.ic_menu_sort_by_size)
                    .error(android.R.drawable.ic_menu_sort_by_size)
                    .into(holder.ivCategoryIcon);
        } else {
            // Fallback to hardcoded icon map
            String key = name.toLowerCase();
            int iconRes = ICON_MAP.containsKey(key) ? ICON_MAP.get(key) : android.R.drawable.ic_menu_sort_by_size;
            holder.ivCategoryIcon.setImageResource(iconRes);
            holder.ivCategoryIcon.setImageTintList(isSelected
                    ? android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
                    : android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.grey_icon)));
        }

        if (isSelected) {
            holder.flCategoryIcon.setBackgroundResource(R.drawable.bg_category_icon_selected);
            holder.tvCategoryName.setTextColor(ContextCompat.getColor(context, R.color.text_dark));
        } else {
            holder.flCategoryIcon.setBackgroundResource(R.drawable.bg_category_icon);
            holder.tvCategoryName.setTextColor(ContextCompat.getColor(context, R.color.grey_inactive));
        }

        holder.itemView.setOnClickListener(v -> {
            int prev = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);
            listener.onCategoryClick(
                    selectedPosition == 0 ? null : name,
                    selectedPosition);
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        final FrameLayout flCategoryIcon;
        final ImageView ivCategoryIcon;
        final TextView tvCategoryName;

        CategoryViewHolder(View itemView) {
            super(itemView);
            flCategoryIcon = itemView.findViewById(R.id.flCategoryIcon);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
        }
    }
}
