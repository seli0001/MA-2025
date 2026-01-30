package com.example.rpghabittracker.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Helper class for Firebase Cloud Messaging operations
 */
public class FirebaseHelper {
    
    private static final String TAG = "FirebaseHelper";
    
    /**
     * Get FCM registration token
     */
    public static void getFCMToken(OnTokenReceivedListener listener) {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                    listener.onFailure(task.getException());
                    return;
                }
                
                // Get new FCM registration token
                String token = task.getResult();
                Log.d(TAG, "FCM Token: " + token);
                listener.onTokenReceived(token);
            });
    }
    
    /**
     * Subscribe to a topic for receiving notifications
     */
    public static void subscribeToTopic(String topic) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener(task -> {
                String msg = task.isSuccessful() ? 
                    "Subscribed to " + topic : "Failed to subscribe to " + topic;
                Log.d(TAG, msg);
            });
    }
    
    /**
     * Unsubscribe from a topic
     */
    public static void unsubscribeFromTopic(String topic) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener(task -> {
                String msg = task.isSuccessful() ? 
                    "Unsubscribed from " + topic : "Failed to unsubscribe from " + topic;
                Log.d(TAG, msg);
            });
    }
    
    /**
     * Callback interface for FCM token
     */
    public interface OnTokenReceivedListener {
        void onTokenReceived(String token);
        void onFailure(Exception e);
    }
}
