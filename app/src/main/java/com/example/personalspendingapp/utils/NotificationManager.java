package com.example.personalspendingapp.utils;

import android.app.NotificationChannel;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.personalspendingapp.R;
import com.example.personalspendingapp.models.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class NotificationManager {
    private static final String CHANNEL_ID = "personal_spending_channel";
    private static final String CHANNEL_NAME = "Personal Spending Notifications";
    private static final int NOTIFICATION_ID = 1;
    
    private final Context context;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    
    public NotificationManager(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        createNotificationChannel();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for Personal Spending App");
            
            android.app.NotificationManager notificationManager = 
                context.getSystemService(android.app.NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    public void showNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);
            
        android.app.NotificationManager notificationManager = 
            (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
    
    public void saveNotification(Notification notification) {
        db.collection("notifications")
            .document(notification.getId())
            .set(notification)
            .addOnSuccessListener(aVoid -> {
                showNotification(notification.getTitle(), notification.getMessage());
            });
    }
    
    public interface OnNotificationsLoadedListener {
        void onNotificationsLoaded(List<Notification> notifications);
    }
    
    public void getNotifications(OnNotificationsLoadedListener listener) {
        String userId = auth.getCurrentUser().getUid();
        
        db.collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Notification> notifications = new ArrayList<>();
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    notifications.add(doc.toObject(Notification.class));
                }
                listener.onNotificationsLoaded(notifications);
            });
    }
    
    public void markAsRead(String notificationId) {
        db.collection("notifications")
            .document(notificationId)
            .update("isRead", true);
    }
} 