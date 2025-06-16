package com.example.personalspendingapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.personalspendingapp.fragments.ExpenseFragment;
import com.example.personalspendingapp.fragments.IncomeFragment;
import com.example.personalspendingapp.fragments.OtherFragment;
import com.example.personalspendingapp.fragments.CalendarFragment;
import com.example.personalspendingapp.fragments.ReportFragment;

import com.example.personalspendingapp.adapters.TabPagerAdapter;
import com.example.personalspendingapp.data.DataManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Date;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.personalspendingapp.workers.DailyReminderWorker;
import com.example.personalspendingapp.workers.WeeklySummaryWorker;
import com.example.personalspendingapp.models.Transaction;

import com.example.personalspendingapp.utils.NotificationHelper;

public class MainActivity extends AppCompatActivity implements CalendarFragment.OnDaySelectedListener {
    private static final String TAG = "MainActivity";
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
    private LinearLayout inputTabsContainer;
    private TextView tvTabExpense;
    private TextView tvTabIncome;
    private FragmentManager fragmentManager;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    private ExpenseFragment expenseFragment;
    private IncomeFragment incomeFragment;
    private CalendarFragment calendarFragment;
    private ReportFragment reportFragment;
    private OtherFragment otherFragment;

    private Date selectedDateForInputTabs;

    private NotificationHelper notificationHelper;

    private boolean isNavigatingToCalendar = false;

    // Interface for callback to the hosting Activity
    public interface OnDaySelectedListener {
        void onDaySelected(Date selectedDate);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        notificationHelper = new NotificationHelper(this);

        // Initialize views
        viewPager = findViewById(R.id.viewPager);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        inputTabsContainer = findViewById(R.id.inputTabsContainer);
        tvTabExpense = findViewById(R.id.tvTabExpense);
        tvTabIncome = findViewById(R.id.tvTabIncome);
        fragmentManager = getSupportFragmentManager();

        // Lên lịch thông báo hàng ngày lúc 9h tối
        scheduleDailyReminder();
        
        // Lên lịch thông báo tổng kết tuần vào Chủ nhật
        scheduleWeeklySummary();

        // Setup ViewPager2
        setupViewPager();

        // Setup BottomNavigationView
        setupBottomNavigationView();

        // Set default selected item
        bottomNavigationView.setSelectedItemId(R.id.navigation_input);

        // Setup Auth State Listener
        setupAuthStateListener();

        // Set initial state of custom tabs
        updateCustomTabsState(viewPager.getCurrentItem());

        // Khởi tạo DataManager và load dữ liệu
        DataManager dataManager = DataManager.getInstance();
        dataManager.setDataLoadedListener(new DataManager.OnDataLoadedListener() {
            @Override
            public void onDataLoaded() {
                runOnUiThread(() -> {
                    try {
                        // Khởi tạo các fragment và UI khác
                        setupUI();
                    } catch (Exception e) {
                        Log.e(TAG, "Error in onDataLoaded", e);
                        Toast.makeText(MainActivity.this, "Lỗi khi khởi tạo giao diện", Toast.LENGTH_SHORT).show();
                    }catch (NoClassDefFoundError e) {
                         Log.e(TAG, "NoClassDefFoundError in onDataLoaded", e);
                         Toast.makeText(MainActivity.this, "Lỗi tải class: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Lỗi khi tải dữ liệu: " + error, Toast.LENGTH_LONG).show();
                    // Chuyển về màn hình đăng nhập nếu có lỗi
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }
        });

        // Load dữ liệu
        dataManager.loadUserData();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authStateListener != null) {
            mAuth.removeAuthStateListener(authStateListener);
        }
    }

    private void setupAuthStateListener() {
        authStateListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user == null) {
                // User is signed out
                Toast.makeText(MainActivity.this, "User signed out", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                // User is signed in
                // Optional: Update UI based on signed-in user
                // Set default selected item if this is the first time loading after login
                 if (inputTabsContainer.getVisibility() != View.VISIBLE || fragmentManager.getFragments().isEmpty()) {
                     bottomNavigationView.setSelectedItemId(R.id.navigation_input);
                 }
            }
        };
    }

