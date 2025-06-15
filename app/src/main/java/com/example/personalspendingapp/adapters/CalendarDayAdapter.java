package com.example.personalspendingapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personalspendingapp.R;
import com.example.personalspendingapp.models.CalendarDay;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.ViewHolder> {

    private List<CalendarDay> calendarDays;
    private final NumberFormat currencyFormat;
    private final OnDayDoubleClickListener doubleClickListener;

    public interface OnDayDoubleClickListener {
        void onDayDoubleClick(CalendarDay day);
    }

    public CalendarDayAdapter(List<CalendarDay> calendarDays, OnDayDoubleClickListener doubleClickListener, NumberFormat currencyFormat) {
        this.calendarDays = calendarDays;
        this.doubleClickListener = doubleClickListener;
        this.currencyFormat = currencyFormat;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CalendarDay day = calendarDays.get(position);

        // Don't display day number for empty cells
        if (day.getDayOfMonth() > 0) {
            holder.tvDayNumber.setText(String.valueOf(day.getDayOfMonth()));
            holder.itemView.setVisibility(View.VISIBLE);
             // Handle double click
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                private static final long DOUBLE_CLICK_TIME_DELTA = 300; // milliseconds
                long lastClickTime = 0;

                @Override
                public void onClick(View v) {
                    long clickTime = System.currentTimeMillis();
                    if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                        if (doubleClickListener != null) {
                            doubleClickListener.onDayDoubleClick(day);
                        }
                        lastClickTime = 0; // Reset after double click
                    } else {
                        // Single click logic if needed, currently none specified
                        lastClickTime = clickTime;
                    }
                }
            });

            // Format and display income/expense
            // Use the NumberFormat and ensure TextViews are visible even if amount is 0
            holder.tvDailyIncome.setVisibility(View.VISIBLE); // Always visible
            holder.tvDailyIncome.setText(currencyFormat.format(day.getTotalIncome()));
            holder.tvDailyIncome.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark)); // Set income color to green

            holder.tvDailyExpense.setVisibility(View.VISIBLE); // Always visible
            holder.tvDailyExpense.setText(currencyFormat.format(day.getTotalExpense()));
            holder.tvDailyExpense.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark)); // Set expense color to red

        } else {
            // Hide empty cells and disable clicks
            holder.tvDayNumber.setText("");
            holder.tvDailyIncome.setText(""); // Clear text for empty cells
            holder.tvDailyExpense.setText(""); // Clear text for empty cells
            holder.itemView.setVisibility(View.INVISIBLE);
             holder.itemView.setOnClickListener(null); // Disable click for empty items
        }
    }

    @Override
    public int getItemCount() {
        return calendarDays.size();
    }

    public void updateData(List<CalendarDay> newCalendarDays, NumberFormat currencyFormat) {
        this.calendarDays = newCalendarDays;
        // The currencyFormat is now final and set in the constructor, no need to update here
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayNumber;
        TextView tvDailyIncome;
        TextView tvDailyExpense;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
            tvDailyIncome = itemView.findViewById(R.id.tvDailyIncome);
            tvDailyExpense = itemView.findViewById(R.id.tvDailyExpense);
        }
    }
} 