package com.example.personalspendingapp.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserData {
    private String userId;
    private UserProfile profile;
    private Map<String, List<Category>> categories;
    private List<Transaction> transactions;
    private double budget;

    public UserData() {
        // Required empty constructor for Firestore
    }

    public UserData(String userId) {
        this.userId = userId;
        this.categories = new HashMap<>();
        this.categories.put("income", new ArrayList<>());
        this.categories.put("expense", new ArrayList<>());
        this.transactions = new ArrayList<>();
        this.budget = 0.0;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public UserProfile getProfile() { return profile; }
    public void setProfile(UserProfile profile) { this.profile = profile; }

    public Map<String, List<Category>> getCategories() { return categories; }
    public void setCategories(Map<String, List<Category>> categories) { this.categories = categories; }

    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }

    public double getBudget() { return budget; }
    public void setBudget(double budget) { this.budget = budget; }

    // Helper methods
    public void addTransaction(Transaction transaction) {
        if (transactions == null) {
            transactions = new ArrayList<>();
        }
        transactions.add(transaction);
    }

    public void addCategory(Category category) {
        if (categories == null) {
            categories = new HashMap<>();
            categories.put("income", new ArrayList<>());
            categories.put("expense", new ArrayList<>());
        }
        List<Category> categoryList = categories.get(category.getType());
        if (categoryList != null) {
            categoryList.add(category);
        }
    }

    public List<Transaction> getTransactionsByType(String type) {
        List<Transaction> filteredTransactions = new ArrayList<>();
        if (transactions != null) {
            for (Transaction transaction : transactions) {
                if (transaction.getType().equals(type)) {
                    filteredTransactions.add(transaction);
                }
            }
        }
        return filteredTransactions;
    }

    public List<Transaction> getTransactionsByDateRange(long startDate, long endDate) {
        List<Transaction> result = new ArrayList<>();
        if (transactions != null) {
            for (Transaction t : transactions) {
                long transactionTime = t.getDate().getTime();
                if (transactionTime >= startDate && transactionTime <= endDate) {
                    result.add(t);
                }
            }
        }
        return result;
    }
} 