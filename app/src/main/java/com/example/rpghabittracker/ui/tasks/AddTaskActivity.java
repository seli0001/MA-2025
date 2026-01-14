package com.example.rpghabittracker.ui.tasks;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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
import com.example.rpghabittracker.data.model.Category;
import com.example.rpghabittracker.data.model.Task;
import com.example.rpghabittracker.data.model.User;
import com.example.rpghabittracker.ui.viewmodel.CategoryViewModel;
import com.example.rpghabittracker.ui.viewmodel.TaskViewModel;
import com.example.rpghabittracker.ui.viewmodel.UserViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity for creating and editing tasks
 */
public class AddTaskActivity extends AppCompatActivity {
    
    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_IS_RECURRING = "isRecurring";
    
    private MaterialToolbar toolbar;
    private TextInputLayout layoutTaskName, layoutDescription;
    private TextInputEditText editTaskName, editDescription, editRepeatInterval;
    private ChipGroup chipGroupDifficulty, chipGroupImportance, chipGroupRepeatUnit;
    private Chip chipVeryEasy, chipEasy, chipHard, chipExtreme;
    private Chip chipNormal, chipImportant, chipVeryImportant, chipSpecial;
    private Chip chipDay, chipWeek;
    private TextView textXpPreview, textDueDate, textEndDate, textCategory;
    private MaterialCardView cardDueDate, cardEndDate, cardXpPreview, cardCategory;
    private View viewCategoryColor;
    private LinearLayout layoutRecurring;
    private MaterialButton buttonSave;
    
    private TaskViewModel viewModel;
    private UserViewModel userViewModel;
    private CategoryViewModel categoryViewModel;
    private boolean isRecurring = false;
    private boolean isEditMode = false;
    private String editTaskId = null;
    private Task existingTask = null;
    private long selectedDueDate = 0;
    private long selectedEndDate = 0;
    private int userLevel = 1;
    
