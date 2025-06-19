package com.example.personalspendingapp.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.Button;

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
        dailyTransactionsContainer.removeAllViews();
        if (transactions == null || transactions.isEmpty()) {
            TextView emptyView = new TextView(getContext());
            emptyView.setText("Không có giao dịch nào trong ngày này");
            emptyView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            emptyView.setPadding(16, 32, 16, 32);
            emptyView.setTextColor(getResources().getColor(R.color.text_secondary));
            dailyTransactionsContainer.addView(emptyView);
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

        // Sắp xếp các ngày theo thứ tự mới nhất lên đầu
        List<String> sortedDates = new ArrayList<>(transactionsByDay.keySet());
        Collections.sort(sortedDates, Collections.reverseOrder());

        for (String dateKey : sortedDates) {
            List<Transaction> dailyList = transactionsByDay.get(dateKey);

            // Header cho ngày
            TextView dateHeader = new TextView(getContext());
            dateHeader.setText(dateKey);
            dateHeader.setTextSize(18);
            dateHeader.setTypeface(null, Typeface.BOLD);
            dateHeader.setTextColor(getResources().getColor(R.color.text_primary));
            dateHeader.setPadding(16, 24, 16, 16);
            dailyTransactionsContainer.addView(dateHeader);

            // Container cho các giao dịch trong ngày
            LinearLayout dayContainer = new LinearLayout(getContext());
            dayContainer.setOrientation(LinearLayout.VERTICAL);
            dayContainer.setPadding(0, 0, 0, 24); // Thêm padding bottom cho container ngày

            // Hiển thị từng giao dịch
            for (int i = 0; i < dailyList.size(); i++) {
                Transaction transaction = dailyList.get(i);
                View transactionView = getLayoutInflater().inflate(R.layout.item_transaction, dayContainer, false);
                
                // Set category name
                TextView tvCategoryName = transactionView.findViewById(R.id.tvCategoryName);
                Category category = DataManager.getInstance().getCategoryById(transaction.getCategoryId(), transaction.getType());
                tvCategoryName.setText(category != null ? category.getName() : "Unknown Category");

                // Set time with full format
                TextView tvTime = transactionView.findViewById(R.id.tvTime);
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
                tvTime.setText(timeFormat.format(transaction.getDate()));

                // Set amount
                TextView tvAmount = transactionView.findViewById(R.id.tvAmount);
                tvAmount.setText(currencyFormat.format(transaction.getAmount()));
                tvAmount.setTextColor(getResources().getColor(
                    transaction.getType().equals("income") ? R.color.green_500 : R.color.red_500
                ));

                // Set note if exists
                TextView tvNote = transactionView.findViewById(R.id.tvNote);
                if (transaction.getNote() != null && !transaction.getNote().isEmpty()) {
                    tvNote.setText(transaction.getNote());
                    tvNote.setVisibility(View.VISIBLE);
                }

                    // Thêm swipe-to-delete
                    View deleteBackground = getLayoutInflater().inflate(R.layout.item_transaction_delete_background, null);
                    ViewGroup parent = (ViewGroup) transactionView.getParent();
                    if (parent != null) {
                        parent.removeView(transactionView);
                    }

                    SwipeLayout swipeLayout = new SwipeLayout(getContext());
                    swipeLayout.addView(transactionView);
                    swipeLayout.addView(deleteBackground);

                    // Thêm margin cho item
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    params.setMargins(0, 0, 0, 16); // Thêm margin bottom 16dp
                    swipeLayout.setLayoutParams(params);

                    swipeLayout.setOnSwipeListener(new SwipeLayout.OnSwipeListener() {
                    @Override
                    public void onSwipe(float progress) {
                        deleteBackground.setAlpha(progress);
                    }

                    @Override
                    public void onSwipeComplete() {
                    }

                    @Override
                    public void onDeleteClick() {
                        DataManager.getInstance().deleteTransaction(transaction.getId());
                        Toast.makeText(getContext(), "Đã xóa giao dịch", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onClose() {
                        swipeLayout.close();
                    }
                });

                // Thêm click listener cho nút xóa
                View deleteButton = deleteBackground.findViewById(R.id.deleteButton);
                deleteButton.setOnClickListener(v -> {
                    if (swipeLayout.isRevealed()) {
                        swipeLayout.listener.onDeleteClick();
                    }
                });

                // Thêm click listener cho vùng còn lại để đóng
                deleteBackground.setOnClickListener(v -> {
                    if (swipeLayout.isRevealed()) {
                        swipeLayout.listener.onClose();
                    }
                });

                // Thêm click listener cho item để đóng khi click vào vùng khác
                transactionView.setOnClickListener(v -> {
                    if (swipeLayout.isRevealed()) {
                        swipeLayout.listener.onClose();
                    } else {
                        showTransactionDetails(transaction);
                    }
                });

                dayContainer.addView(swipeLayout);
            }

            dailyTransactionsContainer.addView(dayContainer);
        }

        // Thêm click listener cho container để đóng tất cả các item đang mở
        dailyTransactionsContainer.setOnClickListener(v -> {
            for (int i = 0; i < dailyTransactionsContainer.getChildCount(); i++) {
                View child = dailyTransactionsContainer.getChildAt(i);
                if (child instanceof LinearLayout) {
                    LinearLayout dayContainer = (LinearLayout) child;
                    for (int j = 0; j < dayContainer.getChildCount(); j++) {
                        View item = dayContainer.getChildAt(j);
                        if (item instanceof SwipeLayout) {
                            SwipeLayout swipeLayout = (SwipeLayout) item;
                            if (swipeLayout.isRevealed()) {
                                swipeLayout.close();
                            }
                        }
                    }
                }
            }
        });
    }

    private void showSwipeActions(View view, Transaction transaction) {
        // Create swipe actions layout
        LinearLayout swipeActions = new LinearLayout(getContext());
        swipeActions.setOrientation(LinearLayout.HORIZONTAL);
        swipeActions.setBackgroundColor(getResources().getColor(R.color.white));

        // Edit button
        ImageButton btnEdit = new ImageButton(getContext());
        btnEdit.setImageResource(R.drawable.ic_edit);
        btnEdit.setBackgroundResource(android.R.color.transparent);
        btnEdit.setPadding(16, 16, 16, 16);
        btnEdit.setOnClickListener(v -> {
            showEditTransactionDialog(transaction);
            swipeActions.setVisibility(View.GONE);
        });

        // Delete button
        ImageButton btnDelete = new ImageButton(getContext());
        btnDelete.setImageResource(R.drawable.ic_delete);
        btnDelete.setBackgroundResource(android.R.color.transparent);
        btnDelete.setPadding(16, 16, 16, 16);
        btnDelete.setOnClickListener(v -> {
            showDeleteConfirmationDialog(transaction);
            swipeActions.setVisibility(View.GONE);
        });

        swipeActions.addView(btnEdit);
        swipeActions.addView(btnDelete);

        // Add swipe actions to the view
        ViewGroup parent = (ViewGroup) view.getParent();
        int index = parent.indexOfChild(view);
        parent.addView(swipeActions, index + 1);

        // Animate swipe actions
        swipeActions.setTranslationX(swipeActions.getWidth());
        swipeActions.animate()
            .translationX(0)
            .setDuration(200)
            .start();
    }

    private void showTransactionDetails(Transaction transaction) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_transaction_details, null);
        TextView tvAmount = dialogView.findViewById(R.id.tvAmount);
        TextView tvCategory = dialogView.findViewById(R.id.tvCategory);
        TextView tvDate = dialogView.findViewById(R.id.tvDate);
        TextView tvNote = dialogView.findViewById(R.id.tvNote);
        Button btnEdit = dialogView.findViewById(R.id.btnEdit);
        Button btnClose = dialogView.findViewById(R.id.btnClose);

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

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
            .setTitle(transaction.getType().equals("income") ? "Chi tiết khoản thu" : "Chi tiết khoản chi")
            .setView(dialogView)
            .create();

        // Set button click listeners
        btnEdit.setOnClickListener(v -> {
            dialog.dismiss();
            showEditTransactionDialog(transaction);
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showDeleteConfirmationDialog(Transaction transaction) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận xóa")
            .setMessage("Bạn có chắc chắn muốn xóa giao dịch này?")
            .setPositiveButton("Xóa", (dialog, which) -> {
                DataManager.getInstance().deleteTransaction(transaction.getId());
                Toast.makeText(getContext(), "Đã xóa giao dịch", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Hủy", null)
            .show();
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

    // Thêm class SwipeLayout
    private static class

    SwipeLayout extends ViewGroup {
        private static final float SWIPE_THRESHOLD = 0.5f;
        private float startX;
        private float startY;
        private float currentX;
        private View contentView;
        private View backgroundView;
        private OnSwipeListener listener;
        private boolean isSwiping = false;
        private boolean isRevealed = false;
        private boolean isHorizontalSwipe = false;

        public interface OnSwipeListener {
            void onSwipe(float progress);
            void onSwipeComplete();
            void onDeleteClick();
            void onClose();
        }

        public SwipeLayout(Context context) {
            super(context);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            if (getChildCount() != 2) return;
            
            contentView = getChildAt(0);
            backgroundView = getChildAt(1);
            
            // Layout background view
            backgroundView.layout(0, 0, getWidth(), getHeight());
            
            // Layout content view
            contentView.layout((int) currentX, 0, (int) currentX + contentView.getMeasuredWidth(), getHeight());
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            if (getChildCount() != 2) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }

            View contentView = getChildAt(0);
            View backgroundView = getChildAt(1);

            measureChild(contentView, widthMeasureSpec, heightMeasureSpec);
            measureChild(backgroundView, widthMeasureSpec, heightMeasureSpec);

            setMeasuredDimension(contentView.getMeasuredWidth(), contentView.getMeasuredHeight());
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = ev.getX();
                    startY = ev.getY();
                    currentX = isRevealed ? -getWidth() * SWIPE_THRESHOLD : 0;
                    isSwiping = false;
                    isHorizontalSwipe = false;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return false;

                case MotionEvent.ACTION_MOVE:
                    float deltaX = ev.getX() - startX;
                    float deltaY = ev.getY() - startY;
                    
                    // Nếu chưa xác định hướng swipe
                    if (!isHorizontalSwipe && !isSwiping) {
                        // Nếu kéo ngang nhiều hơn dọc
                        if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 10) {
                            isHorizontalSwipe = true;
                            isSwiping = true;
                            getParent().requestDisallowInterceptTouchEvent(true);
                            return true;
                        }
                        // Nếu kéo dọc nhiều hơn ngang
                        else if (Math.abs(deltaY) > Math.abs(deltaX) && Math.abs(deltaY) > 10) {
                            isHorizontalSwipe = false;
                            return false;
                        }
                    }
                    // Nếu đã xác định là swipe ngang
                    else if (isHorizontalSwipe) {
                        return true;
                    }
                    return false;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isSwiping = false;
                    isHorizontalSwipe = false;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    return false;
            }
            return super.onInterceptTouchEvent(ev);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (!isSwiping || !isHorizontalSwipe) {
                return super.onTouchEvent(event);
            }

            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getX() - startX;
                    // Cho phép kéo cả hai chiều
                    if (isRevealed) {
                        // Nếu đang mở, cho phép kéo về phải
                        currentX = Math.max(Math.min(deltaX - getWidth() * SWIPE_THRESHOLD, 0), -getWidth() * SWIPE_THRESHOLD);
                    } else {
                        // Nếu đang đóng, cho phép kéo sang trái
                        currentX = Math.max(Math.min(deltaX, 0), -getWidth() * SWIPE_THRESHOLD);
                    }
                    float progress = Math.min(Math.abs(currentX) / (getWidth() * SWIPE_THRESHOLD), 1.0f);
                    if (listener != null) {
                        listener.onSwipe(progress);
                    }
                    requestLayout();
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    float finalDeltaX = event.getX() - startX;
                    if (isRevealed) {
                        // Nếu đang mở, kiểm tra xem có kéo về đủ xa không
                        if (finalDeltaX > getWidth() * SWIPE_THRESHOLD / 2) {
                            // Kéo về đủ xa, đóng lại
                            currentX = 0;
                            isRevealed = false;
                        } else {
                            // Không kéo đủ xa, giữ nguyên trạng thái mở
                            currentX = -getWidth() * SWIPE_THRESHOLD;
                        }
                    } else {
                        // Nếu đang đóng, kiểm tra xem có kéo sang đủ xa không
                        if (Math.abs(finalDeltaX) > getWidth() * SWIPE_THRESHOLD / 2) {
                            // Kéo sang đủ xa, mở ra
                            currentX = -getWidth() * SWIPE_THRESHOLD;
                            isRevealed = true;
                        } else {
                            // Không kéo đủ xa, đóng lại
                            currentX = 0;
                        }
                    }
                    if (listener != null) {
                        listener.onSwipe(isRevealed ? 1.0f : 0.0f);
                    }
                    requestLayout();
                    return true;
            }
            return super.onTouchEvent(event);
        }

        public void setOnSwipeListener(OnSwipeListener listener) {
            this.listener = listener;
        }

        public void close() {
            if (isRevealed) {
                currentX = 0;
                isRevealed = false;
                if (listener != null) {
                    listener.onSwipe(0);
                }
                requestLayout();
            }
        }

        public boolean isRevealed() {
            return isRevealed;
        }
    }
} 