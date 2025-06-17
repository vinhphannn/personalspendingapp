package com.example.personalspendingapp.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.example.personalspendingapp.MainActivity;
import com.example.personalspendingapp.R;
import com.example.personalspendingapp.data.DataManager;
import com.example.personalspendingapp.models.Transaction;
import com.example.personalspendingapp.models.Category;
import com.example.personalspendingapp.models.UserData;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class NotificationHelper {
    private static final String CHANNEL_ID = "personal_spending_channel";
    private static final String CHANNEL_NAME = "Personal Spending Notifications";
    private static final String CHANNEL_DESCRIPTION = "Notifications for personal spending app";
    
    private final Context context;
    private final NotificationManagerCompat notificationManager;
    private final DataManager dataManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = NotificationManagerCompat.from(context);
        this.dataManager = DataManager.getInstance();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Thông báo nhắc nhở cập nhật tài chính hàng ngày
    public void showDailyReminder() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Nhắc nhở cập nhật tài chính")
            .setContentText("Đừng quên cập nhật các khoản chi tiêu và thu nhập hôm nay!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);

        notificationManager.notify(1, builder.build());
    }

    // Thông báo khi chi tiêu vượt quá ngân sách
    public void showBudgetExceededNotification(double currentSpending, double budget) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Cảnh báo chi tiêu")
            .setContentText(String.format("Bạn đã chi tiêu %.0f%% ngân sách tháng này!", (currentSpending/budget) * 100))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);

        notificationManager.notify(2, builder.build());
    }

    // Thông báo khi có khoản chi tiêu lớn bất thường
    public void showUnusualExpenseNotification(Transaction transaction) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        // Lấy tên danh mục từ DataManager
        String categoryName = "Unknown Category";
        Category category = dataManager.getCategoryById(transaction.getCategoryId(), transaction.getType());
        if (category != null) {
            categoryName = category.getName();
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Khoản chi tiêu lớn")
            .setContentText(String.format("Bạn vừa có khoản chi tiêu lớn: %.0f - %s", 
                transaction.getAmount(), categoryName))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);

        notificationManager.notify(3, builder.build());
    }

    // Thông báo tổng kết chi tiêu tuần
    public void showWeeklySummaryNotification() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        Date startDate = calendar.getTime();
        Date endDate = new Date();

        List<Transaction> transactions = dataManager.getTransactionsByDateRange(startDate, endDate);
        double totalExpense = 0;
        double totalIncome = 0;
        
        for (Transaction transaction : transactions) {
            if (transaction.getType().equals("expense")) {
                totalExpense += transaction.getAmount();
            } else {
                totalIncome += transaction.getAmount();
            }
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Tổng kết tuần")
            .setContentText(String.format("Tuần này bạn đã chi: %.0f, thu: %.0f", 
                totalExpense, totalIncome))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);

        notificationManager.notify(4, builder.build());
    }

    // Thông báo khi có khoản thu nhập mới
    public void showNewIncomeNotification(Transaction transaction) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        // Lấy tên danh mục từ DataManager
        String categoryName = "Unknown Category";
        Category category = dataManager.getCategoryById(transaction.getCategoryId(), transaction.getType());
        if (category != null) {
            categoryName = category.getName();
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Khoản thu nhập mới")
            .setContentText(String.format("Bạn vừa nhận được khoản thu: %.0f - %s", 
                transaction.getAmount(), categoryName))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);

        notificationManager.notify(5, builder.build());
    }
} 