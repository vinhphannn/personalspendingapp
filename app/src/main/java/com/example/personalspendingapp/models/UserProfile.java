package com.example.personalspendingapp.models;

public class UserProfile {
    private String name;
    private String email;
    private String phone;
    private String currency;
    private String language;
    private long createdAt;

    public UserProfile() {
        // Required empty constructor for Firestore
    }

    public UserProfile(String name, String email, String phone, String currency, String language) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.currency = currency;
        this.language = language;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
} 