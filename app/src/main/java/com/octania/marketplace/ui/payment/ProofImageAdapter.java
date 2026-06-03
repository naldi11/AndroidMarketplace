package com.octania.marketplace.ui.payment;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.octania.marketplace.R;

import java.util.List;

/**
 * Adapter for displaying multiple payment proof images
 */
public class ProofImageAdapter extends RecyclerView.Adapter<ProofImageAdapter.ViewHolder> {
    private final Context context;
    private final List<Uri> images;

    public ProofImageAdapter(Context context, List<Uri> images) {
        this.context = context;
        this.images = images;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Uri imageUri = images.get(position);
        
        // Load image using Glide
        Glide.with(context)
                .load(imageUri)
                .centerCrop()
                .into(holder.imageView);
        
        // Delete button click — use getAdapterPosition() to avoid stale index after shifts
        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            new androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle("Hapus Gambar")
                    .setMessage("Apakah Anda yakin ingin menghapus gambar ini?")
                    .setPositiveButton("Hapus", (dialog, which) -> {
                        int p = holder.getAdapterPosition();
                        if (p != RecyclerView.NO_POSITION) {
                            images.remove(p);
                            notifyItemRemoved(p);
                            notifyItemRangeChanged(p, images.size());
                        }
                    })
                    .setNegativeButton("Batal", null)
                    .show();
        });

        // Image preview click
        holder.imageView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            new androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle("Pratinjau Gambar")
                    .setMessage("Pilih tindakan:")
                    .setPositiveButton("Hapus", (dialog, which) -> {
                        int p = holder.getAdapterPosition();
                        if (p != RecyclerView.NO_POSITION) {
                            images.remove(p);
                            notifyItemRemoved(p);
                            notifyItemRangeChanged(p, images.size());
                        }
                    })
                    .setNegativeButton("Tutup", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivThumb);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