    private void setupViewPager() {
        // Use the TabPagerAdapter from the adapters package
        TabPagerAdapter tabPagerAdapter = new TabPagerAdapter(this); // Create adapter
        viewPager.setAdapter(tabPagerAdapter); // Set adapter

        // Add a listener to ViewPager2 to update BottomNavigationView and TabLayout visibility
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                 // Update BottomNavigationView selection
                 // Important: This must be done first to ensure correct tab is selected before checking its type
                 int bottomNavItemId = -1;
                 if (position == 0 || position == 1) bottomNavItemId = R.id.navigation_input;
                 else if (position == 2) bottomNavItemId = R.id.navigation_calendar;
                 else if (position == 3) bottomNavItemId = R.id.navigation_report;
                 else if (position == 4) bottomNavItemId = R.id.navigation_other;

                 if (bottomNavItemId != -1) {
                     // Remove listener temporarily to prevent infinite loop
                     bottomNavigationView.setOnNavigationItemSelectedListener(null);
                     bottomNavigationView.setSelectedItemId(bottomNavItemId);
                     setupBottomNavigationView(); // Re-attach listener
                 }

                 // Handle showing/hiding TabLayout based on current tab
                 if (position >= 0 && position < 2) { // Input tabs (Expense and Income)
                     showInputTabs(); // Ensure TabLayout is visible
                    // Cập nhật trạng thái tab ngay khi chuyển trang
                    updateCustomTabsState(position);
                 } else { // Other tabs (Calendar, Report, Other)
                     hideInputTabs(); // Ensure TabLayout is hidden
                 }

                 // When switching to Input tabs (Expense or Income) after selecting date from Calendar,
                 // ensure the selected date is set on the fragment
                 if (selectedDateForInputTabs != null) {
                     // We need to get the currently active fragment from the ViewPager
                     // Use findFragmentByTag with ViewPager2 fragment tag format: "f" + adapter.getItemId(position)
                     String fragmentTag = "f" + tabPagerAdapter.getItemId(position);
                     Fragment activeFragment = getSupportFragmentManager().findFragmentByTag(fragmentTag);

                     if (position == 0 && activeFragment instanceof ExpenseFragment) { // Expense tab
                         ((ExpenseFragment) activeFragment).setSelectedDate(selectedDateForInputTabs);
                     } else if (position == 1 && activeFragment instanceof IncomeFragment) { // Income tab
                         ((IncomeFragment) activeFragment).setSelectedDate(selectedDateForInputTabs);
                     }
                 }
            }
        });

         // Add click listeners for custom tabs
        tvTabExpense.setOnClickListener(v -> {
            viewPager.setCurrentItem(0, false); // Switch to Expense fragment
            updateCustomTabsState(0); // Cập nhật trạng thái tab ngay khi click
        });
        
        tvTabIncome.setOnClickListener(v -> {
            viewPager.setCurrentItem(1, false); // Switch to Income fragment
            updateCustomTabsState(1); // Cập nhật trạng thái tab ngay khi click
        });
    }

    private void setupBottomNavigationView() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            int targetViewPagerPosition = -1;

            if (itemId == R.id.navigation_input) {
                // When selecting input, default to the currently selected tab in TabLayout if available,
                // otherwise default to Expense (position 0). This ensures we return to the last viewed input tab.
                 targetViewPagerPosition = viewPager.getCurrentItem();
                 if (targetViewPagerPosition < 0 || targetViewPagerPosition > 1) {
                      targetViewPagerPosition = 0; // Default to Expense
                 }
                showInputTabs(); // Ensure TabLayout is visible
                updateCustomTabsState(targetViewPagerPosition); // Update the visual state of custom tabs
            } else if (itemId == R.id.navigation_calendar) {
                targetViewPagerPosition = 2; // Calendar is at position 2
                hideInputTabs(); // Ensure TabLayout is hidden
            } else if (itemId == R.id.navigation_report) {
                targetViewPagerPosition = 3; // Report is at position 3
                hideInputTabs(); // Ensure TabLayout is hidden
            } else if (itemId == R.id.navigation_other) {
                targetViewPagerPosition = 4; // Other is at position 4
                hideInputTabs(); // Ensure TabLayout is hidden
            }

            if (targetViewPagerPosition != -1) {
                // Sync ViewPager2 with BottomNavigationView selection
                // Use smoothScroll=false for instant switching
                viewPager.setCurrentItem(targetViewPagerPosition, false);

                 // If switching to input tabs and a date was selected from Calendar, update the date on the fragment
                 if (targetViewPagerPosition >= 0 && targetViewPagerPosition < 2 && selectedDateForInputTabs != null) {
                     // We need to get the currently active fragment from the ViewPager
                     // Use findFragmentByTag with ViewPager2 fragment tag format: "f" + adapter.getItemId(position)
                     String fragmentTag = "f" + viewPager.getAdapter().getItemId(targetViewPagerPosition);
                     Fragment activeFragment = getSupportFragmentManager().findFragmentByTag(fragmentTag);

                     if (targetViewPagerPosition == 0 && activeFragment instanceof ExpenseFragment) { // Expense tab
                         ((ExpenseFragment) activeFragment).setSelectedDate(selectedDateForInputTabs);
                     } else if (targetViewPagerPosition == 1 && activeFragment instanceof IncomeFragment) { // Income tab
                         ((IncomeFragment) activeFragment).setSelectedDate(selectedDateForInputTabs);
                     }
                 }

                return true; // Indicate that the selection has been handled
            }

            return false; // Indicate that the selection was not handled
        });
    }

    private void showInputTabs() {
        inputTabsContainer.setVisibility(View.VISIBLE);
    }

    private void hideInputTabs() {
        inputTabsContainer.setVisibility(View.GONE);
    }

    // Helper method to update the visual state of custom tabs
    private void updateCustomTabsState(int position) {
        if (position == 0) { // Expense tab is selected
            tvTabExpense.setTextColor(getResources().getColor(R.color.purple_700)); // Màu tím đậm cho tab đang chọn
            tvTabExpense.setTypeface(null, android.graphics.Typeface.BOLD); // Chữ đậm
            tvTabExpense.setBackgroundResource(R.drawable.tab_background_selected); // Thêm background cho tab đang chọn
            
            tvTabIncome.setTextColor(getResources().getColor(android.R.color.darker_gray)); // Màu xám cho tab không chọn
            tvTabIncome.setTypeface(null, android.graphics.Typeface.NORMAL); // Chữ thường
            tvTabIncome.setBackgroundResource(android.R.color.transparent); // Không có background
        } else if (position == 1) { // Income tab is selected
            tvTabExpense.setTextColor(getResources().getColor(android.R.color.darker_gray)); // Màu xám cho tab không chọn
            tvTabExpense.setTypeface(null, android.graphics.Typeface.NORMAL); // Chữ thường
            tvTabExpense.setBackgroundResource(android.R.color.transparent); // Không có background
            
            tvTabIncome.setTextColor(getResources().getColor(R.color.purple_700)); // Màu tím đậm cho tab đang chọn
            tvTabIncome.setTypeface(null, android.graphics.Typeface.BOLD); // Chữ đậm
            tvTabIncome.setBackgroundResource(R.drawable.tab_background_selected); // Thêm background cho tab đang chọn
        }
         // For other positions, the inputTabsContainer is hidden, so no need to update state
    }

    // Implement OnDaySelectedListener interface
    @Override
    public void onDaySelected(Date selectedDate) {
        this.selectedDateForInputTabs = selectedDate; // Store selected date
        // Set the selected item in BottomNavigationView, which will trigger tab switch via listener
        bottomNavigationView.setSelectedItemId(R.id.navigation_input);
        // The date will be set on the appropriate fragment by the BottomNavigationView listener after the tab switch
    }

    private void setupUI() {
        try {
            // Khởi tạo các fragment
            expenseFragment = new ExpenseFragment();
            incomeFragment = new IncomeFragment();

            // Cập nhật UI
            updateCustomTabsState(viewPager.getCurrentItem());
        } catch (Exception e) {
            Log.e(TAG, "Error setting up UI", e);
            throw e; // Ném lỗi để xử lý ở trên
        } catch (NoClassDefFoundError e) {
            Log.e(TAG, "NoClassDefFoundError setting up UI", e);
            Toast.makeText(this, "Lỗi tải class: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void scheduleDailyReminder() {
        // Tạo thời gian 9h tối
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 21);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        
        // Nếu thời gian đã qua, lên lịch cho ngày hôm sau
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        // Tạo WorkRequest
        PeriodicWorkRequest reminderWork = 
            new PeriodicWorkRequest.Builder(DailyReminderWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(calendar.getTimeInMillis() - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .build();
                
        // Lên lịch
        WorkManager.getInstance(this).enqueue(reminderWork);
    }

    private void scheduleWeeklySummary() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 20);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        
        // Nếu thời gian đã qua, lên lịch cho tuần sau
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
        }
        
        // Tạo WorkRequest
        PeriodicWorkRequest weeklyWork = 
            new PeriodicWorkRequest.Builder(WeeklySummaryWorker.class, 7, TimeUnit.DAYS)
                .setInitialDelay(calendar.getTimeInMillis() - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .build();
                
        // Lên lịch
        WorkManager.getInstance(this).enqueue(weeklyWork);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Đảm bảo dừng spam khi Activity bị hủy (nếu có)
        // stopSpamming();
    }

    // Phương thức để hiển thị thông báo khi có giao dịch mới
    public void showTransactionNotification(Transaction transaction) {
        if (transaction.getType().equals("income")) {
            notificationHelper.showNewIncomeNotification(transaction);
        } else if (transaction.getAmount() > 1000000) { // Nếu chi tiêu lớn hơn 1 triệu
            notificationHelper.showUnusualExpenseNotification(transaction);
        }
    }

    public void navigateToCalendarWithDate(Date date) {
        if (isNavigatingToCalendar) return;
        isNavigatingToCalendar = true;
        // Switch to calendar tab (index 2)
        viewPager.setCurrentItem(2, true);
        // Đợi một chút để đảm bảo CalendarFragment đã được khởi tạo
        viewPager.post(() -> {
            // Update the calendar fragment to show the selected month
            String fragmentTag = "f" + viewPager.getAdapter().getItemId(2);
            Fragment calendarFragment = getSupportFragmentManager().findFragmentByTag(fragmentTag);
            if (calendarFragment instanceof CalendarFragment) {
                ((CalendarFragment) calendarFragment).setSelectedDate(date);
            }
            isNavigatingToCalendar = false;
        });
        // Hide input tabs when switching to calendar
        hideInputTabs();
    }
}