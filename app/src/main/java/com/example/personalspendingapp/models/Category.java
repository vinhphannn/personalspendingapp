package com.example.personalspendingapp.models;

public class Category {
    private String id;
    private String userId;
    private String name;
    private String type; // "expense" or "income"
    private boolean isDefault;

    public Category() {
        // Required empty constructor for Firebase
    }

    public Category(String id, String userId, String name, String type, boolean isDefault) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.isDefault = isDefault;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
} 