package com.octania.marketplace.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.octania.marketplace.R;
import com.octania.marketplace.data.model.Message;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private List<Message> messages;
    private int currentUserId;

    public ChatAdapter(List<Message> messages, int currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        if (messages.get(position).getSenderId() == currentUserId) {
            return TYPE_SENT;
        } else {
            return TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).bind(message);
        } else {
            ((ReceivedViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void updateMessages(List<Message> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        android.widget.ImageView ivAttachment;

        SentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivAttachment = itemView.findViewById(R.id.ivAttachment);
        }

        void bind(Message message) {
            if (message.getMessage() != null && !message.getMessage().isEmpty()) {
                tvMessage.setVisibility(View.VISIBLE);
                tvMessage.setText(message.getMessage());
            } else {
                tvMessage.setVisibility(View.GONE);
            }

            if (message.getAttachment() != null && !message.getAttachment().isEmpty()) {
                ivAttachment.setVisibility(View.VISIBLE);
                String rawUrl = com.octania.marketplace.data.remote.ApiClient.BASE_URL
                        .replace("api/", "storage/");
                String attachmentUrl = rawUrl + message.getAttachment();
                com.bumptech.glide.Glide.with(itemView.getContext())
                        .load(attachmentUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .override(600, 600)
                        .centerCrop()
                        .into(ivAttachment);
            } else {
                ivAttachment.setVisibility(View.GONE);
            }

            // Extract time from createdAt (e.g., 2023-10-10 10:00:00)
            if (message.getCreatedAt() != null && message.getCreatedAt().length() > 16) {
                tvTime.setText(message.getCreatedAt().substring(11, 16));
            }
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        android.widget.ImageView ivAttachment;

        ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            ivAttachment = itemView.findViewById(R.id.ivAttachment);
        }

        void bind(Message message) {
            if (message.getMessage() != null && !message.getMessage().isEmpty()) {
                tvMessage.setVisibility(View.VISIBLE);
                tvMessage.setText(message.getMessage());
            } else {
                tvMessage.setVisibility(View.GONE);
            }

            if (message.getAttachment() != null && !message.getAttachment().isEmpty()) {
                ivAttachment.setVisibility(View.VISIBLE);
                String rawUrl = com.octania.marketplace.data.remote.ApiClient.BASE_URL
                        .replace("api/", "storage/");
                String attachmentUrl = rawUrl + message.getAttachment();
                com.bumptech.glide.Glide.with(itemView.getContext())
                        .load(attachmentUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .override(600, 600)
                        .centerCrop()
                        .into(ivAttachment);
            } else {
                ivAttachment.setVisibility(View.GONE);
            }

            if (message.getCreatedAt() != null && message.getCreatedAt().length() > 16) {
                tvTime.setText(message.getCreatedAt().substring(11, 16));
            }
        }
    }
}
