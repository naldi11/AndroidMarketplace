package com.octania.marketplace.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.octania.marketplace.R;
import com.octania.marketplace.data.model.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private List<Message> messages;
    private int currentUserId;

    public ChatAdapter(List<Message> messages, int currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    private static final int TYPE_ADMIN = 3;
    private static final int TYPE_SYSTEM = 4;

    private static boolean isAdminMessage(Message msg) {
        if (msg.getMessage() == null) return false;
        String m = msg.getMessage();
        return m.startsWith("[Admin]");
    }

    private static boolean isSystemMessage(Message msg) {
        if (msg.getMessage() == null) return false;
        String m = msg.getMessage();
        return m.startsWith("Pembeli membuka laporan") ||
               m.startsWith("Laporan diselesaikan") ||
               m.startsWith("Pembeli mengirim barang kembali") ||
               m.startsWith("Penjual konfirmasi") ||
               m.startsWith("Penjual telah menerima") ||
               m.startsWith("Laporan sedang ditinjau oleh admin") ||
               m.startsWith("Admin mengkonfirmasi penjual") ||
               m.contains("akan di-refund ke rekening pembeli") ||
               m.contains("Catatan admin:");
    }

    @Override
    public int getItemViewType(int position) {
        Message msg = messages.get(position);
        if (isSystemMessage(msg)) return TYPE_SYSTEM;
        if (isAdminMessage(msg)) return TYPE_ADMIN;
        if (msg.getSenderId() == currentUserId) return TYPE_SENT;
        return TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(view);
        } else if (viewType == TYPE_ADMIN) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_admin, parent, false);
            return new AdminViewHolder(view);
        } else if (viewType == TYPE_SYSTEM) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_system, parent, false);
            return new SystemViewHolder(view);
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
        } else if (holder instanceof AdminViewHolder) {
            ((AdminViewHolder) holder).bind(message);
        } else if (holder instanceof SystemViewHolder) {
            ((SystemViewHolder) holder).bind(message);
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

            if (message.getCreatedAt() != null) {
                tvTime.setText(formatWIB(message.getCreatedAt()));
            }
        }
    }

    /** Bubble khusus untuk pesan intervensi Admin — pakai layout item_message_admin.xml */
    static class AdminViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;

        AdminViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime    = itemView.findViewById(R.id.tvTime);
        }

        void bind(Message message) {
            String raw = message.getMessage() != null ? message.getMessage() : "";
            // Hilangkan prefix [Admin]
            String display = raw.startsWith("[Admin]") ? raw.substring(7).trim() : raw;
            tvMessage.setText(display);
            tvMessage.setVisibility(View.VISIBLE);
            if (tvTime != null && message.getCreatedAt() != null) {
                tvTime.setText(formatWIB(message.getCreatedAt()));
            }
        }
    }

    /** Bubble khusus untuk notifikasi otomatis sistem — pakai layout item_message_system.xml */
    static class SystemViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;

        SystemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }

        void bind(Message message) {
            if (message.getMessage() != null) {
                tvMessage.setText(message.getMessage());
            }
        }
    }

    /** Parse timestamp ISO 8601 (UTC) dan tampilkan dalam WIB (Asia/Jakarta) */
    private static String formatWIB(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        SimpleDateFormat out = new SimpleDateFormat("HH:mm", Locale.getDefault());
        out.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));
        String[] formats = {
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss"
        };
        for (String fmt : formats) {
            try {
                SimpleDateFormat in = new SimpleDateFormat(fmt, Locale.US);
                if (fmt.endsWith("'Z'") || fmt.endsWith("HH:mm:ss")) {
                    in.setTimeZone(TimeZone.getTimeZone("UTC"));
                }
                Date d = in.parse(raw);
                if (d != null) return out.format(d);
            } catch (Exception ignored) {}
        }
        // Fallback: ambil karakter HH:mm jika gagal parse
        return raw.length() >= 16 ? raw.substring(11, 16) : raw;
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
