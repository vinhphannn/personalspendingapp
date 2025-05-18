package com.example.personalspendingapp.data.model;

public class Category {
    private String name;

    // Constructor mặc định (cần cho Firestore)
    public Category() {}

    public Category(String name) {
        this.name = name;
    }

    // Getters và Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}