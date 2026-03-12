package com.octania.marketplace.utils;

import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationItemView;

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
}
