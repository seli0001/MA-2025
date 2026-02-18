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
import com.example.rpghabittracker.utils.AllianceMissionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Activity for viewing task details and performing actions.
 *
 * Buttons:
 *  buttonComplete  - mark as done (active tasks only)
 *  buttonEdit      - for recurring occurrences: Pause/Resume series
 *                    for one-time tasks: open edit screen
 *  buttonDelete    - cancel task (active) / delete (non-active one-time)
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

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd. MMM yyyy.", new Locale("sr"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);

        taskId = getIntent().getStringExtra(EXTRA_TASK_ID);
        if (taskId == null) {
            Toast.makeText(this, "Greska: Zadatak nije pronadjen", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) { finish(); return; }
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

        boolean isOccurrence = task.isRecurring()
                && task.getParentTaskId() != null
                && !task.getParentTaskId().isEmpty();
        boolean isActive = Task.STATUS_ACTIVE.equals(task.getStatus());

        // Status label
        switch (task.getStatus() != null ? task.getStatus() : "") {
            case Task.STATUS_COMPLETED:
                textStatus.setText("Zavrseno");
                textStatus.setTextColor(getColor(R.color.status_completed));
                break;
            case Task.STATUS_FAILED:
                textStatus.setText("Neuspesno");
                textStatus.setTextColor(getColor(R.color.status_failed));
                break;
            case Task.STATUS_CANCELLED:
                textStatus.setText("Otkazano");
                textStatus.setTextColor(getColor(R.color.status_cancelled));
                break;
            case Task.STATUS_PAUSED:
                textStatus.setText("Pauzirano");
                textStatus.setTextColor(getColor(R.color.status_paused));
                break;
            default:
                textStatus.setText("Aktivan");
                textStatus.setTextColor(getColor(R.color.status_active));
                break;
        }

        // Complete button
        buttonComplete.setVisibility(isActive ? View.VISIBLE : View.GONE);
        buttonComplete.setOnClickListener(v -> completeTask());

        // Edit / Pause-Resume button
        if (isOccurrence) {
            buttonEdit.setVisibility(View.VISIBLE);
            // Show pause or resume depending on series template status
            taskViewModel.getTaskByIdSync(task.getParentTaskId(), template -> runOnUiThread(() -> {
                if (template == null) return;
                if (Task.STATUS_PAUSED.equals(template.getStatus())) {
                    buttonEdit.setText("Nastavi seriju");
                    buttonEdit.setOnClickListener(v -> {
                        taskViewModel.resumeRecurringSeries(task.getParentTaskId());
                        Toast.makeText(this, "Serija nastavljena", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    buttonEdit.setText("Pauziraj seriju");
                    buttonEdit.setOnClickListener(v -> {
                        taskViewModel.pauseRecurringSeries(task.getParentTaskId());
                        Toast.makeText(this, "Serija pauzirana", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }));
        } else {
            buttonEdit.setText("Izmeni");
            buttonEdit.setVisibility(isActive ? View.VISIBLE : View.GONE);
            buttonEdit.setOnClickListener(v -> editTask());
        }

        // Delete / Cancel button
        if (isActive) {
            buttonDelete.setText("Otkazi zadatak");
            buttonDelete.setVisibility(View.VISIBLE);
            buttonDelete.setOnClickListener(v -> confirmCancel());
        } else if (!isOccurrence && !Task.STATUS_FAILED.equals(task.getStatus())) {
            // Completed / cancelled one-time tasks can be deleted
            buttonDelete.setText("Obrisi");
            buttonDelete.setVisibility(View.VISIBLE);
            buttonDelete.setOnClickListener(v -> confirmDelete());
        } else {
            buttonDelete.setVisibility(View.GONE);
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
            categoryViewModel.getCategoryByIdSync(task.getCategoryId(), category -> runOnUiThread(() -> {
                if (category != null) {
                    textCategory.setText(category.getName());
                    try {
                        GradientDrawable bg = (GradientDrawable) categoryColorIndicator.getBackground();
                        bg.setColor(Color.parseColor(category.getColor()));
                    } catch (Exception ignored) {}
                } else {
                    textCategory.setText("Bez kategorije");
                }
            }));
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
        textDueDate.setText(task.getDueDate() > 0
                ? dateFormat.format(new Date(task.getDueDate()))
                : "Nije postavljeno");

        if (task.getCreatedAt() > 0) {
            textCreatedAt.setText(dateFormat.format(new Date(task.getCreatedAt())));
        }

        // Recurring info
        if (task.isRecurring()) {
            layoutRecurring.setVisibility(View.VISIBLE);
            textRecurring.setText(getRecurringText(task.getRepeatInterval(), task.getRepeatUnit()));
        } else {
            layoutRecurring.setVisibility(View.GONE);
        }
    }

    private void completeTask() {
        if (currentTask == null) return;

        taskViewModel.completeTask(taskId, () -> {
            int xp = currentTask.getTotalXp();
            AllianceMissionManager.recordTaskCompletion(
                    FirebaseFirestore.getInstance(),
                    userId,
                    currentTask,
                    null
            );
            userViewModel.addXp(xp, (success, xpGained, leveledUp, user) -> runOnUiThread(() -> {
                if (leveledUp && user != null) {
                    showLevelUpDialog(user.getLevel());
                } else {
                    Toast.makeText(this, "+" + xp + " XP", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }));
        });
    }

    private void showLevelUpDialog(int newLevel) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Level Up!")
                .setMessage("Cestitamo! Dostigli ste level " + newLevel + "!")
                .setPositiveButton("Super!", (d, w) -> finish())
                .show();
    }

    private void editTask() {
        if (currentTask == null) return;
        Intent intent = new Intent(this, AddTaskActivity.class);
        intent.putExtra(AddTaskActivity.EXTRA_TASK_ID, taskId);
        intent.putExtra(AddTaskActivity.EXTRA_IS_RECURRING, currentTask.isRecurring());
        startActivity(intent);
    }

    private void confirmCancel() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Otkazi zadatak")
                .setMessage("Da li ste sigurni? Otkazivanje znaci da zadatak nije uradjen ne vasoj krivici.")
                .setPositiveButton("Otkazi zadatak", (dialog, which) -> {
                    taskViewModel.updateStatus(taskId, Task.STATUS_CANCELLED);
                    Toast.makeText(this, "Zadatak otkazan", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Nazad", null)
                .show();
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Obrisi zadatak")
                .setMessage("Da li ste sigurni da zelite da obrisete ovaj zadatak?")
                .setPositiveButton("Obrisi", (dialog, which) -> {
                    if (currentTask != null) {
                        taskViewModel.deleteTask(currentTask);
                        Toast.makeText(this, "Zadatak obrisan", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .setNegativeButton("Otkazi", null)
                .show();
    }

    private String getDifficultyLabel(String difficulty) {
        if (difficulty == null) return "Normalan";
        switch (difficulty) {
            case "VERY_EASY": return "Veoma lako";
            case "EASY": return "Lako";
            case "HARD": return "Tesko";
            case "EXTREME": return "Ekstremno tesko";
            default: return difficulty;
        }
    }

    private String getImportanceLabel(String importance) {
        if (importance == null) return "Normalno";
        switch (importance) {
            case "NORMAL": return "Normalno";
            case "IMPORTANT": return "Vazno";
            case "VERY_IMPORTANT": return "Veoma vazno";
            case "SPECIAL": return "Specijalno";
            default: return importance;
        }
    }

    private String getRecurringText(int interval, String unit) {
        if (unit == null) return "Svaki dan";
        if ("WEEK".equals(unit)) {
            return interval == 1 ? "Svake nedelje" : "Svake " + interval + " nedelje";
        }
        return interval == 1 ? "Svaki dan" : "Svakih " + interval + " dana";
    }
}
