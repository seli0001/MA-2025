package com.example.rpghabittracker.ui.calendar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpghabittracker.R;
import com.example.rpghabittracker.data.model.Task;
import com.example.rpghabittracker.ui.adapters.CalendarAdapter;
import com.example.rpghabittracker.ui.adapters.TaskAdapter;
import com.example.rpghabittracker.ui.tasks.TaskDetailsActivity;
import com.example.rpghabittracker.ui.viewmodel.TaskViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Calendar view for tasks with month navigation
 */
public class CalendarActivity extends AppCompatActivity implements 
        CalendarAdapter.OnDayClickListener, TaskAdapter.TaskClickListener {

    private TaskViewModel taskViewModel;
    private String userId;
    
    private TextView textCurrentMonth;
    private TextView textSelectedDate;
    private RecyclerView calendarGrid;
    private RecyclerView tasksRecyclerView;
    private LinearLayout emptyState;
    
    private CalendarAdapter calendarAdapter;
    private TaskAdapter taskAdapter;
    
    private Calendar currentCalendar;
    private Calendar selectedDate;
    private List<Task> allTasks = new ArrayList<>();
    
    private final SimpleDateFormat monthYearFormat = new SimpleDateFormat("LLLL yyyy", new Locale("sr"));
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE, d. LLLL", new Locale("sr"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }
        userId = currentUser.getUid();
        
        currentCalendar = Calendar.getInstance();
        selectedDate = Calendar.getInstance();
        
        initViews();
        setupViewModel();
        updateCalendar();
    }
    
    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        textCurrentMonth = findViewById(R.id.textCurrentMonth);
        textSelectedDate = findViewById(R.id.textSelectedDate);
        calendarGrid = findViewById(R.id.calendarGrid);
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView);
        emptyState = findViewById(R.id.emptyState);
        
        ImageButton btnPrevMonth = findViewById(R.id.btnPrevMonth);
        ImageButton btnNextMonth = findViewById(R.id.btnNextMonth);
        
        btnPrevMonth.setOnClickListener(v -> navigateMonth(-1));
        btnNextMonth.setOnClickListener(v -> navigateMonth(1));
        
        // Calendar grid setup
        calendarAdapter = new CalendarAdapter(this);
        calendarGrid.setLayoutManager(new GridLayoutManager(this, 7));
        calendarGrid.setAdapter(calendarAdapter);
        
        // Tasks list setup
        taskAdapter = new TaskAdapter(this);
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tasksRecyclerView.setAdapter(taskAdapter);
        
        updateSelectedDateText();
    }
    
    private void setupViewModel() {
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        taskViewModel.setUserId(userId);
        
        // Observe all tasks
        taskViewModel.getAllTasks().observe(this, tasks -> {
            if (tasks != null) {
                allTasks = tasks;
                updateCalendar();
                loadTasksForSelectedDate();
            }
        });
    }
    
    private void navigateMonth(int direction) {
        currentCalendar.add(Calendar.MONTH, direction);
        updateCalendar();
    }
    
    private void updateCalendar() {
        // Update month/year header
        textCurrentMonth.setText(capitalize(monthYearFormat.format(currentCalendar.getTime())));
        
        // Generate calendar days
        List<CalendarAdapter.CalendarDay> days = generateCalendarDays();
        
        // Find today's position
        int todayPosition = -1;
        Calendar today = Calendar.getInstance();
        if (currentCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            currentCalendar.get(Calendar.MONTH) == today.get(Calendar.MONTH)) {
            for (int i = 0; i < days.size(); i++) {
                if (days.get(i).isToday) {
                    todayPosition = i;
                    break;
                }
            }
        }
        
        calendarAdapter.setDays(days, todayPosition);
        
        // Update task indicators
        updateTaskIndicators();
    }
    
    private List<CalendarAdapter.CalendarDay> generateCalendarDays() {
        List<CalendarAdapter.CalendarDay> days = new ArrayList<>();
        
        Calendar cal = (Calendar) currentCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        
        // Get first day of week (Monday = 1 in Serbia)
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        // Adjust for Monday start (Calendar.MONDAY = 2)
        int emptyDays = (firstDayOfWeek + 5) % 7;
        
        // Add empty cells for days before first of month
        for (int i = 0; i < emptyDays; i++) {
            days.add(CalendarAdapter.CalendarDay.empty());
        }
        
        // Get today for comparison
        Calendar today = Calendar.getInstance();
        int todayDay = today.get(Calendar.DAY_OF_MONTH);
        int todayMonth = today.get(Calendar.MONTH);
        int todayYear = today.get(Calendar.YEAR);
        
        int currentMonth = cal.get(Calendar.MONTH);
        int currentYear = cal.get(Calendar.YEAR);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        // Add days of month
        for (int day = 1; day <= daysInMonth; day++) {
            cal.set(Calendar.DAY_OF_MONTH, day);
            boolean isToday = (day == todayDay && currentMonth == todayMonth && currentYear == todayYear);
            days.add(new CalendarAdapter.CalendarDay(day, true, isToday, cal.getTimeInMillis()));
        }
        
        // Fill remaining cells to complete the grid (6 rows * 7 days = 42)
        while (days.size() < 42) {
            days.add(CalendarAdapter.CalendarDay.empty());
        }
        
        return days;
    }
    
    private void updateTaskIndicators() {
        Map<Integer, List<Integer>> indicators = new HashMap<>();
        
        int currentMonth = currentCalendar.get(Calendar.MONTH);
        int currentYear = currentCalendar.get(Calendar.YEAR);
        
        for (Task task : allTasks) {
            if (task.getDueDate() <= 0) continue;
            
            Calendar taskCal = Calendar.getInstance();
            taskCal.setTimeInMillis(task.getDueDate());
            
            if (taskCal.get(Calendar.MONTH) == currentMonth && 
                taskCal.get(Calendar.YEAR) == currentYear) {
                
                int day = taskCal.get(Calendar.DAY_OF_MONTH);
                List<Integer> colors = indicators.get(day);
                if (colors == null) {
                    colors = new ArrayList<>();
                    indicators.put(day, colors);
                }
                
                // Add color based on task status (max 3 indicators)
                if (colors.size() < 3) {
                    int color;
                    if (Task.STATUS_COMPLETED.equals(task.getStatus())) {
                        color = ContextCompat.getColor(this, R.color.status_completed);
                    } else if (Task.STATUS_FAILED.equals(task.getStatus())) {
                        color = ContextCompat.getColor(this, R.color.status_failed);
                    } else {
                        color = ContextCompat.getColor(this, R.color.status_active);
                    }
                    colors.add(color);
                }
            }
        }
        
        calendarAdapter.setTaskIndicators(indicators);
    }
    
    private void loadTasksForSelectedDate() {
        List<Task> tasksForDay = new ArrayList<>();
        
        int selectedDay = selectedDate.get(Calendar.DAY_OF_MONTH);
        int selectedMonth = selectedDate.get(Calendar.MONTH);
        int selectedYear = selectedDate.get(Calendar.YEAR);
        
        for (Task task : allTasks) {
            if (task.getDueDate() <= 0) continue;
            
            Calendar taskCal = Calendar.getInstance();
            taskCal.setTimeInMillis(task.getDueDate());
            
            if (taskCal.get(Calendar.DAY_OF_MONTH) == selectedDay &&
                taskCal.get(Calendar.MONTH) == selectedMonth &&
                taskCal.get(Calendar.YEAR) == selectedYear) {
                tasksForDay.add(task);
            }
        }
        
        if (tasksForDay.isEmpty()) {
            tasksRecyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            tasksRecyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            taskAdapter.submitList(tasksForDay);
        }
    }
    
    private void updateSelectedDateText() {
        Calendar today = Calendar.getInstance();
        boolean isToday = selectedDate.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH) &&
                         selectedDate.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                         selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR);
        
        String dateText;
        if (isToday) {
            dateText = "Danas, " + new SimpleDateFormat("d. LLLL", new Locale("sr")).format(selectedDate.getTime());
        } else {
            dateText = capitalize(dayFormat.format(selectedDate.getTime()));
        }
        textSelectedDate.setText(dateText);
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    // CalendarAdapter.OnDayClickListener
    @Override
    public void onDayClick(CalendarAdapter.CalendarDay day) {
        selectedDate.set(currentCalendar.get(Calendar.YEAR), 
                        currentCalendar.get(Calendar.MONTH), 
                        day.dayOfMonth);
        updateSelectedDateText();
        loadTasksForSelectedDate();
    }
    
    // TaskAdapter.TaskClickListener
    @Override
    public void onTaskClick(Task task) {
        Intent intent = new Intent(this, TaskDetailsActivity.class);
        intent.putExtra(TaskDetailsActivity.EXTRA_TASK_ID, task.getId());
        startActivity(intent);
    }
    
    @Override
    public void onTaskComplete(Task task, boolean isChecked) {
        if (isChecked) {
            taskViewModel.completeTask(task, () -> {
                runOnUiThread(() -> loadTasksForSelectedDate());
            });
        }
    }
    
    @Override
    public void onTaskLongClick(Task task) {
        // Open task details on long click
        onTaskClick(task);
    }
}
