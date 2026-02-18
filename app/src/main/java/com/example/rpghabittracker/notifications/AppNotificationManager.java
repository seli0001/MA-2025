package com.example.rpghabittracker.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.rpghabittracker.R;
import com.example.rpghabittracker.ui.alliance.AllianceActivity;
import com.example.rpghabittracker.ui.alliance.AllianceChatActivity;
import com.example.rpghabittracker.ui.friends.FriendsActivity;
import com.example.rpghabittracker.ui.home.MainActivity;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * In-app notification utilities:
 * - creating notification events in Firestore
 * - listening and showing Android system notifications
 * - resolving actionable notifications
 */
public final class AppNotificationManager {

    private AppNotificationManager() {}

    public static final String CHANNEL_ID = "rpg_notifications";
    private static final String CHANNEL_NAME = "RPG Habit Tracker Notifications";

    public static final String COLLECTION_USER_NOTIFICATIONS = "user_notifications";

    public static final String STATE_NEW = "NEW";
    public static final String STATE_SHOWN = "SHOWN";
    public static final String STATE_RESOLVED = "RESOLVED";

    public static final String TYPE_FRIEND_REQUEST = "FRIEND_REQUEST";
    public static final String TYPE_FRIEND_REQUEST_ACCEPTED = "FRIEND_REQUEST_ACCEPTED";
    public static final String TYPE_ALLIANCE_MESSAGE = "ALLIANCE_MESSAGE";
    public static final String TYPE_ALLIANCE_MEMBER_JOINED = "ALLIANCE_MEMBER_JOINED";

    public static final String ACTION_FRIEND_REQUEST_ACCEPT =
            "com.example.rpghabittracker.notifications.ACTION_FRIEND_REQUEST_ACCEPT";
    public static final String ACTION_FRIEND_REQUEST_REJECT =
            "com.example.rpghabittracker.notifications.ACTION_FRIEND_REQUEST_REJECT";

    public static final String EXTRA_NOTIFICATION_DOC_ID = "notificationDocId";
    public static final String EXTRA_FRIENDSHIP_ID = "friendshipId";
    public static final String EXTRA_SENDER_ID = "senderId";
    public static final String EXTRA_RECEIVER_ID = "receiverId";

    public static void ensureNotificationChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        NotificationChannel existing = notificationManager.getNotificationChannel(CHANNEL_ID);
        if (existing != null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("System notifications for social and mission events.");
        notificationManager.createNotificationChannel(channel);
    }

