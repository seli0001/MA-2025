package com.example.rpghabittracker.ui.tasks;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.rpghabittracker.R;
import com.example.rpghabittracker.data.model.Task;
import com.example.rpghabittracker.ui.viewmodel.CategoryViewModel;
import com.example.rpghabittracker.ui.viewmodel.TaskViewModel;
import com.example.rpghabittracker.ui.viewmodel.UserViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Activity for viewing task details and performing actions
 */
public class TaskDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_TASK_ID = "task_id";
    
    private TaskViewModel taskViewModel;
    private UserViewModel userViewModel;
    private CategoryViewModel categoryViewModel;
    
    private String taskId;
    private Task currentTask;
    private String userId;
    
    // Views
    private TextView textStatus, textTaskName, textCategory, textDescription;
    private View categoryColorIndicator;
    private TextView textDifficultyXp, textDifficulty;
    private TextView textImportanceXp, textImportance;
    private TextView textTotalXp;
    private TextView textDueDate, textRecurring, textCreatedAt;
    private LinearLayout layoutRecurring;
    private MaterialButton buttonComplete, buttonEdit, buttonDelete;
    
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd. MMM yyyy.", new Locale("sr"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);
        
        // Get task ID from intent
        taskId = getIntent().getStringExtra(EXTRA_TASK_ID);
        if (taskId == null) {
            Toast.makeText(this, "Greška: Zadatak nije pronađen", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }
        userId = currentUser.getUid();
        
        initViews();
        setupViewModels();
        loadTask();
    }
    
    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        textStatus = findViewById(R.id.textStatus);
        textTaskName = findViewById(R.id.textTaskName);
        textCategory = findViewById(R.id.textCategory);
        textDescription = findViewById(R.id.textDescription);
        categoryColorIndicator = findViewById(R.id.categoryColorIndicator);
        
        textDifficultyXp = findViewById(R.id.textDifficultyXp);
        textDifficulty = findViewById(R.id.textDifficulty);
        textImportanceXp = findViewById(R.id.textImportanceXp);
        textImportance = findViewById(R.id.textImportance);
        textTotalXp = findViewById(R.id.textTotalXp);
        
        textDueDate = findViewById(R.id.textDueDate);
        textRecurring = findViewById(R.id.textRecurring);
        layoutRecurring = findViewById(R.id.layoutRecurring);
        textCreatedAt = findViewById(R.id.textCreatedAt);
        
        buttonComplete = findViewById(R.id.buttonComplete);
        buttonEdit = findViewById(R.id.buttonEdit);
        buttonDelete = findViewById(R.id.buttonDelete);
        
        buttonComplete.setOnClickListener(v -> completeTask());
        buttonEdit.setOnClickListener(v -> editTask());
        buttonDelete.setOnClickListener(v -> confirmDelete());
    }
    
    private void setupViewModels() {
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        
        taskViewModel.setUserId(userId);
        userViewModel.setUserId(userId);
    }
    
    private void loadTask() {
        taskViewModel.getTaskById(taskId).observe(this, task -> {
            if (task != null) {
                currentTask = task;
                displayTask(task);
            }
        });
    }
    
    private void displayTask(Task task) {
        textTaskName.setText(task.getName());
        
        // Status
        String status = task.getStatus();
        if (status != null) {
            switch (status) {
                case Task.STATUS_COMPLETED:
                    textStatus.setText("Završeno");
                    textStatus.setTextColor(getColor(R.color.status_completed));
                    buttonComplete.setVisibility(View.GONE);
                    break;
                case Task.STATUS_FAILED:
                    textStatus.setText("Neuspešno");
                    textStatus.setTextColor(getColor(R.color.status_failed));
                    buttonComplete.setVisibility(View.GONE);
                    break;
                default:
                    textStatus.setText("Aktivan");
                    textStatus.setTextColor(getColor(R.color.status_active));
                    buttonComplete.setVisibility(View.VISIBLE);
                    break;
            }
        }
        
        // Description
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            textDescription.setText(task.getDescription());
            textDescription.setVisibility(View.VISIBLE);
        } else {
            textDescription.setVisibility(View.GONE);
        }
        
        // Category
        if (task.getCategoryId() != null) {
            categoryViewModel.getCategoryByIdSync(task.getCategoryId(), category -> {
                runOnUiThread(() -> {
                    if (category != null) {
                        textCategory.setText(category.getName());
                        try {
                            GradientDrawable bg = (GradientDrawable) categoryColorIndicator.getBackground();
                            bg.setColor(Color.parseColor(category.getColor()));
                        } catch (Exception e) {
                            // Ignore color parsing errors
                        }
                    } else {
                        textCategory.setText("Bez kategorije");
                    }
                });
            });
        } else {
            textCategory.setText("Bez kategorije");
        }
        
        // XP info
        textDifficultyXp.setText("+" + task.getDifficultyXp());
        textDifficulty.setText(getDifficultyLabel(task.getDifficulty()));
        
        textImportanceXp.setText("+" + task.getImportanceXp());
        textImportance.setText(getImportanceLabel(task.getImportance()));
        
        textTotalXp.setText("+" + task.getTotalXp());
        
        // Dates
        if (task.getDueDate() > 0) {
            textDueDate.setText(dateFormat.format(new Date(task.getDueDate())));
        } else {
            textDueDate.setText("Nije postavljeno");
        }
        
        if (task.getCreatedAt() > 0) {
            textCreatedAt.setText(dateFormat.format(new Date(task.getCreatedAt())));
        }
        
        // Recurring
        if (task.isRecurring()) {
            layoutRecurring.setVisibility(View.VISIBLE);
            String recurringText = getRecurringText(task.getRepeatInterval(), task.getRepeatUnit());
            textRecurring.setText(recurringText);
        } else {
            layoutRecurring.setVisibility(View.GONE);
        }
    }
    
    private String getDifficultyLabel(String difficulty) {
        if (difficulty == null) return "Normalan";
        switch (difficulty) {
            case "VERY_EASY": return "Veoma lako";
            case "EASY": return "Lako";
            case "NORMAL": return "Normalno";
            case "HARD": return "Teško";
            case "EXTREME": return "Ekstremno";
            case "SPECIAL": return "Specijalno";
            default: return "Normalno";
        }
    }
    
    private String getImportanceLabel(String importance) {
        if (importance == null) return "Normalno";
        switch (importance) {
            case "TRIVIAL": return "Trivijalno";
            case "NORMAL": return "Normalno";
            case "IMPORTANT": return "Važno";
            case "VERY_IMPORTANT": return "Veoma važno";
            default: return "Normalno";
        }
    }
    
    private String getRecurringText(int interval, String unit) {
        if (unit == null) return "Svaki dan";
        
        String unitText;
        if ("WEEK".equals(unit)) {
            unitText = interval == 1 ? "nedelju" : interval + " nedelje";
        } else {
            unitText = interval == 1 ? "dan" : interval + " dana";
        }
        return "Svaki " + unitText;
    }
    
    private void completeTask() {
        if (currentTask == null) return;
        
        taskViewModel.completeTask(taskId, () -> {
            // Award XP
            int xp = currentTask.getTotalXp();
            userViewModel.addXp(xp, (success, xpGained, leveledUp, user) -> {
                runOnUiThread(() -> {
                    if (leveledUp && user != null) {
                        showLevelUpDialog(user.getLevel());
                    } else {
                        Toast.makeText(this, "+" + xp + " XP", Toast.LENGTH_SHORT).show();
                    }
                    finish();
                });
            });
        });
    }
    
    private void showLevelUpDialog(int newLevel) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("🎉 Level Up!")
                .setMessage("Čestitamo! Dostigli ste level " + newLevel + "!")
                .setPositiveButton("Super!", null)
                .show();
    }
    
    private void editTask() {
        if (currentTask == null) return;
        
        Intent intent = new Intent(this, AddTaskActivity.class);
        intent.putExtra(AddTaskActivity.EXTRA_TASK_ID, taskId);
        intent.putExtra(AddTaskActivity.EXTRA_IS_RECURRING, currentTask.isRecurring());
        startActivity(intent);
    }
    
    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Obriši zadatak")
                .setMessage("Da li ste sigurni da želite da obrišete ovaj zadatak?")
                .setPositiveButton("Obriši", (dialog, which) -> {
                    if (currentTask != null) {
                        taskViewModel.deleteTask(currentTask);
                        Toast.makeText(this, "Zadatak obrisan", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }
}
