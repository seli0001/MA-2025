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
                    userLevel = userData.getLevel();
                    textCoins.setText(String.valueOf(userCoins));
                    
                    // Update adapters with new coin count
                    if (potionsAdapter != null) potionsAdapter.setUserCoins(userCoins);
                    if (clothingAdapter != null) clothingAdapter.setUserCoins(userCoins);
                    if (weaponsAdapter != null) weaponsAdapter.setUserCoins(userCoins);
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
                    if (level != null) {
                        userLevel = level.intValue();
                    }
                });
    }
    
    private void setupAdapters() {
        potionsAdapter = new ShopItemAdapter(this);
        clothingAdapter = new ShopItemAdapter(this);
        weaponsAdapter = new ShopItemAdapter(this);
        
        recyclerPotions.setAdapter(potionsAdapter);
        recyclerClothing.setAdapter(clothingAdapter);
        recyclerWeapons.setAdapter(weaponsAdapter);
    }
    
    private void setupTabs() {
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("Sve"));
        tabLayout.addTab(tabLayout.newTab().setText("Napici"));
        tabLayout.addTab(tabLayout.newTab().setText("Oprema"));
        tabLayout.addTab(tabLayout.newTab().setText("Oružje"));
        
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
        // Show/hide sections based on selected tab
        switch (position) {
            case 0: // All
                showSection(recyclerPotions, true);
                showSection(recyclerClothing, true);
                showSection(recyclerWeapons, true);
                break;
            case 1: // Potions
                showSection(recyclerPotions, true);
                showSection(recyclerClothing, false);
                showSection(recyclerWeapons, false);
                break;
            case 2: // Clothing
                showSection(recyclerPotions, false);
                showSection(recyclerClothing, true);
                showSection(recyclerWeapons, false);
                break;
            case 3: // Weapons
                showSection(recyclerPotions, false);
                showSection(recyclerClothing, false);
                showSection(recyclerWeapons, true);
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
    
    private void loadShopItems() {
        // Create shop items based on equipment types
        List<ShopItemAdapter.ShopItem> potions = new ArrayList<>();
        potions.add(new ShopItemAdapter.ShopItem("potion_health", "Napitak zdravlja", 
                "Vraća 50 HP u bici", Equipment.TYPE_POTION, 100, "RESTORE_HP", 50));
        potions.add(new ShopItemAdapter.ShopItem("potion_power", "Napitak snage", 
                "+10 PP za jednu bitku", Equipment.TYPE_POTION, 150, "BOOST_PP", 10));
        potions.add(new ShopItemAdapter.ShopItem("potion_luck", "Napitak sreće", 
                "+20% šanse za kritičan udarac", Equipment.TYPE_POTION, 200, "CRIT_CHANCE", 0.2));
        potions.add(new ShopItemAdapter.ShopItem("potion_shield", "Napitak štita", 
                "Smanjuje štetu za 30%", Equipment.TYPE_POTION, 250, "DAMAGE_REDUCTION", 0.3));
        
        List<ShopItemAdapter.ShopItem> clothing = new ArrayList<>();
        clothing.add(new ShopItemAdapter.ShopItem("armor_warrior", "Oklop ratnika", 
                "+5% smanjenje štete", Equipment.TYPE_CLOTHING, 500, "DAMAGE_REDUCTION", 0.05));
        clothing.add(new ShopItemAdapter.ShopItem("boots_swift", "Čizme brzine", 
                "+10% brzine napada", Equipment.TYPE_CLOTHING, 400, "ATTACK_SPEED", 0.1));
        clothing.add(new ShopItemAdapter.ShopItem("cloak_magic", "Magični ogrtač", 
                "+5% XP bonus", Equipment.TYPE_CLOTHING, 600, "XP_BONUS", 0.05));
        clothing.add(new ShopItemAdapter.ShopItem("helmet_dragon", "Kaciga zmaja", 
                "+15% snage napada", Equipment.TYPE_CLOTHING, 800, "ATTACK_POWER", 0.15));
        
        List<ShopItemAdapter.ShopItem> weapons = new ArrayList<>();
        weapons.add(new ShopItemAdapter.ShopItem("weapon_iron_sword", "Gvozdeni mač", 
                "Osnovno oružje", Equipment.TYPE_WEAPON, 300, "ATTACK_POWER", 1.0));
        weapons.add(new ShopItemAdapter.ShopItem("weapon_steel_axe", "Čelična sekira", 
                "+20% štete", Equipment.TYPE_WEAPON, 800, "ATTACK_POWER", 1.2));
        weapons.add(new ShopItemAdapter.ShopItem("weapon_dragon_blade", "Oštrica zmaja", 
                "+50% štete", Equipment.TYPE_WEAPON, 2000, "ATTACK_POWER", 1.5));
        weapons.add(new ShopItemAdapter.ShopItem("weapon_legendary", "Legendarni mač", 
                "+100% štete, +10% crit", Equipment.TYPE_WEAPON, 5000, "ATTACK_POWER", 2.0));
        
        // Set items to adapters
        potionsAdapter.setItems(potions);
        clothingAdapter.setItems(clothing);
        weaponsAdapter.setItems(weapons);
        
        // Set initial coins
        potionsAdapter.setUserCoins(userCoins);
        clothingAdapter.setUserCoins(userCoins);
        weaponsAdapter.setUserCoins(userCoins);
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
            requireActivity().runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(requireContext(), "Kupili ste " + item.name + "!", Toast.LENGTH_SHORT).show();
                    item.ownedCount++;
                    
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
                        equipmentData.put("description", item.description);
                        equipmentData.put("quantity", 1);
                        equipmentData.put("active", false);
                        equipmentData.put("bonus", item.effectValue);
                        equipmentData.put("battlesRemaining", 0);
                        equipmentData.put("effect", item.effect);
                        
                        db.collection("users").document(userId)
                                .collection("equipment").document(item.id)
                                .set(equipmentData)
                                .addOnSuccessListener(aVoid -> updateAdapters());
                    }
                });
    }
    
    private void updateAdapters() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                potionsAdapter.notifyDataSetChanged();
                clothingAdapter.notifyDataSetChanged();
                weaponsAdapter.notifyDataSetChanged();
            });
        }
    }
}
