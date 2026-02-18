package com.example.rpghabittracker.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity(tableName = "users")
public class User implements Serializable {
    
    @PrimaryKey(autoGenerate = false)
    @NonNull
    private String id;
    private String email;
    
    @Exclude
    private String password; // Not stored in Firestore
    
    private String username;
    private String avatar;
    private int level;
    private String title;
    private int powerPoints; // PP - total power
    private int basePowerPoints; // PP from leveling
    private int experiencePoints; // Current XP
    private int coins;
    private boolean isActive; // Email verification status
    private long createdAt;
    private long lastLoginAt;
    
    // Inventory - IDs of equipment
    private List<String> potionIds;
    private List<String> clothingIds;
    private List<String> weaponIds;
    
    // Active equipment
    private List<String> activePotionIds;
    private Map<String, Integer> activeClothingDurability; // clothing ID -> battles remaining
    private Map<String, Double> weaponUpgrades; // weapon type -> upgrade level
    
    // Statistics
    private int totalTasksCreated;
    private int totalTasksCompleted;
    private int totalTasksFailed;
    private int totalTasksCancelled;
    private int currentStreak;
    private int longestStreak;
    private int activeDays;
    private int specialMissionsCompleted;
    
    // Current stage tracking
    private long currentLevelStartTime;
    private int tasksCompletedThisLevel;
    private int tasksCreatedThisLevel;
    
    // Badges
    private List<String> badges;
    
    public User() {
        // Default constructor for Firebase and Room
        this.id = "";
        this.potionIds = new ArrayList<>();
        this.clothingIds = new ArrayList<>();
        this.weaponIds = new ArrayList<>();
        this.activePotionIds = new ArrayList<>();
        this.activeClothingDurability = new HashMap<>();
        this.weaponUpgrades = new HashMap<>();
        this.badges = new ArrayList<>();
    }
    
    @Ignore
    public User(String email, String password, String username, String avatar) {
        this();
        this.email = email;
        this.password = password;
        this.username = username;
        this.avatar = avatar;
        this.level = 1;
        this.title = "Novajlija"; // Default title for level 1
        this.powerPoints = 0;
        this.basePowerPoints = 0;
        this.experiencePoints = 0;
        this.coins = 0;
        this.isActive = false;
        this.createdAt = System.currentTimeMillis();
        this.lastLoginAt = System.currentTimeMillis();
        this.currentLevelStartTime = System.currentTimeMillis();
        this.totalTasksCreated = 0;
        this.totalTasksCompleted = 0;
        this.totalTasksFailed = 0;
        this.totalTasksCancelled = 0;
        this.currentStreak = 0;
        this.longestStreak = 0;
        this.activeDays = 0;
        this.specialMissionsCompleted = 0;
        this.tasksCompletedThisLevel = 0;
        this.tasksCreatedThisLevel = 0;
    }
    
    // Calculate XP needed for next level
    @Exclude
    public int getXpForNextLevel() {
        // getXpForLevel(level) returns the XP required to complete the current level:
        // level 1 → 200, level 2 → 500, level 3 → 1250, …
        return getXpForLevel(level);
    }
    
    // Get XP required for a specific level
    @Exclude
    public static int getXpForLevel(int targetLevel) {
        if (targetLevel <= 1) return 200;
        int xp = 200;
        for (int i = 2; i <= targetLevel; i++) {
            // Formula: XP_prev * 2 + XP_prev / 2 (without rounding to hundreds)
            xp = (int) Math.round(xp * 2 + xp / 2.0);
        }
        return xp;
    }
    
    // Calculate base PP for current level (without equipment).
    // Expected progression by level value: 0, 40, 70, 123, ...
    @Exclude
    public int getPpForCurrentLevel() {
        return getBasePpForLevel(level);
    }

    @Exclude
    public static int getBasePpForLevel(int currentLevel) {
        int safeLevel = Math.max(1, currentLevel);
        if (safeLevel <= 1) return 0;

        int pp = 40; // Value after reaching level 2
        for (int reachedLevel = 3; reachedLevel <= safeLevel; reachedLevel++) {
            pp = (int) Math.round(pp + pp * 3.0 / 4.0);
        }
        return pp;
    }

    // PP reward granted when the user reaches the given level.
    // Example values: reached level 2 -> 40, level 3 -> 70, level 4 -> 123.
    @Exclude
    public static int getPpRewardForReachedLevel(int reachedLevel) {
        return getBasePpForLevel(reachedLevel);
    }
    
