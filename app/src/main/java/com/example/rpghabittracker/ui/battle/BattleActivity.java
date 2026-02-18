package com.example.rpghabittracker.ui.battle;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.animation.ObjectAnimator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.example.rpghabittracker.R;
import com.example.rpghabittracker.data.model.Boss;
import com.example.rpghabittracker.data.model.Task;
import com.example.rpghabittracker.ui.viewmodel.UserViewModel;
import com.example.rpghabittracker.utils.AllianceMissionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.SetOptions;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Activity for Boss Battle with shake sensor implementation
 */
public class BattleActivity extends AppCompatActivity implements SensorEventListener {
    
    public static final String EXTRA_BOSS_LEVEL = "boss_level";
    public static final String EXTRA_USER_LEVEL = "user_level";
    
    private MaterialToolbar toolbar;
    private TextView textBossName, textBossLevel, textBossHp, textPlayerPp;
    private TextView textInstructions, textAttackResult;
    private ProgressBar progressBossHp;
    private ImageView imageBoss;
    private View viewBossGlow, viewHitEffect;
    private MaterialButton buttonAttack;
    private FrameLayout layoutResult, layoutTreasureChest;
    private MaterialCardView cardResult;
    private TextView textResultTitle, textResultMessage, textRewardCoins, textRewardXp;
    private TextView textShakeToOpen;
    private MaterialButton buttonContinue;
    
    // Lottie animations
    private LottieAnimationView lottieHit, lottieAttack;
    private LottieAnimationView lottieTreasureChest, lottieConfetti;
    
    // ViewModel
    private UserViewModel userViewModel;
    private String userIdForBattle;
    
    // Sensor
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTime = 0;
    private static final int SHAKE_THRESHOLD = 12;
    private static final int SHAKE_COOLDOWN = 500; // ms
    
    // Battle state
    private int bossMaxHp = 200;
    private int bossCurrentHp = 200;
    private int playerPp = 0;
    private int maxPlayerPp = 0;
    private int bossLevel = 1;
    private int userLevel = 1;
    private String bossName = "Goblin";
    private boolean battleEnded = false;
    private boolean rewardsAwarded = false;
    private boolean treasureChestOpened = false;
    private boolean isVictory = false;
    
    // Attack limit (spec: 5 base; boots can add extra attacks)
    private static final int BASE_MAX_ATTACKS = 5;
    private int maxAttacks = BASE_MAX_ATTACKS;
    private int attackCount = 0;
    
    // Hit chance based on success rate
    private float hitChance = 0.7f; // Default 70%, modified by success rate
    
    // Equipment bonuses
    private int equipmentPpBonus = 0;
    private float equipmentDamageMultiplier = 1.0f;
    private float equipmentCritChanceBonus = 0.0f;
    private float equipmentHitChanceBonus = 0.0f; // Shield: +10% per piece
    private static final float BASE_CRIT_CHANCE = 0.2f; // 20%
    
