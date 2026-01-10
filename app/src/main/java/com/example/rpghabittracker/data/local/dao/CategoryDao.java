package com.example.rpghabittracker.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.rpghabittracker.data.model.Category;

import java.util.List;

/**
 * Data Access Object for Category entity
 */
@Dao
public interface CategoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Category category);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Category> categories);
    
    @Update
    void update(Category category);
    
    @Delete
    void delete(Category category);
    
    @Query("DELETE FROM categories WHERE id = :categoryId")
    void deleteById(String categoryId);
    
    @Query("SELECT * FROM categories WHERE id = :categoryId")
    LiveData<Category> getCategoryById(String categoryId);
    
    @Query("SELECT * FROM categories WHERE id = :categoryId")
    Category getCategoryByIdSync(String categoryId);
    
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY name ASC")
    LiveData<List<Category>> getUserCategories(String userId);
    
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY name ASC")
    List<Category> getUserCategoriesSync(String userId);
    
    @Query("SELECT * FROM categories WHERE userId = :userId AND color = :color LIMIT 1")
    Category getCategoryByColor(String userId, String color);
    
    @Query("SELECT COUNT(*) FROM categories WHERE userId = :userId AND color = :color")
    int isColorUsed(String userId, String color);
    
    @Query("SELECT COUNT(*) FROM categories WHERE userId = :userId")
    int getCategoryCount(String userId);
}
