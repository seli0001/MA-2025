package com.example.rpghabittracker.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.rpghabittracker.data.model.Equipment;

import java.util.List;

/**
 * Data Access Object for Equipment entity
 */
@Dao
public interface EquipmentDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Equipment equipment);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Equipment> equipmentList);
    
    @Update
    void update(Equipment equipment);
    
    @Delete
    void delete(Equipment equipment);
    
    @Query("DELETE FROM equipment WHERE id = :equipmentId")
    void deleteById(String equipmentId);
    
    @Query("SELECT * FROM equipment WHERE id = :equipmentId")
    LiveData<Equipment> getEquipmentById(String equipmentId);
    
    @Query("SELECT * FROM equipment WHERE id = :equipmentId")
    Equipment getEquipmentByIdSync(String equipmentId);
    
    // Get all user equipment
    @Query("SELECT * FROM equipment WHERE userId = :userId ORDER BY type, subType")
    LiveData<List<Equipment>> getUserEquipment(String userId);
    
    @Query("SELECT * FROM equipment WHERE userId = :userId ORDER BY type, subType")
    List<Equipment> getUserEquipmentSync(String userId);
    
    // Get equipment by type
    @Query("SELECT * FROM equipment WHERE userId = :userId AND type = :type ORDER BY subType")
    LiveData<List<Equipment>> getEquipmentByType(String userId, String type);
    
    @Query("SELECT * FROM equipment WHERE userId = :userId AND type = :type ORDER BY subType")
    List<Equipment> getEquipmentByTypeSync(String userId, String type);
    
    // Get active equipment
    @Query("SELECT * FROM equipment WHERE userId = :userId AND isActive = 1")
    LiveData<List<Equipment>> getActiveEquipment(String userId);
    
    @Query("SELECT * FROM equipment WHERE userId = :userId AND isActive = 1")
    List<Equipment> getActiveEquipmentSync(String userId);
    
    // Activate/deactivate
    @Query("UPDATE equipment SET isActive = :isActive WHERE id = :equipmentId")
    void setActive(String equipmentId, boolean isActive);
    
    // Update durability (for clothing)
    @Query("UPDATE equipment SET durability = durability - 1 WHERE id = :equipmentId")
    void decrementDurability(String equipmentId);
    
    @Query("UPDATE equipment SET durability = :durability WHERE id = :equipmentId")
    void setDurability(String equipmentId, int durability);
    
    // Upgrade weapon
    @Query("UPDATE equipment SET upgradeLevel = upgradeLevel + 1, bonusPercentage = bonusPercentage + :bonusIncrease WHERE id = :equipmentId")
    void upgradeWeapon(String equipmentId, double bonusIncrease);
    
    // Count equipment
    @Query("SELECT COUNT(*) FROM equipment WHERE userId = :userId AND type = :type")
    int countEquipmentByType(String userId, String type);
    
    // Get potions (quantity > 0)
    @Query("SELECT * FROM equipment WHERE userId = :userId AND type = 'POTION' AND quantity > 0")
    LiveData<List<Equipment>> getAvailablePotions(String userId);
    
    // Update potion quantity
    @Query("UPDATE equipment SET quantity = quantity - 1 WHERE id = :equipmentId")
    void consumePotion(String equipmentId);
    
    @Query("UPDATE equipment SET quantity = quantity + 1 WHERE id = :equipmentId")
    void addPotion(String equipmentId);
    
    // Delete expired clothing (durability = 0)
    @Query("DELETE FROM equipment WHERE userId = :userId AND type = 'CLOTHING' AND durability <= 0")
    void deleteExpiredClothing(String userId);
}
