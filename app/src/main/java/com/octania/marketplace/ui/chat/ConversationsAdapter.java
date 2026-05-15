package com.octania.marketplace.ui.chat;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.octania.marketplace.R;
import com.octania.marketplace.data.model.User;
import com.octania.marketplace.data.remote.ApiClient;

import java.util.List;

public class ConversationsAdapter extends RecyclerView.Adapter<ConversationsAdapter.ViewHolder> {

    private List<User> contacts;
    private OnContactClickListener listener;

    public interface OnContactClickListener {
        void onContactClick(User contact);
    }

    public ConversationsAdapter(List<User> contacts, OnContactClickListener listener) {
        this.contacts = contacts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(contacts.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName, tvLastMessage, tvUnread;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            // Reuse tvShopName to show last message preview
            tvLastMessage = itemView.findViewById(R.id.tvShopName);
            // Optional unread badge — may not exist in layout, handle gracefully
            tvUnread = itemView.findViewById(R.id.tvUnreadCount);
        }

        void bind(User contact, OnContactClickListener listener) {
            tvName.setText(contact.getName());

            // Show last message preview
            String preview = contact.getLastMessage();
            if (preview != null && !preview.isEmpty()) {
                // Truncate long messages
                if (preview.length() > 50) {
                    preview = preview.substring(0, 47) + "...";
                }
                tvLastMessage.setText(preview);
            } else {
                tvLastMessage.setText("Ketuk untuk mulai percakapan");
            }

            // Bold if there are unread messages
            int unread = contact.getUnreadCount();
            if (unread > 0) {
                tvName.setTypeface(null, Typeface.BOLD);
                tvLastMessage.setTypeface(null, Typeface.BOLD);
                if (tvUnread != null) {
                    tvUnread.setVisibility(View.VISIBLE);
                    tvUnread.setText(unread > 9 ? "9+" : String.valueOf(unread));
                }
            } else {
                tvName.setTypeface(null, Typeface.NORMAL);
                tvLastMessage.setTypeface(null, Typeface.NORMAL);
                if (tvUnread != null) {
                    tvUnread.setVisibility(View.GONE);
                }
            }

            // Load avatar
            String avatar = contact.getAvatar();
            String avatarUrl = (avatar != null && !avatar.isEmpty())
                    ? ApiClient.BASE_URL.replace("api/", "storage/") + avatar
                    : null;

            Glide.with(itemView.getContext())
                    .load(avatarUrl)
                    .circleCrop()
                    .placeholder(R.mipmap.ic_launcher_round)
                    .error(R.mipmap.ic_launcher_round)
                    .into(ivAvatar);

            itemView.setOnClickListener(v -> listener.onContactClick(contact));
        }
    }
}
