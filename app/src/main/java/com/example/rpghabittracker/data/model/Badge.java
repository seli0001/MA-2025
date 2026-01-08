package com.example.rpghabittracker.data.model;

import java.io.Serializable;

/**
 * Badge model for achievements
 */
public class Badge implements Serializable {
    
    private String id;
    private String name;
    private String description;
    private String icon;
    private boolean unlocked;
    private long unlockedAt;
    private BadgeType type;
    private int requirement;
    
    public enum BadgeType {
        TASKS_COMPLETED,    // Complete X tasks
        STREAK,             // Achieve X day streak
        LEVEL,              // Reach level X
        BOSSES_DEFEATED,    // Defeat X bosses
        XP_EARNED,          // Earn X total XP
        COINS_EARNED,       // Earn X total coins
        SPECIAL_MISSIONS,   // Complete X special missions
        FIRST_TASK,         // Complete first task
        FIRST_BOSS,         // Defeat first boss
        ALLIANCE_JOIN       // Join an alliance
    }
    
    // Predefined badges
    public static final Badge[] ALL_BADGES = {
        new Badge("first_task", "Prvi Korak", "Završi svoj prvi zadatak", "🎯", BadgeType.FIRST_TASK, 1),
        new Badge("task_10", "Učenik", "Završi 10 zadataka", "📚", BadgeType.TASKS_COMPLETED, 10),
        new Badge("task_50", "Marljivi", "Završi 50 zadataka", "💪", BadgeType.TASKS_COMPLETED, 50),
        new Badge("task_100", "Veteran", "Završi 100 zadataka", "🏆", BadgeType.TASKS_COMPLETED, 100),
        new Badge("task_500", "Legenda", "Završi 500 zadataka", "👑", BadgeType.TASKS_COMPLETED, 500),
        
        new Badge("streak_3", "Uporni", "Održi niz od 3 dana", "🔥", BadgeType.STREAK, 3),
        new Badge("streak_7", "Nedelja Fokusa", "Održi niz od 7 dana", "⚡", BadgeType.STREAK, 7),
        new Badge("streak_30", "Mesečna Pobeda", "Održi niz od 30 dana", "🌟", BadgeType.STREAK, 30),
        
        new Badge("level_5", "Avanturista", "Dostigni nivo 5", "⚔️", BadgeType.LEVEL, 5),
        new Badge("level_10", "Ratnik", "Dostigni nivo 10", "🗡️", BadgeType.LEVEL, 10),
        new Badge("level_20", "Šampion", "Dostigni nivo 20", "🛡️", BadgeType.LEVEL, 20),
        
        new Badge("first_boss", "Ubica Zmajeva", "Pobedi svog prvog bosa", "🐉", BadgeType.FIRST_BOSS, 1),
        new Badge("boss_10", "Lovac na Čudovišta", "Pobedi 10 bosova", "👹", BadgeType.BOSSES_DEFEATED, 10),
        
        new Badge("xp_1000", "XP Kolektor", "Zaradi 1000 XP", "✨", BadgeType.XP_EARNED, 1000),
        new Badge("xp_10000", "XP Master", "Zaradi 10000 XP", "💫", BadgeType.XP_EARNED, 10000),
        
        new Badge("alliance", "Timski Igrač", "Pridruži se savezu", "🤝", BadgeType.ALLIANCE_JOIN, 1)
    };
    
    public Badge() {}
    
    public Badge(String id, String name, String description, String icon, BadgeType type, int requirement) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.type = type;
        this.requirement = requirement;
        this.unlocked = false;
    }
    
    // Check if badge should be unlocked based on user stats
    public boolean checkUnlock(int tasksCompleted, int streak, int level, int bossesDefeated, 
                               int xpEarned, boolean hasAlliance) {
        switch (type) {
            case FIRST_TASK:
            case TASKS_COMPLETED:
                return tasksCompleted >= requirement;
            case STREAK:
                return streak >= requirement;
            case LEVEL:
                return level >= requirement;
            case FIRST_BOSS:
            case BOSSES_DEFEATED:
                return bossesDefeated >= requirement;
            case XP_EARNED:
                return xpEarned >= requirement;
            case ALLIANCE_JOIN:
                return hasAlliance;
            default:
                return false;
        }
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    
    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
    
    public long getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(long unlockedAt) { this.unlockedAt = unlockedAt; }
    
    public BadgeType getType() { return type; }
    public void setType(BadgeType type) { this.type = type; }
    
    public int getRequirement() { return requirement; }
    public void setRequirement(int requirement) { this.requirement = requirement; }
}
