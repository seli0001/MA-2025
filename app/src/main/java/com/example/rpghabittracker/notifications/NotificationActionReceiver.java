package com.example.rpghabittracker.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles actions clicked from actionable system notifications.
 */
public class NotificationActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        String action = intent.getAction();
        if (action == null) return;

        boolean acceptAction = AppNotificationManager.ACTION_FRIEND_REQUEST_ACCEPT.equals(action);
        boolean rejectAction = AppNotificationManager.ACTION_FRIEND_REQUEST_REJECT.equals(action);
        if (!acceptAction && !rejectAction) return;

        String friendshipId = intent.getStringExtra(AppNotificationManager.EXTRA_FRIENDSHIP_ID);
        String notificationDocId = intent.getStringExtra(AppNotificationManager.EXTRA_NOTIFICATION_DOC_ID);
        String senderId = intent.getStringExtra(AppNotificationManager.EXTRA_SENDER_ID);
        String receiverId = intent.getStringExtra(AppNotificationManager.EXTRA_RECEIVER_ID);
        if (friendshipId == null || friendshipId.trim().isEmpty()) return;

        PendingResult pendingResult = goAsync();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String newStatus = acceptAction ? "ACCEPTED" : "REJECTED";

        Map<String, Object> friendshipUpdates = new HashMap<>();
        friendshipUpdates.put("status", newStatus);
        friendshipUpdates.put("respondedAt", FieldValue.serverTimestamp());

        firestore.collection("friendships")
                .document(friendshipId)
                .set(friendshipUpdates, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    AppNotificationManager.resolveNotificationById(firestore, notificationDocId, newStatus);
                    AppNotificationManager.cancelNotification(context, notificationDocId);

                    if (acceptAction && senderId != null && !senderId.trim().isEmpty()) {
                        sendAcceptedNotification(firestore, senderId, receiverId, pendingResult);
                    } else {
                        pendingResult.finish();
                    }
                })
                .addOnFailureListener(e -> pendingResult.finish());
    }

    private void sendAcceptedNotification(
            @NonNull FirebaseFirestore firestore,
            @NonNull String senderId,
            @Nullable String receiverId,
            @NonNull PendingResult pendingResult
    ) {
        if (receiverId == null || receiverId.trim().isEmpty()) {
            AppNotificationManager.createFriendRequestAcceptedNotification(
                    firestore,
                    senderId,
                    "",
                    "Korisnik"
            );
            pendingResult.finish();
            return;
        }

        firestore.collection("users").document(receiverId)
                .get()
                .addOnSuccessListener(doc -> {
                    String acceptorName = doc.getString("username");
                    if (acceptorName == null || acceptorName.trim().isEmpty()) {
                        acceptorName = "Korisnik";
                    }

                    AppNotificationManager.createFriendRequestAcceptedNotification(
                            firestore,
                            senderId,
                            receiverId,
                            acceptorName
                    );
                    pendingResult.finish();
                })
                .addOnFailureListener(e -> {
                    AppNotificationManager.createFriendRequestAcceptedNotification(
                            firestore,
                            senderId,
                            receiverId,
                            "Korisnik"
                    );
                    pendingResult.finish();
                });
    }
}