    private String selectedDifficulty = Task.DIFFICULTY_EASY;
    private String selectedImportance = Task.IMPORTANCE_NORMAL;
    private String selectedCategoryId = null;
    private List<Category> availableCategories = new ArrayList<>();
    
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);
        
        isRecurring = getIntent().getBooleanExtra(EXTRA_IS_RECURRING, false);
        editTaskId = getIntent().getStringExtra(EXTRA_TASK_ID);
        isEditMode = editTaskId != null;
        
        initViews();
        setupToolbar();
        setupViewModel();
        setupCategorySelector();
        setupDifficultyChips();
        setupImportanceChips();
        setupDatePickers();
        setupRecurringOptions();
        setupSaveButton();
        
        updateXpPreview();
    }
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        layoutTaskName = findViewById(R.id.layoutTaskName);
        layoutDescription = findViewById(R.id.layoutDescription);
        editTaskName = findViewById(R.id.editTaskName);
        editDescription = findViewById(R.id.editDescription);
        editRepeatInterval = findViewById(R.id.editRepeatInterval);
        
        chipGroupDifficulty = findViewById(R.id.chipGroupDifficulty);
        chipGroupImportance = findViewById(R.id.chipGroupImportance);
        chipGroupRepeatUnit = findViewById(R.id.chipGroupRepeatUnit);
        
        chipVeryEasy = findViewById(R.id.chipVeryEasy);
        chipEasy = findViewById(R.id.chipEasy);
        chipHard = findViewById(R.id.chipHard);
        chipExtreme = findViewById(R.id.chipExtreme);
        
        chipNormal = findViewById(R.id.chipNormal);
        chipImportant = findViewById(R.id.chipImportant);
        chipVeryImportant = findViewById(R.id.chipVeryImportant);
        chipSpecial = findViewById(R.id.chipSpecial);
        
        chipDay = findViewById(R.id.chipDay);
        chipWeek = findViewById(R.id.chipWeek);
        
        textXpPreview = findViewById(R.id.textXpPreview);
        textDueDate = findViewById(R.id.textDueDate);
        textEndDate = findViewById(R.id.textEndDate);
        textCategory = findViewById(R.id.textCategory);
        
        cardDueDate = findViewById(R.id.cardDueDate);
        cardEndDate = findViewById(R.id.cardEndDate);
        cardXpPreview = findViewById(R.id.cardXpPreview);
        cardCategory = findViewById(R.id.cardCategory);
        viewCategoryColor = findViewById(R.id.viewCategoryColor);
        
        layoutRecurring = findViewById(R.id.layoutRecurring);
        buttonSave = findViewById(R.id.buttonSave);
    }
    
    private void setupToolbar() {
        if (isEditMode) {
            toolbar.setTitle(isRecurring ? "Uredi ponavljajući zadatak" : "Uredi zadatak");
        } else {
            toolbar.setTitle(isRecurring ? "Novi ponavljajući zadatak" : "Novi zadatak");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            viewModel.setUserId(user.getUid());
            userViewModel.setUserId(user.getUid());
            categoryViewModel.setUserId(user.getUid());
            
            // Get actual user level from database
            userViewModel.getUserLevel(level -> {
                userLevel = level;
                runOnUiThread(this::updateXpPreview);
            });
            
            // Load categories
            categoryViewModel.getUserCategories().observe(this, categories -> {
                if (categories != null) {
                    availableCategories = categories;
                    // Load task data after categories are available (for edit mode)
                    if (isEditMode && existingTask == null) {
                        loadTaskForEditing();
                    }
                }
            });
        }
    }
    
    private void loadTaskForEditing() {
        if (editTaskId == null) return;
        
        viewModel.getTaskById(editTaskId).observe(this, task -> {
            if (task != null && existingTask == null) {
                existingTask = task;
                populateFormWithTask(task);
            }
        });
    }
    
    private void populateFormWithTask(Task task) {
        // Basic info
        editTaskName.setText(task.getName());
        editDescription.setText(task.getDescription());
        
        // Difficulty
        selectedDifficulty = task.getDifficulty();
        selectDifficultyChip(selectedDifficulty);
        
        // Importance
        selectedImportance = task.getImportance();
        selectImportanceChip(selectedImportance);
        
        // Category
        selectedCategoryId = task.getCategoryId();
        updateCategoryDisplay();
        
        // Due date
        if (task.getDueDate() > 0) {
            selectedDueDate = task.getDueDate();
            textDueDate.setText(dateTimeFormat.format(new Date(selectedDueDate)));
        }
        
        // Recurring options
        isRecurring = task.isRecurring();
        if (isRecurring) {
            layoutRecurring.setVisibility(View.VISIBLE);
            editRepeatInterval.setText(String.valueOf(task.getRepeatInterval()));
            
            if ("WEEK".equals(task.getRepeatUnit())) {
                chipWeek.setChecked(true);
            } else {
                chipDay.setChecked(true);
            }
            
            if (task.getEndDate() > 0) {
                selectedEndDate = task.getEndDate();
                textEndDate.setText(dateFormat.format(new Date(selectedEndDate)));
            }
        }
        
        // Update button text
        buttonSave.setText("Sačuvaj izmene");
        
        updateXpPreview();
    }
    
    private void selectDifficultyChip(String difficulty) {
        chipVeryEasy.setChecked(Task.DIFFICULTY_VERY_EASY.equals(difficulty));
        chipEasy.setChecked(Task.DIFFICULTY_EASY.equals(difficulty));
        chipHard.setChecked(Task.DIFFICULTY_HARD.equals(difficulty));
        chipExtreme.setChecked(Task.DIFFICULTY_EXTREME.equals(difficulty));
    }
    
    private void selectImportanceChip(String importance) {
        chipNormal.setChecked(Task.IMPORTANCE_NORMAL.equals(importance));
        chipImportant.setChecked(Task.IMPORTANCE_IMPORTANT.equals(importance));
        chipVeryImportant.setChecked(Task.IMPORTANCE_VERY_IMPORTANT.equals(importance));
        chipSpecial.setChecked(Task.IMPORTANCE_SPECIAL.equals(importance));
    }
    
    private void updateCategoryDisplay() {
        if (selectedCategoryId != null && !availableCategories.isEmpty()) {
            for (Category cat : availableCategories) {
                if (cat.getId().equals(selectedCategoryId)) {
                    textCategory.setText(cat.getName());
                    try {
                        GradientDrawable bg = (GradientDrawable) viewCategoryColor.getBackground();
                        bg.setColor(Color.parseColor(cat.getColor()));
                    } catch (Exception e) {
                        // Ignore
                    }
                    break;
                }
            }
        }
    }
    
    private void setupCategorySelector() {
        cardCategory.setOnClickListener(v -> showCategoryDialog());
    }
    
    private void showCategoryDialog() {
        if (availableCategories.isEmpty()) {
            Toast.makeText(this, "Nema dostupnih kategorija", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] categoryNames = new String[availableCategories.size()];
        for (int i = 0; i < availableCategories.size(); i++) {
            categoryNames[i] = availableCategories.get(i).getName();
        }
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Izaberi kategoriju")
                .setItems(categoryNames, (dialog, which) -> {
                    Category selected = availableCategories.get(which);
                    selectedCategoryId = selected.getId();
                    textCategory.setText(selected.getName());
                    textCategory.setTextColor(getColor(R.color.text_primary));
                    
                    // Update color indicator
                    try {
                        int color = Color.parseColor(selected.getColor());
                        GradientDrawable drawable = (GradientDrawable) viewCategoryColor.getBackground();
                        drawable.setColor(color);
                    } catch (Exception e) {
                        // Ignore color parsing errors
                    }
                })
                .show();
    }
    
    private void setupDifficultyChips() {
        chipEasy.setChecked(true);
        
        chipGroupDifficulty.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipVeryEasy)) {
                selectedDifficulty = Task.DIFFICULTY_VERY_EASY;
            } else if (checkedIds.contains(R.id.chipEasy)) {
                selectedDifficulty = Task.DIFFICULTY_EASY;
            } else if (checkedIds.contains(R.id.chipHard)) {
                selectedDifficulty = Task.DIFFICULTY_HARD;
            } else if (checkedIds.contains(R.id.chipExtreme)) {
                selectedDifficulty = Task.DIFFICULTY_EXTREME;
            }
            updateXpPreview();
        });
    }
    
    private void setupImportanceChips() {
        chipNormal.setChecked(true);
        
        chipGroupImportance.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipNormal)) {
                selectedImportance = Task.IMPORTANCE_NORMAL;
            } else if (checkedIds.contains(R.id.chipImportant)) {
                selectedImportance = Task.IMPORTANCE_IMPORTANT;
            } else if (checkedIds.contains(R.id.chipVeryImportant)) {
                selectedImportance = Task.IMPORTANCE_VERY_IMPORTANT;
            } else if (checkedIds.contains(R.id.chipSpecial)) {
                selectedImportance = Task.IMPORTANCE_SPECIAL;
            }
            updateXpPreview();
        });
    }
    
    private void setupDatePickers() {
        cardDueDate.setOnClickListener(v -> showDateTimePicker(true));
        cardEndDate.setOnClickListener(v -> showDateTimePicker(false));
    }
    
    private void setupRecurringOptions() {
        if (isRecurring) {
            layoutRecurring.setVisibility(View.VISIBLE);
            buttonSave.setText("Create Recurring Task");
        }
    }
    
    private void setupSaveButton() {
        buttonSave.setOnClickListener(v -> saveTask());
    }
    
    private void showDateTimePicker(boolean isDueDate) {
        Calendar calendar = Calendar.getInstance();
        
        if (isDueDate && selectedDueDate > 0) {
            calendar.setTimeInMillis(selectedDueDate);
        } else if (!isDueDate && selectedEndDate > 0) {
            calendar.setTimeInMillis(selectedEndDate);
        }
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                R.style.DatePickerTheme,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    
                    // Show time picker for due date
                    if (isDueDate) {
                        showTimePicker(calendar, true);
                    } else {
                        selectedEndDate = calendar.getTimeInMillis();
                        textEndDate.setText(dateFormat.format(new Date(selectedEndDate)));
                        textEndDate.setTextColor(getColor(R.color.text_primary));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }
    
    private void showTimePicker(Calendar calendar, boolean isDueDate) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                R.style.TimePickerTheme,
                (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    
                    if (isDueDate) {
                        selectedDueDate = calendar.getTimeInMillis();
                        textDueDate.setText(dateTimeFormat.format(new Date(selectedDueDate)));
                        textDueDate.setTextColor(getColor(R.color.text_primary));
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
        );
        timePickerDialog.show();
    }
    
    private void updateXpPreview() {
        int difficultyXp = Task.getDifficultyXpForLevel(selectedDifficulty, userLevel);
        int importanceXp = Task.getImportanceXpForLevel(selectedImportance, userLevel);
        int totalXp = difficultyXp + importanceXp;
        
        textXpPreview.setText("+" + totalXp);
    }
    
    private void saveTask() {
        String name = editTaskName.getText() != null ? editTaskName.getText().toString().trim() : "";
        String description = editDescription.getText() != null ? editDescription.getText().toString().trim() : "";
        
        // Validation
        if (name.isEmpty()) {
            layoutTaskName.setError("Naziv zadatka je obavezan");
            editTaskName.requestFocus();
            return;
        }
        layoutTaskName.setError(null);
        
        if (isEditMode) {
            updateTask(name, description);
        } else {
            // Check quota only for new tasks
            viewModel.checkQuota(selectedDifficulty, selectedImportance, (canCreate, message) -> {
                runOnUiThread(() -> {
                    if (!canCreate) {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        return;
                    }
                    
                    createTask(name, description);
                });
            });
        }
    }
    
    private void updateTask(String name, String description) {
        if (existingTask == null) {
            Toast.makeText(this, "Greška pri učitavanju zadatka", Toast.LENGTH_SHORT).show();
            return;
        }
        
        existingTask.setName(name);
        existingTask.setDescription(description);
        existingTask.setDifficulty(selectedDifficulty);
        existingTask.setImportance(selectedImportance);
        existingTask.setDueDate(selectedDueDate);
        existingTask.setCategoryId(selectedCategoryId);
        existingTask.setRecurring(isRecurring);
        
        if (isRecurring) {
            String intervalStr = editRepeatInterval.getText() != null ? 
                    editRepeatInterval.getText().toString() : "1";
            int interval = intervalStr.isEmpty() ? 1 : Integer.parseInt(intervalStr);
            existingTask.setRepeatInterval(interval);
            
            String repeatUnit = chipDay.isChecked() ? "DAY" : "WEEK";
            existingTask.setRepeatUnit(repeatUnit);
            
            if (selectedEndDate > 0) {
                existingTask.setEndDate(selectedEndDate);
            }
        }
        
        // Recalculate XP based on current level (or keep original level)
        existingTask.setDifficultyXp(Task.getDifficultyXpForLevel(selectedDifficulty, existingTask.getUserLevelAtCreation()));
        existingTask.setImportanceXp(Task.getImportanceXpForLevel(selectedImportance, existingTask.getUserLevelAtCreation()));
        
        viewModel.update(existingTask);
        
        Toast.makeText(this, "Zadatak ažuriran!", Toast.LENGTH_SHORT).show();
        finish();
    }
    
    private void createTask(String name, String description) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Morate se prijaviti da biste kreirali zadatak", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Task task = new Task(user.getUid(), name, selectedDifficulty, selectedImportance, userLevel);
        task.setDescription(description);
        task.setDueDate(selectedDueDate);
        task.setRecurring(isRecurring);
        task.setCategoryId(selectedCategoryId);
        
        if (isRecurring) {
            String intervalStr = editRepeatInterval.getText() != null ? 
                    editRepeatInterval.getText().toString() : "1";
            int interval = intervalStr.isEmpty() ? 1 : Integer.parseInt(intervalStr);
            task.setRepeatInterval(interval);
            
            String repeatUnit = chipDay.isChecked() ? "DAY" : "WEEK";
            task.setRepeatUnit(repeatUnit);
            
            if (selectedEndDate > 0) {
                task.setEndDate(selectedEndDate);
            }
        }
        
        viewModel.insert(task);
        
        // Track task creation for success rate calculation
        userViewModel.incrementTaskCreated();
        
        Toast.makeText(this, "Zadatak kreiran! +" + task.getTotalXp() + " XP dostupno", Toast.LENGTH_SHORT).show();
        finish();
    }
}
