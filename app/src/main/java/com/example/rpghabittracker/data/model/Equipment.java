package com.example.rpghabittracker.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "equipment")
public class Equipment implements Serializable {
    
    // Equipment Types
    public static final String TYPE_POTION = "POTION";
    public static final String TYPE_CLOTHING = "CLOTHING";
    public static final String TYPE_WEAPON = "WEAPON";
    
    // Potion subtypes
    public static final String POTION_PP_20 = "PP_20_SINGLE"; // +20% PP single use
    public static final String POTION_PP_40 = "PP_40_SINGLE"; // +40% PP single use
    public static final String POTION_PP_5_PERM = "PP_5_PERMANENT"; // +5% PP permanent
    public static final String POTION_PP_10_PERM = "PP_10_PERMANENT"; // +10% PP permanent
    
    // Clothing subtypes
    public static final String CLOTHING_GLOVES = "GLOVES"; // +10% PP
    public static final String CLOTHING_SHIELD = "SHIELD"; // +10% attack success rate
    public static final String CLOTHING_BOOTS = "BOOTS"; // 40% chance for +1 attack
    
    // Weapon subtypes
    public static final String WEAPON_SWORD = "SWORD"; // +5% PP permanent
    public static final String WEAPON_BOW = "BOW"; // +5% coins earned
    
    @PrimaryKey
    @NonNull
    private String id;
    private String userId; // Owner of this equipment
    private String name;
    private String type; // POTION, CLOTHING, WEAPON
    private String subType; // Specific item type
    private String description;
    private int cost; // In coins
    private double bonusPercentage; // Percentage bonus (0.20 = 20%)
    private String iconUrl;
    private boolean isPermanent; // For potions
    private int durability; // For clothing (battles remaining)
    private int upgradeLevel; // For weapons
    private int quantity; // For potions (stackable)
    private boolean isActive; // Currently equipped/active
    private long purchasedAt;
    
    public Equipment() {
        this.id = "";
    }
    
    @Ignore
    public Equipment(String userId, String name, String type, String subType, String description, int cost, double bonusPercentage) {
        this.id = userId + "_" + type + "_" + subType + "_" + System.currentTimeMillis();
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.subType = subType;
        this.description = description;
        this.cost = cost;
        this.bonusPercentage = bonusPercentage;
        this.purchasedAt = System.currentTimeMillis();
        this.quantity = 1;
        this.isActive = false;
        
        // Set defaults based on type
        if (TYPE_CLOTHING.equals(type)) {
            this.durability = 2; // 2 battles
        }
        if (TYPE_WEAPON.equals(type)) {
            this.upgradeLevel = 1;
        }
        if (TYPE_POTION.equals(type)) {
            this.isPermanent = subType.contains("PERMANENT");
        }
    }
    
    // Calculate cost based on boss reward
    // Used for shop pricing
    public static int calculateCost(String subType, int bossRewardForPreviousLevel) {
        switch (subType) {
            // Potions
            case POTION_PP_20:
                return (int) (bossRewardForPreviousLevel * 0.5);
            case POTION_PP_40:
                return (int) (bossRewardForPreviousLevel * 0.7);
            case POTION_PP_5_PERM:
                return (int) (bossRewardForPreviousLevel * 2.0);
            case POTION_PP_10_PERM:
                return (int) (bossRewardForPreviousLevel * 10.0);
            
            // Clothing
            case CLOTHING_GLOVES:
            case CLOTHING_SHIELD:
                return (int) (bossRewardForPreviousLevel * 0.6);
            case CLOTHING_BOOTS:
                return (int) (bossRewardForPreviousLevel * 0.8);
            
            // Weapon upgrade cost
            case WEAPON_SWORD:
            case WEAPON_BOW:
                return (int) (bossRewardForPreviousLevel * 0.6);
            
            default:
                return 100;
        }
    }
    
    // Get bonus value based on subtype
    public static double getBonusValue(String subType) {
        switch (subType) {
            case POTION_PP_20: return 0.20;
            case POTION_PP_40: return 0.40;
            case POTION_PP_5_PERM: return 0.05;
            case POTION_PP_10_PERM: return 0.10;
            case CLOTHING_GLOVES: return 0.10;
            case CLOTHING_SHIELD: return 0.10;
            case CLOTHING_BOOTS: return 0.40;
            case WEAPON_SWORD: return 0.05;
            case WEAPON_BOW: return 0.05;
            default: return 0;
        }
    }
    
    // Getters and Setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getSubType() { return subType; }
    public void setSubType(String subType) { this.subType = subType; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public int getCost() { return cost; }
    public void setCost(int cost) { this.cost = cost; }
    
    public double getBonusPercentage() { return bonusPercentage; }
    public void setBonusPercentage(double bonusPercentage) { this.bonusPercentage = bonusPercentage; }
    
    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
    
    public boolean isPermanent() { return isPermanent; }
    public void setPermanent(boolean permanent) { isPermanent = permanent; }
    
    public int getDurability() { return durability; }
    public void setDurability(int durability) { this.durability = durability; }
    
    public int getUpgradeLevel() { return upgradeLevel; }
    public void setUpgradeLevel(int upgradeLevel) { this.upgradeLevel = upgradeLevel; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public long getPurchasedAt() { return purchasedAt; }
    public void setPurchasedAt(long purchasedAt) { this.purchasedAt = purchasedAt; }
}
