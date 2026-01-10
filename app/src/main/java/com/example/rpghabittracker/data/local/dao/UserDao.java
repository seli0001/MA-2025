package com.example.rpghabittracker.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.rpghabittracker.data.model.User;

import java.util.List;

/**
 * Data Access Object for User entity
 */
@Dao
public interface UserDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(User user);
    
    @Update
    void update(User user);
    
    @Delete
    void delete(User user);
    
    @Query("SELECT * FROM users WHERE id = :userId")
    LiveData<User> getUserById(String userId);
    
    @Query("SELECT * FROM users WHERE id = :userId")
    User getUserByIdSync(String userId);
    
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    User getUserByUsername(String username);
    
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);
    
    @Query("SELECT * FROM users")
    LiveData<List<User>> getAllUsers();
    
    // Update specific fields
    @Query("UPDATE users SET experiencePoints = :xp WHERE id = :userId")
    void updateXp(String userId, int xp);
    
    @Query("UPDATE users SET level = :level WHERE id = :userId")
    void updateLevel(String userId, int level);
    
    @Query("UPDATE users SET coins = :coins WHERE id = :userId")
    void updateCoins(String userId, int coins);
    
    @Query("UPDATE users SET powerPoints = :pp WHERE id = :userId")
    void updatePowerPoints(String userId, int pp);
    
    @Query("UPDATE users SET title = :title WHERE id = :userId")
    void updateTitle(String userId, String title);
    
    @Query("UPDATE users SET lastLoginAt = :timestamp WHERE id = :userId")
    void updateLastLogin(String userId, long timestamp);
    
    // Statistics updates
    @Query("UPDATE users SET totalTasksCompleted = totalTasksCompleted + 1 WHERE id = :userId")
    void incrementTasksCompleted(String userId);
    
    @Query("UPDATE users SET totalTasksFailed = totalTasksFailed + 1 WHERE id = :userId")
    void incrementTasksFailed(String userId);
    
    @Query("UPDATE users SET totalTasksCreated = totalTasksCreated + 1 WHERE id = :userId")
    void incrementTasksCreated(String userId);
    
    @Query("UPDATE users SET currentStreak = :streak WHERE id = :userId")
    void updateStreak(String userId, int streak);
    
    @Query("UPDATE users SET longestStreak = :streak WHERE id = :userId")
    void updateLongestStreak(String userId, int streak);
    
    // Check existence
    @Query("SELECT COUNT(*) FROM users WHERE id = :userId")
    int userExists(String userId);
    
    // Search users
    @Query("SELECT * FROM users WHERE username LIKE :query || '%'")
    LiveData<List<User>> searchUsers(String query);
}
