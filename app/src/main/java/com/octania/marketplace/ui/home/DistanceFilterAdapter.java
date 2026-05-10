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

import com.octania.marketplace.R;

import java.util.ArrayList;
import java.util.List;

public class DistanceFilterAdapter extends RecyclerView.Adapter<DistanceFilterAdapter.ViewHolder> {

    public interface OnDistanceClickListener {
        void onDistanceClick(Integer radius);
    }

    private final Context context;
    private final List<DistanceModel> items = new ArrayList<>();
    private final OnDistanceClickListener listener;
    private int selectedPosition = 0;

    public DistanceFilterAdapter(Context context, OnDistanceClickListener listener) {
        this.context = context;
        this.listener = listener;
        setupItems();
    }

    private void setupItems() {
        items.add(new DistanceModel("Semua", null));
        items.add(new DistanceModel("1 KM", 1));
        items.add(new DistanceModel("2 KM", 2));
        items.add(new DistanceModel("3 KM", 3));
        items.add(new DistanceModel("4 KM", 4));
        items.add(new DistanceModel("5 KM", 5));
        items.add(new DistanceModel("5++ KM", 100)); // Assuming 100 is "unlimited" or large enough
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DistanceModel item = items.get(position);
        holder.tvName.setText(item.label);
        
        boolean isSelected = position == selectedPosition;

        holder.ivIcon.setImageResource(R.drawable.ic_location);
        holder.ivIcon.setImageTintList(isSelected
                ? android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
                : android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.grey_icon)));

        if (isSelected) {
            holder.flIcon.setBackgroundResource(R.drawable.bg_category_icon_selected);
            holder.tvName.setTextColor(ContextCompat.getColor(context, R.color.text_dark));
        } else {
            holder.flIcon.setBackgroundResource(R.drawable.bg_category_icon);
            holder.tvName.setTextColor(ContextCompat.getColor(context, R.color.grey_inactive));
        }

        holder.itemView.setOnClickListener(v -> {
            int prev = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);
            listener.onDistanceClick(item.radius);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final FrameLayout flIcon;
        final ImageView ivIcon;
        final TextView tvName;

        ViewHolder(View itemView) {
            super(itemView);
            flIcon = itemView.findViewById(R.id.flCategoryIcon);
            ivIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvName = itemView.findViewById(R.id.tvCategoryName);
        }
    }

    static class DistanceModel {
        String label;
        Integer radius;

        DistanceModel(String label, Integer radius) {
            this.label = label;
            this.radius = radius;
        }
    }
}
