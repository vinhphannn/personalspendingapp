package com.example.personalspendingapp.models;

public class Category {
    private String id;
    private String name;
    private String icon;
    private String type;

    public Category() {
        // Required empty constructor for Firestore
    }

    public Category(String id, String name, String icon, String type) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.type = type;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
} 