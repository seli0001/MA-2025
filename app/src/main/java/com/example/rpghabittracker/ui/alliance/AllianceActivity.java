package com.example.rpghabittracker.ui.alliance;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpghabittracker.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
                .addOnSuccessListener(this::displayAllianceData)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Greška: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @SuppressWarnings("unchecked")
    private void displayAllianceData(DocumentSnapshot doc) {
        if (!doc.exists()) {
            showNoAllianceView();
            return;
        }

        String name = doc.getString("name");
        String leaderId = doc.getString("leaderId");
        List<String> memberIds = (List<String>) doc.get("memberIds");
        Boolean missionActive = doc.getBoolean("missionActive");
        Long missionBossHp = doc.getLong("missionBossHp");
        Long missionCurrentDamage = doc.getLong("missionCurrentDamage");

        textAllianceName.setText(name != null ? name : "Savez");
        textMemberCount.setText((memberIds != null ? memberIds.size() : 0) + " članova");

        isLeader = currentUserId.equals(leaderId);
        buttonStartMission.setVisibility(isLeader ? View.VISIBLE : View.GONE);

        // Mission status
        if (missionActive != null && missionActive) {
            textMissionStatus.setText("Misija u toku");
            textMissionStatus.setTextColor(getResources().getColor(R.color.success, null));
            buttonStartMission.setEnabled(false);
            buttonStartMission.setText("Misija aktivna");

            int bossHp = missionBossHp != null ? missionBossHp.intValue() : 100;
            int damage = missionCurrentDamage != null ? missionCurrentDamage.intValue() : 0;
            progressMission.setMax(bossHp);
            progressMission.setProgress(damage);
            textMissionProgress.setText(damage + " / " + bossHp + " štete");
            progressMission.setVisibility(View.VISIBLE);
            textMissionProgress.setVisibility(View.VISIBLE);
        } else {
            textMissionStatus.setText("Nema aktivne misije");
            textMissionStatus.setTextColor(getResources().getColor(R.color.text_secondary, null));
            buttonStartMission.setEnabled(true);
            buttonStartMission.setText("Pokreni misiju");
            progressMission.setVisibility(View.GONE);
            textMissionProgress.setVisibility(View.GONE);
        }

        // Load members
        if (memberIds != null && !memberIds.isEmpty()) {
            loadMembers(memberIds);
        }
    }

    private void loadMembers(List<String> memberIds) {
        List<MemberItem> members = new ArrayList<>();
        
        for (String memberId : memberIds) {
            firestore.collection("users").document(memberId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            MemberItem item = new MemberItem();
                            item.id = doc.getId();
                            item.username = doc.getString("username");
                            item.avatar = doc.getString("avatar");
                            Long level = doc.getLong("level");
                            item.level = level != null ? level.intValue() : 1;
                            members.add(item);
                            
                            if (members.size() == memberIds.size()) {
                                displayMembers(members);
                            }
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
        alliance.put("missionCurrentDamage", 0);

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
                        names.add(doc.getString("name"));
                        ids.add(doc.getId());
                    }
                    
                    if (names.isEmpty()) {
                        Toast.makeText(this, "Nema dostupnih saveza", Toast.LENGTH_SHORT).show();
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
        // Add user to alliance
        firestore.collection("alliances").document(allianceId)
                .update("memberIds", FieldValue.arrayUnion(currentUserId))
                .addOnSuccessListener(aVoid -> {
                    firestore.collection("users").document(currentUserId)
                            .update("allianceId", allianceId)
                            .addOnSuccessListener(aVoid2 -> {
                                currentAllianceId = allianceId;
                                Toast.makeText(this, "Pridružili ste se savezu!", Toast.LENGTH_SHORT).show();
                                showAllianceView();
                                loadAllianceData();
                            });
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Greška: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
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

    @SuppressWarnings("unchecked")
    private void startMission() {
        if (!isLeader) return;

        // Get member count for boss HP calculation
        firestore.collection("alliances").document(currentAllianceId)
                .get()
                .addOnSuccessListener(doc -> {
                    List<String> memberIds = (List<String>) doc.get("memberIds");
                    int memberCount = memberIds != null ? memberIds.size() : 1;
                    int bossHp = memberCount * 100;

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("missionActive", true);
                    updates.put("missionBossHp", bossHp);
                    updates.put("missionCurrentDamage", 0);
                    updates.put("missionStartTime", FieldValue.serverTimestamp());

                    firestore.collection("alliances").document(currentAllianceId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Misija pokrenuta! Boss HP: " + bossHp, Toast.LENGTH_SHORT).show();
                                loadAllianceData();
                            });
                });
    }

    private void confirmLeaveAlliance() {
        String message = isLeader 
                ? "Ako napustite savez kao vođa, savez će biti uništen."
                : "Da li ste sigurni da želite da napustite savez?";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Napusti savez")
                .setMessage(message)
                .setPositiveButton("Napusti", (dialog, which) -> leaveAlliance())
                .setNegativeButton("Otkaži", null)
                .show();
    }

    @SuppressWarnings("unchecked")
    private void leaveAlliance() {
        if (isLeader) {
            // Delete alliance
            firestore.collection("alliances").document(currentAllianceId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        List<String> memberIds = (List<String>) doc.get("memberIds");
                        
                        // Clear alliance ID for all members
                        if (memberIds != null) {
                            for (String memberId : memberIds) {
                                firestore.collection("users").document(memberId)
                                        .update("allianceId", null);
                            }
                        }
                        
                        // Delete alliance
                        firestore.collection("alliances").document(currentAllianceId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    currentAllianceId = null;
                                    Toast.makeText(this, "Savez je uništen", Toast.LENGTH_SHORT).show();
                                    showNoAllianceView();
                                });
                    });
        } else {
            // Just remove self from members
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

    // Helper class for member data
    static class MemberItem {
        String id;
        String username;
        String avatar;
        int level;
    }
}
