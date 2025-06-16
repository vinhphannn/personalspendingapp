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
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.ViewHolder> {

    private List<CalendarDay> calendarDays;
    private final NumberFormat currencyFormat;
    private final OnDayDoubleClickListener doubleClickListener;
    private CalendarDay selectedDay;

    public interface OnDayDoubleClickListener {
        void onDayDoubleClick(CalendarDay day);
    }

    public CalendarDayAdapter(List<CalendarDay> calendarDays, OnDayDoubleClickListener doubleClickListener, NumberFormat currencyFormat) {
        this.calendarDays = calendarDays;
        this.doubleClickListener = doubleClickListener;
        this.currencyFormat = currencyFormat;
    }

    public void setSelectedDay(CalendarDay day) {
        this.selectedDay = day;
        notifyDataSetChanged();
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

            // Highlight selected day
            if (selectedDay != null && day.getDate() != null && selectedDay.getDate() != null &&
                    isSameDay(day.getDate(), selectedDay.getDate())) {
                holder.itemView.setBackgroundResource(R.drawable.selected_day_background);
            } else {
                holder.itemView.setBackgroundResource(0);
            }

            // Handle double click
            holder.itemView.setOnClickListener(v -> {
                if (doubleClickListener != null) {
                    setSelectedDay(day);
                    doubleClickListener.onDayDoubleClick(day);
                }
            });

            // Format and display income/expense
            holder.tvDailyIncome.setVisibility(View.VISIBLE);
            holder.tvDailyIncome.setText(currencyFormat.format(day.getTotalIncome()));
            holder.tvDailyIncome.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));

            holder.tvDailyExpense.setVisibility(View.VISIBLE);
            holder.tvDailyExpense.setText(currencyFormat.format(day.getTotalExpense()));
            holder.tvDailyExpense.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
        } else {
            holder.itemView.setVisibility(View.INVISIBLE);
        }
    }

    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    @Override
    public int getItemCount() {
        return calendarDays.size();
    }

    public void updateData(List<CalendarDay> newCalendarDays, NumberFormat currencyFormat) {
        this.calendarDays = newCalendarDays;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayNumber, tvDailyIncome, tvDailyExpense;

        ViewHolder(View view) {
            super(view);
            tvDayNumber = view.findViewById(R.id.tvDayNumber);
            tvDailyIncome = view.findViewById(R.id.tvDailyIncome);
            tvDailyExpense = view.findViewById(R.id.tvDailyExpense);
        }
    }
} 