package com.octania.marketplace.utils;

import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationItemView;

@android.annotation.SuppressLint("RestrictedApi")
public class NavigationUtils {

    /**
     * Applies a "floating" effect to the active item in a BottomNavigationView.
     * The active item is elevated (translated Y upwards).
     */
    public static void applyFloatingEffect(BottomNavigationView nav) {
        int selectedId = nav.getSelectedItemId();
        View menuView = nav.getChildAt(0);
        if (menuView instanceof ViewGroup) {
            ViewGroup menuGroup = (ViewGroup) menuView;
            for (int i = 0; i < menuGroup.getChildCount(); i++) {
                View itemView = menuGroup.getChildAt(i);
                if (itemView instanceof BottomNavigationItemView) {
                    BottomNavigationItemView item = (BottomNavigationItemView) itemView;
                    if (item.getId() == selectedId) {
                        // Float active item
                        item.setTranslationY(-22f); // Adjust this value for the "mengambang" height
                    } else {
                        // Reset others
                        item.setTranslationY(0f);
                    }
                }
            }
        }
    }
    public static void showScanDialog(android.app.Activity activity) {
        android.view.View dialogView = activity.getLayoutInflater().inflate(com.octania.marketplace.R.layout.dialog_scan_menu, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(activity, com.octania.marketplace.R.style.CustomAlertDialog)
                .setView(dialogView)
                .create();

        dialogView.findViewById(com.octania.marketplace.R.id.btnScanQRIS).setOnClickListener(v -> {
            dialog.dismiss();
            activity.startActivity(new android.content.Intent(activity, com.octania.marketplace.ui.payment.ScanQrActivity.class));
        });

        dialogView.findViewById(com.octania.marketplace.R.id.btnPayVA).setOnClickListener(v -> {
            dialog.dismiss();
            activity.startActivity(new android.content.Intent(activity, com.octania.marketplace.ui.payment.PendingPaymentsActivity.class));
        });

        dialog.show();
    }
}
