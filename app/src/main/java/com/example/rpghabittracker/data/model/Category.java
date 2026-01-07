package com.example.rpghabittracker.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "categories")
public class Category implements Serializable {
    
    @PrimaryKey(autoGenerate = false)
    @NonNull
    private String id;
    private String userId;
    private String name;
    private String color; // Hex color code
    private long createdAt;
    
    public Category() {
        this.id = "";
        this.createdAt = System.currentTimeMillis();
    }
    
    @Ignore
    public Category(String userId, String name, String color) {
        this();
        this.userId = userId;
        this.name = name;
        this.color = color;
    }
    
    // Getters and Setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
