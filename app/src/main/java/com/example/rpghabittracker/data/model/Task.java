package com.example.rpghabittracker.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

import java.io.Serializable;
import java.util.Locale;
import java.util.UUID;

@Entity(tableName = "tasks")
public class Task implements Serializable {
    
    @PrimaryKey
    @NonNull
    private String id;
    private String userId;
    private String name;
    private String description;
    private String categoryId;
    
    // Difficulty (Težina) - Base XP values
    public static final String DIFFICULTY_VERY_EASY = "VERY_EASY"; // 1 XP (base)
    public static final String DIFFICULTY_EASY = "EASY"; // 3 XP (base)
    public static final String DIFFICULTY_HARD = "HARD"; // 7 XP (base)
    public static final String DIFFICULTY_EXTREME = "EXTREME"; // 20 XP (base)
    
    // Importance (Bitnost) - Base XP values
    public static final String IMPORTANCE_NORMAL = "NORMAL"; // 1 XP (base)
    public static final String IMPORTANCE_IMPORTANT = "IMPORTANT"; // 3 XP (base)
    public static final String IMPORTANCE_VERY_IMPORTANT = "VERY_IMPORTANT"; // 10 XP (base)
    public static final String IMPORTANCE_SPECIAL = "SPECIAL"; // 100 XP (base)
    
    // Status
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED"; // Neurađen (auto-expired after 3 days)
    public static final String STATUS_PAUSED = "PAUSED"; // Only for recurring
    public static final String STATUS_CANCELLED = "CANCELLED"; // Otkazan (not user's fault)
    
    private String difficulty;
    private String importance;
    private int difficultyXp; // XP for difficulty (adjusted for level)
    private int importanceXp; // XP for importance (adjusted for level)
    private int totalXp; // Total XP value
    private String status;
    
    // Recurring task fields
    private boolean isRecurring;
    private String parentTaskId; // For recurring task instances
    private int repeatInterval; // 1, 2, 3...
    private String repeatUnit; // "DAY", "WEEK"
    private long startDate;
    private long endDate;
    
    // Timestamps
    private long createdAt;
    private long dueDate; // When the task should be done
    private long completedDate;
    
    // User level at creation (for XP calculation)
    private int userLevelAtCreation;
    
    // Quota tracking
    private boolean countsTowardQuota;
    
    public Task() {
        this.id = UUID.randomUUID().toString();
        this.status = STATUS_ACTIVE;
        this.createdAt = System.currentTimeMillis();
        this.isRecurring = false;
        this.countsTowardQuota = true;
        this.userLevelAtCreation = 1;
    }
    
    @Ignore
    public Task(String userId, String name, String difficulty, String importance, int userLevel) {
        this();
        this.userId = userId;
        this.name = name;
        this.userLevelAtCreation = Math.max(1, userLevel);
        this.difficulty = normalizeDifficulty(difficulty);
        this.importance = normalizeImportance(importance);
        calculateXpValues(this.userLevelAtCreation);
    }
    
    // Calculate XP values based on difficulty, importance, and user level
    public void calculateXpValues(int userLevel) {
        int safeLevel = Math.max(1, userLevel);
        this.difficultyXp = getDifficultyXpForLevel(difficulty, safeLevel);
        this.importanceXp = getImportanceXpForLevel(importance, safeLevel);
        this.totalXp = difficultyXp + importanceXp;
    }
    
    // Get difficulty XP for a specific level
    // Formula: XP_prev + XP_prev / 2 (rounded) for each level after 1
    public static int getDifficultyXpForLevel(String difficulty, int level) {
        int baseXp = getBaseDifficultyXp(difficulty);
        int safeLevel = Math.max(1, level);
        if (safeLevel == 1) return baseXp;
        
        int xp = baseXp;
        for (int i = 2; i <= safeLevel; i++) {
            xp = (int) Math.round(xp + xp / 2.0);
        }
        return xp;
    }

    // Get importance XP for a specific level
    // Formula: XP_prev + XP_prev / 2 (rounded) for each level after 1
    public static int getImportanceXpForLevel(String importance, int level) {
        int baseXp = getBaseImportanceXp(importance);
        int safeLevel = Math.max(1, level);
        if (safeLevel == 1) return baseXp;

        int xp = baseXp;
        for (int i = 2; i <= safeLevel; i++) {
            xp = (int) Math.round(xp + xp / 2.0);
        }
        return xp;
    }
    
    // Base XP values for difficulty (level 1)
    private static int getBaseDifficultyXp(String difficulty) {
        if (difficulty == null) return 0;
        switch (difficulty) {
            case DIFFICULTY_VERY_EASY: return 1;
            case DIFFICULTY_EASY: return 3;
            case DIFFICULTY_HARD: return 7;
            case DIFFICULTY_EXTREME: return 20;
            default: return 0;
        }
    }
    
