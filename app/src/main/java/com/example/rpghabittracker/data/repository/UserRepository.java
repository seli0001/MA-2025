package com.example.rpghabittracker.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.rpghabittracker.data.local.AppDatabase;
import com.example.rpghabittracker.data.local.dao.UserDao;
import com.example.rpghabittracker.data.model.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for User data operations
 * Handles both local Room database and Firebase Firestore sync
 */
public class UserRepository {
    
    private final UserDao userDao;
    private final FirebaseFirestore firestore;
    private final ExecutorService executor;
    
    // Titles for levels (Serbian)
    private static final String[] TITLES = {
        "Novajlija",      // Level 1
        "Učenik",         // Level 2
        "Šegrt",          // Level 3
        "Avanturista",    // Level 4
        "Borac",          // Level 5
        "Ratnik",         // Level 6
        "Vitez",          // Level 7
        "Šampion",        // Level 8
        "Heroj",          // Level 9
        "Legenda"         // Level 10+
    };
    
    public UserRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        userDao = db.userDao();
        firestore = FirebaseFirestore.getInstance();
        executor = Executors.newSingleThreadExecutor();
    }
    
    // Basic CRUD
    public void insert(User user) {
        executor.execute(() -> {
            userDao.insert(user);
            syncToFirestore(user);
        });
    }
    
    public void update(User user) {
        executor.execute(() -> {
            userDao.update(user);
            syncToFirestore(user);
        });
    }
    
    public void delete(User user) {
        executor.execute(() -> userDao.delete(user));
    }
    
    // Get user
    public LiveData<User> getUserById(String userId) {
        return userDao.getUserById(userId);
    }
    
    public void getUserByIdSync(String userId, UserCallback callback) {
        executor.execute(() -> {
            User user = userDao.getUserByIdSync(userId);
            callback.onResult(user);
        });
    }
    
    public LiveData<List<User>> searchUsers(String query) {
        return userDao.searchUsers(query);
    }
    
    // Add XP to user and handle level up
    public void addXp(String userId, int xpAmount, XpCallback callback) {
        android.util.Log.d("UserRepository", "addXp called: userId=" + userId + ", xpAmount=" + xpAmount);
        
        executor.execute(() -> {
            User user = userDao.getUserByIdSync(userId);
            if (user == null) {
                android.util.Log.e("UserRepository", "addXp: User not found in local DB: " + userId);
                callback.onResult(false, 0, false, null);
                return;
            }
            
            android.util.Log.d("UserRepository", "addXp: User found, current XP=" + user.getExperiencePoints());
            
            boolean leveledUp = applyXpAndHandleLevelUp(user, xpAmount);
            
            // Update statistics
            user.setTotalTasksCompleted(user.getTotalTasksCompleted() + 1);
            user.setTasksCompletedThisLevel(user.getTasksCompletedThisLevel() + 1);
            
            android.util.Log.d("UserRepository", "addXp: After update - XP=" + user.getExperiencePoints() + ", level=" + user.getLevel());
            
            // Save changes
            userDao.update(user);
            syncToFirestore(user);
            
            callback.onResult(true, xpAmount, leveledUp, leveledUp ? user : null);
        });
    }
    
    // Update coins
    public void addCoins(String userId, int amount) {
        executor.execute(() -> {
            User user = userDao.getUserByIdSync(userId);
            if (user != null) {
                user.setCoins(user.getCoins() + amount);
                userDao.update(user);
                syncToFirestore(user);
            }
        });
    }
    
    public void subtractCoins(String userId, int amount, CoinsCallback callback) {
        executor.execute(() -> {
            User user = userDao.getUserByIdSync(userId);
            if (user == null) {
                callback.onResult(false, "User not found");
                return;
            }
            
            if (user.getCoins() < amount) {
                callback.onResult(false, "Insufficient coins");
                return;
            }
            
            user.setCoins(user.getCoins() - amount);
            userDao.update(user);
            syncToFirestore(user);
            callback.onResult(true, null);
        });
    }
    
    // Update streak
    public void updateStreak(String userId, int newStreak) {
        executor.execute(() -> {
            User user = userDao.getUserByIdSync(userId);
            if (user != null) {
                user.setCurrentStreak(newStreak);
                if (newStreak > user.getLongestStreak()) {
                    user.setLongestStreak(newStreak);
                }
                userDao.update(user);
                syncToFirestore(user);
            }
        });
    }
    
    // Increment task created
    public void incrementTaskCreated(String userId) {
        executor.execute(() -> {
            userDao.incrementTasksCreated(userId);
            User user = userDao.getUserByIdSync(userId);
            if (user != null) {
                user.setTasksCreatedThisLevel(user.getTasksCreatedThisLevel() + 1);
                userDao.update(user);
            }
        });
    }
    
    // Increment task failed
    public void incrementTaskFailed(String userId) {
        executor.execute(() -> userDao.incrementTasksFailed(userId));
    }
    
    // Update last login
    public void updateLastLogin(String userId) {
        executor.execute(() -> userDao.updateLastLogin(userId, System.currentTimeMillis()));
    }
    
    // Calculate success rate for boss battles
    public void getSuccessRate(String userId, SuccessRateCallback callback) {
        executor.execute(() -> {
            User user = userDao.getUserByIdSync(userId);
            if (user == null) {
                callback.onResult(0.0);
                return;
            }
            
            int completed = user.getTasksCompletedThisLevel();
            int total = user.getTasksCreatedThisLevel();
            
            if (total == 0) {
                callback.onResult(1.0); // 100% if no tasks
                return;
            }
            
            double rate = (double) completed / total;
            callback.onResult(rate);
        });
    }
    
    // Get title for level
    public static String getTitleForLevel(int level) {
        if (level <= 0) return TITLES[0];
        if (level >= TITLES.length) return TITLES[TITLES.length - 1];
        return TITLES[level - 1];
    }
    
    // Sync user data to Firestore
    private void syncToFirestore(User user) {
        if (user == null || user.getId() == null || user.getId().isEmpty()) {
            android.util.Log.e("UserRepository", "Cannot sync null user to Firestore");
            return;
        }
        
        android.util.Log.d("UserRepository", "Syncing user to Firestore: " + user.getId() + 
                           ", xp=" + user.getExperiencePoints() + ", coins=" + user.getCoins());
        
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", user.getUsername());
        userData.put("usernameLower", user.getUsername() != null
                ? user.getUsername().trim().toLowerCase(Locale.ROOT)
                : null);
        userData.put("email", user.getEmail());
        userData.put("avatar", user.getAvatar());
        userData.put("createdAt", user.getCreatedAt());
        userData.put("lastLoginAt", user.getLastLoginAt());
        userData.put("level", user.getLevel());
        userData.put("title", user.getTitle());
        userData.put("xp", user.getExperiencePoints());
        userData.put("coins", user.getCoins());
        userData.put("powerPoints", user.getPowerPoints());
        userData.put("basePowerPoints", user.getBasePowerPoints());
        userData.put("currentLevelStartTime", user.getCurrentLevelStartTime());
        userData.put("tasksCompletedThisLevel", user.getTasksCompletedThisLevel());
        userData.put("tasksCreatedThisLevel", user.getTasksCreatedThisLevel());
        userData.put("totalTasksCompleted", user.getTotalTasksCompleted());
        userData.put("totalTasksCreated", user.getTotalTasksCreated());
        userData.put("totalTasksFailed", user.getTotalTasksFailed());
        userData.put("currentStreak", user.getCurrentStreak());
        userData.put("longestStreak", user.getLongestStreak());
        userData.put("badges", user.getBadges());
        userData.put("lastUpdated", System.currentTimeMillis());
        
        // Use set with merge to create or update
        firestore.collection("users")
                .document(user.getId())
                .set(userData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("UserRepository", "User synced to Firestore successfully");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("UserRepository", "Failed to sync user to Firestore", e);
                });
    }
    
    // Create or update user from Firebase Auth
    // First checks Firestore for existing data, then falls back to local
    public void createOrUpdateFromFirebase(String firebaseUid, String email, String username, 
                                           String avatar, UserCallback callback) {
        android.util.Log.d("UserRepository", "createOrUpdateFromFirebase: uid=" + firebaseUid);
        
        // First try to load from Firestore
        firestore.collection("users")
                .document(firebaseUid)
                .get()
                .addOnSuccessListener(document -> {
                    android.util.Log.d("UserRepository", "Firestore fetch success, exists=" + document.exists());
                    
                    executor.execute(() -> {
                        if (document.exists()) {
                            // User exists in Firestore - load and sync to local
                            User user = documentToUser(document, firebaseUid, email, username, avatar);
                            android.util.Log.d("UserRepository", "User from Firestore: xp=" + user.getExperiencePoints() + 
                                              ", coins=" + user.getCoins() + ", level=" + user.getLevel());
                            userDao.insert(user); // Insert or replace
                            // Keep searchable fields consistent across old/new schema
                            syncToFirestore(user);
                            callback.onResult(user);
                        } else {
                            // Check local database
                            User existing = userDao.getUserByIdSync(firebaseUid);
                            
                            if (existing != null) {
                                android.util.Log.d("UserRepository", "User exists in local DB");
                                existing.setLastLoginAt(System.currentTimeMillis());
                                userDao.update(existing);
                                syncToFirestore(existing);
                                callback.onResult(existing);
                            } else {
                                // Create new user
                                android.util.Log.d("UserRepository", "Creating new user");
                                User newUser = new User(email, "", username, avatar);
                                newUser.setId(firebaseUid);
                                newUser.setActive(true);
                                int initialPp = newUser.getPpForCurrentLevel();
                                newUser.setPowerPoints(initialPp);
                                newUser.setBasePowerPoints(initialPp);
                                userDao.insert(newUser);
                                syncToFirestore(newUser);
                                callback.onResult(newUser);
                            }
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("UserRepository", "Firestore fetch failed", e);
                    // Firestore failed, fall back to local
                    executor.execute(() -> {
                        User existing = userDao.getUserByIdSync(firebaseUid);
                        
                        if (existing != null) {
                            existing.setLastLoginAt(System.currentTimeMillis());
                            userDao.update(existing);
                            callback.onResult(existing);
                        } else {
                            User newUser = new User(email, "", username, avatar);
                            newUser.setId(firebaseUid);
                            newUser.setActive(true);
                            int initialPp = newUser.getPpForCurrentLevel();
                            newUser.setPowerPoints(initialPp);
                            newUser.setBasePowerPoints(initialPp);
                            userDao.insert(newUser);
                            callback.onResult(newUser);
                        }
                    });
                });
    }
    
    // Convert Firestore document to User
    private User documentToUser(com.google.firebase.firestore.DocumentSnapshot doc, 
                                String firebaseUid, String email, String username, String avatar) {
        String remoteUsername = doc.getString("username");
        String resolvedUsername = (remoteUsername != null && !remoteUsername.trim().isEmpty())
                ? remoteUsername
                : username;

        String remoteEmail = doc.getString("email");
        String resolvedEmail = (remoteEmail != null && !remoteEmail.trim().isEmpty())
                ? remoteEmail
                : email;

        String remoteAvatar = doc.getString("avatar");
        String resolvedAvatar = (remoteAvatar != null && !remoteAvatar.trim().isEmpty())
                ? remoteAvatar
                : avatar;

        User user = new User(resolvedEmail, "", resolvedUsername, resolvedAvatar);
        user.setId(firebaseUid);
        user.setActive(true);

        Long createdAt = doc.getLong("createdAt");
        if (createdAt != null && createdAt > 0) {
            user.setCreatedAt(createdAt);
        }

        Long lastLoginAt = doc.getLong("lastLoginAt");
        if (lastLoginAt != null && lastLoginAt > 0) {
            user.setLastLoginAt(lastLoginAt);
        } else {
            user.setLastLoginAt(System.currentTimeMillis());
        }
        
        // Load saved data from Firestore
        Long level = doc.getLong("level");
        if (level != null) user.setLevel(level.intValue());
        
        String title = doc.getString("title");
        if (title != null) user.setTitle(title);
        
        Long xp = doc.getLong("xp");
        if (xp != null) user.setExperiencePoints(xp.intValue());
        
        Long coins = doc.getLong("coins");
        if (coins != null) user.setCoins(coins.intValue());

        Long currentLevelStartTime = doc.getLong("currentLevelStartTime");
        if (currentLevelStartTime != null && currentLevelStartTime > 0) {
            user.setCurrentLevelStartTime(currentLevelStartTime);
        } else if (createdAt != null && createdAt > 0) {
            // Legacy users may miss stage timestamp; use account creation as best-effort fallback.
            user.setCurrentLevelStartTime(createdAt);
        }
        
        int defaultBasePp = user.getPpForCurrentLevel();
        Long basePp = doc.getLong("basePowerPoints");
        int resolvedBasePp = basePp != null ? Math.max(0, basePp.intValue()) : defaultBasePp;

        Long pp = doc.getLong("powerPoints");
        int resolvedPowerPp = pp != null ? Math.max(0, pp.intValue()) : resolvedBasePp;
        int equipmentBonus = Math.max(0, resolvedPowerPp - resolvedBasePp);

        // Normalize base PP to current formula (legacy data may contain old cumulative values).
        if (resolvedBasePp != defaultBasePp) {
            resolvedBasePp = defaultBasePp;
        }
        resolvedPowerPp = resolvedBasePp + equipmentBonus;

        user.setBasePowerPoints(resolvedBasePp);
        user.setPowerPoints(resolvedPowerPp);
        
        Long totalCompleted = doc.getLong("totalTasksCompleted");
        if (totalCompleted != null) user.setTotalTasksCompleted(totalCompleted.intValue());
        
        Long totalCreated = doc.getLong("totalTasksCreated");
        if (totalCreated != null) user.setTotalTasksCreated(totalCreated.intValue());
        
        Long currentStreak = doc.getLong("currentStreak");
        if (currentStreak != null) user.setCurrentStreak(currentStreak.intValue());
        
        Long longestStreak = doc.getLong("longestStreak");
        if (longestStreak != null) user.setLongestStreak(longestStreak.intValue());

        Long tasksCompletedThisLevel = doc.getLong("tasksCompletedThisLevel");
        if (tasksCompletedThisLevel != null) {
            user.setTasksCompletedThisLevel(Math.max(0, tasksCompletedThisLevel.intValue()));
        }

        Long tasksCreatedThisLevel = doc.getLong("tasksCreatedThisLevel");
        if (tasksCreatedThisLevel != null) {
            user.setTasksCreatedThisLevel(Math.max(0, tasksCreatedThisLevel.intValue()));
        }
        
        @SuppressWarnings("unchecked")
        List<String> badges = (List<String>) doc.get("badges");
        if (badges != null) user.setBadges(badges);
        
        return user;
    }
    
    // Callbacks
    public interface UserCallback {
        void onResult(User user);
    }
    
    public interface XpCallback {
        void onResult(boolean success, int xpGained, boolean leveledUp, User updatedUser);
    }
    
    public interface CoinsCallback {
        void onResult(boolean success, String errorMessage);
    }
    
    public interface SuccessRateCallback {
        void onResult(double rate);
    }
    
    public interface BattleRewardsCallback {
        void onResult(boolean success, int xpGained, int coinsGained, boolean leveledUp, User updatedUser);
    }
    
    // Award battle rewards (coins and XP)
    public void awardBattleRewards(String userId, int xpReward, int coinsReward, BattleRewardsCallback callback) {
        android.util.Log.d("UserRepository", "awardBattleRewards called: userId=" + userId + ", xp=" + xpReward + ", coins=" + coinsReward);
        
        executor.execute(() -> {
            User user = userDao.getUserByIdSync(userId);
            if (user == null) {
                android.util.Log.e("UserRepository", "User not found for battle rewards: " + userId);
                callback.onResult(false, 0, 0, false, null);
                return;
            }
            
            android.util.Log.d("UserRepository", "User before update: coins=" + user.getCoins() + ", xp=" + user.getExperiencePoints() + ", level=" + user.getLevel());
            
            // Add coins
            user.setCoins(user.getCoins() + coinsReward);
            
            // Add XP with level up check
            boolean leveledUp = applyXpAndHandleLevelUp(user, xpReward);
            
            android.util.Log.d("UserRepository", "User after update: coins=" + user.getCoins() + ", xp=" + user.getExperiencePoints() + ", level=" + user.getLevel());
            
            // Update database
            userDao.update(user);
            syncToFirestore(user);
            
            android.util.Log.d("UserRepository", "Battle rewards saved successfully");
            
            callback.onResult(true, xpReward, coinsReward, leveledUp, user);
        });
    }

    private boolean applyXpAndHandleLevelUp(User user, int xpGain) {
        int safeGain = Math.max(0, xpGain);
        int currentLevel = Math.max(1, user.getLevel());
        int currentXp = Math.max(0, user.getExperiencePoints());

        boolean cumulativeXpModel = isCumulativeXpModel(currentLevel, currentXp);
        int updatedXp = currentXp + safeGain;
        boolean leveledUp = false;

        int expectedBaseAtCurrentLevel = User.getBasePpForLevel(currentLevel);
        int storedBase = Math.max(0, user.getBasePowerPoints());
        int storedTotal = Math.max(0, user.getPowerPoints());
        int equipmentBonus = Math.max(0, storedTotal - storedBase);
        int basePowerPoints = expectedBaseAtCurrentLevel;
        int totalPowerPoints = basePowerPoints + equipmentBonus;

        while (true) {
            int requiredXp = cumulativeXpModel
                    ? User.getXpForLevel(currentLevel + 1)
                    : getStageXpRequiredForNextLevel(currentLevel);

            if (updatedXp < requiredXp) {
                break;
            }

            if (!cumulativeXpModel) {
                updatedXp -= requiredXp;
            }

            currentLevel++;
            user.setLevel(currentLevel);
            leveledUp = true;

            // Set PP for reached level using current progression model.
            int ppReward = User.getPpRewardForReachedLevel(currentLevel);
            basePowerPoints = User.getBasePpForLevel(currentLevel);
            totalPowerPoints = basePowerPoints + equipmentBonus;

            // Update title
            String newTitle = getTitleForLevel(currentLevel);
            user.setTitle(newTitle);

            // Reset level tracking
            user.setCurrentLevelStartTime(System.currentTimeMillis());
            user.setTasksCompletedThisLevel(0);
            user.setTasksCreatedThisLevel(0);

            android.util.Log.d("UserRepository", "Level up! New level: " + currentLevel + ", PP reward: " + ppReward);
        }

        user.setBasePowerPoints(basePowerPoints);
        user.setPowerPoints(totalPowerPoints);
        user.setExperiencePoints(updatedXp);
        return leveledUp;
    }

    private boolean isCumulativeXpModel(int level, int xp) {
        if (level <= 1) return false;
        return xp >= User.getXpForLevel(level);
    }

    private int getStageXpRequiredForNextLevel(int level) {
        // getXpForLevel(1)=200, getXpForLevel(2)=500, etc. — each is the XP needed within that level
        return User.getXpForLevel(Math.max(1, level));
    }
    
    // Get user power points
    public void getUserPowerPoints(String userId, PowerPointsCallback callback) {
        executor.execute(() -> {
            User user = userDao.getUserByIdSync(userId);
            if (user != null) {
                int expectedBasePp = user.getPpForCurrentLevel();
                int storedBasePp = Math.max(0, user.getBasePowerPoints());
                int storedPp = Math.max(0, user.getPowerPoints());
                int equipmentBonus = Math.max(0, storedPp - storedBasePp);
                int basePp = expectedBasePp;
                int pp = basePp + equipmentBonus;

                if (pp != user.getPowerPoints() || basePp != user.getBasePowerPoints()) {
                    user.setPowerPoints(pp);
                    user.setBasePowerPoints(basePp);
                    userDao.update(user);
                    syncToFirestore(user);
                }
                callback.onResult(pp);
            } else {
                callback.onResult(0); // Default PP for level 1
            }
        });
    }
    
    public interface PowerPointsCallback {
        void onResult(int powerPoints);
    }
}
