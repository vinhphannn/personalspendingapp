package com.example.personalspendingapp.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personalspendingapp.R;
import com.example.personalspendingapp.adapters.CalendarDayAdapter;
import com.example.personalspendingapp.models.CalendarDay;
import com.example.personalspendingapp.models.Expense; // Import Expense model
import com.example.personalspendingapp.models.Income; // Import Income model

import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat; // Import NumberFormat
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class CalendarFragment extends Fragment implements CalendarDayAdapter.OnDayDoubleClickListener {

    private static final String TAG = "CalendarFragment";

    private TextView tvSelectedMonth;
    private ImageButton btnPreviousMonth, btnNextMonth;
    private RecyclerView rvCalendarGrid;
    private TextView tvMonthlyIncome, tvMonthlyExpense, tvMonthlyBalance;

    private Calendar currentMonthCalendar;
    private CalendarDayAdapter calendarDayAdapter;
    private List<CalendarDay> calendarDays;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN")); // Use standard currency format for Vietnam

    // Firebase instances
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private OnDaySelectedListener onDaySelectedListener; // Callback to Activity

    // Interface for callback to the hosting Activity
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
        setupFirebase();
        setupMonthNavigation();
        loadMonthlyData(); // Load data for the initial month
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

        currentMonthCalendar = Calendar.getInstance();
        calendarDays = new ArrayList<>();
        calendarDayAdapter = new CalendarDayAdapter(calendarDays, this, currencyFormat); // Pass currencyFormat

        rvCalendarGrid.setLayoutManager(new GridLayoutManager(getContext(), 7));
        rvCalendarGrid.setAdapter(calendarDayAdapter);
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private void setupMonthNavigation() {
        updateMonthDisplay();

        btnPreviousMonth.setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, -1);
            updateMonthDisplay();
            loadMonthlyData(); // Load data for the new month
        });

        btnNextMonth.setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, 1);
            updateMonthDisplay();
            loadMonthlyData(); // Load data for the new month
        });
    }

    private void updateMonthDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", new Locale("vi", "VN")); // Format for Vietnamese month name
        tvSelectedMonth.setText(sdf.format(currentMonthCalendar.getTime()));
    }

    private void loadMonthlyData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Người dùng chưa đăng nhập", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "User not logged in, cannot load calendar data.");
            updateCalendarGrid(new HashMap<>(), new HashMap<>()); // Clear grid if not logged in
            calculateAndDisplayMonthlySummary(new HashMap<>(), new HashMap<>()); // Use the correct method
            return;
        }

        String userId = currentUser.getUid();

        // Set calendar to the first day of the month for query range
        Calendar startCalendar = (Calendar) currentMonthCalendar.clone();
        startCalendar.set(Calendar.DAY_OF_MONTH, 1);
        startCalendar.set(Calendar.HOUR_OF_DAY, 0);
        startCalendar.set(Calendar.MINUTE, 0);
        startCalendar.set(Calendar.SECOND, 0);
        startCalendar.set(Calendar.MILLISECOND, 0);
        Date startDate = startCalendar.getTime();

        // Set calendar to the last day of the month for query range
        Calendar endCalendar = (Calendar) currentMonthCalendar.clone();
        endCalendar.set(Calendar.DAY_OF_MONTH, endCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        endCalendar.set(Calendar.HOUR_OF_DAY, 23);
        endCalendar.set(Calendar.MINUTE, 59);
        endCalendar.set(Calendar.SECOND, 59);
        endCalendar.set(Calendar.MILLISECOND, 999);
        Date endDate = endCalendar.getTime();

        // Fetch income and expense data for the month
        Map<Integer, Double> dailyIncome = new HashMap<>();
        Map<Integer, Double> dailyExpense = new HashMap<>();

        // Fetch Income
        db.collection("incomes")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .addOnSuccessListener(incomeSnapshots -> {
                    for (QueryDocumentSnapshot document : incomeSnapshots) {
                        Income income = document.toObject(Income.class);
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(income.getDate());
                        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
                        dailyIncome.merge(dayOfMonth, income.getAmount(), Double::sum);
                    }

                    // Fetch Expense after fetching Income
                    db.collection("expenses")
                            .whereEqualTo("userId", userId)
                            .whereGreaterThanOrEqualTo("date", startDate)
                            .whereLessThanOrEqualTo("date", endDate)
                            .get()
                            .addOnSuccessListener(expenseSnapshots -> {
                                for (QueryDocumentSnapshot document : expenseSnapshots) {
                                    Expense expense = document.toObject(Expense.class);
                                    Calendar cal = Calendar.getInstance();
                                    cal.setTime(expense.getDate());
                                    int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
                                    dailyExpense.merge(dayOfMonth, expense.getAmount(), Double::sum);
                                }

                                // Update UI after fetching both income and expense
                                updateCalendarGrid(dailyIncome, dailyExpense);
                                calculateAndDisplayMonthlySummary(dailyIncome, dailyExpense);

                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error fetching expenses for month", e);
                                Toast.makeText(getContext(), "Lỗi khi tải dữ liệu chi tiêu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                updateCalendarGrid(dailyIncome, new HashMap<>()); // Update with fetched income even if expense fails
                                calculateAndDisplayMonthlySummary(dailyIncome, new HashMap<>());
                            });

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching incomes for month", e);
                    Toast.makeText(getContext(), "Lỗi khi tải dữ liệu thu nhập: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Update UI even if income fetch fails
                    db.collection("expenses") // Still try to fetch expenses
                            .whereEqualTo("userId", userId)
                            .whereGreaterThanOrEqualTo("date", startDate)
                            .whereLessThanOrEqualTo("date", endDate)
                            .get()
                            .addOnSuccessListener(expenseSnapshots -> {
                                for (QueryDocumentSnapshot document : expenseSnapshots) {
                                    Expense expense = document.toObject(Expense.class);
                                    Calendar cal = Calendar.getInstance();
                                    cal.setTime(expense.getDate());
                                    int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
                                    dailyExpense.merge(dayOfMonth, expense.getAmount(), Double::sum);
                                }
                                updateCalendarGrid(new HashMap<>(), dailyExpense); // Update with fetched expense
                                calculateAndDisplayMonthlySummary(new HashMap<>(), dailyExpense);
                            })
                            .addOnFailureListener(e2 -> {
                                Log.e(TAG, "Error fetching expenses after income failure", e2);
                                Toast.makeText(getContext(), "Lỗi khi tải dữ liệu chi tiêu: " + e2.getMessage(), Toast.LENGTH_SHORT).show();
                                updateCalendarGrid(new HashMap<>(), new HashMap<>()); // Clear grid on total failure
                                calculateAndDisplayMonthlySummary(new HashMap<>(), new HashMap<>());
                            });
                });
    }

    // Implement the interface method from CalendarDayAdapter.OnDayDoubleClickListener
    @Override
    public void onDayDoubleClick(CalendarDay day) {
        if (onDaySelectedListener != null && day.getDate() != null) {
            onDaySelectedListener.onDaySelected(day.getDate());
        }
    }

    private void updateCalendarGrid(Map<Integer, Double> dailyIncome, Map<Integer, Double> dailyExpense) {
        calendarDays.clear();

        Calendar calendar = (Calendar) currentMonthCalendar.clone();
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        // Add empty days for the beginning of the week if the month doesn't start on Monday
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK); // Sunday = 1, Monday = 2, ..., Saturday = 7
        int daysToAddBefore = (firstDayOfWeek == Calendar.SUNDAY) ? 6 : firstDayOfWeek - 2; // Adjust to Monday=0 for grid

        for (int i = 0; i < daysToAddBefore; i++) {
            // Use a CalendarDay with day 0 to indicate an empty cell
            calendarDays.add(new CalendarDay(0, null, 0.0, 0.0)); // Use 0.0 for income/expense of empty cells
        }

        // Add days of the month
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 1; i <= daysInMonth; i++) {
            Calendar dayCalendar = (Calendar) calendar.clone();
            dayCalendar.set(Calendar.DAY_OF_MONTH, i);
            Date dayDate = dayCalendar.getTime();

            Double incomeValue = dailyIncome.getOrDefault(i, 0.0);
            double income = incomeValue != null ? incomeValue : 0.0;

            Double expenseValue = dailyExpense.getOrDefault(i, 0.0);
            double expense = expenseValue != null ? expenseValue : 0.0;

            calendarDays.add(new CalendarDay(i, dayDate, income, expense));
        }

        // Add empty days for the end of the week if needed to fill the last row
        int totalCells = calendarDays.size();
        int remainingCells = 7 - (totalCells % 7);
        if (remainingCells > 0 && remainingCells < 7) {
            for (int i = 0; i < remainingCells; i++) {
                // Use a CalendarDay with day 0 for empty cells at the end
                calendarDays.add(new CalendarDay(0, null, 0.0, 0.0)); // Use 0.0 for income/expense of empty cells
            }
        }

        // Pass the currency format to the adapter
        calendarDayAdapter.updateData(calendarDays, currencyFormat);
    }

    private void calculateAndDisplayMonthlySummary(Map<Integer, Double> dailyIncome, Map<Integer, Double> dailyExpense) {
        double totalMonthlyIncome = dailyIncome.values().stream().mapToDouble(Double::doubleValue).sum();
        double totalMonthlyExpense = dailyExpense.values().stream().mapToDouble(Double::doubleValue).sum();
        double monthlyBalance = totalMonthlyIncome - totalMonthlyExpense;

        // Format and display summary amounts using the standard currency format
        tvMonthlyIncome.setText(currencyFormat.format(totalMonthlyIncome));
        tvMonthlyExpense.setText(currencyFormat.format(totalMonthlyExpense));
        tvMonthlyBalance.setText(currencyFormat.format(monthlyBalance));
    }
} 