    // Rewards (stored for showing after chest opens)
    private int coinsReward = 200;
    private int xpReward = 50;
    private int finalCoins = 0;
    private int finalXp = 0;
    private boolean bossLevelProgressed = false;
    
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_battle);
        
        initViews();
        setupToolbar();
        setupSensor();
        setupViewModel();
        setupBattle();
        setupButtons();
    }
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        textBossName = findViewById(R.id.textBossName);
        textBossLevel = findViewById(R.id.textBossLevel);
        textBossHp = findViewById(R.id.textBossHp);
        textPlayerPp = findViewById(R.id.textPlayerPp);
        textInstructions = findViewById(R.id.textInstructions);
        textAttackResult = findViewById(R.id.textAttackResult);
        progressBossHp = findViewById(R.id.progressBossHp);
        imageBoss = findViewById(R.id.imageBoss);
        viewBossGlow = findViewById(R.id.viewBossGlow);
        viewHitEffect = findViewById(R.id.viewHitEffect);
        buttonAttack = findViewById(R.id.buttonAttack);
        
        layoutResult = findViewById(R.id.layoutResult);
        cardResult = findViewById(R.id.cardResult);
        layoutTreasureChest = findViewById(R.id.layoutTreasureChest);
        textResultTitle = findViewById(R.id.textResultTitle);
        textResultMessage = findViewById(R.id.textResultMessage);
        textRewardCoins = findViewById(R.id.textRewardCoins);
        textRewardXp = findViewById(R.id.textRewardXp);
        textShakeToOpen = findViewById(R.id.textShakeToOpen);
        buttonContinue = findViewById(R.id.buttonContinue);
        
        // Lottie animations
        lottieHit = findViewById(R.id.lottieHit);
        lottieAttack = findViewById(R.id.lottieAttack);
        lottieTreasureChest = findViewById(R.id.lottieTreasureChest);
        lottieConfetti = findViewById(R.id.lottieConfetti);
        
        // Setup Lottie animations
        setupLottieAnimations();
    }
    
    private void setupLottieAnimations() {
        // Attack animation
        lottieAttack.setAnimation(R.raw.anim_attack);
        lottieAttack.setRepeatCount(0);
        
        // Hit impact animation  
        lottieHit.setAnimation(R.raw.anim_hit);
        lottieHit.setRepeatCount(0);
        
        // Treasure chest animation
        lottieTreasureChest.setAnimation(R.raw.anim_treasure_chest);
        lottieTreasureChest.setRepeatCount(0);
        lottieTreasureChest.setProgress(0f);
        
        // Confetti animation
        lottieConfetti.setAnimation(R.raw.anim_confetti);
        lottieConfetti.setRepeatCount(2);
    }
    
    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> {
            if (!battleEnded) {
                confirmExit();
            } else {
                finish();
            }
        });
    }
    
    private void confirmExit() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Napusti bitku?")
                .setMessage("Ako napustite bitku sada, nećete dobiti nagrade.")
                .setPositiveButton("Napusti", (dialog, which) -> finish())
                .setNegativeButton("Nastavi borbu", null)
                .show();
    }
    
    private void setupSensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer == null) {
                // No accelerometer - show hint to use button
                if (textInstructions != null) {
                    textInstructions.setText("Tapnite dugme za napad!");
                }
            }
        }
    }
    
    private void setupViewModel() {
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            userIdForBattle = userId;
            String email = user.getEmail() != null ? user.getEmail() : "";
            String displayName = user.getDisplayName();
            String username = displayName != null && !displayName.isEmpty() ? displayName : email.split("@")[0];
            
            userViewModel.setUserId(userId);
            
            // Ensure user exists in local Room database
            userViewModel.createOrUpdateUser(userId, email, username, "avatar_1", syncedUser -> {
                android.util.Log.d("BattleActivity", "User synced for battle: " + (syncedUser != null ? syncedUser.getUsername() : "null"));

                long levelStartTime = 0L;
                if (syncedUser != null) {
                    userLevel = Math.max(1, syncedUser.getLevel());
                    // Use currentLevelStartTime so hit chance only counts tasks from
                    // the current stage (etapa = period between two levels, per spec §5)
                    levelStartTime = syncedUser.getCurrentLevelStartTime() > 0
                            ? syncedUser.getCurrentLevelStartTime()
                            : syncedUser.getCreatedAt();
                }

                // Load hit chance filtered to current stage
                loadHitChanceFromSuccessRate(userId, levelStartTime);

                // After sync, load actual PP from database
                userViewModel.getUserPowerPoints(pp -> {
                    runOnUiThread(() -> {
                        maxPlayerPp = pp + equipmentPpBonus;
                        playerPp = maxPlayerPp;
                        updateUI();
                    });
                });
            });
            
            // Load equipment bonuses
            loadEquipmentBonuses(userId);
        }
    }
    
    /**
     * Calculates hit chance from task success rate in the CURRENT STAGE only.
     * Per spec §5: "Etapa predstavlja vremenski period između dva nivoa."
     * Only tasks created on or after currentLevelStartTime are considered.
     * Recurring templates are skipped — only their daily occurrences count.
     * Tasks that exceed the daily/weekly quota (countsTowardQuota=false) are excluded.
     */
    private void loadHitChanceFromSuccessRate(String userId, long currentLevelStartTime) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int eligibleTasks = 0;
                    int completedTasks = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        // Skip recurring templates — they are never directly completed;
                        // only their daily occurrence copies count toward success rate.
                        Boolean isRecurring = doc.getBoolean("isRecurring");
                        String parentTaskId = doc.getString("parentTaskId");
                        boolean isTemplate = Boolean.TRUE.equals(isRecurring)
                                && (parentTaskId == null || parentTaskId.isEmpty());
                        if (isTemplate) continue;

                        // Only count tasks from the current stage (etapa).
                        Long createdAt = resolveCreatedAtMillis(doc);
                        if (currentLevelStartTime <= 0 || createdAt == null || createdAt < currentLevelStartTime) {
                            continue;
                        }

                        // Exclude tasks that exceeded the daily/weekly quota.
                        Boolean countsTowardQuota = doc.getBoolean("countsTowardQuota");
                        if (countsTowardQuota != null && !countsTowardQuota) {
                            continue;
                        }

                        String status = normalizeTaskStatus(doc.getString("status"));
                        boolean ignoredStatus =
                                Task.STATUS_PAUSED.equals(status) || Task.STATUS_CANCELLED.equals(status);
                        if (ignoredStatus) {
                            continue;
                        }

                        eligibleTasks++;

                        Boolean completed = doc.getBoolean("completed"); // Legacy fallback
                        if (Task.STATUS_COMPLETED.equals(status) || (completed != null && completed)) {
                            completedTasks++;
                        }
                    }

                    // Default to 100% when the current stage has no eligible tasks yet.
                    if (eligibleTasks > 0) {
                        hitChance = (float) completedTasks / eligibleTasks;
                    } else {
                        hitChance = 1.0f;
                    }

                    android.util.Log.d("BattleActivity",
                            "Hit chance calc: levelStart=" + currentLevelStartTime
                                    + ", eligible=" + eligibleTasks
                                    + ", completed=" + completedTasks
                                    + ", hitChance=" + hitChance);

                    updateUI();
                })
                .addOnFailureListener(e -> {
                    // Keep previous/default chance if task data can't be loaded.
                    updateUI();
                });
    }

    private Long resolveCreatedAtMillis(QueryDocumentSnapshot doc) {
        Number createdAtNumber = (Number) doc.get("createdAt");
        if (createdAtNumber != null) {
            return createdAtNumber.longValue();
        }

        Timestamp createdAtTimestamp = doc.getTimestamp("createdAt");
        if (createdAtTimestamp != null) {
            return createdAtTimestamp.toDate().getTime();
        }

        return null;
    }

    private String normalizeTaskStatus(String rawStatus) {
        if (rawStatus == null) return null;
        String normalized = rawStatus.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        if ("CANCELED".equals(normalized)) {
            return Task.STATUS_CANCELLED;
        }
        return normalized;
    }
    
    private void loadEquipmentBonuses(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("users").document(userId)
                .collection("equipment")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int ppBonus = 0;
                    float damageBonus = 1.0f;
                    float critBonus = 0.0f;
                    float hitChanceBonus = 0.0f;
                    int extraAttacks = BASE_MAX_ATTACKS; // start fresh each load

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Number bonus = (Number) doc.get("bonus");
                        String effect = doc.getString("effect");
                        double bonusValue = bonus != null ? bonus.doubleValue() : 0d;

                        if (effect == null) continue;

                        switch (effect) {
                            // Permanent PP potions — treated as flat PP increase
                            case "BOOST_PP_PERMANENT":
                                // bonusValue is a fraction (e.g. 0.05 = 5%)
                                // applied as a percentage of current base PP
                                if (bonusValue > 0d) {
                                    ppBonus += (int) Math.round(maxPlayerPp * bonusValue);
                                }
                                break;

                            // Single-use PP potions — one-battle multiplier on base damage
                            case "BOOST_PP_SINGLE":
                                if (bonusValue > 0d) {
                                    damageBonus *= (1.0f + (float) bonusValue);
                                }
                                break;

                            // Gloves: +10% attack power
                            case "ATTACK_POWER":
                                if (bonusValue > 1.0d) {
                                    damageBonus *= (float) bonusValue;
                                } else if (bonusValue > 0d) {
                                    damageBonus *= (1.0f + (float) bonusValue);
                                }
                                break;

                            // Shield: +10% hit chance (stacks per piece)
                            case "HIT_CHANCE":
                                if (bonusValue > 0d) {
                                    hitChanceBonus += (float) bonusValue;
                                }
                                break;

                            // Boots: 40% chance for +1 attack per active pair (spec §6)
                            case "EXTRA_ATTACK":
                                if (bonusValue > 0d && random.nextFloat() < (float) bonusValue) {
                                    extraAttacks++;
                                }
                                break;

                            case "CRIT_CHANCE":
                                if (bonusValue > 1.0d) {
                                    critBonus += (float) (bonusValue / 100.0d);
                                } else if (bonusValue > 0d) {
                                    critBonus += (float) bonusValue;
                                }
                                break;

                            // Sword: +5% permanent PP (treated same as BOOST_PP_PERMANENT)
                            case "WEAPON_SWORD_PP":
                                if (bonusValue > 0d) {
                                    damageBonus *= (1.0f + (float) bonusValue);
                                }
                                break;
                        }
                    }

                    equipmentPpBonus = ppBonus;
                    equipmentDamageMultiplier = damageBonus;
                    equipmentCritChanceBonus = critBonus;
                    equipmentHitChanceBonus = hitChanceBonus;
                    maxAttacks = extraAttacks;
                    
                    // Update PP with bonus
                    maxPlayerPp = Math.max(0, maxPlayerPp + ppBonus);
                    playerPp = maxPlayerPp;
                    updateUI();
                });
    }
    
    private void setupBattle() {
        // Get parameters from intent
        int intentBossLevel = getIntent().getIntExtra(EXTRA_BOSS_LEVEL, -1);
        userLevel = Math.max(1, getIntent().getIntExtra(EXTRA_USER_LEVEL, userLevel));

        if (intentBossLevel > 0) {
            applyBossLevel(intentBossLevel);
        }

        if (userIdForBattle == null || userIdForBattle.trim().isEmpty()) {
            applyBossLevel(intentBossLevel > 0 ? intentBossLevel : 1);
            return;
        }

        // Always refresh from persisted source to keep level consistent from all entry points.
        loadBossLevelFromFirestore();
    }

    private void loadBossLevelFromFirestore() {
        if (userIdForBattle == null || userIdForBattle.trim().isEmpty()) {
            applyBossLevel(1);
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userIdForBattle)
                .get()
                .addOnSuccessListener(doc -> {
                    Long level = doc.getLong("bossLevel");
                    int resolvedLevel = (level != null && level > 0) ? level.intValue() : 1;

                    if (level == null) {
                        Map<String, Object> init = new HashMap<>();
                        init.put("bossLevel", resolvedLevel);
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(userIdForBattle)
                                .set(init, SetOptions.merge());
                    }

                    applyBossLevel(resolvedLevel);
                })
                .addOnFailureListener(e -> {
                    if (bossLevel <= 0) {
                        Toast.makeText(this, "Boss level nije ucitan, koristi se nivo 1", Toast.LENGTH_SHORT).show();
                        applyBossLevel(1);
                    }
                });
    }

    private void applyBossLevel(int level) {
        bossLevel = Math.max(1, level);

        // Calculate boss stats
        bossMaxHp = Boss.getBossHpForLevel(bossLevel);
        bossCurrentHp = bossMaxHp;
        bossName = Boss.getBossNameForLevel(bossLevel);

        // Calculate rewards
        coinsReward = Boss.getCoinsRewardForLevel(bossLevel);
        xpReward = coinsReward / 4; // XP is about 25% of coins

        updateUI();
    }
    
    private void setupButtons() {
        buttonAttack.setOnClickListener(v -> {
            if (!battleEnded && playerPp > 0) {
                attack();
            }
        });
        
        buttonContinue.setOnClickListener(v -> {
            finish();
        });
    }
    
    private void updateUI() {
        textBossName.setText(bossName);
        textBossLevel.setText("LVL " + bossLevel);
        textBossHp.setText(bossCurrentHp + " / " + bossMaxHp);
        textPlayerPp.setText(playerPp + " PP");
        
        // Show attack count (max 5 attacks)
        String attackInfo = "Napadi: " + attackCount + "/" + maxAttacks;
        textInstructions.setText(
                attackInfo
                        + " | Šansa za pogodak: " + getHitChancePercent() + "%"
                        + " | Kritičan: " + getCritChancePercent() + "%"
        );
        
        int hpPercentage = (int) ((bossCurrentHp / (float) bossMaxHp) * 100);
        progressBossHp.setProgress(hpPercentage);
        
        // Change attack button state
        if (playerPp <= 0 || attackCount >= maxAttacks) {
            buttonAttack.setEnabled(false);
            buttonAttack.setText(attackCount >= maxAttacks ? "Max napada" : "Nizak PP");
        } else {
            buttonAttack.setEnabled(true);
            buttonAttack.setText("Napad");
        }
    }
    
    private int getHitChancePercent() {
        return Math.max(0, Math.min(100, Math.round(hitChance * 100f)));
    }

    private int getCritChancePercent() {
        return Math.max(0, Math.min(100, Math.round(getCritChance() * 100f)));
    }

    private boolean rollForHit() {
        int roll = random.nextInt(100); // 0-99, per spec
        // hitChance = task success rate for current stage; shield adds flat bonus on top
        float effective = Math.min(1.0f, hitChance + equipmentHitChanceBonus);
        int threshold = Math.max(0, Math.min(100, Math.round(effective * 100f)));
        boolean hit = roll < threshold;
        android.util.Log.d("BattleActivity", "Hit RNG roll=" + roll + ", threshold=" + threshold + ", hit=" + hit);
        return hit;
    }

    private int calculateAttackDamage() {
        int baseDamage = Math.max(1, playerPp);
        int totalDamage = Math.max(1, Math.round(baseDamage * equipmentDamageMultiplier));
        return totalDamage;
    }

    private float getCritChance() {
        float levelBonus = Math.min(0.15f, Math.max(0, userLevel - 1) * 0.01f);
        float total = BASE_CRIT_CHANCE + levelBonus + equipmentCritChanceBonus;
        return Math.max(0f, Math.min(0.95f, total));
    }

    private boolean rollForCritical() {
        int roll = random.nextInt(100);
        int threshold = Math.round(getCritChance() * 100f);
        boolean critical = roll < threshold;
        android.util.Log.d("BattleActivity", "Crit RNG roll=" + roll + ", threshold=" + threshold + ", critical=" + critical);
        return critical;
    }

    private void showAttackFeedback(String message, boolean success) {
        View root = findViewById(android.R.id.content);
        if (root == null) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            return;
        }

        Snackbar snackbar = Snackbar.make(root, message, Snackbar.LENGTH_SHORT);
        snackbar.setAnimationMode(Snackbar.ANIMATION_MODE_FADE);
        snackbar.setTextColor(getColor(R.color.white));
        snackbar.setBackgroundTint(getColor(success ? R.color.status_active : R.color.status_failed));
        snackbar.show();
    }

    private void attack() {
        if (battleEnded || playerPp <= 0 || attackCount >= maxAttacks) return;
        
        attackCount++;
        
        // Play attack animation
        playAttackAnimation();
        
        // Check if attack hits based on success rate
        boolean hit = rollForHit();
        
        if (!hit) {
            // Miss!
            showMissEffect();
            showAttackFeedback("Promašaj! Bos nije pogođen.", false);
            vibrate();
            updateUI();
            
            // Check for defeat after miss
            if (attackCount >= maxAttacks && bossCurrentHp > 0) {
                handler.postDelayed(this::showDefeat, 500);
            }
            return;
        }

        // Special mission rule: successful regular-boss hit contributes to alliance mission.
        if (userIdForBattle != null && !userIdForBattle.trim().isEmpty()) {
            AllianceMissionManager.recordBattleHit(FirebaseFirestore.getInstance(), userIdForBattle, null);
        }
        
        // Per project spec: successful hit removes PP value from boss HP.
        int damage = calculateAttackDamage();

        boolean critical = rollForCritical();
        if (critical) {
            damage = Math.max(1, Math.round(damage * 1.5f));
        }
        
        // Apply damage
        bossCurrentHp = Math.max(0, bossCurrentHp - damage);
        
        // Show damage with animations
        showDamageEffect(damage, critical);
        if (critical) {
            showAttackFeedback("Kritičan pogodak! -" + damage + " HP", true);
        } else {
            showAttackFeedback("Pogodak! -" + damage + " HP", true);
        }
        
        // Vibrate
        vibrate();
        
        // Update UI
        updateUI();
        
        // Check if boss defeated
        if (bossCurrentHp <= 0) {
            handler.postDelayed(this::showVictory, 500);
        } else if (attackCount >= maxAttacks) {
            handler.postDelayed(this::showDefeat, 500);
        }
    }
    
    private void playAttackAnimation() {
        lottieAttack.setVisibility(View.VISIBLE);
        lottieAttack.setProgress(0f);
        lottieAttack.playAnimation();
        lottieAttack.addAnimatorListener(new android.animation.Animator.AnimatorListener() {
            @Override public void onAnimationStart(android.animation.Animator animation) {}
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                lottieAttack.setVisibility(View.GONE);
            }
            @Override public void onAnimationCancel(android.animation.Animator animation) {}
            @Override public void onAnimationRepeat(android.animation.Animator animation) {}
        });
    }
    
    private void playHitAnimation() {
        lottieHit.setVisibility(View.VISIBLE);
        lottieHit.setProgress(0f);
        lottieHit.playAnimation();
        lottieHit.addAnimatorListener(new android.animation.Animator.AnimatorListener() {
            @Override public void onAnimationStart(android.animation.Animator animation) {}
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                lottieHit.setVisibility(View.GONE);
            }
            @Override public void onAnimationCancel(android.animation.Animator animation) {}
            @Override public void onAnimationRepeat(android.animation.Animator animation) {}
        });
    }
    
    private void showMissEffect() {
        textAttackResult.setVisibility(View.VISIBLE);
        textAttackResult.setText("PROMAŠAJ!");
        textAttackResult.setTextColor(getColor(R.color.text_secondary));
        
        textAttackResult.setAlpha(1f);
        textAttackResult.setTranslationY(0f);
        textAttackResult.animate()
                .alpha(0f)
                .translationY(-50f)
                .setDuration(1000)
                .withEndAction(() -> textAttackResult.setVisibility(View.INVISIBLE))
                .start();
    }
    
    private void showDamageEffect(int damage, boolean critical) {
        // Play hit animation
        playHitAnimation();
        
        // Show hit effect
        viewHitEffect.setVisibility(View.VISIBLE);
        viewHitEffect.setAlpha(0.5f);
        viewHitEffect.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> viewHitEffect.setVisibility(View.GONE))
                .start();
        
        // Shake boss image
        ObjectAnimator shakeX = ObjectAnimator.ofFloat(imageBoss, "translationX", 0, 25, -25, 15, -15, 5, -5, 0);
        shakeX.setDuration(300);
        shakeX.start();
        
        // Show damage text
        textAttackResult.setVisibility(View.VISIBLE);
        String damageText = critical ? "💥 KRITIČAN! -" + damage + " HP" : "-" + damage + " HP";
        textAttackResult.setText(damageText);
        textAttackResult.setTextColor(getColor(critical ? R.color.rpg_gold : R.color.rpg_health));
        
        // Animate damage text
        textAttackResult.setAlpha(1f);
        textAttackResult.setTranslationY(0f);
        textAttackResult.animate()
                .alpha(0f)
                .translationY(-50f)
                .setDuration(1000)
                .withEndAction(() -> textAttackResult.setVisibility(View.INVISIBLE))
                .start();
        
        // Pulse glow effect
        ObjectAnimator glowPulse = ObjectAnimator.ofFloat(viewBossGlow, "alpha", 0.5f, 0.8f, 0.5f);
        glowPulse.setDuration(300);
        glowPulse.start();
    }
    
    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }
    
    private void vibrateLong() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            // Vibration pattern for opening chest
            long[] pattern = {0, 100, 50, 100, 50, 200};
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        }
    }
    
    private void showVictory() {
        battleEnded = true;
        isVictory = true;

        finalCoins = coinsReward;
        finalXp = xpReward;
        
        // Show treasure chest first
        layoutResult.setVisibility(View.VISIBLE);
        layoutResult.setAlpha(0f);
        layoutResult.animate().alpha(1f).setDuration(300).start();
        
        layoutTreasureChest.setVisibility(View.VISIBLE);
        cardResult.setVisibility(View.GONE);
        
        // Setup treasure chest - it starts closed, waits for shake
        lottieTreasureChest.setProgress(0f);
        textShakeToOpen.setVisibility(View.VISIBLE);
        
        // Also allow tap to open as fallback
        lottieTreasureChest.setOnClickListener(v -> openTreasureChest());
    }
    
    private void openTreasureChest() {
        if (treasureChestOpened) return;
        treasureChestOpened = true;
        
        // Hide shake text
        textShakeToOpen.setVisibility(View.GONE);
        
        // Play chest opening animation
        lottieTreasureChest.playAnimation();
        
        // Vibrate
        vibrateLong();
        
        // After chest opens, show confetti and rewards
        handler.postDelayed(() -> {
            // Play confetti
            lottieConfetti.setVisibility(View.VISIBLE);
            lottieConfetti.playAnimation();
            
            // Award rewards NOW when chest opens
            awardRewards();
            
            // Transition to results card
            handler.postDelayed(() -> {
                layoutTreasureChest.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(() -> {
                            layoutTreasureChest.setVisibility(View.GONE);
                            showResultsCard();
                        })
                        .start();
            }, 1500);
        }, 1000); // Wait for chest to open
    }
    
    private void awardRewards() {
        if (rewardsAwarded) return;
        rewardsAwarded = true;

        // Decrement durability of active clothing
        decrementClothingDurability();

        // Award XP and coins through ViewModel
        userViewModel.awardBattleRewards(finalXp, finalCoins, (success, xpGained, coinsGained, leveledUp, user) -> {
            runOnUiThread(() -> {
                if (success) {
                    if (isVictory) {
                        progressBossLevel();
                        // 20% chance for equipment drop: 95% clothing, 5% weapon (spec §5)
                        rollForEquipmentDrop();
                    }

                    if (user != null) {
                        maxPlayerPp = Math.max(0, user.getPowerPoints() + equipmentPpBonus);
                        playerPp = maxPlayerPp;
                        updateUI();
                    }

                    Toast.makeText(this, "+" + coinsGained + " novčića, +" + xpGained + " XP", Toast.LENGTH_SHORT).show();

                    if (leveledUp && user != null) {
                        handler.postDelayed(() -> showLevelUpDialog(user.getLevel()), 2000);
                    }
                } else {
                    Toast.makeText(this, "Greška pri dodeli nagrada", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * Decrement battlesRemaining for every active clothing piece.
     * When it reaches 0, deactivate and remove the item (spec §6).
     */
    private void decrementClothingDurability() {
        if (userIdForBattle == null) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userIdForBattle)
                .collection("equipment")
                .whereEqualTo("type", "CLOTHING")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snapshots -> {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Long remaining = doc.getLong("battlesRemaining");
                        int left = remaining != null ? remaining.intValue() : 0;
                        if (left <= 1) {
                            // Last battle — deactivate and delete (spec: removed after 2 battles)
                            doc.getReference().delete();
                        } else {
                            doc.getReference().update("battlesRemaining", left - 1);
                        }
                    }
                });
    }

    /**
     * 20% chance to drop equipment on victory.
     * If drop: 95% chance = clothing (random piece), 5% chance = weapon (spec §5).
     * If weapon already owned, adds 0.02% to its upgrade value instead (spec §6).
     */
    private void rollForEquipmentDrop() {
        if (userIdForBattle == null) return;
        if (random.nextFloat() > 0.20f) return; // 80% no drop

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String[] clothingTypes = {
            com.example.rpghabittracker.data.model.Equipment.CLOTHING_GLOVES,
            com.example.rpghabittracker.data.model.Equipment.CLOTHING_SHIELD,
            com.example.rpghabittracker.data.model.Equipment.CLOTHING_BOOTS
        };
        String[] weaponTypes = {
            com.example.rpghabittracker.data.model.Equipment.WEAPON_SWORD,
            com.example.rpghabittracker.data.model.Equipment.WEAPON_BOW
        };

        boolean isWeaponDrop = random.nextFloat() < 0.05f; // 5% weapon, 95% clothing
        String droppedSubType;
        String droppedName;
        String droppedEffect;
        double droppedBonus;
        String droppedType;

        if (isWeaponDrop) {
            droppedSubType = weaponTypes[random.nextInt(weaponTypes.length)];
            droppedType = com.example.rpghabittracker.data.model.Equipment.TYPE_WEAPON;
            if (com.example.rpghabittracker.data.model.Equipment.WEAPON_SWORD.equals(droppedSubType)) {
                droppedName = "Mač";
                droppedEffect = "ATTACK_POWER";
                droppedBonus = 0.05;
            } else {
                droppedName = "Luk i strela";
                droppedEffect = "COIN_BONUS";
                droppedBonus = 0.05;
            }
        } else {
            droppedSubType = clothingTypes[random.nextInt(clothingTypes.length)];
            droppedType = com.example.rpghabittracker.data.model.Equipment.TYPE_CLOTHING;
            switch (droppedSubType) {
                case com.example.rpghabittracker.data.model.Equipment.CLOTHING_GLOVES:
                    droppedName = "Rukavice"; droppedEffect = "ATTACK_POWER"; droppedBonus = 0.10; break;
                case com.example.rpghabittracker.data.model.Equipment.CLOTHING_SHIELD:
                    droppedName = "Štit"; droppedEffect = "HIT_CHANCE"; droppedBonus = 0.10; break;
                default: // BOOTS
                    droppedName = "Čizme"; droppedEffect = "EXTRA_ATTACK"; droppedBonus = 0.40; break;
            }
        }

        final String finalSubType = droppedSubType;
        final String finalName = droppedName;
        final String finalEffect = droppedEffect;
        final double finalBonus = droppedBonus;
        final String finalType = droppedType;

        // Check if weapon already owned — if so, add 0.02% upgrade bonus (spec §6)
        if (isWeaponDrop) {
            db.collection("users").document(userIdForBattle)
                    .collection("equipment").document(finalSubType)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            // Duplicate weapon → +0.02% to bonus
                            Number existing = (Number) doc.get("bonus");
                            double newBonus = (existing != null ? existing.doubleValue() : finalBonus) + 0.0002;
                            doc.getReference().update("bonus", newBonus);
                            runOnUiThread(() -> Toast.makeText(this,
                                    "Duplikat oružja! +" + finalName + " ojačan za 0.02%", Toast.LENGTH_SHORT).show());
                        } else {
                            saveDroppedEquipment(db, finalSubType, finalName, finalType, finalEffect, finalBonus);
                        }
                    });
        } else {
            // Clothing: always add/stack (same type stacks, spec §6)
            saveDroppedEquipment(db, finalSubType, finalName, finalType, finalEffect, finalBonus);
        }
    }

    private void saveDroppedEquipment(FirebaseFirestore db, String subType, String name,
                                      String type, String effect, double bonus) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("type", type);
        data.put("subType", subType);
        data.put("description", name + " (nagrada od bosa)");
        data.put("quantity", 1);
        data.put("active", false);
        data.put("bonus", bonus);
        data.put("effect", effect);
        data.put("battlesRemaining", com.example.rpghabittracker.data.model.Equipment.TYPE_CLOTHING.equals(type) ? 2 : 0);
        data.put("upgradeLevel", 1);

        // Use a unique doc ID so multiple clothing drops of same type stack as separate entries
        String docId = subType + "_" + System.currentTimeMillis();
        db.collection("users").document(userIdForBattle)
                .collection("equipment").document(docId)
                .set(data)
                .addOnSuccessListener(aVoid -> runOnUiThread(() ->
                        Toast.makeText(this, "🎁 Dobili ste opremu: " + name + "!", Toast.LENGTH_LONG).show()));
    }

    private void progressBossLevel() {
        if (bossLevelProgressed) return;
        bossLevelProgressed = true;

        if (userIdForBattle == null || userIdForBattle.trim().isEmpty()) {
            bossLevelProgressed = false;
            return;
        }

        final int nextBossLevel = bossLevel + 1;
        Map<String, Object> updates = new HashMap<>();
        updates.put("bossLevel", nextBossLevel);
        updates.put("lastBossDefeatedAt", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userIdForBattle)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    bossLevel = nextBossLevel;
                    Toast.makeText(this, "Sledeci boss: LVL " + nextBossLevel, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    bossLevelProgressed = false;
                    Toast.makeText(this, "Boss progress nije sacuvan", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void showResultsCard() {
        textResultTitle.setText("🎉 POBEDA!");
        textResultTitle.setTextColor(getColor(R.color.rpg_gold));
        textResultMessage.setText("Pobedili ste " + bossName + "!");
        textRewardCoins.setText("+" + finalCoins);
        textRewardXp.setText("+" + finalXp);
        
        cardResult.setVisibility(View.VISIBLE);
        cardResult.setAlpha(0f);
        cardResult.animate().alpha(1f).setDuration(300).start();
    }
    
    private void showDefeat() {
        battleEnded = true;
        isVictory = false;

        int hpRemoved = bossMaxHp - bossCurrentHp;
        boolean halfHpReached = hpRemoved >= Math.round(bossMaxHp * 0.5f);
        int consolationCoins = halfHpReached ? Math.round(coinsReward * 0.5f) : 0;
        int consolationXp = halfHpReached ? Math.round(xpReward * 0.5f) : 0;
        finalCoins = consolationCoins;
        finalXp = consolationXp;
        
        textResultTitle.setText("💀 PORAZ");
        textResultTitle.setTextColor(getColor(R.color.rpg_health));
        
        if (attackCount >= maxAttacks) {
            textResultMessage.setText("Iskoristili ste svih " + maxAttacks + " napada.\nBos je preživeo sa " + bossCurrentHp + " HP.");
        } else {
            textResultMessage.setText("Borba je završena.\nBos je preživeo sa " + bossCurrentHp + " HP.");
        }

        if (halfHpReached) {
            textResultMessage.append("\nSkinuli ste 50%+ HP, osvajate polovinu nagrade.");
        } else {
            textResultMessage.append("\nSkinuli ste manje od 50% HP, nema nagrade.");
        }
        
        textRewardCoins.setText("+" + consolationCoins);
        textRewardXp.setText("+" + consolationXp);
        
        // Award consolation rewards
        if (!rewardsAwarded && consolationCoins > 0) {
            rewardsAwarded = true;
            userViewModel.awardBattleRewards(consolationXp, consolationCoins, (success, xpGained, coinsGained, leveledUp, user) -> {
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(this, "+" + coinsGained + " novčića, +" + xpGained + " XP", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }
        
        // Show results directly (no treasure chest for defeat)
        layoutResult.setVisibility(View.VISIBLE);
        layoutResult.setAlpha(0f);
        layoutResult.animate().alpha(1f).setDuration(300).start();
        
        cardResult.setVisibility(View.VISIBLE);
        layoutTreasureChest.setVisibility(View.GONE);
    }
    
    private void showLevelUpDialog(int newLevel) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("🎉 Level Up!")
                .setMessage("Čestitamo! Dostigli ste level " + newLevel + "!")
                .setPositiveButton("Super!", null)
                .show();
    }
    
    @Override
    public void onBackPressed() {
        if (!battleEnded) {
            confirmExit();
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            
            float acceleration = (float) Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;
            
            if (acceleration > SHAKE_THRESHOLD) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastShakeTime > SHAKE_COOLDOWN) {
                    lastShakeTime = currentTime;
                    
                    // Handle different states
                    if (battleEnded && isVictory && !treasureChestOpened) {
                        // Shake to open treasure chest
                        openTreasureChest();
                    } else if (!battleEnded) {
                        // Shake to attack during battle
                        attack();
                    }
                }
            }
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }
}
