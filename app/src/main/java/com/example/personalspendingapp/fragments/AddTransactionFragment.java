//import android.app.AlertDialog;
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.recyclerview.widget.GridLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//import com.example.personalspendingapp.R;
//import com.example.personalspendingapp.adapters.CategoryAdapter;
//import com.example.personalspendingapp.models.Category;
//import com.example.personalspendingapp.data.DataManager;
//import com.google.android.material.button.MaterialButton;
//import com.google.android.material.textfield.TextInputEditText;
//import java.util.List;
//import java.util.UUID;
//
//public class AddTransactionFragment extends androidx.fragment.app.Fragment {
//
//    private View view;
//    private Category selectedCategory;
//
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        view = inflater.inflate(R.layout.fragment_add_transaction, container, false);
//        return view;
//    }
//
//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//        setupCategoryRecyclerView();
//    }
//
//    private void setupCategoryRecyclerView() {
//        RecyclerView rvCategories = view.findViewById(R.id.rvCategories);
//        rvCategories.setLayoutManager(new GridLayoutManager(getContext(), 3));
//
//        List<Category> categories = DataManager.getInstance().getCategoriesByType("expense");
//        CategoryAdapter adapter = new CategoryAdapter(categories, new CategoryAdapter.OnCategoryClickListener() {
//            @Override
//            public void onCategoryClick(Category category) {
//                selectedCategory = category;
//                updateCategorySelection();
//            }
//
//            @Override
//            public void onOtherClick() {
//                showAddCategoryDialog();
//            }
//        });
//        rvCategories.setAdapter(adapter);
//    }
//
//    private void showAddCategoryDialog() {
//        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_category, null);
//        TextInputEditText etCategoryName = dialogView.findViewById(R.id.etCategoryName);
//        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
//        MaterialButton btnAdd = dialogView.findViewById(R.id.btnAdd);
//
//        AlertDialog dialog = new AlertDialog.Builder(requireContext())
//                .setView(dialogView)
//                .create();
//
//        btnCancel.setOnClickListener(v -> dialog.dismiss());
//        btnAdd.setOnClickListener(v -> {
//            String categoryName = etCategoryName.getText().toString().trim();
//            if (!categoryName.isEmpty()) {
//                // Tạo danh mục mới
//                Category newCategory = new Category(
//                    UUID.randomUUID().toString(),
//                    categoryName,
//                    "expense",
//                    "#757575" // Màu mặc định
//                );
//
//                // Thêm vào DataManager
//                DataManager.getInstance().addCategory(newCategory);
//
//                // Cập nhật RecyclerView
//                setupCategoryRecyclerView();
//
//                // Chọn danh mục mới
//                selectedCategory = newCategory;
//                updateCategorySelection();
//
//                dialog.dismiss();
//            } else {
//                etCategoryName.setError("Vui lòng nhập tên danh mục");
//            }
//        });
//
//        dialog.show();
//    }
//
//    private void updateCategorySelection() {
//        if (selectedCategory != null) {
//            // Cập nhật UI để hiển thị danh mục đã chọn
//            TextView tvSelectedCategory = view.findViewById(R.id.tvSelectedCategory);
//            tvSelectedCategory.setText(selectedCategory.getName());
//        }
//    }
//}