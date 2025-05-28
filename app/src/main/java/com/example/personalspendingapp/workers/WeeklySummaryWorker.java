package com.example.personalspendingapp.workers;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.personalspendingapp.utils.NotificationHelper;

public class WeeklySummaryWorker extends Worker {
    private final Context context;
    private final NotificationHelper notificationHelper;

    public WeeklySummaryWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
        this.notificationHelper = new NotificationHelper(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            // Hiển thị thông báo tổng kết tuần
            notificationHelper.showWeeklySummaryNotification();
            return Result.success();
        } catch (Exception e) {
            return Result.failure();
        }
    }
} 