    @Nullable
    public static ListenerRegistration listenForUserNotifications(
            @NonNull Context context,
            @NonNull FirebaseFirestore firestore,
            @NonNull String userId
    ) {
        ensureNotificationChannel(context);

        return firestore.collection(COLLECTION_USER_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("state", STATE_NEW)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) return;

                    for (DocumentChange change : snapshot.getDocumentChanges()) {
                        if (change.getType() != DocumentChange.Type.ADDED) continue;
                        showNotificationFromDoc(context, firestore, change.getDocument());
                    }
                });
    }

    public static void createFriendRequestNotification(
            @NonNull FirebaseFirestore firestore,
            @NonNull String receiverId,
            @NonNull String senderId,
            @NonNull String senderName,
            @NonNull String friendshipId
    ) {
        String safeSenderName = senderName.trim().isEmpty() ? "Korisnik" : senderName;
        String docId = "friend_request_" + friendshipId + "_" + receiverId;

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", receiverId);
        payload.put("type", TYPE_FRIEND_REQUEST);
        payload.put("title", "Zahtev za prijateljstvo");
        payload.put("body", safeSenderName + " vam je poslao/la zahtev za prijateljstvo.");
        payload.put("senderId", senderId);
        payload.put("senderName", safeSenderName);
        payload.put("receiverId", receiverId);
        payload.put("friendshipId", friendshipId);
        payload.put("actionRequired", true);
        payload.put("state", STATE_NEW);
        payload.put("createdAt", FieldValue.serverTimestamp());

        firestore.collection(COLLECTION_USER_NOTIFICATIONS)
                .document(docId)
                .set(payload, SetOptions.merge());
    }

    public static void createFriendRequestAcceptedNotification(
            @NonNull FirebaseFirestore firestore,
            @NonNull String receiverId,
            @NonNull String acceptorId,
            @NonNull String acceptorName
    ) {
        String safeName = acceptorName.trim().isEmpty() ? "Korisnik" : acceptorName;

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", receiverId);
        payload.put("type", TYPE_FRIEND_REQUEST_ACCEPTED);
        payload.put("title", "Zahtev prihvaćen");
        payload.put("body", safeName + " je prihvatio/la vaš zahtev za prijateljstvo.");
        payload.put("acceptorId", acceptorId);
        payload.put("acceptorName", safeName);
        payload.put("actionRequired", false);
        payload.put("state", STATE_NEW);
        payload.put("createdAt", FieldValue.serverTimestamp());

        firestore.collection(COLLECTION_USER_NOTIFICATIONS).add(payload);
    }

    public static void createAllianceMessageNotification(
            @NonNull FirebaseFirestore firestore,
            @NonNull String receiverId,
            @NonNull String allianceId,
            @NonNull String messageId,
            @NonNull String senderId,
            @NonNull String senderName,
            @NonNull String messageText
    ) {
        String safeSenderName = senderName.trim().isEmpty() ? "Član saveza" : senderName;
        String trimmedMessage = messageText.trim();
        if (trimmedMessage.length() > 100) {
            trimmedMessage = trimmedMessage.substring(0, 97) + "...";
        }

        String docId = "alliance_message_" + messageId + "_" + receiverId;

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", receiverId);
        payload.put("type", TYPE_ALLIANCE_MESSAGE);
        payload.put("title", "Nova poruka u savezu");
        payload.put("body", safeSenderName + ": " + trimmedMessage);
        payload.put("senderId", senderId);
        payload.put("senderName", safeSenderName);
        payload.put("allianceId", allianceId);
        payload.put("messageId", messageId);
        payload.put("actionRequired", false);
        payload.put("state", STATE_NEW);
        payload.put("createdAt", FieldValue.serverTimestamp());

        firestore.collection(COLLECTION_USER_NOTIFICATIONS)
                .document(docId)
                .set(payload, SetOptions.merge());
    }

    public static void createAllianceMemberJoinedNotification(
            @NonNull FirebaseFirestore firestore,
            @NonNull String receiverId,
            @NonNull String allianceId,
            @NonNull String joinedMemberId,
            @NonNull String joinedMemberName
    ) {
        String safeName = joinedMemberName.trim().isEmpty() ? "Novi član" : joinedMemberName;
        String docId = "alliance_joined_" + allianceId + "_" + joinedMemberId + "_" + receiverId;

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", receiverId);
        payload.put("type", TYPE_ALLIANCE_MEMBER_JOINED);
        payload.put("title", "Novi član saveza");
        payload.put("body", safeName + " se pridružio/la vašem savezu.");
        payload.put("allianceId", allianceId);
        payload.put("joinedMemberId", joinedMemberId);
        payload.put("joinedMemberName", safeName);
        payload.put("actionRequired", false);
        payload.put("state", STATE_NEW);
        payload.put("createdAt", FieldValue.serverTimestamp());

        firestore.collection(COLLECTION_USER_NOTIFICATIONS)
                .document(docId)
                .set(payload, SetOptions.merge());
    }

    public static void resolveFriendRequestNotification(
            @NonNull FirebaseFirestore firestore,
            @Nullable Context context,
            @NonNull String userId,
            @NonNull String friendshipId,
            @NonNull String resolvedAction
    ) {
        firestore.collection(COLLECTION_USER_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", TYPE_FRIEND_REQUEST)
                .whereEqualTo("friendshipId", friendshipId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        resolveNotificationById(firestore, doc.getId(), resolvedAction);
                        if (context != null) {
                            cancelNotification(context, doc.getId());
                        }
                    }
                });
    }

    public static void resolveNotificationById(
            @NonNull FirebaseFirestore firestore,
            @Nullable String notificationDocId,
            @NonNull String resolvedAction
    ) {
        if (notificationDocId == null || notificationDocId.trim().isEmpty()) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("state", STATE_RESOLVED);
        updates.put("resolvedAction", resolvedAction);
        updates.put("resolvedAt", FieldValue.serverTimestamp());

        firestore.collection(COLLECTION_USER_NOTIFICATIONS)
                .document(notificationDocId)
                .set(updates, SetOptions.merge());
    }

    public static void cancelNotification(@NonNull Context context, @Nullable String notificationDocId) {
        if (notificationDocId == null || notificationDocId.trim().isEmpty()) return;
        NotificationManagerCompat.from(context)
                .cancel(notificationIdForDoc(notificationDocId));
    }

    private static void showNotificationFromDoc(
            @NonNull Context context,
            @NonNull FirebaseFirestore firestore,
            @NonNull DocumentSnapshot doc
    ) {
        if (!canPostNotifications(context)) return;

        String type = doc.getString("type");
        String title = doc.getString("title");
        String body = doc.getString("body");
        if (title == null || title.trim().isEmpty()) title = "RPG Habit Tracker";
        if (body == null) body = "";

        int notificationId = notificationIdForDoc(doc.getId());
        PendingIntent contentIntent = buildContentIntent(context, type, doc, notificationId);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setOnlyAlertOnce(true)
                .setContentIntent(contentIntent);

        boolean actionRequired = Boolean.TRUE.equals(doc.getBoolean("actionRequired"));
        if (TYPE_FRIEND_REQUEST.equals(type) && actionRequired) {
            PendingIntent acceptIntent = buildFriendRequestActionIntent(
                    context, ACTION_FRIEND_REQUEST_ACCEPT, doc, notificationId + 100
            );
            PendingIntent rejectIntent = buildFriendRequestActionIntent(
                    context, ACTION_FRIEND_REQUEST_REJECT, doc, notificationId + 200
            );

            builder.setOngoing(true)
                    .setAutoCancel(false)
                    .addAction(android.R.drawable.checkbox_on_background, "Prihvati", acceptIntent)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Odbij", rejectIntent);
        } else {
            builder.setOngoing(false).setAutoCancel(true);
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build());

        Map<String, Object> updates = new HashMap<>();
        updates.put("state", STATE_SHOWN);
        updates.put("shownAt", FieldValue.serverTimestamp());
        firestore.collection(COLLECTION_USER_NOTIFICATIONS)
                .document(doc.getId())
                .set(updates, SetOptions.merge());
    }

    @NonNull
    private static PendingIntent buildContentIntent(
            @NonNull Context context,
            @Nullable String type,
            @NonNull DocumentSnapshot doc,
            int requestCode
    ) {
        Intent intent;
        if (TYPE_ALLIANCE_MESSAGE.equals(type)) {
            intent = new Intent(context, AllianceChatActivity.class);
            String allianceId = doc.getString("allianceId");
            if (allianceId != null) {
                intent.putExtra("allianceId", allianceId);
            }
        } else if (TYPE_ALLIANCE_MEMBER_JOINED.equals(type)) {
            intent = new Intent(context, AllianceActivity.class);
        } else if (TYPE_FRIEND_REQUEST.equals(type) || TYPE_FRIEND_REQUEST_ACCEPTED.equals(type)) {
            intent = new Intent(context, FriendsActivity.class);
        } else {
            intent = new Intent(context, MainActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    @NonNull
    private static PendingIntent buildFriendRequestActionIntent(
            @NonNull Context context,
            @NonNull String action,
            @NonNull DocumentSnapshot doc,
            int requestCode
    ) {
        Intent intent = new Intent(context, NotificationActionReceiver.class);
        intent.setAction(action);
        intent.putExtra(EXTRA_NOTIFICATION_DOC_ID, doc.getId());
        intent.putExtra(EXTRA_FRIENDSHIP_ID, doc.getString("friendshipId"));
        intent.putExtra(EXTRA_SENDER_ID, doc.getString("senderId"));
        intent.putExtra(EXTRA_RECEIVER_ID, doc.getString("receiverId"));

        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static boolean canPostNotifications(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        return ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private static int notificationIdForDoc(@NonNull String docId) {
        return (docId.hashCode() & 0x7fffffff);
    }
}
