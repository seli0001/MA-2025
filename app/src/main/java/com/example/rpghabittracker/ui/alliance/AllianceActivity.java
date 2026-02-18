package com.example.rpghabittracker.ui.alliance;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpghabittracker.R;
import com.example.rpghabittracker.notifications.AppNotificationManager;
import com.example.rpghabittracker.utils.AllianceMissionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Activity for managing alliance/savez - create, view, chat
 */
public class AllianceActivity extends AppCompatActivity {

    private LinearLayout layoutNoAlliance;
    private View layoutHasAlliance;  // Changed to View since it's NestedScrollView
    private MaterialButton buttonCreateAlliance;
    private MaterialButton buttonJoinAlliance;
    
    // Alliance view
    private TextView textAllianceName;
    private TextView textMemberCount;
    private TextView textMissionStatus;
    private ProgressBar progressMission;
    private TextView textMissionProgress;
    private TextView textMissionTimeLeft;
    private TextView textMyMissionProgress;
    private RecyclerView recyclerMembers;
    private MaterialButton buttonChat;
    private MaterialButton buttonStartMission;
    private MaterialButton buttonLeaveAlliance;

    private FirebaseFirestore firestore;
    private String currentUserId;
    private String currentAllianceId;
    private boolean isLeader = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alliance);

        firestore = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        currentUserId = user.getUid();

        initViews();
        setupToolbar();
        checkUserAlliance();
    }

    private void initViews() {
        layoutNoAlliance = findViewById(R.id.layoutNoAlliance);
        layoutHasAlliance = findViewById(R.id.layoutHasAlliance);
        buttonCreateAlliance = findViewById(R.id.buttonCreateAlliance);
        buttonJoinAlliance = findViewById(R.id.buttonJoinAlliance);
        
        textAllianceName = findViewById(R.id.textAllianceName);
        textMemberCount = findViewById(R.id.textMemberCount);
        textMissionStatus = findViewById(R.id.textMissionStatus);
        progressMission = findViewById(R.id.progressMission);
        textMissionProgress = findViewById(R.id.textMissionProgress);
        textMissionTimeLeft = findViewById(R.id.textMissionTimeLeft);
        textMyMissionProgress = findViewById(R.id.textMyMissionProgress);
        recyclerMembers = findViewById(R.id.recyclerMembers);
        buttonChat = findViewById(R.id.buttonChat);
        buttonStartMission = findViewById(R.id.buttonStartMission);
        buttonLeaveAlliance = findViewById(R.id.buttonLeaveAlliance);

        recyclerMembers.setLayoutManager(new LinearLayoutManager(this));

        buttonCreateAlliance.setOnClickListener(v -> showCreateAllianceDialog());
        buttonJoinAlliance.setOnClickListener(v -> showJoinAllianceDialog());
        buttonChat.setOnClickListener(v -> openAllianceChat());
        buttonStartMission.setOnClickListener(v -> startMission());
        buttonLeaveAlliance.setOnClickListener(v -> confirmLeaveAlliance());
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void checkUserAlliance() {
        firestore.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String allianceId = doc.getString("allianceId");
                        if (allianceId != null && !allianceId.isEmpty()) {
                            currentAllianceId = allianceId;
                            showAllianceView();
                            loadAllianceData();
                        } else {
                            showNoAllianceView();
                        }
                    } else {
                        showNoAllianceView();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Greška pri učitavanju: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showNoAllianceView();
                });
    }

    private void showNoAllianceView() {
        layoutNoAlliance.setVisibility(View.VISIBLE);
        layoutHasAlliance.setVisibility(View.GONE);
    }

    private void showAllianceView() {
        layoutNoAlliance.setVisibility(View.GONE);
        layoutHasAlliance.setVisibility(View.VISIBLE);
    }

    private void loadAllianceData() {
        if (currentAllianceId == null) {
            showNoAllianceView();
            return;
        }
        
        firestore.collection("alliances").document(currentAllianceId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        showNoAllianceView();
                        return;
                    }

                    if (shouldFinalizeExpiredMission(doc)) {
                        AllianceMissionManager.finalizeMissionIfExpired(
                                firestore,
                                currentAllianceId,
                                (finalized, won, message) -> loadAllianceData()
                        );
                        return;
                    }

                    displayAllianceData(doc);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Greška: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void displayAllianceData(DocumentSnapshot doc) {
        if (!doc.exists()) {
            showNoAllianceView();
            return;
        }

        String name = doc.getString("name");
        String leaderId = doc.getString("leaderId");
        List<String> memberIds = getStringList(doc.get("memberIds"));
        boolean missionActive = Boolean.TRUE.equals(doc.getBoolean("missionActive"));
        long missionEndTime = getMillis(doc.get("missionEndTime"), 0L);

        textAllianceName.setText(name != null ? name : "Savez");
        textMemberCount.setText(memberIds.size() + " članova");

        isLeader = currentUserId.equals(leaderId);
        buttonStartMission.setVisibility(isLeader ? View.VISIBLE : View.GONE);

        // Mission status
        if (missionActive) {
            textMissionStatus.setText("Misija u toku");
            textMissionStatus.setTextColor(getResources().getColor(R.color.success, null));
            buttonStartMission.setEnabled(false);
            buttonStartMission.setText("Misija aktivna");

            int bossHp = getInt(doc.get("missionBossHp"), Math.max(100, memberIds.size() * 100));
            int damage = getInt(doc.get("missionCurrentDamage"), -1);
            if (damage < 0) {
                int bossCurrentHp = getInt(doc.get("missionBossCurrentHp"), bossHp);
                damage = Math.max(0, bossHp - bossCurrentHp);
            }
            damage = Math.max(0, Math.min(damage, bossHp));

            progressMission.setMax(bossHp);
            progressMission.setProgress(damage);
            textMissionProgress.setText(damage + " / " + bossHp + " štete");
            progressMission.setVisibility(View.VISIBLE);
            textMissionProgress.setVisibility(View.VISIBLE);
            textMissionTimeLeft.setVisibility(View.VISIBLE);
            textMissionTimeLeft.setText(formatTimeLeft(missionEndTime - System.currentTimeMillis()));
            loadMyMissionProgress();
        } else {
            boolean missionProcessed = Boolean.TRUE.equals(doc.getBoolean("missionResultProcessed"));
            boolean missionWon = Boolean.TRUE.equals(doc.getBoolean("missionWon"));
            if (missionProcessed) {
                textMissionStatus.setText(missionWon ? "Poslednja misija: pobeda" : "Poslednja misija: neuspeh");
                int colorRes = missionWon ? R.color.success : R.color.text_secondary;
                textMissionStatus.setTextColor(getResources().getColor(colorRes, null));
            } else {
                textMissionStatus.setText("Nema aktivne misije");
                textMissionStatus.setTextColor(getResources().getColor(R.color.text_secondary, null));
            }
            buttonStartMission.setEnabled(true);
            buttonStartMission.setText("Pokreni misiju");
            progressMission.setVisibility(View.GONE);
            textMissionProgress.setVisibility(View.GONE);
            textMissionTimeLeft.setVisibility(View.GONE);
            textMyMissionProgress.setVisibility(View.GONE);
        }

        // Load members
        if (!memberIds.isEmpty()) {
            loadMembers(memberIds, missionActive);
        } else {
            displayMembers(new ArrayList<>());
        }
    }

    private void loadMembers(List<String> memberIds, boolean missionActive) {
        if (!missionActive) {
            loadMemberProfiles(memberIds, new HashMap<>());
            return;
        }

        firestore.collection("alliances").document(currentAllianceId)
                .collection("missionProgress")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Integer> missionDamageByUser = new HashMap<>();
                    for (DocumentSnapshot progressDoc : snapshot.getDocuments()) {
                        missionDamageByUser.put(progressDoc.getId(), getInt(progressDoc.get("damageDealt"), 0));
                    }
                    loadMemberProfiles(memberIds, missionDamageByUser);
                })
                .addOnFailureListener(e -> loadMemberProfiles(memberIds, new HashMap<>()));
    }

    private void loadMemberProfiles(List<String> memberIds, Map<String, Integer> missionDamageByUser) {
        Map<String, MemberItem> memberMap = new HashMap<>();
        AtomicInteger pending = new AtomicInteger(memberIds.size());

        for (String memberId : memberIds) {
            firestore.collection("users").document(memberId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        MemberItem item = new MemberItem();
                        item.id = memberId;
                        item.username = doc.exists() ? doc.getString("username") : "Nepoznat član";
                        item.avatar = doc.exists() ? doc.getString("avatar") : null;
                        item.level = doc.exists() ? getInt(doc.get("level"), 1) : 1;
                        item.missionDamage = missionDamageByUser.getOrDefault(memberId, 0);
                        memberMap.put(memberId, item);

                        if (pending.decrementAndGet() == 0) {
                            List<MemberItem> ordered = new ArrayList<>();
                            for (String id : memberIds) {
                                MemberItem member = memberMap.get(id);
                                if (member != null) ordered.add(member);
                            }
                            displayMembers(ordered);
                        }
                    })
                    .addOnFailureListener(e -> {
                        MemberItem item = new MemberItem();
                        item.id = memberId;
                        item.username = "Nepoznat član";
                        item.avatar = null;
                        item.level = 1;
                        item.missionDamage = missionDamageByUser.getOrDefault(memberId, 0);
                        memberMap.put(memberId, item);

                        if (pending.decrementAndGet() == 0) {
                            List<MemberItem> ordered = new ArrayList<>();
                            for (String id : memberIds) {
                                MemberItem member = memberMap.get(id);
                                if (member != null) ordered.add(member);
                            }
                            displayMembers(ordered);
                        }
                    });
        }
    }

    private void displayMembers(List<MemberItem> members) {
        AllianceMemberAdapter adapter = new AllianceMemberAdapter(members);
        recyclerMembers.setAdapter(adapter);
    }

    private void showCreateAllianceDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_alliance, null);
        EditText editName = dialogView.findViewById(R.id.editAllianceName);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Kreiraj savez")
                .setView(dialogView)
                .setPositiveButton("Kreiraj", (dialog, which) -> {
                    String name = editName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        createAlliance(name);
                    }
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }

    private void createAlliance(String name) {
        String allianceId = UUID.randomUUID().toString();
        List<String> memberIds = new ArrayList<>();
        memberIds.add(currentUserId);

        Map<String, Object> alliance = new HashMap<>();
        alliance.put("id", allianceId);
        alliance.put("name", name);
        alliance.put("leaderId", currentUserId);
        alliance.put("memberIds", memberIds);
        alliance.put("createdAt", FieldValue.serverTimestamp());
        alliance.put("missionActive", false);
        alliance.put("missionBossHp", 0);
        alliance.put("missionBossCurrentHp", 0);
        alliance.put("missionCurrentDamage", 0);
        alliance.put("missionEndTime", 0L);
        alliance.put("missionResultProcessed", false);
        alliance.put("missionRewardDistributed", false);
        alliance.put("missionWon", false);
        alliance.put("status", "ACTIVE");

        firestore.collection("alliances").document(allianceId)
                .set(alliance)
                .addOnSuccessListener(aVoid -> {
                    // Update user's alliance ID
                    firestore.collection("users").document(currentUserId)
                            .update("allianceId", allianceId)
                            .addOnSuccessListener(aVoid2 -> {
                                currentAllianceId = allianceId;
                                Toast.makeText(this, "Savez kreiran!", Toast.LENGTH_SHORT).show();
                                showAllianceView();
                                loadAllianceData();
                            });
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Greška: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void showJoinAllianceDialog() {
        // Show list of available alliances
        firestore.collection("alliances")
                .limit(20)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> names = new ArrayList<>();
                    List<String> ids = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        if (isMissionLocked(doc)) continue;
                        String allianceName = doc.getString("name");
                        names.add((allianceName == null || allianceName.trim().isEmpty()) ? "Savez" : allianceName);
                        ids.add(doc.getId());
                    }
                    
                    if (names.isEmpty()) {
                        Toast.makeText(this, "Nema dostupnih saveza bez aktivne specijalne misije", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    String[] nameArray = names.toArray(new String[0]);
                    
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Pridruži se savezu")
                            .setItems(nameArray, (dialog, which) -> {
                                requestJoinAlliance(ids.get(which));
                            })
                            .setNegativeButton("Otkaži", null)
                            .show();
                });
    }

    private void requestJoinAlliance(String allianceId) {
        firestore.collection("alliances").document(allianceId)
                .get()
                .addOnSuccessListener(allianceDoc -> {
                    if (!allianceDoc.exists()) {
                        Toast.makeText(this, "Savez ne postoji", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (isMissionLocked(allianceDoc)) {
                        Toast.makeText(this, "Ne možete se pridružiti dok je misija aktivna", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    firestore.collection("alliances").document(allianceId)
                            .update("memberIds", FieldValue.arrayUnion(currentUserId))
                            .addOnSuccessListener(aVoid -> {
                                firestore.collection("users").document(currentUserId)
                                        .update("allianceId", allianceId)
                                        .addOnSuccessListener(aVoid2 -> {
                                            String leaderId = allianceDoc.getString("leaderId");
                                            if (leaderId != null && !leaderId.equals(currentUserId)) {
                                                notifyLeaderAboutJoin(leaderId, allianceId);
                                            }

                                            currentAllianceId = allianceId;
                                            Toast.makeText(this, "Pridružili ste se savezu!", Toast.LENGTH_SHORT).show();
                                            showAllianceView();
                                            loadAllianceData();
                                        });
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Greška: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Greška: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void notifyLeaderAboutJoin(@NonNull String leaderId, @NonNull String allianceId) {
        firestore.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String username = userDoc.getString("username");
                    if (username == null || username.trim().isEmpty()) {
                        username = "Korisnik";
                    }

                    AppNotificationManager.createAllianceMemberJoinedNotification(
                            firestore,
                            leaderId,
                            allianceId,
                            currentUserId,
                            username
                    );
                });
    }

    private void openAllianceChat() {
        if (currentAllianceId == null || currentAllianceId.trim().isEmpty()) {
            Toast.makeText(this, "Savez se još učitava", Toast.LENGTH_SHORT).show();
            checkUserAlliance();
            return;
        }

        Intent intent = new Intent(this, AllianceChatActivity.class);
        intent.putExtra("allianceId", currentAllianceId);
        startActivity(intent);
    }

    private void startMission() {
        if (!isLeader) return;

        AllianceMissionManager.startMission(
                firestore,
                currentAllianceId,
                currentUserId,
                (success, message, bossHp) -> {
                    if (success) {
                        Toast.makeText(this, message + " Boss HP: " + bossHp, Toast.LENGTH_SHORT).show();
                        loadAllianceData();
                    } else {
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void confirmLeaveAlliance() {
        runWhenMissionUnlocked(() -> {
            String message = isLeader
                    ? "Ako napustite savez kao vođa, savez će biti uništen."
                    : "Da li ste sigurni da želite da napustite savez?";

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Napusti savez")
                    .setMessage(message)
                    .setPositiveButton("Napusti", (dialog, which) -> leaveAlliance())
                    .setNegativeButton("Otkaži", null)
                    .show();
        });
    }

    private void leaveAlliance() {
        runWhenMissionUnlocked(this::leaveAllianceInternal);
    }

    private void leaveAllianceInternal() {
        if (isLeader) {
            firestore.collection("alliances").document(currentAllianceId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        List<String> memberIds = getStringList(doc.get("memberIds"));

                        for (String memberId : memberIds) {
                            firestore.collection("users").document(memberId)
                                    .update("allianceId", null);
                        }

                        firestore.collection("alliances").document(currentAllianceId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    currentAllianceId = null;
                                    Toast.makeText(this, "Savez je uništen", Toast.LENGTH_SHORT).show();
                                    showNoAllianceView();
                                });
                    });
        } else {
            firestore.collection("alliances").document(currentAllianceId)
                    .update("memberIds", FieldValue.arrayRemove(currentUserId))
                    .addOnSuccessListener(aVoid -> {
                        firestore.collection("users").document(currentUserId)
                                .update("allianceId", null)
                                .addOnSuccessListener(aVoid2 -> {
                                    currentAllianceId = null;
                                    Toast.makeText(this, "Napustili ste savez", Toast.LENGTH_SHORT).show();
                                    showNoAllianceView();
                                });
                    });
        }
    }

    private void runWhenMissionUnlocked(@NonNull Runnable action) {
        if (currentAllianceId == null || currentAllianceId.trim().isEmpty()) return;

        firestore.collection("alliances").document(currentAllianceId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        showNoAllianceView();
                        return;
                    }
                    if (shouldFinalizeExpiredMission(doc)) {
                        AllianceMissionManager.finalizeMissionIfExpired(
                                firestore,
                                currentAllianceId,
                                (finalized, won, message) -> {
                                    if (finalized) {
                                        runWhenMissionUnlocked(action);
                                    } else if (message != null && !message.trim().isEmpty()) {
                                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                                    }
                                }
                        );
                        return;
                    }
                    if (isMissionLocked(doc)) {
                        Toast.makeText(this, "Specijalna misija je aktivna i ne može se prekinuti", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    action.run();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Greška: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void loadMyMissionProgress() {
        if (currentAllianceId == null || currentAllianceId.trim().isEmpty()) {
            textMyMissionProgress.setVisibility(View.GONE);
            return;
        }

        firestore.collection("alliances").document(currentAllianceId)
                .collection("missionProgress")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(progressDoc -> {
                    int myDamage = getInt(progressDoc.get("damageDealt"), 0);
                    textMyMissionProgress.setText("Moj doprinos: " + myDamage + " HP");
                    textMyMissionProgress.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> textMyMissionProgress.setVisibility(View.GONE));
    }

    private boolean shouldFinalizeExpiredMission(DocumentSnapshot doc) {
        boolean missionActive = Boolean.TRUE.equals(doc.getBoolean("missionActive"));
        long missionEnd = getMillis(doc.get("missionEndTime"), 0L);
        return missionActive && missionEnd > 0L && System.currentTimeMillis() > missionEnd;
    }

    private boolean isMissionLocked(DocumentSnapshot doc) {
        return Boolean.TRUE.equals(doc.getBoolean("missionActive"));
    }

    private String formatTimeLeft(long millisLeft) {
        if (millisLeft <= 0) return "Preostalo: 0 min";

        long totalMinutes = millisLeft / (60 * 1000);
        long days = totalMinutes / (24 * 60);
        long hours = (totalMinutes % (24 * 60)) / 60;
        long minutes = totalMinutes % 60;

        if (days > 0) {
            return String.format(Locale.getDefault(), "Preostalo: %d d %d h", days, hours);
        }
        if (hours > 0) {
            return String.format(Locale.getDefault(), "Preostalo: %d h %d min", hours, minutes);
        }
        return String.format(Locale.getDefault(), "Preostalo: %d min", Math.max(1, minutes));
    }

    private int getInt(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return fallback;
    }

    private long getMillis(Object value, long fallback) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toDate().getTime();
        }
        return fallback;
    }

    @NonNull
    private List<String> getStringList(Object value) {
        if (!(value instanceof List)) return new ArrayList<>();
        List<?> raw = (List<?>) value;
        List<String> out = new ArrayList<>();
        for (Object item : raw) {
            if (item instanceof String) out.add((String) item);
        }
        return out;
    }

    // Helper class for member data
    static class MemberItem {
        String id;
        String username;
        String avatar;
        int level;
        int missionDamage;
    }
}
