package com.example.rpghabittracker.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.rpghabittracker.data.model.Boss;
import com.example.rpghabittracker.data.model.Equipment;
import com.example.rpghabittracker.data.model.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Centralized manager for alliance special mission rules and progress updates.
 */
public final class AllianceMissionManager {

    private AllianceMissionManager() {}

    public interface MissionStartCallback {
        void onResult(boolean success, String message, int bossHp);
    }

    public interface MissionEventCallback {
        void onResult(boolean success, int appliedDamage, String message);
    }

    public interface MissionFinalizeCallback {
        void onResult(boolean finalized, boolean won, String message);
    }

    private static final long TWO_WEEKS_MS = 14L * 24 * 60 * 60 * 1000;

    private static final int MAX_SHOP_PURCHASES = 5;
    private static final int MAX_BATTLE_HITS = 10;
    private static final int MAX_SIMPLE_TASKS = 10;
    private static final int MAX_OTHER_TASKS = 6;

    private static final int DAMAGE_SHOP_PURCHASE = 2;
    private static final int DAMAGE_BATTLE_HIT = 2;
    private static final int DAMAGE_SIMPLE_TASK_UNIT = 1;
    private static final int DAMAGE_OTHER_TASK = 4;
    private static final int DAMAGE_NO_UNRESOLVED_BONUS = 10;
    private static final int DAMAGE_DAILY_MESSAGE = 4;

    public static void startMission(
            @NonNull FirebaseFirestore firestore,
            @NonNull String allianceId,
            @NonNull String leaderId,
            @Nullable MissionStartCallback callback
    ) {
        DocumentReference allianceRef = firestore.collection("alliances").document(allianceId);
        long now = System.currentTimeMillis();

        firestore.runTransaction(transaction -> {
                    DocumentSnapshot allianceDoc = transaction.get(allianceRef);
                    if (!allianceDoc.exists()) {
                        throw new IllegalStateException("Savez ne postoji.");
                    }

                    String dbLeaderId = allianceDoc.getString("leaderId");
                    if (dbLeaderId == null || !dbLeaderId.equals(leaderId)) {
                        throw new IllegalStateException("Samo vođa saveza može pokrenuti misiju.");
                    }

                    boolean missionActive = Boolean.TRUE.equals(allianceDoc.getBoolean("missionActive"));
                    long missionEnd = getMillis(allianceDoc.get("missionEndTime"), 0L);
                    if (missionActive && missionEnd > now) {
                        throw new IllegalStateException("Savez već ima aktivnu specijalnu misiju.");
                    }

                    List<String> memberIds = getStringList(allianceDoc.get("memberIds"));
                    if (memberIds.isEmpty()) {
                        throw new IllegalStateException("Savez nema članove.");
                    }

                    int bossHp = Math.max(100, memberIds.size() * 100);
                    String missionId = UUID.randomUUID().toString();

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("missionActive", true);
                    updates.put("missionId", missionId);
                    updates.put("missionStartTime", now);
                    updates.put("missionEndTime", now + TWO_WEEKS_MS);
                    updates.put("missionBossHp", bossHp);
                    updates.put("missionBossCurrentHp", bossHp);
                    updates.put("missionCurrentDamage", 0);
                    updates.put("missionRewardDistributed", false);
                    updates.put("missionResultProcessed", false);
                    updates.put("missionWon", false);
                    updates.put("status", "IN_MISSION");

                    transaction.update(allianceRef, updates);

                    for (String memberId : memberIds) {
                        DocumentReference progressRef = allianceRef.collection("missionProgress").document(memberId);
                        Map<String, Object> progress = defaultProgress(memberId, missionId, now);
                        transaction.set(progressRef, progress, SetOptions.merge());

                        // Track how many special missions each user has started/participated in.
                        DocumentReference userRef = firestore.collection("users").document(memberId);
                        Map<String, Object> userStartUpdates = new HashMap<>();
                        userStartUpdates.put("specialMissionsStarted", FieldValue.increment(1));
                        userStartUpdates.put("lastUpdated", now);
                        transaction.set(userRef, userStartUpdates, SetOptions.merge());
                    }

                    return bossHp;
                })
                .addOnSuccessListener(bossHp -> {
                    if (callback != null) {
                        callback.onResult(true, "Specijalna misija je pokrenuta.", bossHp);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onResult(false, e.getMessage() != null ? e.getMessage() : "Greška pri pokretanju misije.", 0);
                    }
                });
    }

