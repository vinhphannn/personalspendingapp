package com.example.personalspendingapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personalspendingapp.R;
import com.example.personalspendingapp.models.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
    private List<Category> categories;
    private final OnCategoryClickListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
        void onOtherClick();
    }

    public CategoryAdapter(List<Category> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    public void updateCategories(List<Category> newCategories) {
        this.categories = newCategories;
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        if (oldPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(oldPosition);
        }
        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        if (position == categories.size()) {
            // Item "Khác"
            holder.bindOther();
        } else {
        Category category = categories.get(position);
            holder.bind(category, position == selectedPosition);
        }
    }

    @Override
    public int getItemCount() {
        return categories.size() + 1; // +1 for "Other" item
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCategoryName;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
        }

        void bind(Category category, boolean isSelected) {
            tvCategoryName.setText(category.getName());
            tvCategoryName.setBackgroundResource(isSelected ? R.drawable.bg_category_selected : R.drawable.bg_category_normal);
            tvCategoryName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    setSelectedPosition(position);
                    listener.onCategoryClick(category);
                }
            });
        }

        void bindOther() {
            tvCategoryName.setText("Khác");
            tvCategoryName.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_add, 0);
            tvCategoryName.setBackgroundResource(R.drawable.bg_category_normal);
            itemView.setOnClickListener(v -> listener.onOtherClick());
        }
    }
}