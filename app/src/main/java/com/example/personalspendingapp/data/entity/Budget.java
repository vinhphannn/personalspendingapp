package com.example.personalspendingapp.data.entity;

import com.google.firebase.firestore.DocumentId;

public class Budget {
    @DocumentId
    private String id;
    private String categoryName;
    private double amount;
    private double spentAmount;

    public Budget() {}

    public Budget(String categoryName, double amount, double spentAmount) {
        this.categoryName = categoryName;
        this.amount = amount;
        this.spentAmount = spentAmount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public double getSpentAmount() { return spentAmount; }
    public void setSpentAmount(double spentAmount) { this.spentAmount = spentAmount; }
}