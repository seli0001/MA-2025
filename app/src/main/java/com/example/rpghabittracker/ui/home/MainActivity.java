package com.example.rpghabittracker.ui.home;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.rpghabittracker.R;
import com.example.rpghabittracker.notifications.AppNotificationManager;
import com.example.rpghabittracker.ui.auth.LoginActivity;
import com.example.rpghabittracker.ui.battle.BattleActivity;
import com.example.rpghabittracker.ui.fragments.HomeFragment;
import com.example.rpghabittracker.ui.fragments.ProfileFragment;
import com.example.rpghabittracker.ui.fragments.ShopFragment;
import com.example.rpghabittracker.ui.fragments.TasksFragment;
import com.example.rpghabittracker.ui.tasks.AddTaskActivity;
import com.example.rpghabittracker.ui.viewmodel.UserViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class MainActivity extends AppCompatActivity {
    
    private BottomNavigationView bottomNavigation;
    private FloatingActionButton fabAddTask;
    private UserViewModel userViewModel;
    
    // Fragments
    private HomeFragment homeFragment;
    private TasksFragment tasksFragment;
    private Fragment battleFragment;
    private ShopFragment shopFragment;
    private ProfileFragment profileFragment;
    
    private Fragment activeFragment;
    private ListenerRegistration notificationListenerRegistration;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if user is logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
            return;
        }
        
        setContentView(R.layout.activity_main);
        AppNotificationManager.ensureNotificationChannel(this);
        
        // Initialize ViewModel and sync user first, then load fragments
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        syncUserToLocalDatabase(currentUser);
        
        initializeViews();
        initializeFragments();
        setupNavigation();
        setupFAB();
        
        // Load home fragment by default
        if (savedInstanceState == null) {
            loadFragment(homeFragment);
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        } else {
            Fragment restored = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
            if (restored != null) {
                activeFragment = restored;
            } else {
                loadFragment(homeFragment);
                bottomNavigation.setSelectedItemId(R.id.nav_home);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startNotificationListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopNotificationListener();
    }
    
    private void syncUserToLocalDatabase(FirebaseUser firebaseUser) {
        // This ensures the user exists in local Room database
        String uid = firebaseUser.getUid();
        String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
        String displayName = firebaseUser.getDisplayName();
        String username = displayName != null && !displayName.isEmpty() ? displayName : email.split("@")[0];
        
        userViewModel.setUserId(uid);
        userViewModel.createOrUpdateUser(uid, email, username, "avatar_1", user -> {
            android.util.Log.d("MainActivity", "User synced to local DB: " + 
                (user != null ? "SUCCESS - " + user.getUsername() + ", XP=" + user.getExperiencePoints() + ", coins=" + user.getCoins() : "FAILED - null"));
        });
    }
    
    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void startNotificationListener() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || notificationListenerRegistration != null) return;

        notificationListenerRegistration = AppNotificationManager.listenForUserNotifications(
                this,
                FirebaseFirestore.getInstance(),
                user.getUid()
        );
    }

    private void stopNotificationListener() {
        if (notificationListenerRegistration != null) {
            notificationListenerRegistration.remove();
            notificationListenerRegistration = null;
        }
    }
    
    private void initializeViews() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        fabAddTask = findViewById(R.id.fabAddTask);
    }
    
    private void initializeFragments() {
        homeFragment = new HomeFragment();
        tasksFragment = new TasksFragment();
        battleFragment = new BattleFragment();
        shopFragment = new ShopFragment();
        profileFragment = new ProfileFragment();
        
        activeFragment = null;
    }
    
    private void setupNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_home) {
                loadFragment(homeFragment);
                fabAddTask.show();
                return true;
            } else if (itemId == R.id.nav_tasks) {
                loadFragment(tasksFragment);
                fabAddTask.hide(); // Tasks fragment has its own FAB
                return true;
            } else if (itemId == R.id.nav_battle) {
                loadFragment(battleFragment);
                fabAddTask.hide();
                return true;
            } else if (itemId == R.id.nav_shop) {
                loadFragment(shopFragment);
                fabAddTask.hide();
                return true;
            } else if (itemId == R.id.nav_profile) {
                loadFragment(profileFragment);
                fabAddTask.hide();
                return true;
            }
            
            return false;
        });
    }
    
    private void setupFAB() {
        fabAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddTaskActivity.class);
            startActivity(intent);
        });
    }
    
    private void loadFragment(Fragment fragment) {
        if (fragment == activeFragment) return;
        
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        );
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
        
        activeFragment = fragment;
    }
    
    /**
     * Placeholder fragment for unimplemented screens
     */
    public static class PlaceholderFragment extends Fragment {
        private String message = "Uskoro!";
        
        public PlaceholderFragment() {
            // Required empty public constructor
        }
        
        public PlaceholderFragment(String message) {
            this.message = message;
        }
        
        @Override
        public android.view.View onCreateView(android.view.LayoutInflater inflater, 
                                              android.view.ViewGroup container,
                                              Bundle savedInstanceState) {
            android.widget.LinearLayout layout = new android.widget.LinearLayout(getContext());
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            layout.setGravity(android.view.Gravity.CENTER);
            layout.setBackgroundColor(getResources().getColor(R.color.md_theme_light_background, null));
            layout.setPadding(48, 48, 48, 48);
            
            android.widget.TextView emoji = new android.widget.TextView(getContext());
            emoji.setText("🚧");
            emoji.setTextSize(64);
            emoji.setGravity(android.view.Gravity.CENTER);
            layout.addView(emoji);
            
            android.widget.TextView text = new android.widget.TextView(getContext());
            text.setText(message);
            text.setTextSize(20);
            text.setTextColor(getResources().getColor(R.color.text_primary, null));
            text.setGravity(android.view.Gravity.CENTER);
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.topMargin = 32;
            text.setLayoutParams(params);
            layout.addView(text);
            
            android.widget.TextView subtext = new android.widget.TextView(getContext());
            subtext.setText("Ova funkcionalnost će biti dostupna uskoro");
            subtext.setTextSize(14);
            subtext.setTextColor(getResources().getColor(R.color.text_secondary, null));
            subtext.setGravity(android.view.Gravity.CENTER);
            android.widget.LinearLayout.LayoutParams subParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            subParams.topMargin = 16;
            subtext.setLayoutParams(subParams);
            layout.addView(subtext);
            
            return layout;
        }
    }
}
