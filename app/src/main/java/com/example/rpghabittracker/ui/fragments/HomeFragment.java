package com.example.rpghabittracker.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.rpghabittracker.R;
import com.example.rpghabittracker.data.model.Boss;
import com.example.rpghabittracker.data.model.Task;
import com.example.rpghabittracker.data.model.User;
import com.example.rpghabittracker.ui.battle.BattleActivity;
import com.example.rpghabittracker.ui.viewmodel.UserViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * HomeFragment - Main dashboard with core player and boss information.
 */
public class HomeFragment extends Fragment {
    
    // UI Elements
    private TextView usernameText, levelText, xpText, coinsText;
    private TextView streakText;
    private TextView bossNameText, bossLevelText, bossHpText;
    private View xpProgressBar, bossHpBar;
    private MaterialButton attackButton;
    private MaterialCardView bossCard;
    
    private UserViewModel userViewModel;
    private FirebaseFirestore db;
    private ListenerRegistration userListener;
    private ListenerRegistration tasksListener;
    
    // User data
    private int currentLevel = 1;
    private int currentXp = 0;
    private int xpForNextLevel = 200;
    private int coins = 0;
    private int streak = 0;
    
    // Boss data
    private String bossName = "Zmaj Početnik";
    private int bossLevel = 1;
    private int bossHp = 200;
    private int bossMaxHp = 200;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        db = FirebaseFirestore.getInstance();
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        
        // Set user ID in ViewModel
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userViewModel.setUserId(currentUser.getUid());
        }
        
        initializeViews(view);
        setupClickListeners();
        setupUserObserver();
        loadUserDataRealtime();
        updateUI();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to this fragment
        loadUserDataRealtime();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove Firestore listeners
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
        if (tasksListener != null) {
            tasksListener.remove();
            tasksListener = null;
        }
    }
    
    private void initializeViews(View view) {
        // Header
        usernameText = view.findViewById(R.id.usernameText);
        
        // Level & XP
        levelText = view.findViewById(R.id.levelText);
        xpText = view.findViewById(R.id.xpText);
        coinsText = view.findViewById(R.id.coinsText);
        xpProgressBar = view.findViewById(R.id.xpProgressBar);
        
        // Quick Stats
        streakText = view.findViewById(R.id.streakText);
        
        // Boss
        bossCard = view.findViewById(R.id.bossCard);
        bossNameText = view.findViewById(R.id.bossNameText);
        bossLevelText = view.findViewById(R.id.bossLevelText);
        bossHpText = view.findViewById(R.id.bossHpText);
        bossHpBar = view.findViewById(R.id.bossHpBar);
        attackButton = view.findViewById(R.id.attackButton);
    }
    
    private void setupClickListeners() {
        attackButton.setOnClickListener(v -> {
            // Navigate to BattleActivity
            Intent intent = new Intent(requireContext(), BattleActivity.class);
            intent.putExtra(BattleActivity.EXTRA_BOSS_LEVEL, bossLevel);
            intent.putExtra(BattleActivity.EXTRA_USER_LEVEL, currentLevel);
            startActivity(intent);
        });

        bossCard.setOnClickListener(v -> {
            // Navigate to battle screen
            Intent intent = new Intent(requireContext(), BattleActivity.class);
            intent.putExtra(BattleActivity.EXTRA_BOSS_LEVEL, bossLevel);
            intent.putExtra(BattleActivity.EXTRA_USER_LEVEL, currentLevel);
            startActivity(intent);
        });
    }
    
    private void setupUserObserver() {
        // Observe user data from ViewModel (Room database)
        userViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                updateUIFromUser(user);
            }
        });
    }
    
    private void loadUserDataRealtime() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (currentUser != null) {
            // Set up realtime listener for Firestore user data
            if (userListener != null) {
                userListener.remove();
            }
            
            userListener = db.collection("users").document(currentUser.getUid())
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null || documentSnapshot == null || !documentSnapshot.exists()) {
                        usernameText.setText("Heroj");
                        return;
                    }
                    
                    String username = documentSnapshot.getString("username");
                    if (username != null) {
                        usernameText.setText(username);
                    }
                    
                    Long level = documentSnapshot.getLong("level");
                    if (level != null) currentLevel = level.intValue();
                    
                    Long xp = documentSnapshot.getLong("xp");
                    if (xp != null) currentXp = xp.intValue();
                    
                    Long coinsVal = documentSnapshot.getLong("coins");
                    if (coinsVal != null) coins = coinsVal.intValue();

                    Long currentBossLevel = documentSnapshot.getLong("bossLevel");
                    bossLevel = (currentBossLevel != null && currentBossLevel > 0)
                            ? currentBossLevel.intValue()
                            : 1;
                    bossName = Boss.getBossNameForLevel(bossLevel);
                    bossMaxHp = Boss.getBossHpForLevel(bossLevel);
                    bossHp = bossMaxHp;
                    
                    // XP required to complete the current level (stage model from spec)
                    xpForNextLevel = User.getXpForLevel(Math.max(1, currentLevel));
                    
                    updateUI();
                });

            startTaskStreakListener(currentUser.getUid());
        }
    }
    
    private void updateUIFromUser(User user) {
        if (user == null) return;
        
        usernameText.setText(user.getUsername() != null ? user.getUsername() : "Heroj");
        currentLevel = user.getLevel();
        currentXp = user.getExperiencePoints();
        coins = user.getCoins();
        xpForNextLevel = User.getXpForLevel(Math.max(1, currentLevel));
        
        updateUI();
    }
    
    private void updateUI() {
        // Level & XP
        levelText.setText(String.valueOf(currentLevel));

        int safeLevel = Math.max(1, currentLevel);
        int safeXp = Math.max(0, currentXp);
        boolean cumulativeXpModel = safeLevel > 1 && safeXp >= User.getXpForLevel(safeLevel);

        int xpNeededForNext;
        int xpProgress;
        if (cumulativeXpModel) {
            int currentThreshold = User.getXpForLevel(safeLevel);
            int nextThreshold = User.getXpForLevel(safeLevel + 1);
            xpNeededForNext = Math.max(1, nextThreshold - currentThreshold);
            xpProgress = Math.min(Math.max(0, safeXp - currentThreshold), xpNeededForNext);
        } else {
            xpNeededForNext = Math.max(1, User.getXpForLevel(safeLevel));
            xpProgress = Math.min(safeXp, xpNeededForNext);
        }
        xpForNextLevel = xpNeededForNext;
        
        xpText.setText(xpProgress + " / " + xpNeededForNext + " XP");
        coinsText.setText(String.valueOf(coins));
        
        // XP Progress bar
        float xpProgressPercent = xpNeededForNext > 0 ? (float) xpProgress / xpNeededForNext : 0;
        updateProgressBar(xpProgressBar, Math.min(xpProgressPercent, 1.0f));
        
        // Quick Stats
        streakText.setText(String.valueOf(streak));
        
        // Boss
        bossNameText.setText(bossName);
        bossLevelText.setText("Nivo " + bossLevel);
        bossHpText.setText(bossHp + "/" + bossMaxHp);
        
        float hpProgress = (float) bossHp / bossMaxHp;
        updateProgressBar(bossHpBar, hpProgress);
    }

    private void startTaskStreakListener(String userId) {
        if (tasksListener != null) {
            tasksListener.remove();
        }

        tasksListener = db.collection("tasks")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    streak = calculateCurrentStreakFromTasks(snapshots.getDocuments());
                    streakText.setText(String.valueOf(streak));
                });
    }

    private int calculateCurrentStreakFromTasks(List<DocumentSnapshot> docs) {
        if (docs == null || docs.isEmpty()) return 0;

        Map<Long, DayOutcome> outcomesByDay = new TreeMap<>();
        for (DocumentSnapshot doc : docs) {
            String status = doc.getString("status");
            if (!Task.STATUS_COMPLETED.equals(status) && !Task.STATUS_FAILED.equals(status)) {
                continue;
            }

            long taskDay = getTaskDay(doc);
            if (taskDay <= 0) continue;

            DayOutcome outcome = outcomesByDay.get(taskDay);
            if (outcome == null) outcome = new DayOutcome();

            if (Task.STATUS_FAILED.equals(status)) {
                outcome.failed = true;
            } else if (Task.STATUS_COMPLETED.equals(status)) {
                outcome.completed = true;
            }
            outcomesByDay.put(taskDay, outcome);
        }

        if (outcomesByDay.isEmpty()) return 0;

        List<Long> days = new ArrayList<>(outcomesByDay.keySet());
        Collections.sort(days, Collections.reverseOrder());

        long today = getDayStart(System.currentTimeMillis());
        int currentStreak = 0;
        boolean started = false;

        for (Long day : days) {
            if (day == null || day > today) continue;

            DayOutcome outcome = outcomesByDay.get(day);
            if (outcome == null) continue;

            if (!started) {
                started = true;
                if (outcome.failed) return 0;
                if (outcome.completed) currentStreak++;
                continue;
            }

            if (outcome.failed) break;
            if (outcome.completed) currentStreak++;
        }

        return currentStreak;
    }

    private long getTaskDay(DocumentSnapshot doc) {
        long dueDate = getLongValue(doc, "dueDate");
        if (dueDate > 0) return getDayStart(dueDate);

        long completedDate = getLongValue(doc, "completedDate");
        if (completedDate > 0) return getDayStart(completedDate);

        long createdAt = getLongValue(doc, "createdAt");
        if (createdAt > 0) return getDayStart(createdAt);

        return -1L;
    }

    private long getLongValue(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    private long getDayStart(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private static final class DayOutcome {
        boolean completed;
        boolean failed;
    }
    
    private void updateProgressBar(View progressBar, float progress) {
        if (progressBar == null) return;
        
        ViewGroup.LayoutParams params = progressBar.getLayoutParams();
        if (params instanceof FrameLayout.LayoutParams) {
            // Calculate width based on parent
            progressBar.post(() -> {
                View parent = (View) progressBar.getParent();
                int parentWidth = parent.getWidth();
                int newWidth = (int) (parentWidth * Math.max(0, Math.min(1, progress)));
                
                ViewGroup.LayoutParams newParams = progressBar.getLayoutParams();
                newParams.width = newWidth;
                progressBar.setLayoutParams(newParams);
            });
        }
    }
}
