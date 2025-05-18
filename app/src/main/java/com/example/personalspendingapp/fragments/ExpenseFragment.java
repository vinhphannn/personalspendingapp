package com.example.personalspendingapp.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personalspendingapp.R;
import com.example.personalspendingapp.adapters.CategoryAdapter;
import com.example.personalspendingapp.models.Category;
import com.example.personalspendingapp.models.Expense;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseFragment extends Fragment {
    private static final String TAG = "ExpenseFragment";
    private TextView tvSelectedDate;
    private ImageButton btnPreviousDate, btnNextDate;
    private TextInputEditText etAmount, etNote;
    private RecyclerView rvCategories;
    private Button btnAddCategory, btnSubmit;
    private ProgressBar progressBar;

    private CategoryAdapter categoryAdapter;
    private List<Category> categories;
    private Category selectedCategory;
    private Calendar selectedDate;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expense, container, false);
        initViews(view);
        setupFirebase();
        setupDateSelection();
        setupCategories();
        setupSubmitButton();
        return view;
    }

    private void initViews(View view) {
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        btnPreviousDate = view.findViewById(R.id.btnPreviousDate);
        btnNextDate = view.findViewById(R.id.btnNextDate);
        etAmount = view.findViewById(R.id.etAmount);
        etNote = view.findViewById(R.id.etNote);
        rvCategories = view.findViewById(R.id.rvCategories);
        btnAddCategory = view.findViewById(R.id.btnAddCategory);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        progressBar = view.findViewById(R.id.progressBar);

        selectedDate = Calendar.getInstance();
        categories = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(categories, (category, position) -> {
            selectedCategory = category;
            // Optional: Show selected category name
            // Toast.makeText(getContext(), "Selected: " + category.getName(), Toast.LENGTH_SHORT).show();
        });
        rvCategories.setLayoutManager(new GridLayoutManager(getContext(), 3));
        rvCategories.setAdapter(categoryAdapter);
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        loadCategories();
    }

    private void setupDateSelection() {
        updateDateDisplay();
        
        btnPreviousDate.setOnClickListener(v -> {
            selectedDate.add(Calendar.DAY_OF_MONTH, -1);
            updateDateDisplay();
        });

        btnNextDate.setOnClickListener(v -> {
            selectedDate.add(Calendar.DAY_OF_MONTH, 1);
            updateDateDisplay();
        });
    }

    private void updateDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvSelectedDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void setupCategories() {
        btnAddCategory.setOnClickListener(v -> {
            // TODO: Implement add category dialog
            Toast.makeText(getContext(), "Chức năng thêm danh mục sẽ được cập nhật sau", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadCategories() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("categories")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("type", "expense")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        categories.clear();
                        categories.addAll(queryDocumentSnapshots.toObjects(Category.class));
                        categoryAdapter.updateCategories(categories);
                         Log.d(TAG, "Categories loaded: " + categories.size());
                    })
                    .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi khi tải danh mục: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        } else {
             Toast.makeText(getContext(), "Người dùng chưa đăng nhập", Toast.LENGTH_SHORT).show();
             Log.w(TAG, "User not logged in, cannot load categories.");
        }
    }

    private void setupSubmitButton() {
        btnSubmit.setOnClickListener(v -> {
            // Hide keyboard here if needed
            if (validateInput()) {
                saveExpense();
            }
        });
    }

    private boolean validateInput() {
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return false;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                 Toast.makeText(getContext(), "Số tiền phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                 return false;
            }
        } catch (NumberFormatException e) {
             Toast.makeText(getContext(), "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
             Log.e(TAG, "Invalid amount input", e);
             return false;
        }

        if (selectedCategory == null) {
            Toast.makeText(getContext(), "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show();
            return false;
        }

        // If all validations pass
        return true;
    }

    private void saveExpense() {
        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false); // Disable button to prevent multiple clicks

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            double amount = Double.parseDouble(etAmount.getText().toString().trim());
            String note = etNote.getText().toString().trim();
            Date date = selectedDate.getTime();

            Expense expense = new Expense(
                    db.collection("expenses").document().getId(),
                    userId,
                    amount,
                    date,
                    note,
                    selectedCategory.getName()
            );

            db.collection("expenses")
                    .document(expense.getId())
                    .set(expense)
                    .addOnSuccessListener(aVoid -> {
                        progressBar.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                        Toast.makeText(getContext(), "Đã lưu khoản chi thành công", Toast.LENGTH_SHORT).show();
                        clearInputs();
                        categoryAdapter.setSelectedPosition(RecyclerView.NO_POSITION); // Reset selected category UI
                    })
                    .addOnFailureListener(e ->
                        {
                             progressBar.setVisibility(View.GONE);
                             btnSubmit.setEnabled(true);
                             Toast.makeText(getContext(), "Lỗi khi lưu khoản chi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                             Log.e(TAG, "Error saving expense", e);
                        }
                    );
        } else {
            progressBar.setVisibility(View.GONE);
            btnSubmit.setEnabled(true);
            Toast.makeText(getContext(), "Người dùng chưa đăng nhập", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "User not logged in, cannot save expense.");
        }
    }

    private void clearInputs() {
        etAmount.setText("");
        etNote.setText("");
        selectedCategory = null;
        // Reset selected category UI is handled in onSuccessListener
    }

    // Method to set the selected date from outside
    public void setSelectedDate(Date date) {
        selectedDate.setTime(date);
        updateDateDisplay();
    }
} 