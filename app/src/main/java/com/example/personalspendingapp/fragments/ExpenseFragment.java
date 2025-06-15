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

import com.example.personalspendingapp.R;
import com.example.personalspendingapp.adapters.CategoryAdapter;
import com.example.personalspendingapp.data.DataManager;
import com.example.personalspendingapp.models.Category;
import com.example.personalspendingapp.models.Transaction;
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

public class ExpenseFragment extends Fragment {
    private static final String TAG = "ExpenseFragment";
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
        view = inflater.inflate(R.layout.fragment_expense, container, false);
        initViews(view);
        setupFirebase();
        setupDateSelection();
        setupCategoryRecyclerView();
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

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        
        // Thêm listener cho việc load dữ liệu
        DataManager dataManager = DataManager.getInstance();
        dataManager.setDataLoadedListener(new DataManager.OnDataLoadedListener() {
            @Override
            public void onDataLoaded() {
                // Load lại danh mục khi dữ liệu đã sẵn sàng
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        loadCategories();
                        // Ẩn loading sau khi load xong
                        progressBar.setVisibility(View.GONE);
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Lỗi khi tải dữ liệu: " + error, Toast.LENGTH_SHORT).show();
                        // Ẩn loading khi có lỗi
                        progressBar.setVisibility(View.GONE);
                    });
                }
            }
        });
        
        // Chỉ load categories nếu dữ liệu đã sẵn sàng
        if (dataManager.isDataLoaded()) {
            loadCategories();
        } else {
            // Hiển thị loading và bắt đầu load dữ liệu
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
    }

    private void updateDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvSelectedDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void setupCategoryRecyclerView() {
        RecyclerView rvCategories = view.findViewById(R.id.rvCategories);
        rvCategories.setLayoutManager(new GridLayoutManager(getContext(), 3));
        
        List<Category> categories = DataManager.getInstance().getCategoriesByType("expense");
        categoryAdapter = new CategoryAdapter(categories, new CategoryAdapter.OnCategoryClickListener() {
            @Override
            public void onCategoryClick(Category category) {
                selectedCategory = category;
                updateCategorySelection();
                int position = categories.indexOf(category);
                if (position != -1) {
                    categoryAdapter.setSelectedPosition(position);
                }
            }

            @Override
            public void onOtherClick() {
                showAddCategoryDialog();
            }
        });
        rvCategories.setAdapter(categoryAdapter);
    }

    private void loadCategories() {
        DataManager dataManager = DataManager.getInstance();
        Log.d(TAG, "Loading categories, isDataLoaded: " + dataManager.isDataLoaded());
        
        if (!dataManager.isDataLoaded()) {
            Log.d(TAG, "Data not loaded yet, waiting for data to be ready");
            return;
        }

        List<Category> expenseCategories = dataManager.getCategoriesByType("expense");
        Log.d(TAG, "Expense categories found: " + expenseCategories.size());
        
        if (expenseCategories != null && !expenseCategories.isEmpty()) {
            categories.clear();
            categories.addAll(expenseCategories);
            categoryAdapter.updateCategories(categories);
            Log.d(TAG, "Expense Categories loaded: " + categories.size());
        } else {
            Log.e(TAG, "No expense categories found");
            // Không gọi loadUserData() ở đây nữa vì đã có listener
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
                "expense",
                selectedCategory.getId(),
                note,
                date
        );

        // Thêm listener để đảm bảo dữ liệu được cập nhật
        dataManager.setDataLoadedListener(new DataManager.OnDataLoadedListener() {
            @Override
            public void onDataLoaded() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                        Toast.makeText(getContext(), "Đã lưu khoản chi thành công", Toast.LENGTH_SHORT).show();
                        clearInputs();
                        categoryAdapter.setSelectedPosition(RecyclerView.NO_POSITION);
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                        Toast.makeText(getContext(), "Lỗi khi lưu khoản chi: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });

        dataManager.addTransaction(transaction);
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
                
                // Tạo danh mục mới với type cố định là expense
                Category newCategory = new Category(
                    "cat_expense_" + UUID.randomUUID().toString().substring(0, 8),
                    categoryName,
                    "❓", // Icon mặc định
                    "expense" // Type cố định là expense
                );
                
                // Thêm vào DataManager và database
                DataManager dataManager = DataManager.getInstance();
                dataManager.setDataLoadedListener(new DataManager.OnDataLoadedListener() {
                    @Override
                    public void onDataLoaded() {
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
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Lỗi khi thêm danh mục: " + error, Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                                btnAdd.setEnabled(true);
                            });
                        }
                    }
                });
                
                dataManager.addCategory(newCategory);
            } else {
                etCategoryName.setError("Vui lòng nhập tên danh mục");
            }
        });

        dialog.show();
    }
} 