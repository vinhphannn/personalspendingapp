package com.example.personalspendingapp.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personalspendingapp.R;
import com.example.personalspendingapp.adapters.ManageCategoryAdapter;
import com.example.personalspendingapp.data.DataManager;
import com.example.personalspendingapp.models.Category;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ExpenseCategoryFragment extends Fragment implements ManageCategoryAdapter.OnCategoryActionListener {

    private static final String TAG = "ExpenseCategoryFragment";
    private RecyclerView rvCategories;
    private TextInputEditText etCategoryName;
    private MaterialButton btnAddCategory;
    private ManageCategoryAdapter adapter;
    private List<Category> expenseCategories;
    private DataManager dataManager;
    private Category editingCategory = null;

    public ExpenseCategoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataManager = DataManager.getInstance();
        expenseCategories = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expense_category, container, false);

        rvCategories = view.findViewById(R.id.rvCategories);
        etCategoryName = view.findViewById(R.id.etCategoryName);
        btnAddCategory = view.findViewById(R.id.btnAddCategory);

        setupRecyclerView();
        loadCategories();
        setupListeners();

        return view;
    }

    private void setupRecyclerView() {
        adapter = new ManageCategoryAdapter(getContext(), expenseCategories, this);
        rvCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCategories.setAdapter(adapter);
    }

    private void loadCategories() {
        dataManager.setDataLoadedListener(new DataManager.OnDataLoadedListener() {
            @Override
            public void onDataLoaded() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        expenseCategories = dataManager.getCategoriesByType("expense");
                        adapter.updateCategories(expenseCategories);
                        Log.d(TAG, "Expense categories loaded: " + expenseCategories.size());
                    });
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading expense categories: " + error);
                Toast.makeText(getContext(), "Lỗi tải danh mục: " + error, Toast.LENGTH_SHORT).show();
            }
        });
        // Ensure data is loaded if it wasn't already
        if (!dataManager.isDataLoaded() && !dataManager.isLoading()) {
            dataManager.loadUserData();
        } else if (dataManager.isDataLoaded()) {
            expenseCategories = dataManager.getCategoriesByType("expense");
            adapter.updateCategories(expenseCategories);
            Log.d(TAG, "Expense categories already loaded: " + expenseCategories.size());
        }
    }

    private void setupListeners() {
        btnAddCategory.setOnClickListener(v -> {
            String categoryName = etCategoryName.getText().toString().trim();
            if (!categoryName.isEmpty()) {
                if (editingCategory != null) {
                    // Đang trong chế độ sửa
                    editingCategory.setName(categoryName);
                    dataManager.saveUserData();
                    Toast.makeText(getContext(), "Đã cập nhật danh mục", Toast.LENGTH_SHORT).show();
                    editingCategory = null;
                    btnAddCategory.setText("Thêm");
                } else {
                    // Thêm mới
                    addCategory(categoryName);
                }
                etCategoryName.setText("");
            } else {
                Toast.makeText(getContext(), "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addCategory(String name) {
        String id = UUID.randomUUID().toString();
        Category newCategory = new Category(id, name, "", "expense"); // icon is empty
        dataManager.addCategory(newCategory);
        dataManager.saveUserData();
        Toast.makeText(getContext(), "Đã thêm danh mục chi tiêu", Toast.LENGTH_SHORT).show();
        // Refresh categories
        loadCategories();
    }

    @Override
    public void onEditCategory(Category category, String newName) {
        // Chuyển sang chế độ sửa
        editingCategory = category;
        etCategoryName.setText(category.getName());
        btnAddCategory.setText("Lưu");
    }

    @Override
    public void onDeleteCategory(Category category) {
        if (dataManager.getUserData() != null && dataManager.getUserData().getCategories() != null) {
            List<Category> categoriesByType = dataManager.getUserData().getCategories().get(category.getType());
            if (categoriesByType != null) {
                categoriesByType.remove(category);
                dataManager.saveUserData();
                Toast.makeText(getContext(), "Đã xóa danh mục", Toast.LENGTH_SHORT).show();
                // Refresh categories
                loadCategories();
            }
        }
    }
} 