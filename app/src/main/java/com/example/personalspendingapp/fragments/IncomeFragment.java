package com.example.personalspendingapp.fragments;

import android.app.AlertDialog;
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

import com.example.personalspendingapp.MainActivity;
import com.example.personalspendingapp.R;
import com.example.personalspendingapp.adapters.CategoryAdapter;
import com.example.personalspendingapp.data.DataManager;
import com.example.personalspendingapp.models.Category;
import com.example.personalspendingapp.models.Transaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
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
    private View view;
    private TextView tvSelectedDate;
    private ImageButton btnPreviousDate, btnNextDate;
    private TextInputEditText etAmount, etNote;
    private RecyclerView rvCategories;
    private Button btnSubmit;
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
        view = inflater.inflate(R.layout.fragment_income, container, false);
        initViews(view);
        setupFirebase();
        setupDateSelection();
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
        btnSubmit = view.findViewById(R.id.btnSubmit);
        progressBar = view.findViewById(R.id.progressBar);

        selectedDate = Calendar.getInstance();
        categories = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(categories, new CategoryAdapter.OnCategoryClickListener() {
            @Override
            public void onCategoryClick(Category category) {
                selectedCategory = category;
                updateCategorySelection();
            }

            @Override
            public void onOtherClick() {
                showAddCategoryDialog();
            }
        });
        rvCategories.setLayoutManager(new GridLayoutManager(getContext(), 3));
        rvCategories.setAdapter(categoryAdapter);
    }

    private void updateCategorySelection() {
        if (selectedCategory != null) {
            // Cập nhật UI để hiển thị danh mục đã chọn
            TextView tvSelectedCategory = view.findViewById(R.id.tvSelectedCategory);
            if (tvSelectedCategory != null) {
                tvSelectedCategory.setText(selectedCategory.getName());
            }
        }
    }

    private void showAddCategoryDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_category, null);
        TextInputEditText etCategoryName = dialogView.findViewById(R.id.etCategoryName);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnAdd = dialogView.findViewById(R.id.btnAdd);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnAdd.setOnClickListener(v -> {
            String categoryName = etCategoryName.getText().toString().trim();
            if (!categoryName.isEmpty()) {
                // Hiển thị loading
                progressBar.setVisibility(View.VISIBLE);
                btnAdd.setEnabled(false);

                // Tạo danh mục mới với type cố định là income
                Category newCategory = new Category(
                        "cat_income_" + UUID.randomUUID().toString().substring(0, 8),
                        categoryName,
                        "❓", // Icon mặc định cho danh mục mới
                        "income" // Type cố định là income
                );

                // Thêm vào DataManager và database
                DataManager dataManager = DataManager.getInstance();
                dataManager.addCategory(newCategory);
                
                // Cập nhật UI sau khi thêm thành công (thông qua listener trong DataManager hoặc trực tiếp nếu addCategory là đồng bộ)
                // Hiện tại DataManager.addCategory là bất đồng bộ (lưu Firestore), nên việc cập nhật UI
                // cần dựa vào listener hoặc xử lý trong callback thành công của Firestore.
                // Dựa vào code ExpenseFragment, listener được set trước khi gọi addCategory
                // Tuy nhiên, setDataLoadedListener sẽ ghi đè listener cũ. Cần xem xét lại logic này.

                // Tạm thời cập nhật UI trực tiếp sau khi gọi addCategory (giả định DataManager xử lý bất đồng bộ nội bộ)
                // Logic tốt hơn là xử lý trong callback thành công của Firestore trong DataManager
                 if (getActivity() != null) {
                     getActivity().runOnUiThread(() -> {
                         // Cập nhật RecyclerView
                         loadCategories();

                         // Chọn danh mục mới
                         selectedCategory = newCategory;
                         updateCategorySelection();

                         // Ẩn loading và đóng dialog
                         progressBar.setVisibility(View.GONE);
                         btnAdd.setEnabled(true);
                         dialog.dismiss();

                         Toast.makeText(getContext(), "Đã thêm danh mục mới", Toast.LENGTH_SHORT).show();
                     });
                 }

            } else {
                etCategoryName.setError("Vui lòng nhập tên danh mục");
            }
        });

        dialog.show();
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        // Load categories khi dữ liệu sẵn sàng hoặc bắt đầu load dữ liệu
        DataManager dataManager = DataManager.getInstance();
        dataManager.setDataLoadedListener(new DataManager.OnDataLoadedListener() {
            @Override
            public void onDataLoaded() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        loadCategories();
                        progressBar.setVisibility(View.GONE);
                    });
                }
            }

            @Override
            public void onError(String error) {
                 if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Lỗi khi tải dữ liệu: " + error, Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    });
                }
            }
        });

        if (dataManager.isDataLoaded()) {
            loadCategories();
        } else {
            progressBar.setVisibility(View.VISIBLE);
            dataManager.loadUserData();
        }
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
        tvSelectedDate.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToCalendarWithDate(selectedDate.getTime());
            } else {
                BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottomNavigationView);
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.navigation_calendar);
                }
            }
        });
    }

    private void updateDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvSelectedDate.setText(sdf.format(selectedDate.getTime()));
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

