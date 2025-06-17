package com.example.personalspendingapp.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personalspendingapp.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CategoryDetailAdapter extends RecyclerView.Adapter<CategoryDetailAdapter.ViewHolder> {
    private final List<Map.Entry<String, Double>> categoryDetails;
    private final double totalAmount;
    private final NumberFormat currencyFormat;
    private final NumberFormat percentFormat;
    private final int[] colors;
    private final Context context;

    public CategoryDetailAdapter(Context context, List<Map.Entry<String, Double>> categoryDetails, double totalAmount, int[] colors) {
        this.context = context;
        this.categoryDetails = categoryDetails;
        this.totalAmount = totalAmount;
        this.colors = colors;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        this.percentFormat = NumberFormat.getPercentInstance(new Locale("vi", "VN"));
        this.percentFormat.setMaximumFractionDigits(1);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map.Entry<String, Double> entry = categoryDetails.get(position);
        String categoryName = entry.getKey();
        double amount = entry.getValue();
        double percentage = totalAmount > 0 ? amount / totalAmount : 0;

        // Set background color with rounded corners
        int color = colors[position % colors.length];
        GradientDrawable background = (GradientDrawable) ContextCompat.getDrawable(context, R.drawable.category_item_background);
        if (background != null) {
            background.setColor(color);
            holder.itemView.setBackground(background);
        }
        
        // Set text color based on background brightness
        int textColor = isColorDark(color) ? Color.WHITE : Color.BLACK;
        holder.tvCategoryName.setTextColor(textColor);
        holder.tvCategoryAmount.setTextColor(textColor);
        holder.tvCategoryPercentage.setTextColor(isColorDark(color) ? Color.parseColor("#E0E0E0") : Color.parseColor("#757575"));

        holder.tvCategoryName.setText(categoryName);
        holder.tvCategoryAmount.setText(currencyFormat.format(amount));
        holder.tvCategoryPercentage.setText(percentFormat.format(percentage));
    }

    @Override
    public int getItemCount() {
        return categoryDetails.size();
    }

    private boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName;
        TextView tvCategoryAmount;
        TextView tvCategoryPercentage;

        ViewHolder(View view) {
            super(view);
            tvCategoryName = view.findViewById(R.id.tvCategoryName);
            tvCategoryAmount = view.findViewById(R.id.tvCategoryAmount);
            tvCategoryPercentage = view.findViewById(R.id.tvCategoryPercentage);
        }
    }
} 