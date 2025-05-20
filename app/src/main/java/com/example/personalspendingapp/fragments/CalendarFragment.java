package com.example.personalspendingapp.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personalspendingapp.R;
import com.example.personalspendingapp.adapters.CalendarDayAdapter;
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
            // Hiển thị thông báo hoặc ẩn container nếu không có giao dịch
            // dailyTransactionsContainer.setVisibility(View.GONE);
            return;
        }
        // dailyTransactionsContainer.setVisibility(View.VISIBLE);

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
            if (dailyList == null || dailyList.isEmpty()) continue;

            // Tính tổng thu chi và số dư cho ngày
            double dailyIncome = 0;
            double dailyExpense = 0;
            for (Transaction transaction : dailyList) {
                if ("income".equals(transaction.getType())) {
                    dailyIncome += transaction.getAmount();
                } else if ("expense".equals(transaction.getType())) {
                    dailyExpense += transaction.getAmount();
                }
            }
            double dailyBalance = dailyIncome - dailyExpense;

            // Thêm header ngày
            LinearLayout dateHeaderLayout = new LinearLayout(getContext());
            LinearLayout.LayoutParams headerLayoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            headerLayoutParams.setMargins(0, 0, 0, 8); // Thêm margin dưới cho header
            dateHeaderLayout.setLayoutParams(headerLayoutParams);
            dateHeaderLayout.setOrientation(LinearLayout.HORIZONTAL);
            dateHeaderLayout.setPadding(16, 12, 16, 12); // Thêm padding cho header
            dateHeaderLayout.setBackgroundResource(R.drawable.daily_header_background); // Áp dụng background mới

            // TextView cho ngày tháng
            TextView tvHeaderDate = new TextView(getContext());
            tvHeaderDate.setLayoutParams(new LinearLayout.LayoutParams(
                    0, // width
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f // weight
                    ));
            SimpleDateFormat dayMonthYearFormat = new SimpleDateFormat("dd/MM/yyyy (E)", new Locale("vi", "VN")); // Thêm thứ vào ngày
             // Tìm ngày thực tế từ danh sách dailyList, nếu dailyList không rỗng
            Date actualDate = null;
            if (!dailyList.isEmpty()) {
                 actualDate = dailyList.get(0).getDate();
            }
            if(actualDate != null) {
                tvHeaderDate.setText(dayMonthYearFormat.format(actualDate));
            } else {
                 tvHeaderDate.setText(dateKey + " (Không rõ ngày)"); // Fallback nếu không lấy được ngày
            }

            tvHeaderDate.setTextSize(14); // Font nhỏ hơn
            tvHeaderDate.setTypeface(null, android.graphics.Typeface.BOLD);
            tvHeaderDate.setGravity(android.view.Gravity.START); // Căn trái

            // TextView cho số dư còn lại
            TextView tvHeaderBalance = new TextView(getContext());
            tvHeaderBalance.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            tvHeaderBalance.setText(currencyFormat.format(dailyBalance)); // Chỉ hiển thị số dư, không ngoặc
            tvHeaderBalance.setTextSize(14); // Font nhỏ hơn
            tvHeaderBalance.setTypeface(null, android.graphics.Typeface.BOLD);
            tvHeaderBalance.setGravity(android.view.Gravity.END); // Căn phải

            // Thêm TextView vào layout header
            dateHeaderLayout.addView(tvHeaderDate);
            dateHeaderLayout.addView(tvHeaderBalance);

            // Thêm layout header vào container chính
            dailyTransactionsContainer.addView(dateHeaderLayout);

            // Thêm các giao dịch trong ngày
            for (Transaction transaction : dailyList) {
                // Container cho mỗi giao dịch
                LinearLayout transactionLayout = new LinearLayout(getContext());
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(0, 0, 0, 8); // Thêm margin dưới để tạo khoảng cách giữa các giao dịch
                transactionLayout.setLayoutParams(layoutParams);
                transactionLayout.setOrientation(LinearLayout.VERTICAL);
                transactionLayout.setPadding(16, 12, 16, 12); // Thêm padding bên trong item
                transactionLayout.setBackgroundResource(R.drawable.transaction_item_background); // Thêm background xám nhạt và bo góc

                // Layout cho tên danh mục (căn trái) và số tiền (căn phải)
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
                        1.0f // Dùng weight để tên danh mục chiếm phần lớn không gian
                ));
                // Lấy tên danh mục từ DataManager dựa trên categoryId
                String categoryName = "Unknown Category"; // Default value
                Category category = DataManager.getInstance().getCategoryById(transaction.getCategoryId(), transaction.getType());
                if (category != null) {
                    categoryName = category.getName();
                }
                tvCategoryName.setText(categoryName);
                tvCategoryName.setTextSize(14); // Font nhỏ hơn
                tvCategoryName.setTypeface(null, android.graphics.Typeface.BOLD);
                tvCategoryName.setTextColor(getContext().getResources().getColor(android.R.color.black)); // Màu chữ đen

                // TextView cho số tiền
                TextView tvAmount = new TextView(getContext());
                tvAmount.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                tvAmount.setText(currencyFormat.format(transaction.getAmount()));
                tvAmount.setTextSize(14); // Font nhỏ hơn
                tvAmount.setTypeface(null, android.graphics.Typeface.BOLD);
                // Đặt màu chữ dựa trên loại giao dịch (thu/chi)
                if ("income".equals(transaction.getType())) {
                    tvAmount.setTextColor(getContext().getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    tvAmount.setTextColor(getContext().getResources().getColor(android.R.color.holo_red_dark));
                }
                tvAmount.setGravity(android.view.Gravity.END); // Căn phải

                categoryAmountLayout.addView(tvCategoryName);
                categoryAmountLayout.addView(tvAmount);

                transactionLayout.addView(categoryAmountLayout);

                // TextView cho ghi chú (nếu có)
                if (transaction.getNote() != null && !transaction.getNote().trim().isEmpty()) {
                    TextView tvNote = new TextView(getContext());
                    tvNote.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    tvNote.setText("(" + transaction.getNote() + ")"); // Thêm ngoặc
                    tvNote.setTextSize(12); // Font nhỏ hơn cho ghi chú
                    tvNote.setTextColor(getContext().getResources().getColor(android.R.color.darker_gray)); // Màu xám cho ghi chú
                    tvNote.setPadding(0, 4, 0, 0); // Padding top
                    transactionLayout.addView(tvNote);
                }

                dailyTransactionsContainer.addView(transactionLayout);
            }
        }
    }
} 