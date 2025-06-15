package com.example.personalspendingapp.models;

import java.util.Date;

public class Transaction {
    private String id;
    private double amount;
    private String type;
    private String categoryId;
    private String note;
    private long date;
    private long createdAt;

    public Transaction() {
        // Required empty constructor for Firestore
    }

    public Transaction(String id, double amount, String type, String categoryId, String note, Date date) {
        this.id = id;
        this.amount = amount;
        this.type = type;
        this.categoryId = categoryId;
        this.note = note;
        this.date = date.getTime();
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Date getDate() { return new Date(date); }
    public void setDate(Date date) { this.date = date.getTime(); }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
} 