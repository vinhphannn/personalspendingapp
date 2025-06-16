package com.example.personalspendingapp.models;

import java.util.Date;

public class CalendarDay {
    private int dayOfMonth;
    private double totalIncome;
    private double totalExpense;
    private Date date; // Store the actual date

    public CalendarDay(int dayOfMonth, Date date, double totalIncome, double totalExpense) {
        this.dayOfMonth = dayOfMonth;
        this.date = date;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(int dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public double getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(double totalIncome) {
        this.totalIncome = totalIncome;
    }

    public double getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(double totalExpense) {
        this.totalExpense = totalExpense;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    // Optional: Add a method to check if this day is part of the displayed month
    // Useful for graying out days from previous/next months
} 