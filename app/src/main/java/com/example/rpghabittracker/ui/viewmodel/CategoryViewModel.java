package com.example.rpghabittracker.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.rpghabittracker.data.model.Category;
import com.example.rpghabittracker.data.repository.CategoryRepository;

import java.util.List;
import java.util.UUID;

/**
 * ViewModel for Category-related UI operations
 */
public class CategoryViewModel extends AndroidViewModel {
    
    private final CategoryRepository repository;
    private final MutableLiveData<String> currentUserId = new MutableLiveData<>();
    
    private LiveData<List<Category>> userCategories;
    
    public CategoryViewModel(@NonNull Application application) {
        super(application);
        repository = new CategoryRepository(application);
    }
    
    public void setUserId(String userId) {
        currentUserId.setValue(userId);
        // Create default categories for new users
        repository.createDefaultCategories(userId);
        // Start listening to Firestore for real-time sync
        repository.startListeningToCategories(userId);
    }
    
    // Start listening without creating defaults (for existing users)
    public void startListening(String userId) {
        currentUserId.setValue(userId);
        repository.startListeningToCategories(userId);
    }
    
    // Stop Firestore listener
    public void stopListening() {
        repository.stopListening();
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        repository.stopListening();
    }
    
    // Get all categories for current user
    public LiveData<List<Category>> getUserCategories() {
        if (userCategories == null) {
            userCategories = Transformations.switchMap(currentUserId, userId -> {
                if (userId == null) return new MutableLiveData<>();
                return repository.getUserCategories(userId);
            });
        }
        return userCategories;
    }
    
    // Get all categories by userId directly
    public LiveData<List<Category>> getUserCategories(String userId) {
        return repository.getUserCategories(userId);
    }
    
    // Get category by ID
    public LiveData<Category> getCategoryById(String categoryId) {
        return repository.getCategoryById(categoryId);
    }
    
    // Get category by ID synchronously
    public void getCategoryByIdSync(String categoryId, CategoryRepository.CategoryCallback callback) {
        repository.getCategoryByIdSync(categoryId, callback);
    }
    
    // Insert category directly
    public void insertCategory(Category category) {
        if (category.getId() == null || category.getId().isEmpty()) {
            category.setId(UUID.randomUUID().toString());
        }
        repository.insert(category);
    }
    
    // Create new category
    public void createCategory(String name, String color, CreateCallback callback) {
        String userId = currentUserId.getValue();
        if (userId == null) {
            callback.onResult(false, "Korisnik nije prijavljen");
            return;
        }
        
        // Check if color is already used
        repository.isColorUsed(userId, color, isUsed -> {
            if (isUsed) {
                callback.onResult(false, "Ova boja je već dodeljena drugoj kategoriji");
            } else {
                Category category = new Category(userId, name, color);
                category.setId(UUID.randomUUID().toString());
                repository.insert(category);
                callback.onResult(true, null);
            }
        });
    }
    
    // Update category
    public void updateCategory(Category category) {
        repository.update(category);
    }
    
    // Update category color
    public void updateCategoryColor(String categoryId, String newColor, 
                                    CategoryRepository.UpdateCallback callback) {
        String userId = currentUserId.getValue();
        if (userId == null) {
            callback.onResult(false, "Korisnik nije prijavljen");
            return;
        }
        repository.updateCategoryColor(categoryId, newColor, userId, callback);
    }
    
    // Update category color with explicit userId
    public void updateCategoryColor(String categoryId, String newColor, String userId,
                                    CategoryRepository.UpdateCallback callback) {
        repository.updateCategoryColor(categoryId, newColor, userId, callback);
    }
    
    // Check if a category is referenced by any task (blocks deletion)
    public void isCategoryInUse(String categoryId, CategoryRepository.ColorCheckCallback callback) {
        repository.isCategoryInUse(categoryId, callback);
    }

    // Delete category
    public void deleteCategory(Category category) {
        repository.delete(category);
    }
    
    // Delete category by ID
    public void deleteCategoryById(String categoryId) {
        repository.deleteById(categoryId);
    }
    
    // Get next available color for new category
    public void getNextAvailableColor(CategoryRepository.ColorCallback callback) {
        String userId = currentUserId.getValue();
        if (userId == null) {
            callback.onResult("#4CAF50"); // Default green
            return;
        }
        repository.getNextAvailableColor(userId, callback);
    }
    
    // Get all categories synchronously (for spinners/dialogs)
    public void getAllCategoriesSync(CategoryRepository.CategoriesCallback callback) {
        String userId = currentUserId.getValue();
        if (userId == null) {
            callback.onResult(null);
            return;
        }
        repository.getUserCategoriesSync(userId, callback);
    }
    
    // Callback interface
    public interface CreateCallback {
        void onResult(boolean success, String errorMessage);
    }
}