    public static void recordShopPurchase(
            @NonNull FirebaseFirestore firestore,
            @NonNull String userId,
            @Nullable MissionEventCallback callback
    ) {
        resolveAllianceForUser(firestore, userId, (allianceId, error) -> {
            if (allianceId == null) {
                if (callback != null) callback.onResult(false, 0, error != null ? error : "Korisnik nije u savezu.");
                return;
            }
            applyCounterDamageEvent(
                    firestore,
                    allianceId,
                    userId,
                    "shopPurchasesCount",
                    MAX_SHOP_PURCHASES,
                    1,
                    DAMAGE_SHOP_PURCHASE,
                    callback
            );
        });
    }

    public static void recordBattleHit(
            @NonNull FirebaseFirestore firestore,
            @NonNull String userId,
            @Nullable MissionEventCallback callback
    ) {
        resolveAllianceForUser(firestore, userId, (allianceId, error) -> {
            if (allianceId == null) {
                if (callback != null) callback.onResult(false, 0, error != null ? error : "Korisnik nije u savezu.");
                return;
            }
            applyCounterDamageEvent(
                    firestore,
                    allianceId,
                    userId,
                    "battleHitsCount",
                    MAX_BATTLE_HITS,
                    1,
                    DAMAGE_BATTLE_HIT,
                    callback
            );
        });
    }

    public static void recordTaskCompletion(
            @NonNull FirebaseFirestore firestore,
            @NonNull String userId,
            @NonNull Task task,
            @Nullable MissionEventCallback callback
    ) {
        resolveAllianceForUser(firestore, userId, (allianceId, error) -> {
            if (allianceId == null) {
                if (callback != null) callback.onResult(false, 0, error != null ? error : "Korisnik nije u savezu.");
                return;
            }

            final boolean simpleTask = isSimpleMissionTask(task);
            final int units = simpleTask && isEasyAndNormal(task) ? 2 : 1;

            if (simpleTask) {
                applyCounterDamageEvent(
                        firestore,
                        allianceId,
                        userId,
                        "simpleTasksCount",
                        MAX_SIMPLE_TASKS,
                        units,
                        DAMAGE_SIMPLE_TASK_UNIT,
                        (success, appliedDamage, message) -> {
                            // Bonus check after task completion.
                            tryApplyNoUnresolvedBonus(firestore, allianceId, userId);
                            if (callback != null) callback.onResult(success, appliedDamage, message);
                        }
                );
            } else {
                applyCounterDamageEvent(
                        firestore,
                        allianceId,
                        userId,
                        "otherTasksCount",
                        MAX_OTHER_TASKS,
                        1,
                        DAMAGE_OTHER_TASK,
                        (success, appliedDamage, message) -> {
                            // Bonus check after task completion.
                            tryApplyNoUnresolvedBonus(firestore, allianceId, userId);
                            if (callback != null) callback.onResult(success, appliedDamage, message);
                        }
                );
            }
        });
    }