    // Base XP values for importance (level 1)
    private static int getBaseImportanceXp(String importance) {
        if (importance == null) return 0;
        switch (importance) {
            case IMPORTANCE_NORMAL: return 1;
            case IMPORTANCE_IMPORTANT: return 3;
            case IMPORTANCE_VERY_IMPORTANT: return 10;
            case IMPORTANCE_SPECIAL: return 100;
            default: return 0;
        }
    }

    private static String normalizeDifficulty(String difficulty) {
        if (difficulty == null) return null;
        String normalized = difficulty.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        if (normalized.isEmpty()) return null;
        if ("VERYEASY".equals(normalized)) return DIFFICULTY_VERY_EASY;
        switch (normalized) {
            case DIFFICULTY_VERY_EASY:
            case DIFFICULTY_EASY:
            case DIFFICULTY_HARD:
            case DIFFICULTY_EXTREME:
                return normalized;
            default:
                return null;
        }
    }

    private static String normalizeImportance(String importance) {
        if (importance == null) return null;
        String normalized = importance.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        if (normalized.isEmpty()) return null;
        if ("VERYIMPORTANT".equals(normalized)) return IMPORTANCE_VERY_IMPORTANT;
        switch (normalized) {
            case IMPORTANCE_NORMAL:
            case IMPORTANCE_IMPORTANT:
            case IMPORTANCE_VERY_IMPORTANT:
            case IMPORTANCE_SPECIAL:
                return normalized;
            default:
                return null;
        }
    }

    private static String normalizeStatus(String status) {
        if (status == null) return STATUS_ACTIVE;
        String normalized = status.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        if (normalized.isEmpty()) return STATUS_ACTIVE;
        if ("CANCELED".equals(normalized)) return STATUS_CANCELLED;
        switch (normalized) {
            case STATUS_ACTIVE:
            case STATUS_COMPLETED:
            case STATUS_FAILED:
            case STATUS_PAUSED:
            case STATUS_CANCELLED:
                return normalized;
            default:
                return normalized;
        }
    }
    
    // Check if task is expired (3 days after due date)
    public boolean isExpired() {
        if (STATUS_COMPLETED.equals(status) || STATUS_CANCELLED.equals(status)) {
            return false;
        }
        if (dueDate <= 0L) {
            return false;
        }
        long now = System.currentTimeMillis();
        long threeDaysInMillis = 3 * 24 * 60 * 60 * 1000L;
        return now > (dueDate + threeDaysInMillis);
    }
    
    // Getters and Setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) {
        if (id == null || id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
            return;
        }
        this.id = id.trim();
    }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { 
        this.difficulty = normalizeDifficulty(difficulty);
        calculateXpValues(userLevelAtCreation);
    }
    
    public String getImportance() { return importance; }
    public void setImportance(String importance) { 
        this.importance = normalizeImportance(importance);
        calculateXpValues(userLevelAtCreation);
    }
    
    public int getDifficultyXp() { return difficultyXp; }
    public void setDifficultyXp(int difficultyXp) { this.difficultyXp = difficultyXp; }
    
    public int getImportanceXp() { return importanceXp; }
    public void setImportanceXp(int importanceXp) { this.importanceXp = importanceXp; }
    
    public int getTotalXp() { return totalXp; }
    public void setTotalXp(int totalXp) { this.totalXp = totalXp; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = normalizeStatus(status); }
    
    public boolean isRecurring() { return isRecurring; }
    public void setRecurring(boolean recurring) { isRecurring = recurring; }
    
    public String getParentTaskId() { return parentTaskId; }
    public void setParentTaskId(String parentTaskId) { this.parentTaskId = parentTaskId; }
    
    public int getRepeatInterval() { return repeatInterval; }
    public void setRepeatInterval(int repeatInterval) { this.repeatInterval = repeatInterval; }
    
    public String getRepeatUnit() { return repeatUnit; }
    public void setRepeatUnit(String repeatUnit) { this.repeatUnit = repeatUnit; }
    
    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }
    
    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public long getDueDate() { return dueDate; }
    public void setDueDate(long dueDate) { this.dueDate = dueDate; }
    
    public long getCompletedDate() { return completedDate; }
    public void setCompletedDate(long completedDate) { this.completedDate = completedDate; }
    
    public int getUserLevelAtCreation() { return userLevelAtCreation; }
    public void setUserLevelAtCreation(int userLevelAtCreation) { 
        this.userLevelAtCreation = Math.max(1, userLevelAtCreation);
        calculateXpValues(this.userLevelAtCreation);
    }
    
    public boolean isCountsTowardQuota() { return countsTowardQuota; }
    public void setCountsTowardQuota(boolean countsTowardQuota) { this.countsTowardQuota = countsTowardQuota; }
}
