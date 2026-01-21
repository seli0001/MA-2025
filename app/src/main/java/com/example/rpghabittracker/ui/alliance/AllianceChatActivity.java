package com.example.rpghabittracker.ui.alliance;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpghabittracker.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Real-time chat for alliance members
 */
public class AllianceChatActivity extends AppCompatActivity {

    private RecyclerView recyclerMessages;
    private EditText editMessage;
    private ImageButton buttonSend;

    private FirebaseFirestore firestore;
    private String currentUserId;
    private String currentUsername;
    private String allianceId;
    private ListenerRegistration messageListener;

    private List<ChatMessage> messages = new ArrayList<>();
    private ChatAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alliance_chat);

        allianceId = getIntent().getStringExtra("allianceId");
        if (allianceId == null || allianceId.trim().isEmpty()) {
            Toast.makeText(this, "Savez nije učitan", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firestore = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        currentUserId = user.getUid();

        initViews();
        setupToolbar();
        loadUserData();
        setupMessageListener();
    }

    private void initViews() {
        recyclerMessages = findViewById(R.id.recyclerMessages);
        editMessage = findViewById(R.id.editMessage);
        buttonSend = findViewById(R.id.buttonSend);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerMessages.setLayoutManager(layoutManager);

        adapter = new ChatAdapter(messages, currentUserId);
        recyclerMessages.setAdapter(adapter);

        buttonSend.setOnClickListener(v -> sendMessage());
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadUserData() {
        firestore.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUsername = doc.getString("username");
                    }
                });
    }

    private void setupMessageListener() {
        messageListener = firestore.collection("alliances")
                .document(allianceId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, getFirestoreErrorMessage(error, "učitavanju poruka"), Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snapshot == null) return;

                    messages.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        ChatMessage message = new ChatMessage();
                        message.id = doc.getId();
                        message.senderId = doc.getString("senderId");
                        message.senderName = doc.getString("senderName");
                        message.text = doc.getString("text");
                        message.timestamp = doc.getTimestamp("timestamp");
                        messages.add(message);
                    }

                    adapter.notifyDataSetChanged();
                    if (!messages.isEmpty()) {
                        recyclerMessages.scrollToPosition(messages.size() - 1);
                    }
                });
    }

    private void sendMessage() {
        String text = editMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        String messageId = UUID.randomUUID().toString();
        
        Map<String, Object> message = new HashMap<>();
        message.put("id", messageId);
        message.put("allianceId", allianceId);
        message.put("senderId", currentUserId);
        message.put("senderName", currentUsername != null ? currentUsername : "Unknown");
        message.put("text", text);
        message.put("timestampClient", System.currentTimeMillis());
        message.put("timestamp", FieldValue.serverTimestamp());

        firestore.collection("alliances")
                .document(allianceId)
                .collection("messages")
                .document(messageId)
                .set(message)
                .addOnSuccessListener(aVoid -> {
                    editMessage.setText("");
                    
                    // Deal damage to mission boss (1 damage per message)
                    dealMissionDamage(1);
                })
                .addOnFailureListener(e ->
                    Toast.makeText(this, getFirestoreErrorMessage(e, "slanju poruke"), Toast.LENGTH_LONG).show()
                );
    }

    private void dealMissionDamage(int damage) {
        firestore.collection("alliances").document(allianceId)
                .get()
                .addOnSuccessListener(doc -> {
                    Boolean missionActive = doc.getBoolean("missionActive");
                    if (missionActive != null && missionActive) {
                        firestore.collection("alliances").document(allianceId)
                                .update("missionCurrentDamage", FieldValue.increment(damage));
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
        }
    }

    private String getFirestoreErrorMessage(Exception e, String action) {
        String message = "Greška pri " + action;
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException ff = (FirebaseFirestoreException) e;
            if (ff.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                message = "Nema dozvole za savez chat (proverite Firestore rules).";
            } else if (ff.getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                message = "Nedostaje Firestore indeks za savez chat.";
            } else if (ff.getMessage() != null && !ff.getMessage().trim().isEmpty()) {
                message = "Greška pri " + action + ": " + ff.getMessage();
            }
        } else if (e.getMessage() != null && !e.getMessage().trim().isEmpty()) {
            message = "Greška pri " + action + ": " + e.getMessage();
        }
        return message;
    }

    // Message model
    static class ChatMessage {
        String id;
        String senderId;
        String senderName;
        String text;
        com.google.firebase.Timestamp timestamp;
    }
}
