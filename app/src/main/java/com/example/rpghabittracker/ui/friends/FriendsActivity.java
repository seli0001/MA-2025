package com.example.rpghabittracker.ui.friends;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpghabittracker.R;
import com.example.rpghabittracker.ui.adapters.FriendAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.Locale;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Activity for managing friends - search, add, view friends list
 */
public class FriendsActivity extends AppCompatActivity implements FriendAdapter.OnUserActionListener {

    private EditText editSearch;
    private ImageView buttonScanQr;
    private TabLayout tabLayout;
    private RecyclerView recyclerFriends;
    private LinearLayout layoutEmpty;
    private TextView textEmpty;
    private FloatingActionButton fabShowQr;

    private FriendAdapter adapter;
    private FirebaseFirestore firestore;
    private String currentUserId;
    private String currentUsername;

    private List<String> friendIds = new ArrayList<>();
    private List<String> sentRequestIds = new ArrayList<>();
    private List<String> receivedRequestIds = new ArrayList<>();

    private final ActivityResultLauncher<ScanOptions> qrScanLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() != null) {
                    handleScannedQrCode(result.getContents());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        firestore = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        currentUserId = user.getUid();

        initViews();
        setupToolbar();
        setupTabs();
        setupSearch();
        setupAdapter();
        loadCurrentUserData();
        loadFriendships();
    }

    private void initViews() {
        editSearch = findViewById(R.id.editSearch);
        buttonScanQr = findViewById(R.id.buttonScanQr);
        tabLayout = findViewById(R.id.tabLayout);
        recyclerFriends = findViewById(R.id.recyclerFriends);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        textEmpty = findViewById(R.id.textEmpty);
        fabShowQr = findViewById(R.id.fabShowQr);

        recyclerFriends.setLayoutManager(new LinearLayoutManager(this));

        buttonScanQr.setOnClickListener(v -> scanQrCode());
        fabShowQr.setOnClickListener(v -> showQrCodeDialog());
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Prijatelji"));
        tabLayout.addTab(tabLayout.newTab().setText("Zahtevi"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    adapter.setMode(FriendAdapter.MODE_FRIENDS);
                    loadFriendsList();
                } else {
                    adapter.setMode(FriendAdapter.MODE_REQUESTS);
                    loadFriendRequests();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSearch() {
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.length() >= 2) {
                    searchUsers(query);
                } else if (query.isEmpty()) {
                    // Return to current tab view
                    if (tabLayout.getSelectedTabPosition() == 0) {
                        loadFriendsList();
                    } else {
                        loadFriendRequests();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupAdapter() {
        adapter = new FriendAdapter(currentUserId, this);
        recyclerFriends.setAdapter(adapter);
    }

    private void loadCurrentUserData() {
        firestore.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUsername = doc.getString("username");
                    }
                });
    }

    private void loadFriendships() {
        // Load friend IDs
        firestore.collection("friendships")
                .whereEqualTo("status", "ACCEPTED")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    friendIds.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        String senderId = doc.getString("senderId");
                        String receiverId = doc.getString("receiverId");
                        if (currentUserId.equals(senderId)) {
                            friendIds.add(receiverId);
                        } else if (currentUserId.equals(receiverId)) {
                            friendIds.add(senderId);
                        }
                    }
                    loadFriendsList();
                })
                .addOnFailureListener(e -> showFirestoreError(e, "učitavanju prijatelja"));

        // Load sent requests
        firestore.collection("friendships")
                .whereEqualTo("senderId", currentUserId)
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    sentRequestIds.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        sentRequestIds.add(doc.getString("receiverId"));
                    }
                })
                .addOnFailureListener(e -> showFirestoreError(e, "učitavanju poslatih zahteva"));

        // Load received requests
        firestore.collection("friendships")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    receivedRequestIds.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        receivedRequestIds.add(doc.getString("senderId"));
                    }
                })
                .addOnFailureListener(e -> showFirestoreError(e, "učitavanju pristiglih zahteva"));
    }

    private void loadFriendsList() {
        adapter.setMode(FriendAdapter.MODE_FRIENDS);
        
        if (friendIds.isEmpty()) {
            showEmptyState("Nemate prijatelja");
            return;
        }

        List<FriendAdapter.UserItem> friends = new ArrayList<>();
        
        for (String friendId : friendIds) {
            firestore.collection("users").document(friendId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            FriendAdapter.UserItem item = documentToUserItem(doc);
                            item.isFriend = true;
                            friends.add(item);
                            
                            if (friends.size() == friendIds.size()) {
                                adapter.setUsers(friends);
                                hideEmptyState();
                            }
                        }
                    });
        }
    }

    private void loadFriendRequests() {
        adapter.setMode(FriendAdapter.MODE_REQUESTS);
        
        if (receivedRequestIds.isEmpty()) {
            showEmptyState("Nemate zahteva za prijateljstvo");
            return;
        }

        List<FriendAdapter.UserItem> requests = new ArrayList<>();
        
        for (String senderId : receivedRequestIds) {
            firestore.collection("users").document(senderId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            FriendAdapter.UserItem item = documentToUserItem(doc);
                            item.requestReceived = true;
                            requests.add(item);
                            
                            if (requests.size() == receivedRequestIds.size()) {
                                adapter.setUsers(requests);
                                hideEmptyState();
                            }
                        }
                    });
        }
    }

    private void searchUsers(String query) {
        adapter.setMode(FriendAdapter.MODE_SEARCH);

        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        if (normalizedQuery.isEmpty()) {
            showEmptyState("Unesite pojam za pretragu");
            return;
        }

        // Primary query for case-insensitive search (new schema)
        firestore.collection("users")
                .orderBy("usernameLower")
                .startAt(normalizedQuery)
                .endAt(normalizedQuery + "\uf8ff")
                .limit(20)
                .get()
                .addOnSuccessListener(lowerSnapshot -> {
                    List<FriendAdapter.UserItem> lowerResults = mapSearchResults(lowerSnapshot);
                    if (!lowerResults.isEmpty()) {
                        showSearchResults(lowerResults);
                        return;
                    }

                    // Fallback for legacy users that don't have usernameLower yet
                    searchUsersLegacy(query);
                })
                .addOnFailureListener(e -> {
                    // Fallback query for legacy schema and index mismatches
                    searchUsersLegacy(query);
                });
    }

    private void searchUsersLegacy(String query) {
        firestore.collection("users")
                .orderBy("username")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(20)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<FriendAdapter.UserItem> results = mapSearchResults(querySnapshot);
                    showSearchResults(results);
                })
                .addOnFailureListener(e -> {
                    showFirestoreError(e, "pretrazi korisnika");
                    showEmptyState("Pretraga trenutno nije dostupna");
                });
    }

    private List<FriendAdapter.UserItem> mapSearchResults(com.google.firebase.firestore.QuerySnapshot snapshot) {
        List<FriendAdapter.UserItem> results = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        for (QueryDocumentSnapshot doc : snapshot) {
            String id = doc.getId();
            if (id.equals(currentUserId) || seenIds.contains(id)) continue;

            FriendAdapter.UserItem item = documentToUserItem(doc);
            item.isFriend = friendIds.contains(id);
            item.requestSent = sentRequestIds.contains(id);
            item.requestReceived = receivedRequestIds.contains(id);
            results.add(item);
            seenIds.add(id);
        }

        return results;
    }

    private void showSearchResults(List<FriendAdapter.UserItem> results) {
        if (results.isEmpty()) {
            showEmptyState("Nema rezultata pretrage");
        } else {
            adapter.setUsers(results);
            hideEmptyState();
        }
    }

    private FriendAdapter.UserItem documentToUserItem(DocumentSnapshot doc) {
        String id = doc.getId();
        String username = doc.getString("username");
        if (username == null || username.trim().isEmpty()) {
            username = doc.getString("displayName");
        }
        String avatar = doc.getString("avatar");
        Long level = doc.getLong("level");
        String title = doc.getString("title");
        
        return new FriendAdapter.UserItem(
                id,
                username != null ? username : "Unknown",
                avatar,
                level != null ? level.intValue() : 1,
                title
        );
    }

    private void showFirestoreError(Exception e, String action) {
        String message = "Greška pri " + action;
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException ff = (FirebaseFirestoreException) e;
            if (ff.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                message = "Nije dozvoljeno čitanje korisnika (proverite Firestore rules).";
            } else if (ff.getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                message = "Nedostaje Firestore indeks za pretragu korisnika.";
            }
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showEmptyState(String message) {
        layoutEmpty.setVisibility(View.VISIBLE);
        recyclerFriends.setVisibility(View.GONE);
        textEmpty.setText(message);
    }

    private void hideEmptyState() {
        layoutEmpty.setVisibility(View.GONE);
        recyclerFriends.setVisibility(View.VISIBLE);
    }

    private void scanQrCode() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Skenirajte QR kod prijatelja");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        qrScanLauncher.launch(options);
    }

    private void handleScannedQrCode(String content) {
        // Expected format: rpghabit://user/{userId}
        if (content.startsWith("rpghabit://user/")) {
            String userId = content.replace("rpghabit://user/", "");
            if (userId.equals(currentUserId)) {
                Toast.makeText(this, "Ne možete dodati sebe", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Check if already friends or request pending
            if (friendIds.contains(userId)) {
                Toast.makeText(this, "Već ste prijatelji", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (sentRequestIds.contains(userId)) {
                Toast.makeText(this, "Zahtev je već poslat", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Send friend request
            sendFriendRequest(userId);
        } else {
            Toast.makeText(this, "Nevažeći QR kod", Toast.LENGTH_SHORT).show();
        }
    }

    private void showQrCodeDialog() {
        // Generate QR code with user ID
        String content = "rpghabit://user/" + currentUserId;
        
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            
            // Show dialog with QR code
            ImageView qrImageView = new ImageView(this);
            qrImageView.setImageBitmap(bitmap);
            qrImageView.setPadding(32, 32, 32, 32);
            
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Vaš QR kod")
                    .setMessage("Drugi korisnici mogu da skeniraju ovaj kod da vas dodaju")
                    .setView(qrImageView)
                    .setPositiveButton("Zatvori", null)
                    .show();
                    
        } catch (WriterException e) {
            Toast.makeText(this, "Greška pri generisanju QR koda", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendFriendRequest(String receiverId) {
        String friendshipId = currentUserId + "_" + receiverId;
        
        Map<String, Object> friendship = new HashMap<>();
        friendship.put("senderId", currentUserId);
        friendship.put("receiverId", receiverId);
        friendship.put("status", "PENDING");
        friendship.put("createdAt", FieldValue.serverTimestamp());
        
        firestore.collection("friendships").document(friendshipId)
                .set(friendship)
                .addOnSuccessListener(aVoid -> {
                    sentRequestIds.add(receiverId);
                    Toast.makeText(this, "Zahtev za prijateljstvo poslat!", Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Greška: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // FriendAdapter.OnUserActionListener implementation
    @Override
    public void onAddFriend(FriendAdapter.UserItem user) {
        sendFriendRequest(user.id);
    }

    @Override
    public void onRemoveFriend(FriendAdapter.UserItem user) {
        // TODO: Implement remove friend
        Toast.makeText(this, "Uklanjanje prijatelja - uskoro", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAcceptRequest(FriendAdapter.UserItem user) {
        String friendshipId = user.id + "_" + currentUserId;
        
        firestore.collection("friendships").document(friendshipId)
                .update("status", "ACCEPTED")
                .addOnSuccessListener(aVoid -> {
                    receivedRequestIds.remove(user.id);
                    friendIds.add(user.id);
                    Toast.makeText(this, "Zahtev prihvaćen!", Toast.LENGTH_SHORT).show();
                    loadFriendRequests();
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Greška: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public void onRejectRequest(FriendAdapter.UserItem user) {
        String friendshipId = user.id + "_" + currentUserId;
        
        firestore.collection("friendships").document(friendshipId)
                .update("status", "REJECTED")
                .addOnSuccessListener(aVoid -> {
                    receivedRequestIds.remove(user.id);
                    Toast.makeText(this, "Zahtev odbijen", Toast.LENGTH_SHORT).show();
                    loadFriendRequests();
                });
    }

    @Override
    public void onViewProfile(FriendAdapter.UserItem user) {
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra("userId", user.id);
        startActivity(intent);
    }
}
