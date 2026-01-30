package com.example.rpghabittracker.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.rpghabittracker.data.model.User;
import com.example.rpghabittracker.data.repository.UserRepository;

import java.util.List;

/**
 * ViewModel for User-related UI operations
 * Provides user data to UI and handles user actions
 */
public class UserViewModel extends AndroidViewModel {
    
    private final UserRepository repository;
    private final MutableLiveData<String> currentUserId = new MutableLiveData<>();
    private final MutableLiveData<LevelUpEvent> levelUpEvent = new MutableLiveData<>();
    private final MutableLiveData<XpGainEvent> xpGainEvent = new MutableLiveData<>();
    
    private LiveData<User> currentUser;
    
    public UserViewModel(@NonNull Application application) {
        super(application);
        repository = new UserRepository(application);
    }
    
    // Set current user ID
    public void setUserId(String userId) {
        currentUserId.setValue(userId);
    }
    
    public String getUserId() {
        return currentUserId.getValue();
    }
    
    // Get current user as LiveData
    public LiveData<User> getCurrentUser() {
        if (currentUser == null) {
            currentUser = Transformations.switchMap(currentUserId, userId -> {
                if (userId == null) return new MutableLiveData<>();
                return repository.getUserById(userId);
            });
        }
        return currentUser;
    }
    
    // Get user by ID
    public LiveData<User> getUserById(String userId) {
        return repository.getUserById(userId);
    }
    
    // Search users
    public LiveData<List<User>> searchUsers(String query) {
        return repository.searchUsers(query);
    }
    
    // Create or update user from Firebase login
    public void createOrUpdateUser(String firebaseUid, String email, String username, 
                                   String avatar, UserRepository.UserCallback callback) {
        repository.createOrUpdateFromFirebase(firebaseUid, email, username, avatar, callback);
    }
    
    // Add XP from completing a task
    public void addXpFromTask(int xpAmount) {
        String userId = currentUserId.getValue();
        if (userId == null) {
            android.util.Log.e("UserViewModel", "addXpFromTask: userId is null!");
            return;
        }
        
        android.util.Log.d("UserViewModel", "addXpFromTask: " + xpAmount + " XP for user " + userId);
        
        repository.addXp(userId, xpAmount, (success, xpGained, leveledUp, updatedUser) -> {
            android.util.Log.d("UserViewModel", "addXp callback: success=" + success + ", xpGained=" + xpGained);
            if (success) {
                // Post XP gain event
                xpGainEvent.postValue(new XpGainEvent(xpGained));
                
                // Post level up event if applicable
                if (leveledUp && updatedUser != null) {
                    levelUpEvent.postValue(new LevelUpEvent(
                            updatedUser.getLevel(),
                            updatedUser.getTitle(),
                            updatedUser.getPpForCurrentLevel()
                    ));
                }
            }
        });
    }
    
    // Add XP with callback for custom handling
    public void addXp(int xpAmount, XpCallback callback) {
        String userId = currentUserId.getValue();
        if (userId == null) {
            android.util.Log.e("UserViewModel", "addXp: userId is null!");
            if (callback != null) callback.onResult(false, 0, false, null);
            return;
        }
        
        repository.addXp(userId, xpAmount, (success, xpGained, leveledUp, updatedUser) -> {
            if (callback != null) {
                callback.onResult(success, xpGained, leveledUp, updatedUser);
            }
            
            if (success) {
                xpGainEvent.postValue(new XpGainEvent(xpGained));
                if (leveledUp && updatedUser != null) {
                    levelUpEvent.postValue(new LevelUpEvent(
                            updatedUser.getLevel(),
                            updatedUser.getTitle(),
                            updatedUser.getPpForCurrentLevel()
                    ));
                }
            }
        });
    }
    
    // XP callback interface
    public interface XpCallback {
        void onResult(boolean success, int xpGained, boolean leveledUp, com.example.rpghabittracker.data.model.User user);
    }
    
