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
        items.add(new DistanceModel("0", "Semua", 15));
        items.add(new DistanceModel("1", "1 KM", 1));
        items.add(new DistanceModel("3", "3 KM", 3));
        items.add(new DistanceModel("5", "5 KM", 5));
        items.add(new DistanceModel("10", "10 KM", 10));
        items.add(new DistanceModel("15", "15 KM", 15));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_distance_filter, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DistanceModel item = items.get(position);
        holder.tvLabel.setText(item.iconLabel);
        holder.tvUnit.setText(item.label);
        
        boolean isSelected = position == selectedPosition;

        if (isSelected) {
            holder.flIcon.setBackgroundResource(R.drawable.bg_category_icon_selected);
            holder.tvLabel.setTextColor(ContextCompat.getColor(context, R.color.white));
            holder.tvUnit.setTextColor(ContextCompat.getColor(context, R.color.primary_orange));
        } else {
            holder.flIcon.setBackgroundResource(R.drawable.bg_category_icon);
            holder.tvLabel.setTextColor(ContextCompat.getColor(context, R.color.primary_orange));
            holder.tvUnit.setTextColor(ContextCompat.getColor(context, R.color.grey_inactive));
        }

        holder.itemView.setOnClickListener(v -> {
            int prev = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);
            android.util.Log.d("DISTANCE_DEBUG", "Selected Radius: " + item.radius + " KM");
            listener.onDistanceClick(item.radius);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final FrameLayout flIcon;
        final TextView tvLabel;
        final TextView tvUnit;

        ViewHolder(View itemView) {
            super(itemView);
            flIcon = itemView.findViewById(R.id.flDistanceIcon);
            tvLabel = itemView.findViewById(R.id.tvDistanceLabel);
            tvUnit = itemView.findViewById(R.id.tvDistanceUnit);
        }
    }

    static class DistanceModel {
        String iconLabel;
        String label;
        Integer radius;

        DistanceModel(String iconLabel, String label, Integer radius) {
            this.iconLabel = iconLabel;
            this.label = label;
            this.radius = radius;
        }
    }
}