    // Calculate total PP including equipment bonuses
    @Exclude
    public int getTotalPowerPoints() {
        // TODO: Add equipment bonuses calculation
        return powerPoints;
    }
    
    // Getters and Setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public int getPowerPoints() { return powerPoints; }
    public void setPowerPoints(int powerPoints) { this.powerPoints = powerPoints; }
    
    public int getBasePowerPoints() { return basePowerPoints; }
    public void setBasePowerPoints(int basePowerPoints) { this.basePowerPoints = basePowerPoints; }
    
    public int getExperiencePoints() { return experiencePoints; }
    public void setExperiencePoints(int experiencePoints) { this.experiencePoints = experiencePoints; }
    
    // Alias for getExperiencePoints (convenience method)
    public int getXp() { return experiencePoints; }
    public void setXp(int xp) { this.experiencePoints = xp; }
    
    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public long getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(long lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    
    public List<String> getPotionIds() { 
        if (potionIds == null) potionIds = new ArrayList<>();
        return potionIds; 
    }
    public void setPotionIds(List<String> potionIds) { this.potionIds = potionIds; }
    
    public List<String> getClothingIds() {
        if (clothingIds == null) clothingIds = new ArrayList<>();
        return clothingIds;
    }
    public void setClothingIds(List<String> clothingIds) { this.clothingIds = clothingIds; }
    
    public List<String> getWeaponIds() {
        if (weaponIds == null) weaponIds = new ArrayList<>();
        return weaponIds;
    }
    public void setWeaponIds(List<String> weaponIds) { this.weaponIds = weaponIds; }
    
    public List<String> getActivePotionIds() {
        if (activePotionIds == null) activePotionIds = new ArrayList<>();
        return activePotionIds;
    }
    public void setActivePotionIds(List<String> activePotionIds) { this.activePotionIds = activePotionIds; }
    
    public Map<String, Integer> getActiveClothingDurability() {
        if (activeClothingDurability == null) activeClothingDurability = new HashMap<>();
        return activeClothingDurability;
    }
    public void setActiveClothingDurability(Map<String, Integer> activeClothingDurability) { 
        this.activeClothingDurability = activeClothingDurability;
    }
    
    public Map<String, Double> getWeaponUpgrades() {
        if (weaponUpgrades == null) weaponUpgrades = new HashMap<>();
        return weaponUpgrades;
    }
    public void setWeaponUpgrades(Map<String, Double> weaponUpgrades) { this.weaponUpgrades = weaponUpgrades; }
    
    public int getTotalTasksCreated() { return totalTasksCreated; }
    public void setTotalTasksCreated(int totalTasksCreated) { this.totalTasksCreated = totalTasksCreated; }
    
    public int getTotalTasksCompleted() { return totalTasksCompleted; }
    public void setTotalTasksCompleted(int totalTasksCompleted) { this.totalTasksCompleted = totalTasksCompleted; }
    
    public int getTotalTasksFailed() { return totalTasksFailed; }
    public void setTotalTasksFailed(int totalTasksFailed) { this.totalTasksFailed = totalTasksFailed; }
    
    public int getTotalTasksCancelled() { return totalTasksCancelled; }
    public void setTotalTasksCancelled(int totalTasksCancelled) { this.totalTasksCancelled = totalTasksCancelled; }
    
    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }
    
    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }
    
    public int getActiveDays() { return activeDays; }
    public void setActiveDays(int activeDays) { this.activeDays = activeDays; }
    
    public int getSpecialMissionsCompleted() { return specialMissionsCompleted; }
    public void setSpecialMissionsCompleted(int specialMissionsCompleted) { 
        this.specialMissionsCompleted = specialMissionsCompleted;
    }
    
    public long getCurrentLevelStartTime() { return currentLevelStartTime; }
    public void setCurrentLevelStartTime(long currentLevelStartTime) { 
        this.currentLevelStartTime = currentLevelStartTime;
    }
    
    public int getTasksCompletedThisLevel() { return tasksCompletedThisLevel; }
    public void setTasksCompletedThisLevel(int tasksCompletedThisLevel) { 
        this.tasksCompletedThisLevel = tasksCompletedThisLevel;
    }
    
    public int getTasksCreatedThisLevel() { return tasksCreatedThisLevel; }
    public void setTasksCreatedThisLevel(int tasksCreatedThisLevel) { 
        this.tasksCreatedThisLevel = tasksCreatedThisLevel;
    }
    
    public List<String> getBadges() {
        if (badges == null) badges = new ArrayList<>();
        return badges;
    }
    public void setBadges(List<String> badges) { this.badges = badges; }
}
