package com.example.rpghabittracker.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.rpghabittracker.data.model.Task;
import com.example.rpghabittracker.data.repository.TaskRepository;

import java.util.List;

/**
 * ViewModel for Task-related UI operations
 */
public class TaskViewModel extends AndroidViewModel {

    private final TaskRepository repository;
    private final MutableLiveData<String> currentUserId = new MutableLiveData<>();
    private final MutableLiveData<String> filterStatus = new MutableLiveData<>("ALL");
    private final MutableLiveData<Boolean> showRecurring = new MutableLiveData<>(false);

    private LiveData<List<Task>> allTasks;
    private LiveData<List<Task>> oneTimeTasks;
    private LiveData<List<Task>> recurringOccurrences;
    private LiveData<List<Task>> recurringTemplates;

    public TaskViewModel(@NonNull Application application) {
        super(application);
        repository = new TaskRepository(application);
    }

    public void setUserId(String userId) {
        currentUserId.setValue(userId);
        repository.startListeningToTasks(userId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.stopListening();
    }

    // Get all tasks for user (templates + occurrences + one-time)
    public LiveData<List<Task>> getAllTasks() {
        if (allTasks == null) {
            allTasks = Transformations.switchMap(currentUserId, userId -> {
                if (userId == null) return new MutableLiveData<>();
                return repository.getUserTasks(userId);
            });
        }
        return allTasks;
    }

    // Get one-time tasks only
    public LiveData<List<Task>> getOneTimeTasks() {
        if (oneTimeTasks == null) {
            oneTimeTasks = Transformations.switchMap(currentUserId, userId -> {
                if (userId == null) return new MutableLiveData<>();
                return repository.getOneTimeTasks(userId);
            });
        }
        return oneTimeTasks;
    }

    // Get recurring OCCURRENCES (the daily instances users interact with)
    public LiveData<List<Task>> getRecurringOccurrences() {
        if (recurringOccurrences == null) {
            recurringOccurrences = Transformations.switchMap(currentUserId, userId -> {
                if (userId == null) return new MutableLiveData<>();
                return repository.getRecurringOccurrences(userId);
            });
        }
        return recurringOccurrences;
    }

    // Get recurring TEMPLATES (the parent definition rows)
    public LiveData<List<Task>> getRecurringTemplates() {
        if (recurringTemplates == null) {
            recurringTemplates = Transformations.switchMap(currentUserId, userId -> {
                if (userId == null) return new MutableLiveData<>();
                return repository.getRecurringTemplates(userId);
            });
        }
        return recurringTemplates;
    }

    // Get single task by ID
    public LiveData<Task> getTaskById(String taskId) {
        return repository.getTaskById(taskId);
    }

    public void getTaskByIdSync(String taskId, TaskRepository.TaskCallback callback) {
        repository.getTaskByIdSync(taskId, callback);
    }

    // Delete task
    public void deleteTask(Task task) {
        repository.delete(task);
    }

    // Delete entire recurring series (template + future occurrences)
    public void deleteRecurringSeries(String templateId) {
        repository.deleteRecurringSeries(templateId);
    }

    // Complete task by ID with callback
    public void completeTask(String taskId, Runnable onSuccess) {
        repository.markTaskComplete(taskId, onSuccess);
    }

    // Get tasks by status
    public LiveData<List<Task>> getTasksByStatus(String status) {
        return Transformations.switchMap(currentUserId, userId -> {
            if (userId == null) return new MutableLiveData<>();
            return repository.getTasksByStatus(userId, status);
        });
    }

    // Get today's tasks
    public LiveData<List<Task>> getTodayTasks() {
        return Transformations.switchMap(currentUserId, userId -> {
            if (userId == null) return new MutableLiveData<>();
            return repository.getTodayTasks(userId);
        });
    }

    // Get completed tasks
    public LiveData<List<Task>> getCompletedTasks() {
        return Transformations.switchMap(currentUserId, userId -> {
            if (userId == null) return new MutableLiveData<>();
            return repository.getCompletedTasks(userId);
        });
    }

    // Insert new task
    public void insert(Task task) {
        repository.insert(task);
    }

    // Update task
    public void update(Task task) {
        repository.update(task);
    }

    // Delete task
    public void delete(Task task) {
        repository.delete(task);
    }

    // Mark task as complete
    public void completeTask(Task task, Runnable onSuccess) {
        repository.markTaskComplete(task.getId(), onSuccess);
    }

    // Update task status
    public void updateStatus(String taskId, String status) {
        repository.updateTaskStatus(taskId, status);
    }

    // Pause an entire recurring series (by template ID)
    public void pauseRecurringSeries(String templateId) {
        repository.pauseRecurringSeries(templateId);
    }

    // Resume a paused recurring series
    public void resumeRecurringSeries(String templateId) {
        repository.resumeRecurringSeries(templateId);
    }

    // Generate today's occurrences for all active recurring templates
    public void generateTodayOccurrences() {
        String userId = currentUserId.getValue();
        if (userId != null) {
            repository.generateTodayOccurrences(userId);
        }
    }

    // Check quota before completing a task
    public void checkQuota(String difficulty, String importance, TaskRepository.QuotaCallback callback) {
        String userId = currentUserId.getValue();
        if (userId != null) {
            repository.checkQuota(userId, difficulty, importance, callback);
        } else {
            callback.onResult(false, "User not logged in");
        }
    }

    // Process expired tasks
    public void processExpiredTasks() {
        String userId = currentUserId.getValue();
        if (userId != null) {
            repository.processExpiredTasks(userId);
        }
    }

    // Filter controls
    public void setFilterStatus(String status) {
        filterStatus.setValue(status);
    }

    public LiveData<String> getFilterStatus() {
        return filterStatus;
    }

    public void setShowRecurring(boolean show) {
        showRecurring.setValue(show);
    }

    public LiveData<Boolean> getShowRecurring() {
        return showRecurring;
    }
}
