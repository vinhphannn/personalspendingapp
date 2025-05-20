package com.example.personalspendingapp.models;

import java.util.List;
import java.util.Map;

public class UserCategories {
    private String userId;
    private Map<String, List<String>> categories;

    public UserCategories() {
        // Required empty constructor for Firebase
    }

    public UserCategories(String userId, Map<String, List<String>> categories) {
        this.userId = userId;
        this.categories = categories;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Map<String, List<String>> getCategories() {
        return categories;
    }

    public void setCategories(Map<String, List<String>> categories) {
        this.categories = categories;
    }
} 