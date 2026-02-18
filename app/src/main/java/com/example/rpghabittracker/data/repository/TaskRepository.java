package com.example.rpghabittracker.data.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.rpghabittracker.data.local.AppDatabase;
import com.example.rpghabittracker.data.local.dao.TaskDao;
import com.example.rpghabittracker.data.model.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for Task data operations
 * Syncs data between local Room database and Firebase Firestore
 */
public class TaskRepository {

    private static final String TAG = "TaskRepository";
    private static final String COLLECTION_TASKS = "tasks";

    private final TaskDao taskDao;
    private final FirebaseFirestore firestore;
    private final ExecutorService executor;
    private ListenerRegistration tasksListener;

    // Quota limits per day
    public static final int QUOTA_VERY_EASY_NORMAL = 5;
    public static final int QUOTA_EASY_IMPORTANT = 5;
    public static final int QUOTA_HARD_VERY_IMPORTANT = 2;
    public static final int QUOTA_EXTREME_PER_WEEK = 1;
    public static final int QUOTA_SPECIAL_PER_MONTH = 1;

    public TaskRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        taskDao = db.taskDao();
        firestore = FirebaseFirestore.getInstance();
        executor = Executors.newSingleThreadExecutor();
    }

    // Start listening to Firestore changes for a user
    public void startListeningToTasks(String userId) {
        if (tasksListener != null) {
            tasksListener.remove();
        }

        tasksListener = firestore.collection(COLLECTION_TASKS)
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Firestore listen error", error);
                        return;
                    }

                    if (snapshots != null) {
                        executor.execute(() -> {
                            for (QueryDocumentSnapshot doc : snapshots) {
                                Task task = documentToTask(doc);
                                if (task != null) {
                                    taskDao.insert(task);
                                }
                            }
                        });
                    }
                });
    }

    public void stopListening() {
        if (tasksListener != null) {
            tasksListener.remove();
            tasksListener = null;
        }
    }

    // Basic CRUD with Firebase sync
    public void insert(Task task) {
        if (task.getId() == null || task.getId().isEmpty()) {
            task.setId(UUID.randomUUID().toString());
        }

        executor.execute(() -> {
            taskDao.insert(task);
            syncTaskToFirestore(task);
        });
    }

    public void update(Task task) {
        executor.execute(() -> {
            taskDao.update(task);
            syncTaskToFirestore(task);
        });
    }

    public void delete(Task task) {
        executor.execute(() -> {
            taskDao.delete(task);
            deleteFromFirestore(task.getId());
        });
    }

    // Sync task to Firestore
    private void syncTaskToFirestore(Task task) {
        Map<String, Object> taskData = taskToMap(task);

        firestore.collection(COLLECTION_TASKS)
                .document(task.getId())
                .set(taskData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Task synced to Firestore: " + task.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Error syncing task to Firestore", e));
    }

    private void deleteFromFirestore(String taskId) {
        firestore.collection(COLLECTION_TASKS)
                .document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Task deleted from Firestore: " + taskId))
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting task from Firestore", e));
    }

    // Convert Task to Map for Firestore
    private Map<String, Object> taskToMap(Task task) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", task.getId());
        map.put("userId", task.getUserId());
        map.put("name", task.getName());
        map.put("description", task.getDescription());
        map.put("categoryId", task.getCategoryId());
        map.put("difficulty", task.getDifficulty());
        map.put("importance", task.getImportance());
        map.put("difficultyXp", task.getDifficultyXp());
        map.put("importanceXp", task.getImportanceXp());
        map.put("totalXp", task.getTotalXp());
        map.put("status", task.getStatus());
        map.put("isRecurring", task.isRecurring());
        map.put("parentTaskId", task.getParentTaskId());
        map.put("repeatInterval", task.getRepeatInterval());
        map.put("repeatUnit", task.getRepeatUnit());
        map.put("startDate", task.getStartDate());
        map.put("dueDate", task.getDueDate());
        map.put("endDate", task.getEndDate());
        map.put("createdAt", task.getCreatedAt());
        map.put("completedDate", task.getCompletedDate());
        map.put("userLevelAtCreation", task.getUserLevelAtCreation());
        return map;
    }

    // Convert Firestore document to Task
    private Task documentToTask(DocumentSnapshot doc) {
        try {
            Task task = new Task();
            String taskId = doc.getString("id");
            if (taskId == null || taskId.trim().isEmpty()) {
                taskId = doc.getId();
            }
            task.setId(taskId);
            task.setUserId(doc.getString("userId"));
            task.setName(doc.getString("name"));
            task.setDescription(doc.getString("description"));
            task.setCategoryId(doc.getString("categoryId"));

            Long userLevel = doc.getLong("userLevelAtCreation");
            task.setUserLevelAtCreation(userLevel != null ? userLevel.intValue() : 1);

            task.setDifficulty(doc.getString("difficulty"));
            task.setImportance(doc.getString("importance"));

            Long diffXp = doc.getLong("difficultyXp");
            if (diffXp != null) task.setDifficultyXp(diffXp.intValue());

            Long impXp = doc.getLong("importanceXp");
            if (impXp != null) task.setImportanceXp(impXp.intValue());

            Long totalXp = doc.getLong("totalXp");
            if (totalXp != null) {
                task.setTotalXp(totalXp.intValue());
            } else {
                task.setTotalXp(task.getDifficultyXp() + task.getImportanceXp());
            }

            task.setStatus(doc.getString("status"));

            Boolean isRecurring = doc.getBoolean("isRecurring");
            task.setRecurring(isRecurring != null && isRecurring);

            task.setParentTaskId(doc.getString("parentTaskId"));

            Long repeatInterval = doc.getLong("repeatInterval");
            if (repeatInterval != null) task.setRepeatInterval(repeatInterval.intValue());

            task.setRepeatUnit(doc.getString("repeatUnit"));

            Long startDate = doc.getLong("startDate");
            if (startDate != null) task.setStartDate(startDate);

            Long dueDate = doc.getLong("dueDate");
            if (dueDate != null) task.setDueDate(dueDate);

            Long endDate = doc.getLong("endDate");
            if (endDate != null) task.setEndDate(endDate);

            Long createdAt = doc.getLong("createdAt");
            if (createdAt != null) task.setCreatedAt(createdAt);

            Long completedDate = doc.getLong("completedDate");
            if (completedDate != null) task.setCompletedDate(completedDate);

            return task;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to Task", e);
            return null;
        }
    }

    // Get tasks
    public LiveData<Task> getTaskById(String taskId) {
        return taskDao.getTaskById(taskId);
    }

    public void getTaskByIdSync(String taskId, TaskCallback callback) {
        executor.execute(() -> callback.onResult(taskDao.getTaskByIdSync(taskId)));
    }

    public LiveData<List<Task>> getUserTasks(String userId) {
        return taskDao.getUserTasks(userId);
    }

    public LiveData<List<Task>> getTasksByStatus(String userId, String status) {
        return taskDao.getTasksByStatus(userId, status);
    }

    public LiveData<List<Task>> getTodayTasks(String userId) {
        long[] dayBounds = getDayBounds(System.currentTimeMillis());
        return taskDao.getTodayTasks(userId, dayBounds[0], dayBounds[1]);
    }

    public LiveData<List<Task>> getOneTimeTasks(String userId) {
        return taskDao.getOneTimeTasks(userId);
    }

    public LiveData<List<Task>> getRecurringTemplates(String userId) {
        return taskDao.getRecurringTemplates(userId);
    }

    public LiveData<List<Task>> getRecurringOccurrences(String userId) {
        return taskDao.getRecurringOccurrences(userId);
    }

    public LiveData<List<Task>> getTasksByCategory(String userId, String categoryId) {
        return taskDao.getTasksByCategory(userId, categoryId);
    }

    public LiveData<List<Task>> getCompletedTasks(String userId) {
        return taskDao.getCompletedTasks(userId);
    }

    // Mark task complete
    public void markTaskComplete(String taskId, Runnable onSuccess) {
        executor.execute(() -> {
            long completedAt = System.currentTimeMillis();
            taskDao.markComplete(taskId, Task.STATUS_COMPLETED, completedAt);

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", Task.STATUS_COMPLETED);
            updates.put("completedDate", completedAt);

            firestore.collection(COLLECTION_TASKS)
                    .document(taskId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        if (onSuccess != null) onSuccess.run();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating task status in Firestore", e);
                        if (onSuccess != null) onSuccess.run();
                    });
        });
    }

    // Update status
    public void updateTaskStatus(String taskId, String status) {
        executor.execute(() -> {
            taskDao.updateStatus(taskId, status);

            firestore.collection(COLLECTION_TASKS)
                    .document(taskId)
                    .update("status", status)
                    .addOnFailureListener(e -> Log.e(TAG, "Error updating status in Firestore", e));
        });
    }

    // Pause all future active occurrences for a series AND the template
    public void pauseRecurringSeries(String templateId) {
        executor.execute(() -> {
            taskDao.updateStatus(templateId, Task.STATUS_PAUSED);
            firestore.collection(COLLECTION_TASKS)
                    .document(templateId)
                    .update("status", Task.STATUS_PAUSED)
                    .addOnFailureListener(e -> Log.e(TAG, "Error pausing series in Firestore", e));
        });
    }

    // Resume a paused series template
    public void resumeRecurringSeries(String templateId) {
        executor.execute(() -> {
            taskDao.updateStatus(templateId, Task.STATUS_ACTIVE);
            firestore.collection(COLLECTION_TASKS)
                    .document(templateId)
                    .update("status", Task.STATUS_ACTIVE)
                    .addOnFailureListener(e -> Log.e(TAG, "Error resuming series in Firestore", e));
        });
    }

    // Delete entire series: template + all future active occurrences
    public void deleteRecurringSeries(String templateId) {
        executor.execute(() -> {
            taskDao.deleteFutureOccurrences(templateId);
            taskDao.deleteById(templateId);
            // Delete from Firestore
            deleteFromFirestore(templateId);
            firestore.collection(COLLECTION_TASKS)
                    .whereEqualTo("parentTaskId", templateId)
                    .whereEqualTo("status", Task.STATUS_ACTIVE)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            doc.getReference().delete();
                        }
                    });
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Generate today's occurrence for every active recurring template
    // ──────────────────────────────────────────────────────────────────────────
    public void generateTodayOccurrences(String userId) {
        executor.execute(() -> {
            long[] dayBounds = getDayBounds(System.currentTimeMillis());
            long dayStart = dayBounds[0];
            long dayEnd = dayBounds[1];

            List<Task> templates = taskDao.getRecurringTemplatesSync(userId);

            for (Task template : templates) {
                // Only generate for active templates
                if (!Task.STATUS_ACTIVE.equals(template.getStatus())) continue;

                // Check overall date range
                if (template.getStartDate() > 0) {
                    long[] startDay = getDayBounds(template.getStartDate());
                    if (dayStart < startDay[0]) continue; // Not started yet
                }
                if (template.getEndDate() > 0 && dayStart > template.getEndDate()) continue;

                // Check if today falls on the recurrence schedule
                if (!shouldHaveOccurrenceToday(template, dayStart)) continue;

                // Skip if an occurrence already exists today
                if (taskDao.getTodayOccurrenceSync(template.getId(), dayStart, dayEnd) != null) continue;

                // Create and persist the occurrence
                Task occurrence = buildOccurrence(template, dayStart);
                taskDao.insert(occurrence);
                syncTaskToFirestore(occurrence);
                Log.d(TAG, "Generated occurrence for template " + template.getId() + " on day " + dayStart);
            }
        });
    }

    private Task buildOccurrence(Task template, long dayStart) {
        Task occ = new Task();
        occ.setUserId(template.getUserId());
        occ.setName(template.getName());
        occ.setDescription(template.getDescription());
        occ.setCategoryId(template.getCategoryId());
        occ.setDifficulty(template.getDifficulty());
        occ.setImportance(template.getImportance());
        occ.setDifficultyXp(template.getDifficultyXp());
        occ.setImportanceXp(template.getImportanceXp());
        occ.setTotalXp(template.getTotalXp());
        occ.setRecurring(true);
        occ.setParentTaskId(template.getId());
        occ.setRepeatInterval(template.getRepeatInterval());
        occ.setRepeatUnit(template.getRepeatUnit());
        occ.setStartDate(template.getStartDate());
        occ.setEndDate(template.getEndDate());
        occ.setUserLevelAtCreation(template.getUserLevelAtCreation());
        occ.setCountsTowardQuota(template.isCountsTowardQuota());

        // Preserve original time-of-day from the template's dueDate
        long timeOfDay = template.getDueDate() > 0
                ? (template.getDueDate() % (24L * 60 * 60 * 1000))
                : (9L * 60 * 60 * 1000); // Default 09:00
        occ.setDueDate(dayStart + timeOfDay);

        return occ;
    }

    /**
     * Returns true if the given dayStart (midnight epoch) is a scheduled day
     * for this recurring template.
     */
    private boolean shouldHaveOccurrenceToday(Task template, long dayStart) {
        // Anchor to the start date (or creation date)
        long anchor = template.getStartDate() > 0
                ? getDayBounds(template.getStartDate())[0]
                : getDayBounds(template.getCreatedAt())[0];

        if (dayStart < anchor) return false;

        int interval = Math.max(1, template.getRepeatInterval());
        long daysDiff = (dayStart - anchor) / (24L * 60 * 60 * 1000);

        if ("WEEK".equals(template.getRepeatUnit())) {
            // Must land on the same weekday as anchor AND every N weeks
            return (daysDiff % 7 == 0) && ((daysDiff / 7) % interval == 0);
        } else {
            // DAY unit: every N days
            return daysDiff % interval == 0;
        }
    }

    // Quota checking
    public interface QuotaCallback {
        void onResult(boolean canCreate, String message);
    }

    public interface TaskCallback {
        void onResult(Task task);
    }

    public void checkQuota(String userId, String difficulty, String importance, QuotaCallback callback) {
        executor.execute(() -> {
            long[] dayBounds = getDayBounds(System.currentTimeMillis());
            long dayStart = dayBounds[0];
            long dayEnd = dayBounds[1];

            boolean canCreate = true;
            String message = "";

            if (Task.DIFFICULTY_VERY_EASY.equals(difficulty)) {
                int count = taskDao.countCompletedByDifficultyToday(userId, difficulty, dayStart, dayEnd);
                if (count >= QUOTA_VERY_EASY_NORMAL) {
                    canCreate = false;
                    message = "Dnevni limit za veoma lake zadatke je dostignut (max " + QUOTA_VERY_EASY_NORMAL + ")";
                }
            } else if (Task.DIFFICULTY_EASY.equals(difficulty)) {
                int count = taskDao.countCompletedByDifficultyToday(userId, difficulty, dayStart, dayEnd);
                if (count >= QUOTA_EASY_IMPORTANT) {
                    canCreate = false;
                    message = "Dnevni limit za lake zadatke je dostignut (max " + QUOTA_EASY_IMPORTANT + ")";
                }
            } else if (Task.DIFFICULTY_HARD.equals(difficulty)) {
                int count = taskDao.countCompletedByDifficultyToday(userId, difficulty, dayStart, dayEnd);
                if (count >= QUOTA_HARD_VERY_IMPORTANT) {
                    canCreate = false;
                    message = "Dnevni limit za teške zadatke je dostignut (max " + QUOTA_HARD_VERY_IMPORTANT + ")";
                }
            } else if (Task.DIFFICULTY_EXTREME.equals(difficulty)) {
                long[] weekBounds = getWeekBounds(System.currentTimeMillis());
                int count = taskDao.countCompletedByDifficultyToday(userId, difficulty, weekBounds[0], weekBounds[1]);
                if (count >= QUOTA_EXTREME_PER_WEEK) {
                    canCreate = false;
                    message = "Nedeljni limit za ekstremno teške zadatke je dostignut (max " + QUOTA_EXTREME_PER_WEEK + ")";
                }
            }

            if (canCreate && Task.IMPORTANCE_SPECIAL.equals(importance)) {
                long[] monthBounds = getMonthBounds(System.currentTimeMillis());
                int count = taskDao.countCompletedByImportanceToday(userId, importance, monthBounds[0], monthBounds[1]);
                if (count >= QUOTA_SPECIAL_PER_MONTH) {
                    canCreate = false;
                    message = "Mesečni limit za specijalne zadatke je dostignut (max " + QUOTA_SPECIAL_PER_MONTH + ")";
                }
            }

            callback.onResult(canCreate, message);
        });
    }

    // Statistics
    public void countCompletedToday(String userId, CountCallback callback) {
        executor.execute(() -> {
            long[] dayBounds = getDayBounds(System.currentTimeMillis());
            int count = taskDao.countCompletedTasksForDate(userId, dayBounds[0], dayBounds[1]);
            callback.onResult(count);
        });
    }

    public interface CountCallback {
        void onResult(int count);
    }

    // Mark expired tasks (occurrences + one-time) as FAILED
    public void processExpiredTasks(String userId) {
        executor.execute(() -> {
            long threeDaysAgo = System.currentTimeMillis() - (3L * 24 * 60 * 60 * 1000);

            // Expire recurring occurrences
            List<Task> expiredOcc = taskDao.getExpiredOccurrences(userId, threeDaysAgo);
            for (Task task : expiredOcc) {
                taskDao.updateStatus(task.getId(), Task.STATUS_FAILED);
                firestore.collection(COLLECTION_TASKS)
                        .document(task.getId())
                        .update("status", Task.STATUS_FAILED);
            }

            // Expire one-time tasks
            List<Task> expiredOne = taskDao.getExpiredOneTimeTasks(userId, threeDaysAgo);
            for (Task task : expiredOne) {
                taskDao.updateStatus(task.getId(), Task.STATUS_FAILED);
                firestore.collection(COLLECTION_TASKS)
                        .document(task.getId())
                        .update("status", Task.STATUS_FAILED);
            }
        });
    }

    // Helper methods for date ranges
    private long[] getDayBounds(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long dayStart = calendar.getTimeInMillis();

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        long dayEnd = calendar.getTimeInMillis();

        return new long[]{dayStart, dayEnd};
    }

    private long[] getWeekBounds(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long weekStart = calendar.getTimeInMillis();

        calendar.add(Calendar.WEEK_OF_YEAR, 1);
        long weekEnd = calendar.getTimeInMillis();

        return new long[]{weekStart, weekEnd};
    }

    private long[] getMonthBounds(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long monthStart = calendar.getTimeInMillis();

        calendar.add(Calendar.MONTH, 1);
        long monthEnd = calendar.getTimeInMillis();

        return new long[]{monthStart, monthEnd};
    }
}
