package com.example.personalspendingapp.data.model;

public class Expense {
    private String date;
    private String note;
    private double amount;
    private String category;

    // Constructor mặc định (cần cho Firestore)
    public Expense() {}

    public Expense(String date, String note, double amount, String category) {
        this.date = date;
        this.note = note;
        this.amount = amount;
        this.category = category;
    }

    // Getters và Setters
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
