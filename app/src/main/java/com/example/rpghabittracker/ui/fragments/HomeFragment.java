package com.example.rpghabittracker.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.rpghabittracker.R;
import com.example.rpghabittracker.data.model.Boss;
import com.example.rpghabittracker.data.model.User;
import com.example.rpghabittracker.ui.battle.BattleActivity;
import com.example.rpghabittracker.ui.viewmodel.UserViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

/**
 * HomeFragment - Main dashboard with player stats, boss info, and daily tasks
 */
public class HomeFragment extends Fragment {
    
    // UI Elements
    private TextView greetingText, usernameText, levelText, xpText, coinsText;
    private TextView tasksCompletedText, streakText;
    private TextView bossNameText, bossLevelText, bossHpText;
    private View xpProgressBar, bossHpBar;
    private MaterialButton attackButton, addFirstTaskButton;
    private MaterialCardView bossCard, emptyTasksCard;
    private TextView seeAllTasksText;
    
    private UserViewModel userViewModel;
    private FirebaseFirestore db;
    private ListenerRegistration userListener;
    
    // User data
    private int currentLevel = 1;
    private int currentXp = 0;
    private int xpForNextLevel = 200;
    private int coins = 0;
    private int tasksCompleted = 0;
    private int totalTasks = 0;
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
        // Remove Firestore listener
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
    }
    
    private void initializeViews(View view) {
        // Header
        greetingText = view.findViewById(R.id.greetingText);
        usernameText = view.findViewById(R.id.usernameText);
        
        // Level & XP
        levelText = view.findViewById(R.id.levelText);
        xpText = view.findViewById(R.id.xpText);
        coinsText = view.findViewById(R.id.coinsText);
        xpProgressBar = view.findViewById(R.id.xpProgressBar);
        
        // Quick Stats
        tasksCompletedText = view.findViewById(R.id.tasksCompletedText);
        streakText = view.findViewById(R.id.streakText);
        
        // Boss
        bossCard = view.findViewById(R.id.bossCard);
        bossNameText = view.findViewById(R.id.bossNameText);
        bossLevelText = view.findViewById(R.id.bossLevelText);
        bossHpText = view.findViewById(R.id.bossHpText);
        bossHpBar = view.findViewById(R.id.bossHpBar);
        attackButton = view.findViewById(R.id.attackButton);
        
        // Tasks
        seeAllTasksText = view.findViewById(R.id.seeAllTasksText);
        emptyTasksCard = view.findViewById(R.id.emptyTasksCard);
        addFirstTaskButton = view.findViewById(R.id.addFirstTaskButton);
    }
    
    private void setupClickListeners() {
        attackButton.setOnClickListener(v -> {
            // Navigate to BattleActivity
            Intent intent = new Intent(requireContext(), BattleActivity.class);
            intent.putExtra(BattleActivity.EXTRA_BOSS_LEVEL, bossLevel);
            intent.putExtra(BattleActivity.EXTRA_USER_LEVEL, currentLevel);
            startActivity(intent);
        });
        
        seeAllTasksText.setOnClickListener(v -> {
            // Navigate to tasks tab
            if (getActivity() != null) {
                Toast.makeText(getActivity(), "Prelazak na zadatke", Toast.LENGTH_SHORT).show();
            }
        });
        
        addFirstTaskButton.setOnClickListener(v -> {
            // Open add task dialog
            if (getActivity() != null) {
                Toast.makeText(getActivity(), "Dodavanje zadatka", Toast.LENGTH_SHORT).show();
            }
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
            // Set greeting based on time of day
            int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
            if (hour < 12) {
                greetingText.setText("Dobro jutro! ☀️");
            } else if (hour < 18) {
                greetingText.setText("Dobar dan! 👋");
            } else {
                greetingText.setText("Dobro veče! 🌙");
            }
            
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
                    
                    Long streakVal = documentSnapshot.getLong("currentStreak");
                    if (streakVal != null) streak = streakVal.intValue();
                    
                    Long totalCompleted = documentSnapshot.getLong("totalTasksCompleted");
                    if (totalCompleted != null) tasksCompleted = totalCompleted.intValue();
                    
                    Long totalCreated = documentSnapshot.getLong("totalTasksCreated");
                    if (totalCreated != null) totalTasks = totalCreated.intValue();

                    Long currentBossLevel = documentSnapshot.getLong("bossLevel");
                    bossLevel = (currentBossLevel != null && currentBossLevel > 0)
                            ? currentBossLevel.intValue()
                            : 1;
                    bossName = Boss.getBossNameForLevel(bossLevel);
                    bossMaxHp = Boss.getBossHpForLevel(bossLevel);
                    bossHp = bossMaxHp;
                    
                    // Calculate XP for next level
                    xpForNextLevel = User.getXpForLevel(currentLevel + 1);
                    
                    updateUI();
                });
        }
    }
    
    private void updateUIFromUser(User user) {
        if (user == null) return;
        
        usernameText.setText(user.getUsername() != null ? user.getUsername() : "Heroj");
        currentLevel = user.getLevel();
        currentXp = user.getExperiencePoints();
        coins = user.getCoins();
        streak = user.getCurrentStreak();
        tasksCompleted = user.getTotalTasksCompleted();
        totalTasks = user.getTotalTasksCreated();
        xpForNextLevel = User.getXpForLevel(currentLevel + 1);
        
        updateUI();
    }
    
    private void loadUserData() {
        // Deprecated - kept for backwards compatibility
        loadUserDataRealtime();
    }
    
    private void updateUI() {
        // Level & XP
        levelText.setText(String.valueOf(currentLevel));
        
        // Calculate XP progress with fallback for legacy stage-based XP values.
        int xpForCurrentLevelThreshold = currentLevel > 1 ? User.getXpForLevel(currentLevel) : 0;
        int xpNeededForNext = Math.max(1, xpForNextLevel - xpForCurrentLevelThreshold);
        int xpProgress = currentXp - xpForCurrentLevelThreshold;
        if (xpProgress < 0) {
            xpNeededForNext = Math.max(1, User.getXpForLevel(currentLevel));
            xpProgress = Math.min(Math.max(0, currentXp), xpNeededForNext);
        } else {
            xpProgress = Math.min(xpProgress, xpNeededForNext);
        }
        
        xpText.setText(xpProgress + " / " + xpNeededForNext + " XP");
        coinsText.setText(String.valueOf(coins));
        
        // XP Progress bar
        float xpProgressPercent = xpNeededForNext > 0 ? (float) xpProgress / xpNeededForNext : 0;
        updateProgressBar(xpProgressBar, Math.min(xpProgressPercent, 1.0f));
        
        // Quick Stats
        tasksCompletedText.setText(tasksCompleted + "/" + totalTasks);
        streakText.setText(String.valueOf(streak));
        
        // Boss
        bossNameText.setText(bossName);
        bossLevelText.setText("Nivo " + bossLevel);
        bossHpText.setText(bossHp + "/" + bossMaxHp);
        
        float hpProgress = (float) bossHp / bossMaxHp;
        updateProgressBar(bossHpBar, hpProgress);
        
        // Tasks empty state
        emptyTasksCard.setVisibility(totalTasks == 0 ? View.VISIBLE : View.GONE);
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
    
    private void attackBoss() {
        // Simple attack mechanic
        int damage = 10 + (currentLevel * 2); // Base damage + level bonus
        bossHp = Math.max(0, bossHp - damage);
        
        // Show attack feedback
        Toast.makeText(getActivity(), "⚔️ Zadao si " + damage + " štete!", Toast.LENGTH_SHORT).show();
        
        // Update UI
        bossHpText.setText(bossHp + "/" + bossMaxHp);
        float hpProgress = (float) bossHp / bossMaxHp;
        updateProgressBar(bossHpBar, hpProgress);
        
        // Check if boss defeated
        if (bossHp <= 0) {
            defeatBoss();
        }
    }
    
    private void defeatBoss() {
        // Calculate rewards
        int coinsReward = 200 + (bossLevel * 40); // 200 base + 20% per level
        int xpReward = 50 * bossLevel;
        
        coins += coinsReward;
        currentXp += xpReward;
        
        // Check for level up
        if (currentXp >= xpForNextLevel) {
            levelUp();
        }
        
        // Show victory message
        Toast.makeText(getActivity(), 
            "🎉 Pobeda! +" + coinsReward + " novčića, +" + xpReward + " XP!", 
            Toast.LENGTH_LONG).show();
        
        // Reset boss (next level)
        bossLevel++;
        bossMaxHp = (int) (bossMaxHp * 2.5); // HP formula: HP * 2 + HP/2
        bossHp = bossMaxHp;
        bossName = "Zmaj Nivo " + bossLevel;
        
        updateUI();
    }
    
    private void levelUp() {
        currentXp -= xpForNextLevel;
        currentLevel++;
        xpForNextLevel = (int) (xpForNextLevel * 2.5 / 100) * 100; // Round to 100
        
        Toast.makeText(getActivity(), 
            "🎊 LEVEL UP! Sada si nivo " + currentLevel + "!", 
            Toast.LENGTH_LONG).show();
    }
}
