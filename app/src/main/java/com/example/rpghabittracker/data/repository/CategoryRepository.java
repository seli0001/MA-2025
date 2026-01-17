package com.example.rpghabittracker.data.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.rpghabittracker.data.local.AppDatabase;
import com.example.rpghabittracker.data.local.dao.CategoryDao;
import com.example.rpghabittracker.data.model.Category;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for Category data operations
 * Syncs data between local Room database and Firebase Firestore
 */
public class CategoryRepository {
    
    private static final String TAG = "CategoryRepository";
    private static final String COLLECTION_CATEGORIES = "categories";
    
    private final CategoryDao categoryDao;
    private final FirebaseFirestore firestore;
    private final ExecutorService executor;
    private ListenerRegistration categoriesListener;
    
    // Default category colors
    public static final String[] DEFAULT_COLORS = {
        "#4CAF50", // Green - Health
        "#2196F3", // Blue - Learning
        "#FF9800", // Orange - Fun
        "#9C27B0", // Purple - Personal
        "#F44336", // Red - Work
        "#00BCD4", // Cyan - Sports
        "#795548", // Brown - Home
        "#607D8B"  // Gray - Other
    };
    
    // Default category names (Serbian)
    public static final String[] DEFAULT_NAMES = {
        "Zdravlje",
        "Učenje",
        "Zabava",
        "Lični razvoj",
        "Posao",
        "Sport",
        "Kuća",
        "Ostalo"
    };
    
    public CategoryRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        categoryDao = db.categoryDao();
        firestore = FirebaseFirestore.getInstance();
        executor = Executors.newSingleThreadExecutor();
    }
    
    // Start listening to Firestore changes
    public void startListeningToCategories(String userId) {
        if (categoriesListener != null) {
            categoriesListener.remove();
        }
        
        categoriesListener = firestore.collection(COLLECTION_CATEGORIES)
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Firestore listen error", error);
                        return;
                    }
                    
                    if (snapshots != null) {
                        executor.execute(() -> {
                            for (QueryDocumentSnapshot doc : snapshots) {
                                Category category = documentToCategory(doc);
                                if (category != null) {
                                    categoryDao.insert(category);
                                }
                            }
                        });
                    }
                });
    }
    
    public void stopListening() {
        if (categoriesListener != null) {
            categoriesListener.remove();
            categoriesListener = null;
        }
    }
    
    // Basic CRUD with Firebase sync
    public void insert(Category category) {
        executor.execute(() -> {
            if (category.getId() == null || category.getId().isEmpty()) {
                category.setId(UUID.randomUUID().toString());
            }
            categoryDao.insert(category);
            syncCategoryToFirestore(category);
        });
    }
    
    public void update(Category category) {
        executor.execute(() -> {
            categoryDao.update(category);
            syncCategoryToFirestore(category);
        });
    }
    
    public void delete(Category category) {
        executor.execute(() -> {
            categoryDao.delete(category);
            deleteFromFirestore(category.getId());
        });
    }
    
    public void deleteById(String categoryId) {
        executor.execute(() -> {
            categoryDao.deleteById(categoryId);
            deleteFromFirestore(categoryId);
        });
    }
    
    // Sync category to Firestore
    private void syncCategoryToFirestore(Category category) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", category.getId());
        data.put("userId", category.getUserId());
        data.put("name", category.getName());
        data.put("color", category.getColor());
        data.put("createdAt", category.getCreatedAt());
        
        firestore.collection(COLLECTION_CATEGORIES)
                .document(category.getId())
                .set(data)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Category synced: " + category.getName()))
                .addOnFailureListener(e -> Log.e(TAG, "Error syncing category", e));
    }
    
    private void deleteFromFirestore(String categoryId) {
        firestore.collection(COLLECTION_CATEGORIES)
                .document(categoryId)
                .delete()
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting category", e));
    }
    
    // Convert Firestore document to Category
    private Category documentToCategory(DocumentSnapshot doc) {
        try {
            Category category = new Category();
            category.setId(doc.getString("id"));
            category.setUserId(doc.getString("userId"));
            category.setName(doc.getString("name"));
            category.setColor(doc.getString("color"));
            
            Long createdAt = doc.getLong("createdAt");
            if (createdAt != null) category.setCreatedAt(createdAt);
            
            return category;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to Category", e);
            return null;
        }
    }
    
    // Get categories
    public LiveData<Category> getCategoryById(String categoryId) {
        return categoryDao.getCategoryById(categoryId);
    }
    
    public void getCategoryByIdSync(String categoryId, CategoryCallback callback) {
        executor.execute(() -> {
            Category category = categoryDao.getCategoryByIdSync(categoryId);
            callback.onResult(category);
        });
    }
    
    public LiveData<List<Category>> getUserCategories(String userId) {
        return categoryDao.getUserCategories(userId);
    }
    
    public void getUserCategoriesSync(String userId, CategoriesCallback callback) {
        executor.execute(() -> {
            List<Category> categories = categoryDao.getUserCategoriesSync(userId);
            callback.onResult(categories);
        });
    }
    
    // Check if color is already used
    public void isColorUsed(String userId, String color, ColorCheckCallback callback) {
        executor.execute(() -> {
            int count = categoryDao.isColorUsed(userId, color);
            callback.onResult(count > 0);
        });
    }
    
    // Update category color
    public void updateCategoryColor(String categoryId, String newColor, String userId, 
                                    UpdateCallback callback) {
        executor.execute(() -> {
            int colorUsed = categoryDao.isColorUsed(userId, newColor);
            if (colorUsed > 0) {
                callback.onResult(false, "Ova boja je već dodeljena drugoj kategoriji");
                return;
            }
            
            Category category = categoryDao.getCategoryByIdSync(categoryId);
            if (category != null) {
                category.setColor(newColor);
                categoryDao.update(category);
                syncCategoryToFirestore(category);
                callback.onResult(true, null);
            } else {
                callback.onResult(false, "Kategorija nije pronađena");
            }
        });
    }
    
    // Create default categories for new user
    public void createDefaultCategories(String userId) {
        executor.execute(() -> {
            int existingCount = categoryDao.getCategoryCount(userId);
            if (existingCount == 0) {
                List<Category> defaultCategories = new ArrayList<>();
                for (int i = 0; i < DEFAULT_NAMES.length; i++) {
                    Category category = new Category(userId, DEFAULT_NAMES[i], DEFAULT_COLORS[i]);
                    category.setId(UUID.randomUUID().toString());
                    defaultCategories.add(category);
                    
                    // Sync each to Firestore
                    syncCategoryToFirestore(category);
                }
                categoryDao.insertAll(defaultCategories);
            }
        });
    }
    
    // Get next available color
    public void getNextAvailableColor(String userId, ColorCallback callback) {
        executor.execute(() -> {
            for (String color : DEFAULT_COLORS) {
                int count = categoryDao.isColorUsed(userId, color);
                if (count == 0) {
                    callback.onResult(color);
                    return;
                }
            }
            String randomColor = String.format("#%06X", (int)(Math.random() * 0xFFFFFF));
            callback.onResult(randomColor);
        });
    }
    
    // Callbacks
    public interface CategoryCallback {
        void onResult(Category category);
    }
    
    public interface CategoriesCallback {
        void onResult(List<Category> categories);
    }
    
    public interface ColorCheckCallback {
        void onResult(boolean isUsed);
    }
    
    public interface ColorCallback {
        void onResult(String color);
    }
    
    public interface UpdateCallback {
        void onResult(boolean success, String errorMessage);
    }
}
