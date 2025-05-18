package com.example.personalspendingapp.models;

import java.util.Date;

public class Expense {
    private String id;
    private String userId;
    private double amount;
    private Date date;
    private String note;
    private String category;
    private Date createdAt;

    public Expense() {
        // Required empty constructor for Firebase
    }

    public Expense(String id, String userId, double amount, Date date, String note, String category) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.date = date;
        this.note = note;
        this.category = category;
        this.createdAt = new Date();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
} 