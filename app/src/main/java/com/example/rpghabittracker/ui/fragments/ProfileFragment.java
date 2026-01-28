package com.example.rpghabittracker.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.rpghabittracker.R;
import com.example.rpghabittracker.data.model.User;
import com.example.rpghabittracker.ui.auth.LoginActivity;
import com.example.rpghabittracker.ui.categories.CategoriesActivity;
import com.example.rpghabittracker.ui.equipment.EquipmentActivity;
import com.example.rpghabittracker.ui.friends.FriendsActivity;
import com.example.rpghabittracker.ui.alliance.AllianceActivity;
import com.example.rpghabittracker.ui.settings.SettingsActivity;
import com.example.rpghabittracker.ui.viewmodel.UserViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

/**
 * Fragment for displaying user profile
 */
public class ProfileFragment extends Fragment {
    
    private ImageView imageAvatar;
    private TextView textLevelBadge, textUsername, textTitle;
    private TextView textLevel, textXp, textCoins;
    private TextView textXpProgressLabel;
    private TextView textXpProgress;
    private ProgressBar progressXp;
    private MaterialCardView cardStatistics, cardCategories, cardSettings;
    private MaterialCardView cardFriends, cardAlliance, cardEquipment, cardBadges;
    private MaterialButton buttonLogout;
    
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private UserViewModel userViewModel;
    private ListenerRegistration userListener;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        
        // Set user ID in ViewModel
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            userViewModel.setUserId(currentUser.getUid());
        }
        
        initViews(view);
        setupClickListeners();
        setupUserObserver();
        loadUserDataRealtime();
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
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
    }
    
    private void initViews(View view) {
        imageAvatar = view.findViewById(R.id.imageAvatar);
        textLevelBadge = view.findViewById(R.id.textLevelBadge);
        textUsername = view.findViewById(R.id.textUsername);
        textTitle = view.findViewById(R.id.textTitle);
        textLevel = view.findViewById(R.id.textLevel);
        textXp = view.findViewById(R.id.textXp);
        textCoins = view.findViewById(R.id.textCoins);
        textXpProgressLabel = view.findViewById(R.id.textXpProgressLabel);
        textXpProgress = view.findViewById(R.id.textXpProgress);
        progressXp = view.findViewById(R.id.progressXp);
        cardStatistics = view.findViewById(R.id.cardStatistics);
        cardCategories = view.findViewById(R.id.cardCategories);
        cardSettings = view.findViewById(R.id.cardSettings);
        cardFriends = view.findViewById(R.id.cardFriends);
        cardAlliance = view.findViewById(R.id.cardAlliance);
        cardEquipment = view.findViewById(R.id.cardEquipment);
        cardBadges = view.findViewById(R.id.cardBadges);
        buttonLogout = view.findViewById(R.id.buttonLogout);
    }
    
    private void setupClickListeners() {
        cardStatistics.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), 
                    com.example.rpghabittracker.ui.statistics.StatisticsActivity.class));
        });
        
        cardCategories.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), CategoriesActivity.class));
        });
        
        cardSettings.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), SettingsActivity.class));
        });
        
        cardFriends.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), FriendsActivity.class));
        });
        
        cardAlliance.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), AllianceActivity.class));
        });
        
        cardEquipment.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), EquipmentActivity.class));
        });
        
        cardBadges.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), 
                    com.example.rpghabittracker.ui.badges.BadgesActivity.class));
        });
        
        buttonLogout.setOnClickListener(v -> logout());
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
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
            return;
        }
        
        // Set basic info from Firebase Auth
        String displayName = currentUser.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            textUsername.setText(displayName);
        } else {
            textUsername.setText(currentUser.getEmail());
        }
        
        // Set up realtime listener for Firestore
        if (userListener != null) {
            userListener.remove();
        }
        
        userListener = db.collection("users").document(currentUser.getUid())
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null || documentSnapshot == null || !documentSnapshot.exists()) {
                        setDefaultUserData();
                        return;
                    }
                    
                    updateUIFromDocument(documentSnapshot);
                });
    }
    
    private void updateUIFromDocument(com.google.firebase.firestore.DocumentSnapshot doc) {
        String username = doc.getString("username");
        if (username != null && !username.isEmpty()) {
            textUsername.setText(username);
        }
        
        Long level = doc.getLong("level");
        int userLevel = level != null ? level.intValue() : 1;
        textLevel.setText(String.valueOf(userLevel));
        textLevelBadge.setText(String.valueOf(userLevel));
        
        Long xp = doc.getLong("xp");
        int userXp = xp != null ? xp.intValue() : 0;
        textXp.setText(String.valueOf(userXp));
        
        Long coins = doc.getLong("coins");
        textCoins.setText(coins != null ? String.valueOf(coins) : "0");
        
        // Set title based on level
        String title = getTitleForLevel(userLevel);
        textTitle.setText(title);
        
        updateXpProgress(userLevel, userXp);
    }
    
    private void updateUIFromUser(User user) {
        textUsername.setText(user.getUsername() != null ? user.getUsername() : "Heroj");
        textLevel.setText(String.valueOf(user.getLevel()));
        textLevelBadge.setText(String.valueOf(user.getLevel()));
        textXp.setText(String.valueOf(user.getExperiencePoints()));
        textCoins.setText(String.valueOf(user.getCoins()));
        
        // Set title
        String title = user.getTitle() != null ? user.getTitle() : getTitleForLevel(user.getLevel());
        textTitle.setText(title);
        
        updateXpProgress(user.getLevel(), user.getExperiencePoints());
    }
    
    private void loadUserData() {
        // Deprecated - using realtime updates now
        loadUserDataRealtime();
    }
    
    private void setDefaultUserData() {
        textLevel.setText("1");
        textLevelBadge.setText("1");
        textXp.setText("0");
        textCoins.setText("0");
        textTitle.setText("🏅 Početnik");
        if (textXpProgressLabel != null) {
            textXpProgressLabel.setText("Progress to Level 2");
        }
        textXpProgress.setText("0 / 100");
        progressXp.setProgress(0);
    }

    private void updateXpProgress(int level, int xp) {
        int safeLevel = Math.max(1, level);
        int safeXp = Math.max(0, xp);

        int xpForCurrentLevelThreshold = safeLevel > 1 ? User.getXpForLevel(safeLevel) : 0;
        int xpForNextLevelThreshold = User.getXpForLevel(safeLevel + 1);
        int xpNeeded = Math.max(1, xpForNextLevelThreshold - xpForCurrentLevelThreshold);
        int progress = safeXp - xpForCurrentLevelThreshold;

        // Some legacy users have "XP in current stage" instead of cumulative XP.
        // For that data shape, avoid negative values and show stage progress directly.
        if (progress < 0) {
            xpNeeded = Math.max(1, User.getXpForLevel(safeLevel));
            progress = Math.min(safeXp, xpNeeded);
        } else {
            progress = Math.min(progress, xpNeeded);
        }

        if (textXpProgressLabel != null) {
            textXpProgressLabel.setText("Progress to Level " + (safeLevel + 1));
        }

        textXpProgress.setText(progress + " / " + xpNeeded);
        int progressPercentage = Math.round((progress * 100f) / xpNeeded);
        progressXp.setProgress(Math.min(Math.max(progressPercentage, 0), 100));
    }
    
    private String getTitleForLevel(int level) {
        // Titles based on level ranges
        if (level >= 20) return "👑 Legenda";
        if (level >= 18) return "⚔️ Šampion";
        if (level >= 15) return "🗡️ Vitez";
        if (level >= 12) return "🛡️ Ratnik";
        if (level >= 10) return "🏹 Lovac";
        if (level >= 8) return "⚡ Avanturista";
        if (level >= 5) return "🌟 Učenik";
        if (level >= 3) return "📚 Šegrt";
        return "🏅 Početnik";
    }
    
    private void logout() {
        auth.signOut();
        navigateToLogin();
    }
    
    private void navigateToLogin() {
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
