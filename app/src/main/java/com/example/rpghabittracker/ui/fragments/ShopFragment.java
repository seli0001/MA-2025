package com.example.rpghabittracker.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpghabittracker.R;
import com.example.rpghabittracker.data.model.Equipment;
import com.example.rpghabittracker.ui.adapters.ShopItemAdapter;
import com.example.rpghabittracker.ui.viewmodel.UserViewModel;
import com.example.rpghabittracker.utils.AllianceMissionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment for the in-game shop
 */
public class ShopFragment extends Fragment implements ShopItemAdapter.OnItemClickListener {
    
    private TextView textCoins;
    private TabLayout tabLayout;
    private RecyclerView recyclerPotions, recyclerClothing, recyclerWeapons;
    private View titlePotions, titleClothing, titleWeapons;
    
    private ShopItemAdapter potionsAdapter;
    private ShopItemAdapter clothingAdapter;
    private ShopItemAdapter weaponsAdapter;
    
    private UserViewModel userViewModel;
    private int userCoins = 0;
    private int userLevel = 1;
    private String userId;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shop, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupViewModel();
        setupTabs();
        setupAdapters();
        loadShopItems();
    }
    
    private void initViews(View view) {
        textCoins = view.findViewById(R.id.textCoins);
        tabLayout = view.findViewById(R.id.tabLayout);
        recyclerPotions = view.findViewById(R.id.recyclerPotions);
        recyclerClothing = view.findViewById(R.id.recyclerClothing);
        recyclerWeapons = view.findViewById(R.id.recyclerWeapons);
        
        // Setup RecyclerViews with linear layout
        recyclerPotions.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerClothing.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerWeapons.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // Disable nested scrolling for smooth behavior
        recyclerPotions.setNestedScrollingEnabled(false);
        recyclerClothing.setNestedScrollingEnabled(false);
        recyclerWeapons.setNestedScrollingEnabled(false);
    }
    
    private void setupViewModel() {
        // Use requireActivity() to share ViewModel with MainActivity
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            userViewModel.setUserId(userId);

            // Observe user data from ViewModel (Room)
            userViewModel.getCurrentUser().observe(getViewLifecycleOwner(), userData -> {
                if (userData != null) {
                    userCoins = userData.getCoins();
                    int newLevel = userData.getLevel();
                    boolean levelChanged = newLevel != userLevel;
                    userLevel = newLevel;
                    textCoins.setText(String.valueOf(userCoins));

                    // Update adapters with new coin count
                    if (potionsAdapter != null) potionsAdapter.setUserCoins(userCoins);
                    if (clothingAdapter != null) clothingAdapter.setUserCoins(userCoins);

                    // Reload shop prices whenever level changes (prices depend on level)
                    if (levelChanged) loadShopItems();
                }
            });
            
            // Also load directly from Firestore as backup
            loadCoinsFromFirestore();
        }
    }
    
    private void loadCoinsFromFirestore() {
        if (userId == null) return;
        
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .addSnapshotListener((doc, error) -> {
                    if (error != null || doc == null || !doc.exists()) return;
                    
                    Long coins = doc.getLong("coins");
                    Long level = doc.getLong("level");
                    
                    if (coins != null) {
                        userCoins = coins.intValue();
                        textCoins.setText(String.valueOf(userCoins));
                        
                        if (potionsAdapter != null) potionsAdapter.setUserCoins(userCoins);
                        if (clothingAdapter != null) clothingAdapter.setUserCoins(userCoins);
                        if (weaponsAdapter != null) weaponsAdapter.setUserCoins(userCoins);
                    }
                    if (level != null && level.intValue() != userLevel) {
                        userLevel = level.intValue();
                        loadShopItems();
                    }
                });
    }
    
    private void setupAdapters() {
        potionsAdapter = new ShopItemAdapter(this);
        clothingAdapter = new ShopItemAdapter(this);

        recyclerPotions.setAdapter(potionsAdapter);
        recyclerClothing.setAdapter(clothingAdapter);

        // Weapons are NOT available in the shop (boss-drop only per spec §6)
        recyclerWeapons.setVisibility(View.GONE);
    }
    
    private void setupTabs() {
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("Sve"));
        tabLayout.addTab(tabLayout.newTab().setText("Napici"));
        tabLayout.addTab(tabLayout.newTab().setText("Odeća"));
        // No weapons tab — weapons can only be obtained from boss battles (spec §6)

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterByCategory(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void filterByCategory(int position) {
        switch (position) {
            case 0: // Sve
                showSection(recyclerPotions, true);
                showSection(recyclerClothing, true);
                break;
            case 1: // Napici
                showSection(recyclerPotions, true);
                showSection(recyclerClothing, false);
                break;
            case 2: // Odeća
                showSection(recyclerPotions, false);
                showSection(recyclerClothing, true);
                break;
        }
    }
    
    private void showSection(View view, boolean show) {
        // Find the section title (previous sibling TextView)
        ViewGroup parent = (ViewGroup) view.getParent();
        int index = parent.indexOfChild(view);
        if (index > 0) {
            View title = parent.getChildAt(index - 1);
            title.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        view.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    
    /**
     * Calculates the boss coin reward for a given level.
     * Level 1 boss = 200 coins; each subsequent boss = previous * 1.2 (spec §8.5).
     */
    private int getBossRewardForLevel(int level) {
        int reward = 200;
        for (int i = 2; i <= level; i++) {
            reward = (int) Math.round(reward * 1.2);
        }
        return reward;
    }

    private void loadShopItems() {
        // View can outlive async callbacks; guard against early/late calls.
        if (potionsAdapter == null || clothingAdapter == null) {
            return;
        }

        // "prethodni nivo" reference = boss at end of (userLevel - 1), min level 1 (spec §6)
        int referenceLevel = Math.max(1, userLevel - 1);
        int bossReward = getBossRewardForLevel(referenceLevel);

        // ---- POTIONS (shop only, spec §6) ----
        List<ShopItemAdapter.ShopItem> potions = new ArrayList<>();
        potions.add(new ShopItemAdapter.ShopItem(
                Equipment.POTION_PP_20,
                "Napitak snage (jednokratni)",
                "Povećava PP za 20% u jednoj borbi",
                Equipment.TYPE_POTION,
                (int) (bossReward * 0.5),
                "BOOST_PP_SINGLE", 0.20));
        potions.add(new ShopItemAdapter.ShopItem(
                Equipment.POTION_PP_40,
                "Napitak moći (jednokratni)",
                "Povećava PP za 40% u jednoj borbi",
                Equipment.TYPE_POTION,
                (int) (bossReward * 0.7),
                "BOOST_PP_SINGLE", 0.40));
        potions.add(new ShopItemAdapter.ShopItem(
                Equipment.POTION_PP_5_PERM,
                "Napitak snage (trajni)",
                "Trajno povećava PP za 5%",
                Equipment.TYPE_POTION,
                (int) (bossReward * 2.0),
                "BOOST_PP_PERMANENT", 0.05));
        potions.add(new ShopItemAdapter.ShopItem(
                Equipment.POTION_PP_10_PERM,
                "Napitak moći (trajni)",
                "Trajno povećava PP za 10%",
                Equipment.TYPE_POTION,
                (int) (bossReward * 10.0),
                "BOOST_PP_PERMANENT", 0.10));

        // ---- CLOTHING (shop or boss drop, spec §6) ----
        List<ShopItemAdapter.ShopItem> clothing = new ArrayList<>();
        clothing.add(new ShopItemAdapter.ShopItem(
                Equipment.CLOTHING_GLOVES,
                "Rukavice",
                "+10% snage napada, traju 2 borbe",
                Equipment.TYPE_CLOTHING,
                (int) (bossReward * 0.6),
                "ATTACK_POWER", 0.10));
        clothing.add(new ShopItemAdapter.ShopItem(
                Equipment.CLOTHING_SHIELD,
                "Štit",
                "+10% šanse za uspešan napad, traje 2 borbe",
                Equipment.TYPE_CLOTHING,
                (int) (bossReward * 0.6),
                "HIT_CHANCE", 0.10));
        clothing.add(new ShopItemAdapter.ShopItem(
                Equipment.CLOTHING_BOOTS,
                "Čizme",
                "40% šansa za jedan dodatni napad, traju 2 borbe",
                Equipment.TYPE_CLOTHING,
                (int) (bossReward * 0.8),
                "EXTRA_ATTACK", 0.40));

        // Weapons are NOT available in shop — boss-drop only (spec §6)
        potionsAdapter.setItems(potions);
        clothingAdapter.setItems(clothing);
        potionsAdapter.setUserCoins(userCoins);
        clothingAdapter.setUserCoins(userCoins);
    }
    
    @Override
    public void onBuyClick(ShopItemAdapter.ShopItem item) {
        // Show confirmation dialog
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Kupovina")
                .setMessage("Da li želite da kupite " + item.name + " za " + item.price + " novčića?")
                .setPositiveButton("Kupi", (dialog, which) -> purchaseItem(item))
                .setNegativeButton("Otkaži", null)
                .show();
    }
    
    private void purchaseItem(ShopItemAdapter.ShopItem item) {
        if (userCoins < item.price) {
            Toast.makeText(requireContext(), "Nemate dovoljno novčića!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Deduct coins through ViewModel
        userViewModel.spendCoins(item.price, (success, errorMessage) -> {
            if (!isAdded() || getActivity() == null) {
                return;
            }
            getActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                if (success) {
                    Toast.makeText(requireContext(), "Kupili ste " + item.name + "!", Toast.LENGTH_SHORT).show();
                    item.ownedCount++;

                    // Special mission rule: any shop purchase can damage alliance mission boss.
                    if (userId != null && !userId.trim().isEmpty()) {
                        AllianceMissionManager.recordShopPurchase(FirebaseFirestore.getInstance(), userId, null);
                    }
                    
                    // TODO: Save item to user's inventory in Firestore
                    saveItemToInventory(item);
                } else {
                    Toast.makeText(requireContext(), "Greška: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    
    private void saveItemToInventory(ShopItemAdapter.ShopItem item) {
        // Save to user's equipment collection in Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Check if item already exists
        db.collection("users").document(userId)
                .collection("equipment").document(item.id)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Increase quantity
                        db.collection("users").document(userId)
                                .collection("equipment").document(item.id)
                                .update("quantity", FieldValue.increment(1))
                                .addOnSuccessListener(aVoid -> updateAdapters());
                    } else {
                        // Create new item
                        Map<String, Object> equipmentData = new HashMap<>();
                        equipmentData.put("name", item.name);
                        equipmentData.put("type", item.type);
                        equipmentData.put("subType", item.id); // item.id = subType constant
                        equipmentData.put("description", item.description);
                        equipmentData.put("quantity", 1);
                        equipmentData.put("active", false);
                        equipmentData.put("bonus", item.effectValue);
                        // Clothing starts inactive; battlesRemaining set to 2 on activation
                        equipmentData.put("battlesRemaining",
                                Equipment.TYPE_CLOTHING.equals(item.type) ? 2 : 0);
                        equipmentData.put("effect", item.effect);
                        
                        db.collection("users").document(userId)
                                .collection("equipment").document(item.id)
                                .set(equipmentData)
                                .addOnSuccessListener(aVoid -> updateAdapters());
                    }
                });
    }
    
    private void updateAdapters() {
        if (!isAdded() || getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (!isAdded()) return;
            if (potionsAdapter != null) potionsAdapter.notifyDataSetChanged();
            if (clothingAdapter != null) clothingAdapter.notifyDataSetChanged();
            if (weaponsAdapter != null) weaponsAdapter.notifyDataSetChanged();
        });
    }
}
