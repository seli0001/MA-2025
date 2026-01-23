package com.example.rpghabittracker.ui.friends;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpghabittracker.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Activity for viewing another user's profile
 */
public class UserProfileActivity extends AppCompatActivity {

    private ImageView imageAvatar;
    private TextView textUsername;
    private TextView textTitle;
    private TextView textLevel;
    private TextView textXp;
    private TextView textXpNeeded;
    private ProgressBar progressXp;
    private TextView textTasksCompleted;
    private TextView textBossesDefeated;
    private TextView textAllianceName;
    private LinearLayout layoutStats;
    private LinearLayout layoutBadges;
    private RecyclerView recyclerBadges;

    private FirebaseFirestore firestore;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            finish();
            return;
        }

        firestore = FirebaseFirestore.getInstance();

        initViews();
        setupToolbar();
        loadUserProfile();
        loadUserStats();
    }

    private void initViews() {
        imageAvatar = findViewById(R.id.imageAvatar);
        textUsername = findViewById(R.id.textUsername);
        textTitle = findViewById(R.id.textTitle);
        textLevel = findViewById(R.id.textLevel);
        textXp = findViewById(R.id.textXp);
        textXpNeeded = findViewById(R.id.textXpNeeded);
        progressXp = findViewById(R.id.progressXp);
        textTasksCompleted = findViewById(R.id.textTasksCompleted);
        textBossesDefeated = findViewById(R.id.textBossesDefeated);
        textAllianceName = findViewById(R.id.textAllianceName);
        layoutStats = findViewById(R.id.layoutStats);
        layoutBadges = findViewById(R.id.layoutBadges);
        recyclerBadges = findViewById(R.id.recyclerBadges);
        
        recyclerBadges.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadUserProfile() {
        firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(this::displayUserProfile)
                .addOnFailureListener(e -> finish());
    }

    private void displayUserProfile(DocumentSnapshot doc) {
        if (!doc.exists()) {
            finish();
            return;
        }

        String username = doc.getString("username");
        String avatar = doc.getString("avatar");
        String title = doc.getString("title");
        Long level = doc.getLong("level");
        Long xp = doc.getLong("xp");
        String allianceId = doc.getString("allianceId");

        textUsername.setText(username != null ? username : "Unknown");
        textTitle.setText(title != null ? title : "Početnik");
        textLevel.setText("Level " + (level != null ? level : 1));
        
        int currentXp = xp != null ? xp.intValue() : 0;
        int currentLevel = level != null ? level.intValue() : 1;
        int xpNeeded = calculateXpForLevel(currentLevel + 1);
        int xpForCurrentLevel = calculateXpForLevel(currentLevel);
        int xpProgress = currentXp - xpForCurrentLevel;
        int xpRange = xpNeeded - xpForCurrentLevel;
        
        textXp.setText(currentXp + " XP");
        textXpNeeded.setText("/ " + xpNeeded + " XP");
        progressXp.setMax(xpRange);
        progressXp.setProgress(xpProgress);

        // Set avatar
        int avatarRes = getAvatarResource(avatar);
        imageAvatar.setImageResource(avatarRes);

        // Load alliance name if member
        if (allianceId != null && !allianceId.isEmpty()) {
            loadAllianceName(allianceId);
        } else {
            textAllianceName.setText("Bez saveza");
        }
    }

    private void loadAllianceName(String allianceId) {
        firestore.collection("alliances").document(allianceId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        textAllianceName.setText(name != null ? name : "Unknown");
                    }
                });
    }

    private void loadUserStats() {
        // Load completed tasks count
        firestore.collection("tasks")
                .whereEqualTo("userId", userId)
                .whereEqualTo("completed", true)
                .get()
                .addOnSuccessListener(querySnapshot -> 
                    textTasksCompleted.setText(String.valueOf(querySnapshot.size()))
                );

        // Load bosses defeated (this would require a battles collection)
        firestore.collection("battles")
                .whereEqualTo("userId", userId)
                .whereEqualTo("won", true)
                .get()
                .addOnSuccessListener(querySnapshot ->
                    textBossesDefeated.setText(String.valueOf(querySnapshot.size()))
                )
                .addOnFailureListener(e -> textBossesDefeated.setText("0"));
    }

    private int calculateXpForLevel(int level) {
        if (level <= 1) return 0;
        if (level == 2) return 200;
        
        int prevXp = 200;
        for (int i = 3; i <= level; i++) {
            prevXp = (int) Math.round((prevXp * 2 + prevXp / 2.0) / 100.0) * 100;
        }
        return prevXp;
    }

    private int getAvatarResource(String avatarId) {
        if (avatarId == null) return R.drawable.ic_avatar_placeholder;
        switch (avatarId) {
            case "avatar_1": return R.drawable.ic_avatar_1;
            case "avatar_2": return R.drawable.ic_avatar_2;
            case "avatar_3": return R.drawable.ic_avatar_3;
            case "avatar_4": return R.drawable.ic_avatar_4;
            case "avatar_5": return R.drawable.ic_avatar_5;
            default: return R.drawable.ic_avatar_placeholder;
        }
    }
}
