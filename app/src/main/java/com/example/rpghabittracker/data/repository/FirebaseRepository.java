package com.example.rpghabittracker.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.rpghabittracker.data.model.User;
import com.example.rpghabittracker.data.model.Task;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Repository for Firebase operations (Authentication, Firestore, Realtime Database)
 */
public class FirebaseRepository {
    
    private static final String TAG = "FirebaseRepository";
    
    // Firebase instances
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;
    private final DatabaseReference realtimeDb;
    
    // Firestore collections
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_TASKS = "tasks";
    private static final String COLLECTION_BOSSES = "bosses";
    private static final String COLLECTION_EQUIPMENT = "equipment";
    private static final String COLLECTION_FRIENDS = "friends";
    
    public FirebaseRepository() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        realtimeDb = FirebaseDatabase.getInstance().getReference();
    }
    
    // ==================== AUTHENTICATION ====================
    
    /**
     * Register new user with email and password
     * Sends email verification after successful registration
     */
    public void registerUser(String email, String password, String username, String avatar,
                            OnSuccessListener<FirebaseUser> onSuccess,
                            OnFailureListener onFailure) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser user = authResult.getUser();
                if (user != null) {
                    // Send email verification
                    user.sendEmailVerification()
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Verification email sent to: " + email);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to send verification email", e);
                        });

                    // Create user profile in Firestore
                    createUserProfile(user.getUid(), username, email, avatar, onSuccess, onFailure);
                }
            })
            .addOnFailureListener(onFailure);
    }
    
    /**
     * Resend email verification to current user
     */
    public void resendVerificationEmail(OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
        } else {
            onFailure.onFailure(new Exception("No user logged in"));
        }
    }
    
    /**
     * Check if current user's email is verified
     */
    public boolean isEmailVerified() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null && user.isEmailVerified();
    }
    
    /**
     * Reload current user to get updated email verification status
     */
    public void reloadUser(OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.reload()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
        } else {
            onFailure.onFailure(new Exception("No user logged in"));
        }
    }
    
    /**
     * Login existing user
     */
    public void loginUser(String email, String password,
                         OnSuccessListener<FirebaseUser> onSuccess,
                         OnFailureListener onFailure) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser user = authResult.getUser();
                if (user != null) {
                    onSuccess.onSuccess(user);
                }
            })
            .addOnFailureListener(onFailure);
    }
    
    /**
     * Logout current user
     */
    public void logoutUser() {
        mAuth.signOut();
    }
    
    /**
     * Get current logged in user
     */
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }
    
    // ==================== FIRESTORE - USER PROFILE ====================
    
    /**
     * Create user profile in Firestore
     */
    private void createUserProfile(String userId, String username, String email, String avatar,
                                   OnSuccessListener<FirebaseUser> onSuccess,
                                   OnFailureListener onFailure) {
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("userId", userId);
        userProfile.put("username", username);
        userProfile.put("usernameLower", username != null
                ? username.trim().toLowerCase(Locale.ROOT)
                : null);
        userProfile.put("email", email);
        userProfile.put("avatar", avatar);
        userProfile.put("level", 1);
        userProfile.put("xp", 0);
        userProfile.put("coins", 0);
        userProfile.put("pp", 0);
        userProfile.put("hp", 100);
        userProfile.put("maxHp", 100);
        userProfile.put("attack", 10);
        userProfile.put("defense", 5);
        userProfile.put("createdAt", System.currentTimeMillis());
        
        db.collection(COLLECTION_USERS).document(userId)
            .set(userProfile)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "User profile created successfully");
                onSuccess.onSuccess(mAuth.getCurrentUser());
            })
            .addOnFailureListener(onFailure);
    }
    
    /**
     * Get user profile from Firestore
     */
    public void getUserProfile(String userId, 
                              OnSuccessListener<DocumentSnapshot> onSuccess,
                              OnFailureListener onFailure) {
        db.collection(COLLECTION_USERS).document(userId)
            .get()
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onFailure);
    }
    
    /**
     * Update user profile
     */
    public void updateUserProfile(String userId, Map<String, Object> updates,
                                 OnSuccessListener<Void> onSuccess,
                                 OnFailureListener onFailure) {
        db.collection(COLLECTION_USERS).document(userId)
            .update(updates)
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onFailure);
    }
    
    // ==================== FIRESTORE - TASKS ====================
    
    /**
     * Create new task
     */
    public void createTask(String userId, Map<String, Object> taskData,
                          OnSuccessListener<Void> onSuccess,
                          OnFailureListener onFailure) {
        taskData.put("userId", userId);
        taskData.put("createdAt", System.currentTimeMillis());
        
        db.collection(COLLECTION_TASKS)
            .add(taskData)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Task created with ID: " + documentReference.getId());
                onSuccess.onSuccess(null);
            })
            .addOnFailureListener(onFailure);
    }
    
    /**
     * Get all tasks for user
     */
    public void getUserTasks(String userId,
                            OnSuccessListener<QuerySnapshot> onSuccess,
                            OnFailureListener onFailure) {
        db.collection(COLLECTION_TASKS)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onFailure);
    }
    
    /**
     * Update task
     */
    public void updateTask(String taskId, Map<String, Object> updates,
                          OnSuccessListener<Void> onSuccess,
                          OnFailureListener onFailure) {
        db.collection(COLLECTION_TASKS).document(taskId)
            .update(updates)
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onFailure);
    }
    
    /**
     * Delete task
     */
    public void deleteTask(String taskId,
                          OnSuccessListener<Void> onSuccess,
                          OnFailureListener onFailure) {
        db.collection(COLLECTION_TASKS).document(taskId)
            .delete()
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onFailure);
    }
    
    // ==================== REALTIME DATABASE - ONLINE STATUS ====================
    
    /**
     * Set user online status in Realtime Database
     */
    public void setUserOnlineStatus(String userId, boolean isOnline) {
        Map<String, Object> status = new HashMap<>();
        status.put("online", isOnline);
        status.put("lastSeen", System.currentTimeMillis());
        
        realtimeDb.child("status").child(userId).setValue(status);
    }
    
    /**
     * Listen to friend's online status
     */
    public void listenToUserStatus(String userId, OnUserStatusListener listener) {
        realtimeDb.child("status").child(userId)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Boolean isOnline = snapshot.child("online").getValue(Boolean.class);
                        Long lastSeen = snapshot.child("lastSeen").getValue(Long.class);
                        
                        if (isOnline != null && lastSeen != null) {
                            listener.onStatusChanged(isOnline, lastSeen);
                        }
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to listen to user status: " + error.getMessage());
                }
            });
    }
    
    /**
     * Send friend request
     */
    public void sendFriendRequest(String fromUserId, String toUserId, String fromUsername,
                                 OnSuccessListener<Void> onSuccess,
                                 OnFailureListener onFailure) {
        Map<String, Object> request = new HashMap<>();
        request.put("fromUserId", fromUserId);
        request.put("fromUsername", fromUsername);
        request.put("toUserId", toUserId);
        request.put("status", "pending");
        request.put("timestamp", System.currentTimeMillis());
        
        db.collection(COLLECTION_FRIENDS)
            .add(request)
            .addOnSuccessListener(documentReference -> onSuccess.onSuccess(null))
            .addOnFailureListener(onFailure);
    }
    
    // ==================== INTERFACES ====================
    
    public interface OnUserStatusListener {
        void onStatusChanged(boolean isOnline, long lastSeen);
    }
}
