package com.example.personalspendingapp.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Typeface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personalspendingapp.R;
import com.example.personalspendingapp.adapters.CalendarDayAdapter;
import com.example.personalspendingapp.adapters.CategoryAdapter;
import com.example.personalspendingapp.data.DataManager;
import com.example.personalspendingapp.models.CalendarDay;
import com.example.personalspendingapp.models.Category;
import com.example.personalspendingapp.models.Transaction;

import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarFragment extends Fragment implements CalendarDayAdapter.OnDayDoubleClickListener {

    private static final String TAG = "CalendarFragment";

    private TextView tvSelectedMonth;
    private ImageButton btnPreviousMonth, btnNextMonth;
    private RecyclerView rvCalendarGrid;
    private TextView tvMonthlyIncome, tvMonthlyExpense, tvMonthlyBalance;

    private LinearLayout dailyTransactionsContainer;

    private Calendar currentMonthCalendar;
    private CalendarDayAdapter calendarDayAdapter;
    private List<CalendarDay> calendarDays;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    private OnDaySelectedListener onDaySelectedListener;

    private Category selectedCategory;

    public interface OnDaySelectedListener {
        void onDaySelected(Date selectedDate);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnDaySelectedListener) {
            onDaySelectedListener = (OnDaySelectedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnDaySelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onDaySelectedListener = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        initViews(view);
        setupMonthNavigation();
        setupDataChangeListener();
        loadMonthlyData();
        return view;
    }

    private void initViews(View view) {
        tvSelectedMonth = view.findViewById(R.id.tvSelectedMonth);
        btnPreviousMonth = view.findViewById(R.id.btnPreviousMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);
        rvCalendarGrid = view.findViewById(R.id.rvCalendarGrid);
        tvMonthlyIncome = view.findViewById(R.id.tvMonthlyIncome);
        tvMonthlyExpense = view.findViewById(R.id.tvMonthlyExpense);
        tvMonthlyBalance = view.findViewById(R.id.tvMonthlyBalance);
        dailyTransactionsContainer = view.findViewById(R.id.dailyTransactionsContainer);

        currentMonthCalendar = Calendar.getInstance();
        calendarDays = new ArrayList<>();
        calendarDayAdapter = new CalendarDayAdapter(calendarDays, this, currencyFormat);

        rvCalendarGrid.setLayoutManager(new GridLayoutManager(getContext(), 7));
        rvCalendarGrid.setAdapter(calendarDayAdapter);
    }

    private void setupMonthNavigation() {
        updateMonthDisplay();

        btnPreviousMonth.setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, -1);
            updateMonthDisplay();
            loadMonthlyData();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, 1);
            updateMonthDisplay();
            loadMonthlyData();
        });
    }

    private void updateMonthDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", new Locale("vi", "VN"));
        tvSelectedMonth.setText(sdf.format(currentMonthCalendar.getTime()));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Tải lại dữ liệu mỗi khi fragment được hiển thị
        loadMonthlyData();
    }

    private void loadMonthlyData() {
        DataManager dataManager = DataManager.getInstance();
        if (!dataManager.isDataLoaded()) {
            Toast.makeText(getContext(), "Đang tải dữ liệu, vui lòng đợi...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lấy ngày đầu tiên và cuối cùng của tháng hiện tại
        Calendar calendar = (Calendar) currentMonthCalendar.clone();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startDate = calendar.getTime();

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        Date endDate = calendar.getTime();

        // Lấy tất cả giao dịch trong khoảng thời gian
        List<Transaction> monthlyTransactions = dataManager.getTransactionsByDateRange(startDate, endDate);
        Log.d(TAG, "Loaded " + monthlyTransactions.size() + " transactions for " +
            currentMonthCalendar.get(Calendar.MONTH) + 1 + "/" + currentMonthCalendar.get(Calendar.YEAR));

        // Cập nhật lịch với dữ liệu mới
        updateCalendarGrid(monthlyTransactions);
        calculateAndDisplayMonthlySummary(monthlyTransactions);
        displayDailyTransactions(monthlyTransactions);
    }

    @Override
    public void onDayDoubleClick(CalendarDay day) {
        if (onDaySelectedListener != null && day.getDate() != null) {
            onDaySelectedListener.onDaySelected(day.getDate());
        }
    }

    private void updateCalendarGrid(List<Transaction> transactions) {
        calendarDays.clear();

        Calendar calendar = (Calendar) currentMonthCalendar.clone();
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        // Add empty days for the beginning of the week if the month doesn't start on Monday
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int daysToAddBefore = (firstDayOfWeek == Calendar.SUNDAY) ? 6 : firstDayOfWeek - 2;

        for (int i = 0; i < daysToAddBefore; i++) {
            calendarDays.add(new CalendarDay(0, null, 0.0, 0.0));
        }

        // Tạo map để lưu tổng thu chi cho mỗi ngày
        Map<Integer, Double> dailyIncome = new HashMap<>();
        Map<Integer, Double> dailyExpense = new HashMap<>();

        // Tính toán tổng thu chi cho mỗi ngày
        for (Transaction transaction : transactions) {
            Calendar transCal = Calendar.getInstance();
            transCal.setTime(transaction.getDate());
            int dayOfMonth = transCal.get(Calendar.DAY_OF_MONTH);

            if ("income".equals(transaction.getType())) {
                dailyIncome.merge(dayOfMonth, transaction.getAmount(), Double::sum);
            } else if ("expense".equals(transaction.getType())) {
                dailyExpense.merge(dayOfMonth, transaction.getAmount(), Double::sum);
            }
        }

        // Add days of the month
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 1; i <= daysInMonth; i++) {
            Calendar dayCalendar = (Calendar) calendar.clone();
            dayCalendar.set(Calendar.DAY_OF_MONTH, i);
            Date dayDate = dayCalendar.getTime();

            double income = dailyIncome.getOrDefault(i, 0.0);
            double expense = dailyExpense.getOrDefault(i, 0.0);

            calendarDays.add(new CalendarDay(i, dayDate, income, expense));
        }

        // Add empty days for the end of the week if needed
        int totalCells = calendarDays.size();
        int remainingCells = 7 - (totalCells % 7);
        if (remainingCells > 0 && remainingCells < 7) {
            for (int i = 0; i < remainingCells; i++) {
                calendarDays.add(new CalendarDay(0, null, 0.0, 0.0));
            }
        }

        calendarDayAdapter.updateData(calendarDays, currencyFormat);
    }

    private void calculateAndDisplayMonthlySummary(List<Transaction> transactions) {
        double totalMonthlyIncome = transactions.stream().filter(t -> "income".equals(t.getType())).mapToDouble(Transaction::getAmount).sum();
        double totalMonthlyExpense = transactions.stream().filter(t -> "expense".equals(t.getType())).mapToDouble(Transaction::getAmount).sum();
        double monthlyBalance = totalMonthlyIncome - totalMonthlyExpense;

        tvMonthlyIncome.setText(currencyFormat.format(totalMonthlyIncome));
        tvMonthlyExpense.setText(currencyFormat.format(totalMonthlyExpense));
        tvMonthlyBalance.setText(currencyFormat.format(monthlyBalance));
    }

    private void displayDailyTransactions(List<Transaction> transactions) {
        dailyTransactionsContainer.removeAllViews(); // Xóa các view cũ

        if (transactions == null || transactions.isEmpty()) {
            return;
        }

        // Nhóm giao dịch theo ngày
        Map<String, List<Transaction>> transactionsByDay = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        for (Transaction transaction : transactions) {
            String dateKey = sdf.format(transaction.getDate());
            if (!transactionsByDay.containsKey(dateKey)) {
                transactionsByDay.put(dateKey, new ArrayList<>());
            }
            transactionsByDay.get(dateKey).add(transaction);
        }

        // Sort keys (dates) in ascending order
        List<String> sortedDates = new ArrayList<>(transactionsByDay.keySet());
        Collections.sort(sortedDates, (date1, date2) -> {
            try {
                Date d1 = sdf.parse(date1);
                Date d2 = sdf.parse(date2);
                return d1.compareTo(d2);
            } catch (ParseException e) {
                e.printStackTrace();
                return 0;
            }
        });

        // Hiển thị giao dịch cho từng ngày
        for (String dateKey : sortedDates) {
            List<Transaction> dailyList = transactionsByDay.get(dateKey);

            // Header cho ngày
            TextView dateHeader = new TextView(getContext());
            dateHeader.setText(dateKey);
            dateHeader.setTextSize(16);
            dateHeader.setTypeface(null, android.graphics.Typeface.BOLD);
            dateHeader.setPadding(16, 16, 16, 8);
            dailyTransactionsContainer.addView(dateHeader);

            // Hiển thị từng giao dịch
            for (Transaction transaction : dailyList) {
                // Container cho mỗi giao dịch
                LinearLayout transactionLayout = new LinearLayout(getContext());
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(0, 0, 0, 8);
                transactionLayout.setLayoutParams(layoutParams);
                transactionLayout.setOrientation(LinearLayout.VERTICAL);
                transactionLayout.setPadding(16, 12, 16, 12);
                transactionLayout.setBackgroundResource(R.drawable.transaction_item_background);

                // Layout cho tên danh mục và số tiền
                LinearLayout categoryAmountLayout = new LinearLayout(getContext());
                categoryAmountLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                categoryAmountLayout.setOrientation(LinearLayout.HORIZONTAL);

                // TextView cho tên danh mục
                TextView tvCategoryName = new TextView(getContext());
                tvCategoryName.setLayoutParams(new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.0f
                ));

                // Lấy tên danh mục từ DataManager
                String categoryName = "Unknown Category";
                Category category = DataManager.getInstance().getCategoryById(transaction.getCategoryId(), transaction.getType());
                if (category != null) {
                    categoryName = category.getName();
                }
                tvCategoryName.setText(categoryName);

                // TextView cho số tiền
                TextView tvAmount = new TextView(getContext());
                tvAmount.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                tvAmount.setText(currencyFormat.format(transaction.getAmount()));
                tvAmount.setTextColor(getResources().getColor(
                    transaction.getType().equals("income") ? R.color.green_500 : R.color.red_500
                ));

                // Thêm các view vào layout
                categoryAmountLayout.addView(tvCategoryName);
                categoryAmountLayout.addView(tvAmount);
                transactionLayout.addView(categoryAmountLayout);

                // Thêm ghi chú nếu có
                if (transaction.getNote() != null && !transaction.getNote().isEmpty()) {
                    TextView tvNote = new TextView(getContext());
                    tvNote.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    tvNote.setText(transaction.getNote());
                    tvNote.setTextSize(12);
                    tvNote.setTextColor(ContextCompat.getColor(getContext(), android.R.color.darker_gray));
                    tvNote.setPadding(0, 4, 0, 0);
                    transactionLayout.addView(tvNote);
                }

                // Thêm nút sửa và xóa
                LinearLayout actionLayout = new LinearLayout(getContext());
                actionLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                actionLayout.setOrientation(LinearLayout.HORIZONTAL);
                actionLayout.setGravity(android.view.Gravity.END);

                // Nút sửa
                ImageButton btnEdit = new ImageButton(getContext());
                btnEdit.setImageResource(R.drawable.ic_edit);
                btnEdit.setBackgroundResource(android.R.color.transparent);
                btnEdit.setPadding(8, 8, 8, 8);
                btnEdit.setOnClickListener(v -> showEditTransactionDialog(transaction));

                // Nút xóa
                ImageButton btnDelete = new ImageButton(getContext());
                btnDelete.setImageResource(R.drawable.ic_delete);
                btnDelete.setBackgroundResource(android.R.color.transparent);
                btnDelete.setPadding(8, 8, 8, 8);
                btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog(transaction));

                actionLayout.addView(btnEdit);
                actionLayout.addView(btnDelete);
                transactionLayout.addView(actionLayout);

                // Thêm sự kiện click vào giao dịch
                transactionLayout.setOnClickListener(v -> showTransactionDetails(transaction));

                dailyTransactionsContainer.addView(transactionLayout);
            }
        }
    }

    private void showEditTransactionDialog(Transaction transaction) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_transaction, null);
        androidx.appcompat.widget.AppCompatEditText etAmount = dialogView.findViewById(R.id.etAmount);
        androidx.appcompat.widget.AppCompatEditText etNote = dialogView.findViewById(R.id.etNote);
        RecyclerView rvCategories = dialogView.findViewById(R.id.rvCategories);

        // Set current values
        etAmount.setText(String.valueOf(transaction.getAmount()));
        etNote.setText(transaction.getNote());

        // Setup category selection
        List<Category> categories = DataManager.getInstance().getCategoriesByType(transaction.getType());
        CategoryAdapter categoryAdapter = new CategoryAdapter(categories, new CategoryAdapter.OnCategoryClickListener() {
            @Override
            public void onCategoryClick(Category category) {
                selectedCategory = category;
            }

            @Override
            public void onOtherClick() {
                // Không cho phép thêm danh mục mới khi sửa
            }
        });
        rvCategories.setLayoutManager(new GridLayoutManager(getContext(), 3));
        rvCategories.setAdapter(categoryAdapter);

        // Set selected category
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId().equals(transaction.getCategoryId())) {
                categoryAdapter.setSelectedPosition(i);
                selectedCategory = categories.get(i);
                break;
            }
        }

        new AlertDialog.Builder(requireContext())
            .setTitle("Sửa giao dịch")
            .setView(dialogView)
            .setPositiveButton("Lưu", (dialog, which) -> {
                String amountStr = etAmount.getText().toString().trim();
                String note = etNote.getText().toString().trim();

                if (amountStr.isEmpty()) {
                    Toast.makeText(getContext(), "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (selectedCategory == null) {
                    Toast.makeText(getContext(), "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show();
                    return;
                }

                double amount = Double.parseDouble(amountStr);
                transaction.setAmount(amount);
                transaction.setNote(note);
                transaction.setCategoryId(selectedCategory.getId());

                DataManager.getInstance().updateTransaction(transaction);
                loadMonthlyData(); // Reload data to reflect changes
                Toast.makeText(getContext(), "Đã cập nhật giao dịch", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void showDeleteConfirmationDialog(Transaction transaction) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Xóa giao dịch")
            .setMessage("Bạn có chắc chắn muốn xóa giao dịch này?")
            .setPositiveButton("Xóa", (dialog, which) -> {
                Log.d(TAG, "Deleting transaction: " + transaction.getId());
                DataManager.getInstance().deleteTransaction(transaction.getId());
                Log.d(TAG, "Reloading monthly data after deletion");
                loadMonthlyData(); // Reload data to reflect changes
                Toast.makeText(getContext(), "Đã xóa giao dịch", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void showTransactionDetails(Transaction transaction) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_transaction_details, null);
        TextView tvAmount = dialogView.findViewById(R.id.tvAmount);
        TextView tvCategory = dialogView.findViewById(R.id.tvCategory);
        TextView tvDate = dialogView.findViewById(R.id.tvDate);
        TextView tvNote = dialogView.findViewById(R.id.tvNote);

        // Set values
        tvAmount.setText(currencyFormat.format(transaction.getAmount()));
        tvAmount.setTextColor(getResources().getColor(
            transaction.getType().equals("income") ? R.color.green_500 : R.color.red_500
        ));

        Category category = DataManager.getInstance().getCategoryById(transaction.getCategoryId(), transaction.getType());
        tvCategory.setText(category != null ? category.getName() : "Unknown Category");

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(transaction.getDate()));

        if (transaction.getNote() != null && !transaction.getNote().isEmpty()) {
            tvNote.setText(transaction.getNote());
            tvNote.setVisibility(View.VISIBLE);
        } else {
            tvNote.setVisibility(View.GONE);
        }

        new AlertDialog.Builder(requireContext())
            .setTitle(transaction.getType().equals("income") ? "Chi tiết khoản thu" : "Chi tiết khoản chi")
            .setView(dialogView)
            .setPositiveButton("Đóng", null)
            .show();
    }

    private void setupDataChangeListener() {
        Log.d(TAG, "Setting up data change listener");
        DataManager.getInstance().setDataChangeListener(() -> {
            Log.d(TAG, "Data changed, reloading monthly data");
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    loadMonthlyData();
                    // Cập nhật lại adapter
                    if (calendarDayAdapter != null) {
                        calendarDayAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        DataManager.getInstance().setDataChangeListener(null);
    }
} 