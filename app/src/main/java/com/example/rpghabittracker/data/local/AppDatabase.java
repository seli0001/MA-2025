package com.example.rpghabittracker.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.rpghabittracker.data.local.dao.BossDao;
import com.example.rpghabittracker.data.local.dao.CategoryDao;
import com.example.rpghabittracker.data.local.dao.EquipmentDao;
import com.example.rpghabittracker.data.local.dao.TaskDao;
import com.example.rpghabittracker.data.local.dao.UserDao;
import com.example.rpghabittracker.data.model.Boss;
import com.example.rpghabittracker.data.model.Category;
import com.example.rpghabittracker.data.model.Equipment;
import com.example.rpghabittracker.data.model.Task;
import com.example.rpghabittracker.data.model.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Room Database for RPG Habit Tracker
 * Contains all entities: User, Task, Category, Boss, Equipment
 */
@Database(
    entities = {
        User.class,
        Task.class,
        Category.class,
        Boss.class,
        Equipment.class
    },
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {
    
    // DAOs
    public abstract UserDao userDao();
    public abstract TaskDao taskDao();
    public abstract CategoryDao categoryDao();
    public abstract BossDao bossDao();
    public abstract EquipmentDao equipmentDao();
    
    // Singleton instance
    private static volatile AppDatabase INSTANCE;
    
    // Executor for database operations
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = 
        Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    
    /**
     * Get database instance (singleton)
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "rpg_habit_tracker_db"
                    )
                    .addCallback(sRoomDatabaseCallback)
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * Database callback for prepopulating data on creation
     */
    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            
            // Prepopulate with default categories in background
            databaseWriteExecutor.execute(() -> {
                // Default categories will be created per user after registration
                // No global prepopulation needed
            });
        }
    };
    
    /**
     * Close database instance (for testing)
     */
    public static void destroyInstance() {
        if (INSTANCE != null && INSTANCE.isOpen()) {
            INSTANCE.close();
        }
        INSTANCE = null;
    }
}
