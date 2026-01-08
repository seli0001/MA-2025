package com.example.rpghabittracker.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "bosses")
public class Boss implements Serializable {
    
    @PrimaryKey
    @NonNull
    private String id;
    private String userId; // Owner of this boss instance
    private int level; // Which level this boss is for
    private String name;
    private int maxHp;
    private int currentHp;
    private boolean isDefeated;
    private int coinsReward;
    private String spriteUrl; // Path to boss sprite image
    private long defeatedDate;
    
    public Boss() {
        this.id = "";
    }
    
    @Ignore
    public Boss(String userId, int level) {
        this.id = userId + "_boss_" + level;
        this.userId = userId;
        this.level = level;
        this.name = getBossNameForLevel(level);
        this.maxHp = calculateBossHp(level);
        this.currentHp = this.maxHp;
        this.isDefeated = false;
        this.coinsReward = calculateCoinsReward(level);
    }
    
    // Get boss name for level
    public static String getBossNameForLevel(int level) {
        String[] names = {
            "Zmaj Početnik", "Zmaj Učenik", "Zmaj Borac", "Zmaj Veteran", 
            "Zmaj Majstor", "Zmaj Heroj", "Zmaj Šampion", "Zmaj Legenda",
            "Zmaj Titan", "Zmaj Bog"
        };
        int index = Math.min(level - 1, names.length - 1);
        return names[index];
    }
    
    // Calculate boss HP for a given level
    // Level 1: 200 HP
    // Formula: HP_prev * 2 + HP_prev / 2
    public static int calculateBossHp(int level) {
        if (level == 1) return 200;
        int hp = 200;
        for (int i = 2; i <= level; i++) {
            hp = hp * 2 + hp / 2;
        }
        return hp;
    }
    
    // Alias for calculateBossHp (used by BattleActivity)
    public static int getBossHpForLevel(int level) {
        return calculateBossHp(level);
    }
    
    // Calculate coins reward for defeating boss
    // Level 1: 200 coins
    // Each level: +20% from previous
    public static int calculateCoinsReward(int level) {
        if (level == 1) return 200;
        double coins = 200;
        for (int i = 2; i <= level; i++) {
            coins = coins * 1.2;
        }
        return (int) Math.round(coins);
    }
    
    // Alias for calculateCoinsReward (used by BattleActivity)
    public static int getCoinsRewardForLevel(int level) {
        return calculateCoinsReward(level);
    }
    
    // Getters and Setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    
    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }
    
    public int getCurrentHp() { return currentHp; }
    public void setCurrentHp(int currentHp) { this.currentHp = currentHp; }
    
    public boolean isDefeated() { return isDefeated; }
    public void setDefeated(boolean defeated) { isDefeated = defeated; }
    
    public int getCoinsReward() { return coinsReward; }
    public void setCoinsReward(int coinsReward) { this.coinsReward = coinsReward; }
    
    public String getSpriteUrl() { return spriteUrl; }
    public void setSpriteUrl(String spriteUrl) { this.spriteUrl = spriteUrl; }
    
    public long getDefeatedDate() { return defeatedDate; }
    public void setDefeatedDate(long defeatedDate) { this.defeatedDate = defeatedDate; }
}
