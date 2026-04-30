package com.octania.marketplace.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.octania.marketplace.R;
import com.octania.marketplace.ui.home.HomeActivity;
import com.octania.marketplace.ui.seller.SellerOrdersActivity;
import com.octania.marketplace.utils.ToastManager;

import java.util.Map;

/**
 * Firebase Cloud Messaging Service for push notifications
 */
public class MarketplaceFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID_ORDER_STATUS = "order_status_channel";
    private static final String CHANNEL_ID_PROMO = "promo_channel";
    private static final String CHANNEL_ID_GENERAL = "general_channel";
    
    private static final String NOTIFICATION_TYPE_ORDER = "order_status";
    private static final String NOTIFICATION_TYPE_PROMO = "promo";
    
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Send token to server
        sendTokenToServer(token);
    }
    
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        Map<String, String> data = remoteMessage.getData();
        String type = data.get("type");

        if (isDuplicateNotification(type, data)) {
            return;
        }
        
        if (remoteMessage.getNotification() != null) {
            // Handle notification message
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            
            showNotification(title, body, type, data);
        } else if (!data.isEmpty()) {
            // Handle data message
            String title = data.get("title");
            String body = data.get("body");

            showNotification(title, body, type, data);
        }
    }
    
    private void showNotification(String title, String body, String type, Map<String, String> data) {
        createNotificationChannels();
        
        String channelId = getChannelIdForType(type);
        int notificationId = (int) System.currentTimeMillis();
        
        Intent intent = getIntentForType(type, data);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent);
        
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
    
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            
            // Order Status Channel
            NotificationChannel orderChannel = new NotificationChannel(
                CHANNEL_ID_ORDER_STATUS,
                "Status Pesanan",
                NotificationManager.IMPORTANCE_HIGH
            );
            orderChannel.setDescription("Notifikasi update status pesanan");
            notificationManager.createNotificationChannel(orderChannel);
            
            // Promo Channel
            NotificationChannel promoChannel = new NotificationChannel(
                CHANNEL_ID_PROMO,
                "Promo & Voucher",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            promoChannel.setDescription("Notifikasi promo dan voucher terbaru");
            notificationManager.createNotificationChannel(promoChannel);
            
            // General Channel
            NotificationChannel generalChannel = new NotificationChannel(
                CHANNEL_ID_GENERAL,
                "Umum",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            generalChannel.setDescription("Notifikasi umum lainnya");
            notificationManager.createNotificationChannel(generalChannel);
        }
    }
    
    private String getChannelIdForType(String type) {
        if (type == null) return CHANNEL_ID_GENERAL;
        
        switch (type) {
            case NOTIFICATION_TYPE_ORDER:
                return CHANNEL_ID_ORDER_STATUS;
            case NOTIFICATION_TYPE_PROMO:
                return CHANNEL_ID_PROMO;
            default:
                return CHANNEL_ID_GENERAL;
        }
    }
    
    private Intent getIntentForType(String type, Map<String, String> data) {
        Intent intent;
        
        if (NOTIFICATION_TYPE_ORDER.equals(type)) {
            String userType = data.get("user_type");
            if ("seller".equals(userType)) {
                intent = new Intent(this, SellerOrdersActivity.class);
            } else {
                intent = new Intent(this, HomeActivity.class);
            }
        } else {
            intent = new Intent(this, HomeActivity.class);
        }
        
        // Add extras from data
        for (Map.Entry<String, String> entry : data.entrySet()) {
            intent.putExtra(entry.getKey(), entry.getValue());
        }
        
        return intent;
    }
    
    private void sendTokenToServer(String token) {
        // TODO: Implement API call to send FCM token to backend
        // This will be handled when user logs in
    }

    private boolean isDuplicateNotification(String type, Map<String, String> data) {
        String body = data.get("body");
        String keySource = data.containsKey("id") ? data.get("id") : (type + "|" + body);
        if (keySource == null) return false;

        String key = "notif_" + keySource.hashCode();
        android.content.SharedPreferences prefs = getSharedPreferences("fcm_notif_prefs", MODE_PRIVATE);
        long lastTime = prefs.getLong(key, 0);
        long now = System.currentTimeMillis();

        if (now - lastTime < 5000) {
            return true;
        }

        prefs.edit().putLong(key, now).apply();
        return false;
    }
}
