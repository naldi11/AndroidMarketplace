package com.octania.marketplace.ui.profile;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.octania.marketplace.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.VH> {

    private final Context context;
    private final List<Map<String, Object>> addresses = new ArrayList<>();
    private final boolean isSelectionMode;
    private final AddressActionCallback callback;

    public interface AddressActionCallback {
        void onAddressSelected(Map<String, Object> address);

        void onSetDefault(Map<String, Object> address);

        void onEdit(Map<String, Object> address);

        void onDelete(Map<String, Object> address);
    }

    public AddressAdapter(Context context, boolean isSelectionMode, AddressActionCallback callback) {
        this.context = context;
        this.isSelectionMode = isSelectionMode;
        this.callback = callback;
    }

    public void setAddresses(List<Map<String, Object>> newAddresses) {
        addresses.clear();
        addresses.addAll(newAddresses);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_address_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Map<String, Object> addr = addresses.get(position);

        String name = addr.containsKey("recipient_name") ? String.valueOf(addr.get("recipient_name")) : "";
        String phone = addr.containsKey("phone") ? String.valueOf(addr.get("phone")) : "";
        String full = addr.containsKey("full_address") ? String.valueOf(addr.get("full_address")) : "";
        boolean isDef = addr.containsKey("is_default") && Boolean.parseBoolean(String.valueOf(addr.get("is_default")));

        h.tvRecipientName.setText(name);
        h.tvPhoneNumber.setText(phone);
        h.tvFullAddress.setText(full);
        h.tvDefaultBadge.setVisibility(isDef ? View.VISIBLE : View.GONE);

        if (isSelectionMode) {
            h.actionLayout.setVisibility(View.GONE);
            h.itemView.setOnClickListener(v -> {
                if (callback != null)
                    callback.onAddressSelected(addr);
            });
        } else {
            h.actionLayout.setVisibility(View.VISIBLE);
            h.btnSetDefault.setEnabled(!isDef);
            h.btnSetDefault.setText(isDef ? "Utama" : "Jadikan Utama");
            h.itemView.setOnClickListener(null); // Click has no main effect in manage mode

            if (callback != null) {
                h.btnSetDefault.setOnClickListener(v -> callback.onSetDefault(addr));
                h.btnEdit.setOnClickListener(v -> callback.onEdit(addr));
                h.btnDelete.setOnClickListener(v -> callback.onDelete(addr));
            }
        }
    }

    @Override
    public int getItemCount() {
        return addresses.size();
    }

    class VH extends RecyclerView.ViewHolder {
        TextView tvRecipientName, tvDefaultBadge, tvPhoneNumber, tvFullAddress;
        LinearLayout actionLayout;
        MaterialButton btnSetDefault;
        ImageView btnEdit, btnDelete;

        VH(View itemView) {
            super(itemView);
            tvRecipientName = itemView.findViewById(R.id.tvRecipientName);
            tvDefaultBadge = itemView.findViewById(R.id.tvDefaultBadge);
            tvPhoneNumber = itemView.findViewById(R.id.tvPhoneNumber);
            tvFullAddress = itemView.findViewById(R.id.tvFullAddress);
            actionLayout = itemView.findViewById(R.id.actionLayout);
            btnSetDefault = itemView.findViewById(R.id.btnSetDefault);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
