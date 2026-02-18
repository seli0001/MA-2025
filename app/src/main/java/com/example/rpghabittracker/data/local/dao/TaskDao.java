package com.example.rpghabittracker.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.rpghabittracker.data.model.Task;

import java.util.List;

/**
 * Data Access Object for Task entity
 */
@Dao
public interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Task task);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Task> tasks);

    @Update
    void update(Task task);

    @Delete
    void delete(Task task);

    @Query("DELETE FROM tasks WHERE id = :taskId")
    void deleteById(String taskId);

    // Get single task
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    LiveData<Task> getTaskById(String taskId);

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    Task getTaskByIdSync(String taskId);

    // Get all tasks for user
    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY dueDate ASC")
    LiveData<List<Task>> getUserTasks(String userId);

    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY dueDate ASC")
    List<Task> getUserTasksSync(String userId);

    // Get tasks by status
    @Query("SELECT * FROM tasks WHERE userId = :userId AND status = :status ORDER BY dueDate ASC")
    LiveData<List<Task>> getTasksByStatus(String userId, String status);

    // Get tasks in date range
    @Query("SELECT * FROM tasks WHERE userId = :userId AND dueDate >= :startDate AND dueDate <= :endDate ORDER BY dueDate ASC")
    LiveData<List<Task>> getTasksInDateRange(String userId, long startDate, long endDate);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND dueDate >= :startDate AND dueDate <= :endDate ORDER BY dueDate ASC")
    List<Task> getTasksInDateRangeSync(String userId, long startDate, long endDate);

    // Get today's tasks
    @Query("SELECT * FROM tasks WHERE userId = :userId AND dueDate >= :dayStart AND dueDate < :dayEnd ORDER BY dueDate ASC")
    LiveData<List<Task>> getTodayTasks(String userId, long dayStart, long dayEnd);

    // Get tasks by category
    @Query("SELECT * FROM tasks WHERE userId = :userId AND categoryId = :categoryId ORDER BY dueDate ASC")
    LiveData<List<Task>> getTasksByCategory(String userId, String categoryId);

    // Get one-time tasks only (not recurring)
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isRecurring = 0 ORDER BY dueDate ASC")
    LiveData<List<Task>> getOneTimeTasks(String userId);

    // Get recurring TEMPLATES only (parent rows — no parentTaskId)
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isRecurring = 1 AND (parentTaskId IS NULL OR parentTaskId = '') ORDER BY startDate ASC")
    LiveData<List<Task>> getRecurringTemplates(String userId);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isRecurring = 1 AND (parentTaskId IS NULL OR parentTaskId = '')")
    List<Task> getRecurringTemplatesSync(String userId);

    // Get recurring OCCURRENCES only (child rows — have a parentTaskId)
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isRecurring = 1 AND parentTaskId IS NOT NULL AND parentTaskId != '' ORDER BY dueDate ASC")
    LiveData<List<Task>> getRecurringOccurrences(String userId);

    // Get today's occurrence for a specific template (returns null if none yet)
    @Query("SELECT * FROM tasks WHERE parentTaskId = :parentId AND dueDate >= :dayStart AND dueDate < :dayEnd LIMIT 1")
    Task getTodayOccurrenceSync(String parentId, long dayStart, long dayEnd);

    // Delete all future occurrences for a template (used when deleting a series)
    @Query("DELETE FROM tasks WHERE parentTaskId = :parentId AND status = 'ACTIVE'")
    void deleteFutureOccurrences(String parentId);

    // Update status
    @Query("UPDATE tasks SET status = :status WHERE id = :taskId")
    void updateStatus(String taskId, String status);

    @Query("UPDATE tasks SET status = :status, completedDate = :completedDate WHERE id = :taskId")
    void markComplete(String taskId, String status, long completedDate);

    // Count tasks
    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId AND status = :status")
    int countTasksByStatus(String userId, String status);

    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId AND completedDate >= :dayStart AND completedDate < :dayEnd AND status = 'COMPLETED'")
    int countCompletedTasksForDate(String userId, long dayStart, long dayEnd);

    // Quota tracking - count tasks by difficulty completed today
    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId AND difficulty = :difficulty AND status = 'COMPLETED' AND completedDate >= :dayStart AND completedDate < :dayEnd")
    int countCompletedByDifficultyToday(String userId, String difficulty, long dayStart, long dayEnd);

    // Quota tracking - count tasks by importance completed today
    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId AND importance = :importance AND status = 'COMPLETED' AND completedDate >= :dayStart AND completedDate < :dayEnd")
    int countCompletedByImportanceToday(String userId, String importance, long dayStart, long dayEnd);

    // Get ACTIVE occurrences (not templates) past their expiry window — for auto-failing
    @Query("SELECT * FROM tasks WHERE userId = :userId AND status = 'ACTIVE' AND dueDate < :cutoffTime AND parentTaskId IS NOT NULL AND parentTaskId != ''")
    List<Task> getExpiredOccurrences(String userId, long cutoffTime);

    // Get ACTIVE one-time tasks past their expiry window
    @Query("SELECT * FROM tasks WHERE userId = :userId AND status = 'ACTIVE' AND dueDate < :cutoffTime AND isRecurring = 0")
    List<Task> getExpiredOneTimeTasks(String userId, long cutoffTime);

    // Statistics
    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId AND status = 'COMPLETED' AND categoryId = :categoryId")
    int countCompletedByCategory(String userId, String categoryId);

    // Check if any task (any status) references this category
    @Query("SELECT COUNT(*) FROM tasks WHERE categoryId = :categoryId")
    int countTasksByCategory(String categoryId);

    @Query("SELECT COALESCE(SUM(totalXp), 0) FROM tasks WHERE userId = :userId AND status = 'COMPLETED' AND completedDate >= :sinceDate")
    int getTotalXpSince(String userId, long sinceDate);

    // Get all completed tasks for statistics
    @Query("SELECT * FROM tasks WHERE userId = :userId AND status = 'COMPLETED' ORDER BY completedDate DESC")
    LiveData<List<Task>> getCompletedTasks(String userId);
}
