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
import com.example.personalspendingapp.data.DataManager;
import com.example.personalspendingapp.models.Category;
import com.example.personalspendingapp.models.Income;
import com.example.personalspendingapp.models.Transaction;
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
import java.util.Map;
import java.util.UUID;

public class IncomeFragment extends Fragment {
    private static final String TAG = "IncomeFragment";
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
        View view = inflater.inflate(R.layout.fragment_income, container, false);
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
            categoryAdapter.setSelectedPosition(position);
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
            // TODO: Implement add category dialog for Income categories
            Toast.makeText(getContext(), "Chức năng thêm danh mục thu nhập sẽ được cập nhật sau", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadCategories() {
        DataManager dataManager = DataManager.getInstance();
        if (!dataManager.isDataLoaded()) {
            Toast.makeText(getContext(), "Đang tải dữ liệu, vui lòng đợi...", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Data not loaded yet");
            return;
        }

        Map<String, List<Category>> allCategories = dataManager.getCategories();
        Log.d(TAG, "All categories: " + allCategories.toString());
        
        if (allCategories != null && allCategories.containsKey("income")) {
            List<Category> incomeCategories = allCategories.get("income");
            Log.d(TAG, "Income categories found: " + incomeCategories.toString());
            
                        categories.clear();
            for (Category category : incomeCategories) {
                categories.add(category);
                Log.d(TAG, "Added category: " + category.getName());
            }
                        categoryAdapter.updateCategories(categories);
                        Log.d(TAG, "Income Categories loaded: " + categories.size());
        } else {
            Log.e(TAG, "No income categories found in: " + allCategories);
            // Thử tải lại dữ liệu nếu không tìm thấy danh mục
            dataManager.loadUserData();
        }
    }

    private void setupSubmitButton() {
        btnSubmit.setOnClickListener(v -> {
            // Hide keyboard here if needed
            if (validateInput()) {
                saveIncome(); // Call saveIncome
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

    private void saveIncome() {
        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        if (!validateInput()) {
            progressBar.setVisibility(View.GONE);
            btnSubmit.setEnabled(true);
            return;
        }

        DataManager dataManager = DataManager.getInstance();
        if (!dataManager.isDataLoaded()) {
            Toast.makeText(getContext(), "Đang tải dữ liệu, vui lòng đợi...", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            btnSubmit.setEnabled(true);
            return;
        }

            double amount = Double.parseDouble(etAmount.getText().toString().trim());
            String note = etNote.getText().toString().trim();
            Date date = selectedDate.getTime();

        Transaction transaction = new Transaction(
                UUID.randomUUID().toString(),
                    amount,
                "income",
                selectedCategory.getId(),
                    note,
                date
        );

        dataManager.addTransaction(transaction);
        
                        progressBar.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                        Toast.makeText(getContext(), "Đã lưu khoản thu thành công", Toast.LENGTH_SHORT).show();
                        clearInputs();
        categoryAdapter.setSelectedPosition(RecyclerView.NO_POSITION);
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

