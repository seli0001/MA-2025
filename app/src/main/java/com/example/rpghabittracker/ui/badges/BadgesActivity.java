package com.example.rpghabittracker.ui.badges;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpghabittracker.R;
import com.example.rpghabittracker.data.model.Badge;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for displaying user badges/achievements
 */
public class BadgesActivity extends AppCompatActivity {
    
    private RecyclerView recyclerBadges;
    private TextView textUnlockedCount;
    private BadgeAdapter adapter;
    private FirebaseFirestore db;
    private String userId;
    
    // User stats for badge checking
    private int tasksCompleted = 0;
    private int currentStreak = 0;
    private int longestStreak = 0;
    private int level = 1;
    private int bossesDefeated = 0;
    private int totalXp = 0;
    private boolean hasAlliance = false;
    private int specialMissionsCompleted = 0;
    private List<String> unlockedBadgeIds = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_badges);
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }
        userId = currentUser.getUid();
        db = FirebaseFirestore.getInstance();
        
        initViews();
        loadUserStats();
    }
    
    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        textUnlockedCount = findViewById(R.id.textUnlockedCount);
        recyclerBadges = findViewById(R.id.recyclerBadges);
        recyclerBadges.setLayoutManager(new GridLayoutManager(this, 3));
        
        adapter = new BadgeAdapter(badge -> {
            // Show badge details
            showBadgeDetails(badge);
        });
        recyclerBadges.setAdapter(adapter);
    }
    
    private void loadUserStats() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Long tc = document.getLong("totalTasksCompleted");
                        if (tc != null) tasksCompleted = tc.intValue();
                        
                        Long cs = document.getLong("currentStreak");
                        if (cs != null) currentStreak = cs.intValue();
                        
                        Long ls = document.getLong("longestStreak");
                        if (ls != null) longestStreak = ls.intValue();
                        
                        Long lvl = document.getLong("level");
                        if (lvl != null) level = lvl.intValue();
                        
                        Long bd = document.getLong("bossesDefeated");
                        if (bd != null) bossesDefeated = bd.intValue();
                        
                        Long xp = document.getLong("xp");
                        if (xp != null) totalXp = xp.intValue();

                        Long sm = document.getLong("specialMissionsCompleted");
                        if (sm != null) specialMissionsCompleted = sm.intValue();
                        
                        String allianceId = document.getString("allianceId");
                        hasAlliance = allianceId != null && !allianceId.isEmpty();
                        
                        @SuppressWarnings("unchecked")
                        List<String> badges = (List<String>) document.get("badges");
                        if (badges != null) {
                            unlockedBadgeIds = badges;
                        }
                        
                        updateBadges();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Greška pri učitavanju", Toast.LENGTH_SHORT).show();
                    updateBadges();
                });
    }
    
    private void updateBadges() {
        List<Badge> badges = new ArrayList<>();
        int unlocked = 0;
        int maxStreak = Math.max(currentStreak, longestStreak);
        
        for (Badge template : Badge.ALL_BADGES) {
            Badge badge = new Badge(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getIcon(),
                template.getType(),
                template.getRequirement()
            );
            
            // Check if already unlocked
            if (unlockedBadgeIds.contains(badge.getId())) {
                badge.setUnlocked(true);
                unlocked++;
            } else if (badge.checkUnlock(
                    tasksCompleted,
                    maxStreak,
                    level,
                    bossesDefeated,
                    totalXp,
                    hasAlliance,
                    specialMissionsCompleted
            )) {
                // Newly unlocked - save to Firebase
                badge.setUnlocked(true);
                badge.setUnlockedAt(System.currentTimeMillis());
                unlocked++;
                
                // Save to Firebase
                saveBadgeUnlock(badge.getId());
            }
            
            badges.add(badge);
        }
        
        adapter.setBadges(badges);
        textUnlockedCount.setText(unlocked + " / " + Badge.ALL_BADGES.length + " otključano");
    }
    
    private void saveBadgeUnlock(String badgeId) {
        if (!unlockedBadgeIds.contains(badgeId)) {
            unlockedBadgeIds.add(badgeId);
            
            db.collection("users").document(userId)
                    .update("badges", unlockedBadgeIds)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "🎉 Nova značka otključana!", Toast.LENGTH_SHORT).show();
                    });
        }
    }
    
    private void showBadgeDetails(Badge badge) {
        String status = badge.isUnlocked() ? "✅ Otključano" : "🔒 Zaključano";
        String message = badge.getIcon() + " " + badge.getName() + "\n\n" + 
                        badge.getDescription() + "\n\n" + status;
        
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Značka")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
