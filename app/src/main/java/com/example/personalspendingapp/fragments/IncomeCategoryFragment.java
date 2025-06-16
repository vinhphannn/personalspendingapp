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

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IncomeCategoryFragment extends Fragment implements ManageCategoryAdapter.OnCategoryActionListener {

    private static final String TAG = "IncomeCategoryFragment";
    private RecyclerView rvCategories;
    private TextInputEditText etCategoryName;
    private MaterialButton btnAddCategory;
    private ManageCategoryAdapter adapter;
    private List<Category> incomeCategories;
    private DataManager dataManager;
    private Category editingCategory = null;

    public IncomeCategoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataManager = DataManager.getInstance();
        incomeCategories = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_income_category, container, false);

        rvCategories = view.findViewById(R.id.rvCategories);
        etCategoryName = view.findViewById(R.id.etCategoryName);
        btnAddCategory = view.findViewById(R.id.btnAddCategory);

        setupRecyclerView();
        loadCategories();
        setupListeners();

        return view;
    }

    private void setupRecyclerView() {
        adapter = new ManageCategoryAdapter(getContext(), incomeCategories, this);
        rvCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCategories.setAdapter(adapter);
    }

    private void loadCategories() {
        dataManager.setDataLoadedListener(new DataManager.OnDataLoadedListener() {
            @Override
            public void onDataLoaded() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        incomeCategories = dataManager.getCategoriesByType("income");
                        adapter.updateCategories(incomeCategories);
                        Log.d(TAG, "Income categories loaded: " + incomeCategories.size());
                    });
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading income categories: " + error);
                Toast.makeText(getContext(), "Lỗi tải danh mục: " + error, Toast.LENGTH_SHORT).show();
            }
        });
        // Ensure data is loaded if it wasn't already
        if (!dataManager.isDataLoaded() && !dataManager.isLoading()) {
            dataManager.loadUserData();
        } else if (dataManager.isDataLoaded()) {
            incomeCategories = dataManager.getCategoriesByType("income");
            adapter.updateCategories(incomeCategories);
            Log.d(TAG, "Income categories already loaded: " + incomeCategories.size());
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
        Category newCategory = new Category(id, name, "", "income"); // icon is empty
        DataManager.getInstance().addCategory(newCategory);
        Toast.makeText(getContext(), "Đã thêm danh mục thu nhập", Toast.LENGTH_SHORT).show();
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
        DataManager.getInstance().deleteCategory(category);
        Toast.makeText(getContext(), "Đã xóa danh mục", Toast.LENGTH_SHORT).show();
        // Refresh categories
        loadCategories();
    }
} 