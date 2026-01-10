package com.example.rpghabittracker.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.rpghabittracker.data.model.Boss;

import java.util.List;

/**
 * Data Access Object for Boss entity
 */
@Dao
public interface BossDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Boss boss);
    
    @Update
    void update(Boss boss);
    
    @Delete
    void delete(Boss boss);
    
    @Query("SELECT * FROM bosses WHERE id = :bossId")
    LiveData<Boss> getBossById(String bossId);
    
    @Query("SELECT * FROM bosses WHERE id = :bossId")
    Boss getBossByIdSync(String bossId);
    
    @Query("SELECT * FROM bosses WHERE level = :level LIMIT 1")
    Boss getBossByLevel(int level);
    
    @Query("SELECT * FROM bosses WHERE userId = :userId AND isDefeated = 0 ORDER BY level ASC LIMIT 1")
    LiveData<Boss> getCurrentBoss(String userId);
    
    @Query("SELECT * FROM bosses WHERE userId = :userId AND isDefeated = 0 ORDER BY level ASC LIMIT 1")
    Boss getCurrentBossSync(String userId);
    
    @Query("SELECT * FROM bosses WHERE userId = :userId ORDER BY level DESC")
    LiveData<List<Boss>> getUserBosses(String userId);
    
    @Query("UPDATE bosses SET currentHp = :hp WHERE id = :bossId")
    void updateBossHp(String bossId, int hp);
    
    @Query("UPDATE bosses SET isDefeated = 1, defeatedDate = :defeatedDate WHERE id = :bossId")
    void markBossDefeated(String bossId, long defeatedDate);
    
    @Query("SELECT COUNT(*) FROM bosses WHERE userId = :userId AND isDefeated = 1")
    int countDefeatedBosses(String userId);
    
    @Query("SELECT MAX(level) FROM bosses WHERE userId = :userId")
    int getHighestBossLevel(String userId);
}
