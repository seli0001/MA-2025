package com.example.rpghabittracker.services;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.rpghabittracker.R;
import com.example.rpghabittracker.notifications.AppNotificationManager;
import com.example.rpghabittracker.ui.home.MainActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Firebase Cloud Messaging Service for handling push notifications
 */
public class RPGFirebaseMessagingService extends FirebaseMessagingService {
    
    private static final String TAG = "RPGFCMService";
    
    @Override
    public void onCreate() {
        super.onCreate();
        AppNotificationManager.ensureNotificationChannel(this);
    }
    
    /**
     * Called when a new FCM token is generated
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM Token: " + token);
        
        // TODO: Send token to your server
        // You can save this to Firebase Firestore associated with the user
        sendTokenToServer(token);
    }
    
    /**
     * Called when a message is received
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        
        Log.d(TAG, "Message received from: " + message.getFrom());
        
        // Handle data payload
        if (!message.getData().isEmpty()) {
            Log.d(TAG, "Message data: " + message.getData());
            handleDataMessage(message);
        }
        
        // Handle notification payload
        if (message.getNotification() != null) {
            String title = message.getNotification().getTitle();
            String body = message.getNotification().getBody();
            Log.d(TAG, "Notification - Title: " + title + ", Body: " + body);
            
            showNotification(title, body);
        }
    }
    
    /**
     * Handle custom data messages (for friend requests, boss battle invites, etc.)
     */
    private void handleDataMessage(RemoteMessage message) {
        String type = message.getData().get("type");
        
        if (type != null) {
            switch (type) {
                case "friend_request":
                    String friendName = message.getData().get("friendName");
                    showNotification("Novi zahtev za prijateljstvo", 
                                   friendName + " želi da bude tvoj prijatelj!");
                    break;
                    
                case "boss_battle_invite":
                    String bossName = message.getData().get("bossName");
                    showNotification("Poziv za Boss borbu", 
                                   "Pridruži se borbi protiv " + bossName + "!");
                    break;
                    
                case "achievement_unlocked":
                    String achievement = message.getData().get("achievement");
                    showNotification("Nova značka otključana!", achievement);
                    break;
                    
                case "level_up":
                    String level = message.getData().get("level");
                    showNotification("Čestitamo! ⭐", 
                                   "Dostigao si nivo " + level + "!");
                    break;
                    
                default:
                    showNotification("RPG Habit Tracker", 
                                   message.getData().get("message"));
                    break;
            }
        }
    }
    
    /**
     * Display notification
     */
    private void showNotification(String title, String body) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent,
            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, AppNotificationManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(this).notify(0, builder.build());
    }
    
    /**
     * Send FCM token to your backend server or Firestore
     */
    private void sendTokenToServer(String token) {
        // TODO: Implement sending token to Firestore
        // Example:
        // FirebaseFirestore db = FirebaseFirestore.getInstance();
        // String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // db.collection("users").document(userId)
        //   .update("fcmToken", token);
        
        Log.d(TAG, "Token should be sent to server: " + token);
    }
}