    public static void recordMessageDay(
            @NonNull FirebaseFirestore firestore,
            @NonNull String allianceId,
            @NonNull String userId,
            @Nullable MissionEventCallback callback
    ) {
        DocumentReference allianceRef = firestore.collection("alliances").document(allianceId);
        String todayKey = dayKeyUtc(System.currentTimeMillis());

        firestore.runTransaction(transaction -> {
                    DocumentSnapshot allianceDoc = transaction.get(allianceRef);
                    if (!allianceDoc.exists()) {
                        return EventResult.error("Savez ne postoji.");
                    }

                    boolean missionActive = Boolean.TRUE.equals(allianceDoc.getBoolean("missionActive"));
                    if (!missionActive) {
                        return EventResult.error("Nema aktivne misije.");
                    }

                    long now = System.currentTimeMillis();
                    long missionEnd = getMillis(allianceDoc.get("missionEndTime"), 0L);
                    if (missionEnd > 0 && now > missionEnd) {
                        return EventResult.expired();
                    }

                    String missionId = allianceDoc.getString("missionId");
                    if (missionId == null) missionId = "";

                    int bossHp = getInt(allianceDoc.get("missionBossHp"), 0);
                    int currentDamage = getInt(allianceDoc.get("missionCurrentDamage"), 0);

                    DocumentReference progressRef = allianceRef.collection("missionProgress").document(userId);
                    DocumentSnapshot progressDoc = transaction.get(progressRef);
                    Map<String, Object> progress = progressDoc.exists()
                            ? new HashMap<>(progressDoc.getData() != null ? progressDoc.getData() : new HashMap<>())
                            : defaultProgress(userId, missionId, now);

                    @SuppressWarnings("unchecked")
                    List<String> days = progress.get("messageDays") instanceof List
                            ? new ArrayList<>((List<String>) progress.get("messageDays"))
                            : new ArrayList<>();

                    if (days.contains(todayKey)) {
                        return EventResult.noop("Dnevni bonus poruke je već obračunat.");
                    }
                    days.add(todayKey);

                    int appliedDamage = Math.min(DAMAGE_DAILY_MESSAGE, Math.max(0, bossHp - currentDamage));
                    int newDamage = currentDamage + appliedDamage;

                    int dealt = getInt(progress.get("damageDealt"), 0) + appliedDamage;
                    progress.put("missionId", missionId);
                    progress.put("userId", userId);
                    progress.put("messageDays", days);
                    progress.put("damageDealt", dealt);
                    progress.put("lastUpdated", now);
                    transaction.set(progressRef, progress, SetOptions.merge());

                    Map<String, Object> allianceUpdates = new HashMap<>();
                    allianceUpdates.put("missionCurrentDamage", newDamage);
                    allianceUpdates.put("missionBossCurrentHp", Math.max(0, bossHp - newDamage));
                    allianceUpdates.put("missionLastUpdated", now);
                    transaction.update(allianceRef, allianceUpdates);

                    return EventResult.success(appliedDamage, "Dnevni chat doprinos je obračunat.");
                })
                .addOnSuccessListener(result -> {
                    if (result.expired) {
                        finalizeMissionIfExpired(firestore, allianceId, null);
                    }
                    if (callback != null) {
                        callback.onResult(result.success, result.appliedDamage, result.message);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onResult(false, 0, e.getMessage() != null ? e.getMessage() : "Greška pri obračunu chat doprinosa.");
                    }
                });
    }