    // Add coins
    public void addCoins(int amount) {
        String userId = currentUserId.getValue();
        if (userId != null) {
            repository.addCoins(userId, amount);
        }
    }
    
    // Spend coins
    public void spendCoins(int amount, UserRepository.CoinsCallback callback) {
        String userId = currentUserId.getValue();
        if (userId == null) {
            callback.onResult(false, "User not logged in");
            return;
        }
        repository.subtractCoins(userId, amount, callback);
    }
    
    // Update streak
    public void updateStreak(int newStreak) {
        String userId = currentUserId.getValue();
        if (userId != null) {
            repository.updateStreak(userId, newStreak);
        }
    }
    
    // Increment task created
    public void incrementTaskCreated() {
        String userId = currentUserId.getValue();
        if (userId != null) {
            repository.incrementTaskCreated(userId);
        }
    }
    
    // Increment task failed
    public void incrementTaskFailed() {
        String userId = currentUserId.getValue();
        if (userId != null) {
            repository.incrementTaskFailed(userId);
        }
    }
    
    // Update last login
    public void updateLastLogin() {
        String userId = currentUserId.getValue();
        if (userId != null) {
            repository.updateLastLogin(userId);
        }
    }
    
    // Get success rate for boss battles
    public void getSuccessRate(UserRepository.SuccessRateCallback callback) {
        String userId = currentUserId.getValue();
        if (userId == null) {
            callback.onResult(0.5); // Default 50%
            return;
        }
        repository.getSuccessRate(userId, callback);
    }
    
    // Get user level synchronously (for task creation)
    public void getUserLevel(LevelCallback callback) {
        String userId = currentUserId.getValue();
        if (userId == null) {
            callback.onResult(1);
            return;
        }
        repository.getUserByIdSync(userId, user -> {
            callback.onResult(user != null ? user.getLevel() : 1);
        });
    }
    
    // Event LiveData getters
    public LiveData<LevelUpEvent> getLevelUpEvent() {
        return levelUpEvent;
    }
    
    public LiveData<XpGainEvent> getXpGainEvent() {
        return xpGainEvent;
    }
    
    // Clear events after handling
    public void clearLevelUpEvent() {
        levelUpEvent.setValue(null);
    }
    
    public void clearXpGainEvent() {
        xpGainEvent.setValue(null);
    }
    
    // Award battle rewards
    public void awardBattleRewards(int xpReward, int coinsReward, BattleRewardsCallback callback) {
        String userId = currentUserId.getValue();
        android.util.Log.d("UserViewModel", "awardBattleRewards: userId=" + userId + ", xp=" + xpReward + ", coins=" + coinsReward);
        if (userId == null) {
            android.util.Log.e("UserViewModel", "awardBattleRewards: userId is null!");
            callback.onResult(false, 0, 0, false, null);
            return;
        }
        repository.awardBattleRewards(userId, xpReward, coinsReward, callback::onResult);
    }
    
    // Get user power points
    public void getUserPowerPoints(PowerPointsCallback callback) {
        String userId = currentUserId.getValue();
        if (userId == null) {
            callback.onResult(40); // Default
            return;
        }
        repository.getUserPowerPoints(userId, callback::onResult);
    }
    
    public interface BattleRewardsCallback {
        void onResult(boolean success, int xpGained, int coinsGained, boolean leveledUp, com.example.rpghabittracker.data.model.User user);
    }
    
    public interface PowerPointsCallback {
        void onResult(int powerPoints);
    }
    
    // Event classes
    public static class LevelUpEvent {
        public final int newLevel;
        public final String newTitle;
        public final int ppReward;
        
        public LevelUpEvent(int newLevel, String newTitle, int ppReward) {
            this.newLevel = newLevel;
            this.newTitle = newTitle;
            this.ppReward = ppReward;
        }
    }
    
    public static class XpGainEvent {
        public final int xpGained;
        
        public XpGainEvent(int xpGained) {
            this.xpGained = xpGained;
        }
    }
    
    public interface LevelCallback {
        void onResult(int level);
    }
}
