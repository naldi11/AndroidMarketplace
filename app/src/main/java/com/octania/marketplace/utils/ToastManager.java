package com.octania.marketplace.utils;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.StringRes;

/**
 * Toast utility with debounce mechanism to prevent spam
 */
public class ToastManager {
    
    private static final long DEBOUNCE_INTERVAL = 2000; // 2 seconds
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    private static String lastMessage = "";
    private static long lastToastTime = 0;
    private static Toast currentToast;
    
    /**
     * Show toast with debounce - prevents spam of same message
     */
    public static void showToast(android.content.Context context, String message) {
        long currentTime = System.currentTimeMillis();
        
        // Cancel previous toast
        if (currentToast != null) {
            currentToast.cancel();
        }
        
        // Check if same message shown recently
        if (message.equals(lastMessage) && (currentTime - lastToastTime) < DEBOUNCE_INTERVAL) {
            return; // Skip duplicate
        }
        
        // Update tracking
        lastMessage = message;
        lastToastTime = currentTime;
        
        // Show new toast on main thread
        mainHandler.post(() -> {
            currentToast = Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT);
            currentToast.show();
        });
    }
    
    /**
     * Show toast with debounce using string resource
     */
    public static void showToast(android.content.Context context, @StringRes int resId) {
        showToast(context, context.getString(resId));
    }
    
    /**
     * Show long toast with debounce
     */
    public static void showLongToast(android.content.Context context, String message) {
        long currentTime = System.currentTimeMillis();
        
        // Cancel previous toast
        if (currentToast != null) {
            currentToast.cancel();
        }
        
        // Check if same message shown recently
        if (message.equals(lastMessage) && (currentTime - lastToastTime) < DEBOUNCE_INTERVAL) {
            return; // Skip duplicate
        }
        
        // Update tracking
        lastMessage = message;
        lastToastTime = currentTime;
        
        // Show new toast on main thread
        mainHandler.post(() -> {
            currentToast = Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_LONG);
            currentToast.show();
        });
    }
    
    /**
     * Show toast immediately without debounce (for critical messages)
     */
    public static void showToastImmediate(android.content.Context context, String message) {
        mainHandler.post(() -> {
            if (currentToast != null) {
                currentToast.cancel();
            }
            currentToast = Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT);
            currentToast.show();
        });
    }
    
    /**
     * Cancel any showing toast
     */
    public static void cancelToast() {
        if (currentToast != null) {
            currentToast.cancel();
            currentToast = null;
        }
    }
    
    /**
     * Reset debounce state (call on activity destroy if needed)
     */
    public static void reset() {
        lastMessage = "";
        lastToastTime = 0;
        cancelToast();
    }
}
