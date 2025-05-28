package com.example.personalspendingapp.workers;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.personalspendingapp.data.DataManager;
import com.example.personalspendingapp.models.Transaction;
import com.example.personalspendingapp.models.UserData;
import com.example.personalspendingapp.utils.NotificationHelper;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DailyReminderWorker extends Worker {
    private final Context context;
    private final DataManager dataManager;
    private final NotificationHelper notificationHelper;

    public DailyReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
        this.dataManager = DataManager.getInstance();
        this.notificationHelper = new NotificationHelper(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            // Lấy ngày hiện tại
            Calendar calendar = Calendar.getInstance();
            Date today = calendar.getTime();
            
            // Lấy ngày đầu tháng
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            Date startOfMonth = calendar.getTime();
            
            // Lấy dữ liệu người dùng
            UserData userData = dataManager.getUserData();
            if (userData != null) {
                // Kiểm tra chi tiêu trong ngày
                List<Transaction> todayTransactions = dataManager.getTransactionsByDateRange(today, today);
                if (todayTransactions.isEmpty()) {
                    // Nếu chưa có giao dịch nào trong ngày, gửi thông báo nhắc nhở
                    notificationHelper.showDailyReminder();
                }
                
                // Kiểm tra chi tiêu trong tháng
                List<Transaction> monthlyTransactions = dataManager.getTransactionsByDateRange(startOfMonth, today);
                double totalExpense = 0;
                for (Transaction transaction : monthlyTransactions) {
                    if (transaction.getType().equals("expense")) {
                        totalExpense += transaction.getAmount();
                    }
                }
                
                // Nếu chi tiêu vượt quá 80% ngân sách, gửi thông báo cảnh báo
                if (totalExpense > userData.getBudget() * 0.8) {
                    notificationHelper.showBudgetExceededNotification(totalExpense, userData.getBudget());
                }
                
                // Kiểm tra các khoản chi tiêu lớn bất thường
                for (Transaction transaction : monthlyTransactions) {
                    if (transaction.getType().equals("expense") && 
                        transaction.getAmount() > userData.getBudget() * 0.3) {
                        notificationHelper.showUnusualExpenseNotification(transaction);
                    }
                }
            }
            
            return Result.success();
        } catch (Exception e) {
            return Result.failure();
        }
    }
} 