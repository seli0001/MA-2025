package com.example.rpghabittracker.ui.equipment;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpghabittracker.R;
import com.example.rpghabittracker.data.model.Equipment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Activity for viewing and managing user's equipment inventory
 */
public class EquipmentActivity extends AppCompatActivity implements EquipmentAdapter.OnEquipmentListener {

    private ChipGroup chipGroupFilter;
    private RecyclerView recyclerEquipment;
    private LinearLayout layoutEmpty;
    private TextView textActiveBonus;

    private FirebaseFirestore firestore;
    private String currentUserId;
    private EquipmentAdapter adapter;
    private List<EquipmentItem> allItems = new ArrayList<>();
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equipment);

        firestore = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        currentUserId = user.getUid();

        initViews();
        setupToolbar();
        setupFilters();
        setupAdapter();
        loadEquipment();
    }

    private void initViews() {
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        recyclerEquipment = findViewById(R.id.recyclerEquipment);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        textActiveBonus = findViewById(R.id.textActiveBonus);

        recyclerEquipment.setLayoutManager(new GridLayoutManager(this, 2));
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupFilters() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            
            Chip chip = findViewById(checkedIds.get(0));
            if (chip != null) {
                String tag = (String) chip.getTag();
                currentFilter = tag != null ? tag : "all";
                filterItems();
            }
        });
    }

    private void setupAdapter() {
        adapter = new EquipmentAdapter(this);
        recyclerEquipment.setAdapter(adapter);
    }

    private void loadEquipment() {
        firestore.collection("users").document(currentUserId)
                .collection("equipment")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allItems.clear();
                    
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        EquipmentItem item = new EquipmentItem();
                        item.id = doc.getId();
                        item.name = doc.getString("name");
                        item.type = doc.getString("type");
                        item.description = doc.getString("description");
                        item.icon = doc.getString("icon");
                        Long quantity = doc.getLong("quantity");
                        item.quantity = quantity != null ? quantity.intValue() : 1;
                        Boolean active = doc.getBoolean("active");
                        item.active = active != null && active;
                        Long battleRemaining = doc.getLong("battlesRemaining");
                        item.battlesRemaining = battleRemaining != null ? battleRemaining.intValue() : 0;
                        Number bonus = (Number) doc.get("bonus");
                        item.bonus = bonus != null ? (int) Math.round(bonus.doubleValue() * 100) : 0;
                        Long upgradeLevel = doc.getLong("upgradeLevel");
                        item.upgradeLevel = upgradeLevel != null ? upgradeLevel.intValue() : 1;

                        allItems.add(item);
                    }
                    
                    filterItems();
                    updateActiveBonuses();
                });
    }

    private void filterItems() {
        List<EquipmentItem> filtered;
        
        if ("all".equals(currentFilter)) {
            filtered = new ArrayList<>(allItems);
        } else {
            filtered = new ArrayList<>();
            for (EquipmentItem item : allItems) {
                if (currentFilter.equals(item.type)) {
                    filtered.add(item);
                }
            }
        }

        if (filtered.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerEquipment.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerEquipment.setVisibility(View.VISIBLE);
            adapter.setItems(filtered);
        }
    }

    private void updateActiveBonuses() {
        int totalPpBonus = 0;
        StringBuilder bonusText = new StringBuilder();
        
        for (EquipmentItem item : allItems) {
            if (item.active && item.bonus > 0) {
                totalPpBonus += item.bonus;
                if (bonusText.length() > 0) bonusText.append(", ");
                bonusText.append(item.name).append(" (+").append(item.bonus).append(" PP)");
            }
        }
        
        if (totalPpBonus > 0) {
            textActiveBonus.setText("Aktivni bonus: +" + totalPpBonus + " PP");
            textActiveBonus.setVisibility(View.VISIBLE);
        } else {
            textActiveBonus.setVisibility(View.GONE);
        }
    }

    @Override
    public void onItemClick(EquipmentItem item) {
        showItemDetails(item);
    }

    @Override
    public void onActivate(EquipmentItem item) {
        if ("potion".equals(item.type)) {
            usePotion(item);
        } else {
            activateEquipment(item);
        }
    }

    private void showItemDetails(EquipmentItem item) {
        StringBuilder message = new StringBuilder(item.description != null ? item.description : "");
        message.append("\n\nBonus: ").append(String.format("%.2f%%", item.bonus * 100.0 / 100.0));

        if (Equipment.TYPE_CLOTHING.equalsIgnoreCase(item.type) && item.battlesRemaining > 0) {
            message.append("\nPreostalo borbi: ").append(item.battlesRemaining);
        }
        if (Equipment.TYPE_POTION.equalsIgnoreCase(item.type)) {
            message.append("\nKoličina: ").append(item.quantity);
        }
        if (Equipment.TYPE_WEAPON.equalsIgnoreCase(item.type)) {
            message.append("\nNivo unapređenja: ").append(item.upgradeLevel);
        }

        androidx.appcompat.app.AlertDialog.Builder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(item.name)
                .setMessage(message.toString())
                .setPositiveButton("OK", null);

        // Weapon upgrade button (spec §6: costs 60% of previous boss reward, +0.01% bonus)
        if (Equipment.TYPE_WEAPON.equalsIgnoreCase(item.type)) {
            builder.setNeutralButton("Unapredi", (d, w) -> upgradeWeapon(item));
        }

        builder.show();
    }

    private void usePotion(EquipmentItem item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Koristi napitak")
                .setMessage("Da li želite da koristite " + item.name + "?")
                .setPositiveButton("Da", (dialog, which) -> {
                    // Apply potion effect to user
                    if (item.bonus > 0) {
                        // Add PP to user
                        firestore.collection("users").document(currentUserId)
                                .get()
                                .addOnSuccessListener(doc -> {
                                    Long currentPp = doc.getLong("powerPoints");
                                    int newPp = (currentPp != null ? currentPp.intValue() : 0) + item.bonus;
                                    
                                    firestore.collection("users").document(currentUserId)
                                            .update("powerPoints", newPp)
                                            .addOnSuccessListener(aVoid -> {
                                                // Decrease quantity or delete item
                                                if (item.quantity > 1) {
                                                    firestore.collection("users").document(currentUserId)
                                                            .collection("equipment").document(item.id)
                                                            .update("quantity", item.quantity - 1);
                                                } else {
                                                    firestore.collection("users").document(currentUserId)
                                                            .collection("equipment").document(item.id)
                                                            .delete();
                                                }
                                                
                                                Toast.makeText(this, "Dobili ste +" + item.bonus + " PP!", Toast.LENGTH_SHORT).show();
                                                loadEquipment();
                                            });
                                });
                    }
                })
                .setNegativeButton("Ne", null)
                .show();
    }

    private void activateEquipment(EquipmentItem item) {
        if (item.active) {
            // Deactivate
            firestore.collection("users").document(currentUserId)
                    .collection("equipment").document(item.id)
                    .update("active", false)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, item.name + " deaktivirano", Toast.LENGTH_SHORT).show();
                        loadEquipment();
                    });
        } else {
            // Activate
            int battlesRemaining = "clothing".equals(item.type) ? 2 : 0;
            
            firestore.collection("users").document(currentUserId)
                    .collection("equipment").document(item.id)
                    .update("active", true, "battlesRemaining", battlesRemaining)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, item.name + " aktivirano!", Toast.LENGTH_SHORT).show();
                        loadEquipment();
                    });
        }
    }

    /**
     * Upgrade a weapon: costs 60% of previous boss reward, adds +0.01% to bonus (spec §6).
     * Boss reward level N: 200 * 1.2^(N-1). Previous level = user's current level - 1.
     */
    private void upgradeWeapon(EquipmentItem item) {
        // Load user level to calculate upgrade cost
        firestore.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    Long levelLong = doc.getLong("level");
                    int userLevel = levelLong != null ? levelLong.intValue() : 1;
                    int referenceLevel = Math.max(1, userLevel - 1);
                    int bossReward = 200;
                    for (int i = 2; i <= referenceLevel; i++) {
                        bossReward = (int) Math.round(bossReward * 1.2);
                    }
                    int upgradeCost = (int) (bossReward * 0.6);

                    Long coinsLong = doc.getLong("coins");
                    int userCoins = coinsLong != null ? coinsLong.intValue() : 0;

                    if (userCoins < upgradeCost) {
                        Toast.makeText(this,
                                "Nemate dovoljno novčića! Potrebno: " + upgradeCost, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Unapredi " + item.name)
                            .setMessage("Cena unapređenja: " + upgradeCost + " novčića.\n+0.01% snage.\n\nNastaviti?")
                            .setPositiveButton("Unapredi", (d, w) -> {
                                // Deduct coins and increase weapon bonus by 0.0001 (= 0.01%)
                                Number existingBonus = null;
                                // Re-read to get latest bonus value
                                firestore.collection("users").document(currentUserId)
                                        .collection("equipment").document(item.id)
                                        .get()
                                        .addOnSuccessListener(eqDoc -> {
                                            Number eb = (Number) eqDoc.get("bonus");
                                            double currentBonus = eb != null ? eb.doubleValue() : 0.05;
                                            double newBonus = currentBonus + 0.0001; // +0.01%
                                            int newUpgradeLevel = item.upgradeLevel + 1;
                                            int newCoins = userCoins - upgradeCost;

                                            // Update weapon
                                            eqDoc.getReference().update(
                                                    "bonus", newBonus,
                                                    "upgradeLevel", newUpgradeLevel);

                                            // Deduct coins
                                            firestore.collection("users").document(currentUserId)
                                                    .update("coins", newCoins)
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(this, item.name + " unapređen!",
                                                                Toast.LENGTH_SHORT).show();
                                                        loadEquipment();
                                                    });
                                        });
                            })
                            .setNegativeButton("Otkaži", null)
                            .show();
                });
    }

    // Equipment item model
    public static class EquipmentItem {
        public String id;
        public String name;
        public String type;
        public String description;
        public String icon;
        public int quantity;
        public boolean active;
        public int battlesRemaining;
        public int bonus;
        public int upgradeLevel = 1;
    }
}