    public static void finalizeMissionIfExpired(
            @NonNull FirebaseFirestore firestore,
            @NonNull String allianceId,
            @Nullable MissionFinalizeCallback callback
    ) {
        DocumentReference allianceRef = firestore.collection("alliances").document(allianceId);
        long now = System.currentTimeMillis();

        firestore.runTransaction(transaction -> {
                    DocumentSnapshot allianceDoc = transaction.get(allianceRef);
                    if (!allianceDoc.exists()) {
                        return FinalizeResult.notFinalized("Savez ne postoji.");
                    }

                    boolean missionActive = Boolean.TRUE.equals(allianceDoc.getBoolean("missionActive"));
                    if (!missionActive) {
                        return FinalizeResult.notFinalized("Nema aktivne misije.");
                    }

                    long missionEnd = getMillis(allianceDoc.get("missionEndTime"), 0L);
                    if (missionEnd <= 0 || now < missionEnd) {
                        return FinalizeResult.notFinalized("Misija još traje.");
                    }

                    int bossHp = getInt(allianceDoc.get("missionBossHp"), 0);
                    int currentDamage = getInt(allianceDoc.get("missionCurrentDamage"), 0);
                    boolean won = bossHp > 0 && currentDamage >= bossHp;
                    boolean rewardDistributed = Boolean.TRUE.equals(allianceDoc.getBoolean("missionRewardDistributed"));
                    boolean shouldReward = won && !rewardDistributed;

                    List<String> memberIds = getStringList(allianceDoc.get("memberIds"));
                    String missionId = allianceDoc.getString("missionId");
                    if (missionId == null) missionId = "";

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("missionActive", false);
                    updates.put("missionResultProcessed", true);
                    updates.put("missionWon", won);
                    updates.put("missionFinishedTime", now);
                    updates.put("status", "ACTIVE");
                    if (shouldReward) {
                        updates.put("missionRewardDistributed", true);
                    }
                    transaction.update(allianceRef, updates);

                    return FinalizeResult.finalized(won, shouldReward, memberIds, missionId);
                })
                .addOnSuccessListener(result -> {
                    if (!result.finalized) {
                        if (callback != null) callback.onResult(false, false, result.message);
                        return;
                    }

                    if (result.shouldReward) {
                        distributeMissionRewards(firestore, result.memberIds, () -> {
                            if (callback != null) {
                                callback.onResult(true, true, "Misija je završena uspešno. Nagrade su dodeljene.");
                            }
                        });
                    } else {
                        if (callback != null) {
                            String msg = result.won
                                    ? "Misija je već obrađena."
                                    : "Misija je istekla bez poraza bosa.";
                            callback.onResult(true, result.won, msg);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onResult(false, false, e.getMessage() != null ? e.getMessage() : "Greška pri završetku misije.");
                    }
                });
    }

    private static void applyCounterDamageEvent(
            @NonNull FirebaseFirestore firestore,
            @NonNull String allianceId,
            @NonNull String userId,
            @NonNull String counterField,
            int counterMax,
            int requestedUnits,
            int damagePerUnit,
            @Nullable MissionEventCallback callback
    ) {
        DocumentReference allianceRef = firestore.collection("alliances").document(allianceId);

        firestore.runTransaction(transaction -> {
                    DocumentSnapshot allianceDoc = transaction.get(allianceRef);
                    if (!allianceDoc.exists()) {
                        return EventResult.error("Savez ne postoji.");
                    }

                    boolean missionActive = Boolean.TRUE.equals(allianceDoc.getBoolean("missionActive"));
                    if (!missionActive) {
                        return EventResult.error("Nema aktivne misije.");
                    }

                    long now = System.currentTimeMillis();
                    long missionEnd = getMillis(allianceDoc.get("missionEndTime"), 0L);
                    if (missionEnd > 0 && now > missionEnd) {
                        return EventResult.expired();
                    }

                    String missionId = allianceDoc.getString("missionId");
                    if (missionId == null) missionId = "";

                    int bossHp = getInt(allianceDoc.get("missionBossHp"), 0);
                    int currentDamage = getInt(allianceDoc.get("missionCurrentDamage"), 0);

                    DocumentReference progressRef = allianceRef.collection("missionProgress").document(userId);
                    DocumentSnapshot progressDoc = transaction.get(progressRef);
                    Map<String, Object> progress = progressDoc.exists()
                            ? new HashMap<>(progressDoc.getData() != null ? progressDoc.getData() : new HashMap<>())
                            : defaultProgress(userId, missionId, now);

                    int currentCount = getInt(progress.get(counterField), 0);
                    int applicableUnits = Math.min(requestedUnits, Math.max(0, counterMax - currentCount));
                    if (applicableUnits <= 0) {
                        return EventResult.noop("Limit za ovu akciju je već dostignut.");
                    }

                    int potentialDamage = applicableUnits * damagePerUnit;
                    int appliedDamage = Math.min(potentialDamage, Math.max(0, bossHp - currentDamage));
                    int newDamage = currentDamage + appliedDamage;

                    progress.put("missionId", missionId);
                    progress.put("userId", userId);
                    progress.put(counterField, currentCount + applicableUnits);
                    progress.put("damageDealt", getInt(progress.get("damageDealt"), 0) + appliedDamage);
                    progress.put("lastUpdated", now);
                    transaction.set(progressRef, progress, SetOptions.merge());

                    Map<String, Object> allianceUpdates = new HashMap<>();
                    allianceUpdates.put("missionCurrentDamage", newDamage);
                    allianceUpdates.put("missionBossCurrentHp", Math.max(0, bossHp - newDamage));
                    allianceUpdates.put("missionLastUpdated", now);
                    transaction.update(allianceRef, allianceUpdates);

                    return EventResult.success(appliedDamage, "Napredak misije je ažuriran.");
                })
                .addOnSuccessListener(result -> {
                    if (result.expired) {
                        finalizeMissionIfExpired(firestore, allianceId, null);
                    }
                    if (callback != null) {
                        callback.onResult(result.success, result.appliedDamage, result.message);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onResult(false, 0, e.getMessage() != null ? e.getMessage() : "Greška pri ažuriranju misije.");
                    }
                });
    }

    private static void tryApplyNoUnresolvedBonus(
            @NonNull FirebaseFirestore firestore,
            @NonNull String allianceId,
            @NonNull String userId
    ) {
        DocumentReference allianceRef = firestore.collection("alliances").document(allianceId);
        allianceRef.get().addOnSuccessListener(allianceDoc -> {
            if (!allianceDoc.exists()) return;
            if (!Boolean.TRUE.equals(allianceDoc.getBoolean("missionActive"))) return;

            long now = System.currentTimeMillis();
            long missionEnd = getMillis(allianceDoc.get("missionEndTime"), 0L);
            if (missionEnd > 0 && now > missionEnd) {
                finalizeMissionIfExpired(firestore, allianceId, null);
                return;
            }

            long missionStart = getMillis(allianceDoc.get("missionStartTime"), 0L);
            firestore.collection("tasks")
                    .whereEqualTo("userId", userId)
                    .whereIn("status", java.util.Arrays.asList(Task.STATUS_ACTIVE, Task.STATUS_FAILED))
                    .get()
                    .addOnSuccessListener(tasks -> {
                        boolean hasUnresolved = false;
                        for (DocumentSnapshot taskDoc : tasks.getDocuments()) {
                            long createdAt = getMillis(taskDoc.get("createdAt"), 0L);
                            if (createdAt >= missionStart) {
                                hasUnresolved = true;
                                break;
                            }
                        }
                        if (!hasUnresolved) {
                            applyNoUnresolvedBonusTransaction(firestore, allianceId, userId);
                        }
                    });
        });
    }

    private static void applyNoUnresolvedBonusTransaction(
            @NonNull FirebaseFirestore firestore,
            @NonNull String allianceId,
            @NonNull String userId
    ) {
        DocumentReference allianceRef = firestore.collection("alliances").document(allianceId);

        firestore.runTransaction(transaction -> {
            DocumentSnapshot allianceDoc = transaction.get(allianceRef);
            if (!allianceDoc.exists()) return EventResult.noop("Savez ne postoji.");
            if (!Boolean.TRUE.equals(allianceDoc.getBoolean("missionActive"))) return EventResult.noop("Nema aktivne misije.");

            long now = System.currentTimeMillis();
            long missionEnd = getMillis(allianceDoc.get("missionEndTime"), 0L);
            if (missionEnd > 0 && now > missionEnd) return EventResult.expired();

            String missionId = allianceDoc.getString("missionId");
            if (missionId == null) missionId = "";
            int bossHp = getInt(allianceDoc.get("missionBossHp"), 0);
            int currentDamage = getInt(allianceDoc.get("missionCurrentDamage"), 0);

            DocumentReference progressRef = allianceRef.collection("missionProgress").document(userId);
            DocumentSnapshot progressDoc = transaction.get(progressRef);
            Map<String, Object> progress = progressDoc.exists()
                    ? new HashMap<>(progressDoc.getData() != null ? progressDoc.getData() : new HashMap<>())
                    : defaultProgress(userId, missionId, now);

            if (Boolean.TRUE.equals(progress.get("noUnresolvedAwarded"))) {
                return EventResult.noop("Bonus je već dodeljen.");
            }

            int appliedDamage = Math.min(DAMAGE_NO_UNRESOLVED_BONUS, Math.max(0, bossHp - currentDamage));
            int newDamage = currentDamage + appliedDamage;

            progress.put("missionId", missionId);
            progress.put("userId", userId);
            progress.put("noUnresolvedAwarded", true);
            progress.put("damageDealt", getInt(progress.get("damageDealt"), 0) + appliedDamage);
            progress.put("lastUpdated", now);
            transaction.set(progressRef, progress, SetOptions.merge());

            Map<String, Object> allianceUpdates = new HashMap<>();
            allianceUpdates.put("missionCurrentDamage", newDamage);
            allianceUpdates.put("missionBossCurrentHp", Math.max(0, bossHp - newDamage));
            allianceUpdates.put("missionLastUpdated", now);
            transaction.update(allianceRef, allianceUpdates);

            return EventResult.success(appliedDamage, "Bonus za nerešene zadatke je dodeljen.");
        }).addOnSuccessListener(result -> {
            if (result.expired) {
                finalizeMissionIfExpired(firestore, allianceId, null);
            }
        });
    }

    private static void distributeMissionRewards(
            @NonNull FirebaseFirestore firestore,
            @NonNull List<String> memberIds,
            @Nullable Runnable onDone
    ) {
        if (memberIds.isEmpty()) {
            if (onDone != null) onDone.run();
            return;
        }

        AtomicInteger pending = new AtomicInteger(memberIds.size());
        for (String memberId : memberIds) {
            rewardMember(firestore, memberId, () -> {
                if (pending.decrementAndGet() == 0 && onDone != null) {
                    onDone.run();
                }
            });
        }
    }

    private static void rewardMember(
            @NonNull FirebaseFirestore firestore,
            @NonNull String userId,
            @NonNull Runnable onDone
    ) {
        DocumentReference userRef = firestore.collection("users").document(userId);

        firestore.runTransaction(transaction -> {
                    DocumentSnapshot userDoc = transaction.get(userRef);
                    if (!userDoc.exists()) return false;

                    int coins = getInt(userDoc.get("coins"), 0);
                    int bossLevel = Math.max(1, getInt(userDoc.get("bossLevel"), 1));
                    int nextBossReward = Boss.getCoinsRewardForLevel(bossLevel + 1);
                    int bonusCoins = Math.max(1, Math.round(nextBossReward * 0.5f));

                    int specialMissions = getInt(userDoc.get("specialMissionsCompleted"), 0) + 1;
                    List<String> badges = getStringList(userDoc.get("badges"));
                    if (specialMissions >= 1) addBadgeIfMissing(badges, "special_mission_1");
                    if (specialMissions >= 3) addBadgeIfMissing(badges, "special_mission_3");
                    if (specialMissions >= 5) addBadgeIfMissing(badges, "special_mission_5");

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("coins", coins + bonusCoins);
                    updates.put("specialMissionsCompleted", specialMissions);
                    updates.put("badges", badges);
                    updates.put("lastUpdated", System.currentTimeMillis());
                    transaction.update(userRef, updates);
                    return true;
                })
                .addOnSuccessListener(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        grantRandomPotion(firestore, userId);
                        grantRandomClothing(firestore, userId);
                    }
                    onDone.run();
                })
                .addOnFailureListener(e -> onDone.run());
    }

    private static void grantRandomPotion(@NonNull FirebaseFirestore firestore, @NonNull String userId) {
        String[] subTypes = new String[] {
                Equipment.POTION_PP_20,
                Equipment.POTION_PP_40,
                Equipment.POTION_PP_5_PERM,
                Equipment.POTION_PP_10_PERM
        };
        String subType = subTypes[new Random().nextInt(subTypes.length)];

        String name;
        String description;
        String effect;
        double bonus;

        switch (subType) {
            case Equipment.POTION_PP_20:
                name = "Napitak snage (jednokratni)";
                description = "Povećava PP za 20% u jednoj borbi";
                effect = "BOOST_PP_SINGLE";
                bonus = 0.20;
                break;
            case Equipment.POTION_PP_40:
                name = "Napitak moći (jednokratni)";
                description = "Povećava PP za 40% u jednoj borbi";
                effect = "BOOST_PP_SINGLE";
                bonus = 0.40;
                break;
            case Equipment.POTION_PP_10_PERM:
                name = "Napitak moći (trajni)";
                description = "Trajno povećava PP za 10%";
                effect = "BOOST_PP_PERMANENT";
                bonus = 0.10;
                break;
            default:
                name = "Napitak snage (trajni)";
                description = "Trajno povećava PP za 5%";
                effect = "BOOST_PP_PERMANENT";
                bonus = 0.05;
                break;
        }

        DocumentReference equipmentRef = firestore.collection("users")
                .document(userId)
                .collection("equipment")
                .document(subType);

        equipmentRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                equipmentRef.update("quantity", com.google.firebase.firestore.FieldValue.increment(1));
            } else {
                Map<String, Object> equipment = new HashMap<>();
                equipment.put("name", name);
                equipment.put("type", Equipment.TYPE_POTION);
                equipment.put("subType", subType);
                equipment.put("description", description);
                equipment.put("quantity", 1);
                equipment.put("active", false);
                equipment.put("bonus", bonus);
                equipment.put("effect", effect);
                equipment.put("battlesRemaining", 0);
                equipment.put("upgradeLevel", 1);
                equipment.put("createdAt", System.currentTimeMillis());
                equipmentRef.set(equipment);
            }
        });
    }

    private static void grantRandomClothing(@NonNull FirebaseFirestore firestore, @NonNull String userId) {
        String[] subTypes = new String[] {
                Equipment.CLOTHING_GLOVES,
                Equipment.CLOTHING_SHIELD,
                Equipment.CLOTHING_BOOTS
        };
        String subType = subTypes[new Random().nextInt(subTypes.length)];

        String name;
        String description;
        String effect;
        double bonus;

        switch (subType) {
            case Equipment.CLOTHING_SHIELD:
                name = "Štit";
                description = "+10% šanse za uspešan napad, traje 2 borbe";
                effect = "HIT_CHANCE";
                bonus = 0.10;
                break;
            case Equipment.CLOTHING_BOOTS:
                name = "Čizme";
                description = "40% šansa za jedan dodatni napad, traju 2 borbe";
                effect = "EXTRA_ATTACK";
                bonus = 0.40;
                break;
            default:
                name = "Rukavice";
                description = "+10% snage napada, traju 2 borbe";
                effect = "ATTACK_POWER";
                bonus = 0.10;
                break;
        }

        String docId = "mission_" + subType + "_" + System.currentTimeMillis() + "_" + Math.abs(new Random().nextInt());
        Map<String, Object> equipment = new HashMap<>();
        equipment.put("name", name);
        equipment.put("type", Equipment.TYPE_CLOTHING);
        equipment.put("subType", subType);
        equipment.put("description", description);
        equipment.put("quantity", 1);
        equipment.put("active", false);
        equipment.put("bonus", bonus);
        equipment.put("effect", effect);
        equipment.put("battlesRemaining", 2);
        equipment.put("upgradeLevel", 1);
        equipment.put("createdAt", System.currentTimeMillis());

        firestore.collection("users")
                .document(userId)
                .collection("equipment")
                .document(docId)
                .set(equipment);
    }

    private static boolean isSimpleMissionTask(@NonNull Task task) {
        String difficulty = task.getDifficulty();
        String importance = task.getImportance();
        return Task.DIFFICULTY_VERY_EASY.equals(difficulty)
                || Task.DIFFICULTY_EASY.equals(difficulty)
                || Task.IMPORTANCE_NORMAL.equals(importance)
                || Task.IMPORTANCE_IMPORTANT.equals(importance);
    }

    private static boolean isEasyAndNormal(@NonNull Task task) {
        return Task.DIFFICULTY_EASY.equals(task.getDifficulty())
                && Task.IMPORTANCE_NORMAL.equals(task.getImportance());
    }

    private static Map<String, Object> defaultProgress(String userId, String missionId, long now) {
        Map<String, Object> progress = new HashMap<>();
        progress.put("userId", userId);
        progress.put("missionId", missionId);
        progress.put("damageDealt", 0);
        progress.put("shopPurchasesCount", 0);
        progress.put("battleHitsCount", 0);
        progress.put("simpleTasksCount", 0);
        progress.put("otherTasksCount", 0);
        progress.put("noUnresolvedAwarded", false);
        progress.put("messageDays", new ArrayList<String>());
        progress.put("createdAt", now);
        progress.put("lastUpdated", now);
        return progress;
    }

    private static int getInt(@Nullable Object value, int fallback) {
        if (value instanceof Number) return ((Number) value).intValue();
        return fallback;
    }

    private static long getMillis(@Nullable Object value, long fallback) {
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof Timestamp) return ((Timestamp) value).toDate().getTime();
        if (value instanceof Date) return ((Date) value).getTime();
        return fallback;
    }

    @NonNull
    private static List<String> getStringList(@Nullable Object value) {
        if (!(value instanceof List)) return new ArrayList<>();
        List<?> raw = (List<?>) value;
        List<String> out = new ArrayList<>();
        for (Object item : raw) {
            if (item instanceof String) out.add((String) item);
        }
        return out;
    }

    @NonNull
    private static String dayKeyUtc(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(millis));
    }

    private static void addBadgeIfMissing(@NonNull List<String> badges, @NonNull String badgeId) {
        if (!badges.contains(badgeId)) badges.add(badgeId);
    }

    private interface AllianceResolverCallback {
        void onResolved(@Nullable String allianceId, @Nullable String error);
    }

    private static void resolveAllianceForUser(
            @NonNull FirebaseFirestore firestore,
            @NonNull String userId,
            @NonNull AllianceResolverCallback callback
    ) {
        firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) {
                        callback.onResolved(null, "Korisnik ne postoji.");
                        return;
                    }
                    String allianceId = userDoc.getString("allianceId");
                    if (allianceId == null || allianceId.trim().isEmpty()) {
                        callback.onResolved(null, "Korisnik nije član saveza.");
                        return;
                    }
                    callback.onResolved(allianceId, null);
                })
                .addOnFailureListener(e -> callback.onResolved(null, e.getMessage()));
    }

    private static class EventResult {
        final boolean success;
        final int appliedDamage;
        final String message;
        final boolean expired;

        private EventResult(boolean success, int appliedDamage, String message, boolean expired) {
            this.success = success;
            this.appliedDamage = appliedDamage;
            this.message = message;
            this.expired = expired;
        }

        static EventResult success(int appliedDamage, String message) {
            return new EventResult(true, appliedDamage, message, false);
        }

        static EventResult noop(String message) {
            return new EventResult(true, 0, message, false);
        }

        static EventResult error(String message) {
            return new EventResult(false, 0, message, false);
        }

        static EventResult expired() {
            return new EventResult(true, 0, "Misija je istekla.", true);
        }
    }

    private static class FinalizeResult {
        final boolean finalized;
        final boolean won;
        final boolean shouldReward;
        final List<String> memberIds;
        final String message;

        private FinalizeResult(
                boolean finalized,
                boolean won,
                boolean shouldReward,
                List<String> memberIds,
                String message
        ) {
            this.finalized = finalized;
            this.won = won;
            this.shouldReward = shouldReward;
            this.memberIds = memberIds;
            this.message = message;
        }

        static FinalizeResult finalized(boolean won, boolean shouldReward, List<String> memberIds, String missionId) {
            return new FinalizeResult(true, won, shouldReward, memberIds, missionId);
        }

        static FinalizeResult notFinalized(String message) {
            return new FinalizeResult(false, false, false, new ArrayList<>(), message);
        }
    }
}
