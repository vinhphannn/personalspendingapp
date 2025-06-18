package com.example.personalspendingapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personalspendingapp.R;
import com.example.personalspendingapp.data.DataManager;
import com.example.personalspendingapp.adapters.CategoryDetailAdapter;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

public class ReportFragment extends Fragment {

    private TextView tvReportPeriod;
    private TextView tvReportTotalExpense;
    private TextView tvReportTotalIncome;
    private TextView tvReportBalance;
    private com.google.android.material.button.MaterialButtonToggleGroup toggleTimePeriod;
    private com.google.android.material.button.MaterialButton btnToggleMonth;
    private com.google.android.material.button.MaterialButton btnToggleYear;
    private com.google.android.material.tabs.TabLayout tabLayoutReport;
    private ImageView btnPreviousPeriod;
    private ImageView btnNextPeriod;
    private PieChart pieChartReport;
    private RecyclerView rvCategoryDetails;
    private CategoryDetailAdapter categoryDetailAdapter;
    // TODO: Add RecyclerView

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private Calendar currentPeriodCalendar;
    private boolean isMonthlyReport = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report, container, false);
        initViews(view);
        setupTimePeriodToggle();
        setupDateNavigationButtons();
        setupTabLayout();
        currentPeriodCalendar = Calendar.getInstance();
        loadReportData(); // Load initial data (e.g., for the current month)
        return view;
    }

    private void initViews(View view) {
        tvReportPeriod = view.findViewById(R.id.tvReportPeriod);
        tvReportTotalExpense = view.findViewById(R.id.tvReportTotalExpense);
        tvReportTotalIncome = view.findViewById(R.id.tvReportTotalIncome);
        tvReportBalance = view.findViewById(R.id.tvReportBalance);
        toggleTimePeriod = view.findViewById(R.id.toggleTimePeriod);
        btnToggleMonth = view.findViewById(R.id.btnToggleMonth);
        btnToggleYear = view.findViewById(R.id.btnToggleYear);
        tabLayoutReport = view.findViewById(R.id.tabLayoutReport);
        btnPreviousPeriod = view.findViewById(R.id.btnPreviousPeriod);
        btnNextPeriod = view.findViewById(R.id.btnNextPeriod);
        pieChartReport = view.findViewById(R.id.pieChartReport);
        rvCategoryDetails = view.findViewById(R.id.rvCategoryDetails);
        rvCategoryDetails.setLayoutManager(new LinearLayoutManager(getContext()));
        // TODO: Initialize RecyclerView
    }

    private void setupTimePeriodToggle() {
        // Set default selection to Monthly
        toggleTimePeriod.check(R.id.btnToggleMonth);

        toggleTimePeriod.addOnButtonCheckedListener((toggleButton, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnToggleMonth) {
                    isMonthlyReport = true;
                } else if (checkedId == R.id.btnToggleYear) {
                    isMonthlyReport = false;
                }
                // Reload data when the time period type (Month/Year) changes
                loadReportData();
            }
        });
    }

    private void setupDateNavigationButtons() {
        btnPreviousPeriod.setOnClickListener(v -> {
            if (isMonthlyReport) {
                currentPeriodCalendar.add(Calendar.MONTH, -1);
            } else {
                currentPeriodCalendar.add(Calendar.YEAR, -1);
            }
            loadReportData();
        });

        btnNextPeriod.setOnClickListener(v -> {
            if (isMonthlyReport) {
                currentPeriodCalendar.add(Calendar.MONTH, 1);
            } else {
                currentPeriodCalendar.add(Calendar.YEAR, 1);
            }
            loadReportData();
        });
    }

    private void loadReportData() {
        // TODO: Implement logic to load data based on selected time period (Month/Year) and tab (Expense/Income)
        DataManager dataManager = DataManager.getInstance();

        if (!dataManager.isDataLoaded()) {
            // Handle case where data is not loaded yet
            // Maybe show a loading indicator or a message
            return;
        }

        Date startDate, endDate;
        SimpleDateFormat periodFormat;

        if (isMonthlyReport) {
            // Calculate start and end dates for the current month
            Calendar calendar = (Calendar) currentPeriodCalendar.clone();
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            startDate = calendar.getTime();

            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
            endDate = calendar.getTime();

            // Format the period text for month
            SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", new Locale("vi", "VN"));
            SimpleDateFormat startDayMonthFormat = new SimpleDateFormat("dd/MM", new Locale("vi", "VN"));
            SimpleDateFormat endDayMonthFormat = new SimpleDateFormat("dd/MM", new Locale("vi", "VN"));
            tvReportPeriod.setText(monthYearFormat.format(currentPeriodCalendar.getTime()) + " (" + startDayMonthFormat.format(startDate) + " – " + endDayMonthFormat.format(endDate) + ")");

        } else { // Yearly Report
            // Calculate start and end dates for the current year
            Calendar calendar = (Calendar) currentPeriodCalendar.clone();
            calendar.set(Calendar.DAY_OF_YEAR, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            startDate = calendar.getTime();

            calendar.set(Calendar.DAY_OF_YEAR, calendar.getActualMaximum(Calendar.DAY_OF_YEAR));
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
            endDate = calendar.getTime();

            // Format the period text for year
            periodFormat = new SimpleDateFormat("yyyy", new Locale("vi", "VN"));
            tvReportPeriod.setText(periodFormat.format(currentPeriodCalendar.getTime()));
        }

        // Get transactions for the calculated period
        List<com.example.personalspendingapp.models.Transaction> transactions = dataManager.getTransactionsByDateRange(startDate, endDate);

        // Calculate summary
        double totalExpense = transactions.stream()
                .filter(t -> "expense".equals(t.getType()))
                .mapToDouble(com.example.personalspendingapp.models.Transaction::getAmount)
                .sum();
        double totalIncome = transactions.stream()
                .filter(t -> "income".equals(t.getType()))
                .mapToDouble(com.example.personalspendingapp.models.Transaction::getAmount)
                .sum();
        double balance = totalIncome - totalExpense;

        // Display summary data
        tvReportTotalExpense.setText(currencyFormat.format(totalExpense));
        tvReportTotalIncome.setText(currencyFormat.format(totalIncome));
        tvReportBalance.setText(currencyFormat.format(balance));

        // Update chart and detailed list based on filtered transactions
        updateChartAndDetailedList(transactions);
    }

    private void setupTabLayout() {
        tabLayoutReport.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                // Khi tab được chọn thay đổi, load lại dữ liệu cho biểu đồ và danh sách chi tiết
                updateChartAndDetailedList(DataManager.getInstance().getTransactionsByDateRange(getStartDate(), getEndDate()));
            }

            @Override
            public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {
                // Do nothing
            }

            @Override
            public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {
                // Do nothing
            }
        });
    }

    private void updateChartAndDetailedList(List<com.example.personalspendingapp.models.Transaction> transactions) {
        String selectedTab = tabLayoutReport.getTabAt(tabLayoutReport.getSelectedTabPosition()).getText().toString();
        boolean isExpense = "Chi tiêu".equals(selectedTab);

        // Hiển thị biểu đồ và danh sách cho cả hai tab
        pieChartReport.setVisibility(View.VISIBLE);
        rvCategoryDetails.setVisibility(View.VISIBLE);

        // Lọc giao dịch theo loại
        List<com.example.personalspendingapp.models.Transaction> filteredTransactions = transactions.stream()
                .filter(t -> (isExpense ? "expense" : "income").equals(t.getType()))
                .collect(java.util.stream.Collectors.toList());

        // Tính tổng theo danh mục
        Map<String, Double> amountByCategory = new HashMap<>();
        double totalAmount = 0;
        for (com.example.personalspendingapp.models.Transaction transaction : filteredTransactions) {
            String categoryId = transaction.getCategoryId();
            com.example.personalspendingapp.models.Category category = DataManager.getInstance().getCategoryById(categoryId, transaction.getType());
            if (category != null) {
                String categoryName = category.getName();
                double currentTotal = amountByCategory.getOrDefault(categoryName, 0.0);
                double newTotal = currentTotal + transaction.getAmount();
                amountByCategory.put(categoryName, newTotal);
                totalAmount += transaction.getAmount();
            }
        }

        // Tạo PieEntries từ tổng theo danh mục
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : amountByCategory.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        // Tạo PieDataSet
        PieDataSet dataSet = new PieDataSet(entries, isExpense ? "Chi tiêu theo danh mục" : "Thu nhập theo danh mục");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(android.R.color.black);
        dataSet.setValueTextSize(12f);

        // Tạo PieData
        PieData data = new PieData(dataSet);
        pieChartReport.setData(data);

        // Cấu hình PieChart
        pieChartReport.getDescription().setEnabled(false);
        pieChartReport.animateY(1000);
        pieChartReport.invalidate();

        // Cập nhật RecyclerView với chi tiết danh mục
        List<Map.Entry<String, Double>> sortedCategories = new ArrayList<>(amountByCategory.entrySet());
        sortedCategories.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue())); // Sắp xếp theo số tiền giảm dần
        categoryDetailAdapter = new CategoryDetailAdapter(requireContext(), sortedCategories, totalAmount, ColorTemplate.MATERIAL_COLORS);
        rvCategoryDetails.setAdapter(categoryDetailAdapter);
    }

    // Helper methods to get start and end dates based on currentPeriodCalendar and isMonthlyReport
    private Date getStartDate() {
        Calendar calendar = (Calendar) currentPeriodCalendar.clone();
        if (isMonthlyReport) {
            calendar.set(Calendar.DAY_OF_MONTH, 1);
        } else {
            calendar.set(Calendar.DAY_OF_YEAR, 1);
        }
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date getEndDate() {
        Calendar calendar = (Calendar) currentPeriodCalendar.clone();
        if (isMonthlyReport) {
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        } else {
            calendar.set(Calendar.DAY_OF_YEAR, calendar.getActualMaximum(Calendar.DAY_OF_YEAR));
        }
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }
} 