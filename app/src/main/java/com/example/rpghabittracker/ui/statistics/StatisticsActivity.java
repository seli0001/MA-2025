package com.example.rpghabittracker.ui.statistics;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.rpghabittracker.R;
import com.example.rpghabittracker.data.model.Task;
import com.example.rpghabittracker.data.model.User;
import com.example.rpghabittracker.ui.viewmodel.TaskViewModel;
import com.example.rpghabittracker.ui.viewmodel.UserViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity showing user statistics and progress with charts
 */
public class StatisticsActivity extends AppCompatActivity {

    private TaskViewModel taskViewModel;
    private UserViewModel userViewModel;
    private String userId;
    
    private FirebaseFirestore db;
    private ListenerRegistration userListener;
    
    // Overview stats
    private TextView textTotalTasks, textCompletedTasks, textFailedTasks;
    
    // Success rate
    private TextView textSuccessRate, textSuccessRateDescription;
    private ProgressBar progressSuccessRate;
    
    // XP stats
    private TextView textTotalXp, textCurrentLevel;
    
    // Streak
    private TextView textCurrentStreak, textLongestStreak;
    
    // Weekly activity
    private View dayMon, dayTue, dayWed, dayThu, dayFri, daySat, daySun;
    
    // Charts
    private BarChart chartTaskCompletion;
    private PieChart chartCategoryDistribution;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }
        userId = currentUser.getUid();
        db = FirebaseFirestore.getInstance();
        
        initViews();
        setupCharts();
        setupViewModels();
        loadUserDataRealtime();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null) {
            userListener.remove();
        }
    }
    
    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // Overview
        textTotalTasks = findViewById(R.id.textTotalTasks);
        textCompletedTasks = findViewById(R.id.textCompletedTasks);
        textFailedTasks = findViewById(R.id.textFailedTasks);
        
        // Success rate
        textSuccessRate = findViewById(R.id.textSuccessRate);
        textSuccessRateDescription = findViewById(R.id.textSuccessRateDescription);
        progressSuccessRate = findViewById(R.id.progressSuccessRate);
        
        // XP
        textTotalXp = findViewById(R.id.textTotalXp);
        textCurrentLevel = findViewById(R.id.textCurrentLevel);
        
        // Streak
        textCurrentStreak = findViewById(R.id.textCurrentStreak);
        textLongestStreak = findViewById(R.id.textLongestStreak);
        
        // Weekly activity
        dayMon = findViewById(R.id.dayMon);
        dayTue = findViewById(R.id.dayTue);
        dayWed = findViewById(R.id.dayWed);
        dayThu = findViewById(R.id.dayThu);
        dayFri = findViewById(R.id.dayFri);
        daySat = findViewById(R.id.daySat);
        daySun = findViewById(R.id.daySun);
        
        // Charts
        chartTaskCompletion = findViewById(R.id.chartTaskCompletion);
        chartCategoryDistribution = findViewById(R.id.chartCategoryDistribution);
    }
    
    private void setupCharts() {
        // Setup Bar Chart
        chartTaskCompletion.getDescription().setEnabled(false);
        chartTaskCompletion.setDrawGridBackground(false);
        chartTaskCompletion.setDrawBarShadow(false);
        chartTaskCompletion.setHighlightFullBarEnabled(false);
        chartTaskCompletion.getLegend().setEnabled(false);
        chartTaskCompletion.setScaleEnabled(false);
        chartTaskCompletion.setPinchZoom(false);
        chartTaskCompletion.setDoubleTapToZoomEnabled(false);
        
        XAxis xAxis = chartTaskCompletion.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
        
        YAxis leftAxis = chartTaskCompletion.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(ContextCompat.getColor(this, R.color.card_stroke));
        leftAxis.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        
        chartTaskCompletion.getAxisRight().setEnabled(false);
        
        // Setup Pie Chart
        chartCategoryDistribution.getDescription().setEnabled(false);
        chartCategoryDistribution.setDrawHoleEnabled(true);
        chartCategoryDistribution.setHoleColor(ContextCompat.getColor(this, R.color.card_background));
        chartCategoryDistribution.setHoleRadius(50f);
        chartCategoryDistribution.setTransparentCircleRadius(55f);
        chartCategoryDistribution.setTransparentCircleColor(ContextCompat.getColor(this, R.color.card_background));
        chartCategoryDistribution.setDrawCenterText(true);
        chartCategoryDistribution.setCenterTextColor(ContextCompat.getColor(this, R.color.text_primary));
        chartCategoryDistribution.setCenterTextSize(14f);
        chartCategoryDistribution.getLegend().setEnabled(true);
        chartCategoryDistribution.getLegend().setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
        chartCategoryDistribution.setRotationEnabled(false);
    }
    
    private void loadUserDataRealtime() {
        // Listen to user data from Firestore
        userListener = db.collection("users").document(userId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null || documentSnapshot == null || !documentSnapshot.exists()) {
                        return;
                    }
                    
                    Long xp = documentSnapshot.getLong("xp");
                    Long level = documentSnapshot.getLong("level");
                    Long currentStreak = documentSnapshot.getLong("currentStreak");
                    Long longestStreak = documentSnapshot.getLong("longestStreak");
                    
                    textTotalXp.setText((xp != null ? xp : 0) + " XP");
                    textCurrentLevel.setText("Level " + (level != null ? level : 1));
                    textCurrentStreak.setText(String.valueOf(currentStreak != null ? currentStreak : 0));
                    textLongestStreak.setText(String.valueOf(longestStreak != null ? longestStreak : 0));
                });
    }
    
    private void setupViewModels() {
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        
        taskViewModel.setUserId(userId);
        userViewModel.setUserId(userId);
        
        // Observe tasks for statistics
        taskViewModel.getAllTasks().observe(this, this::updateTaskStats);
        
        // Observe user for XP and streak
        userViewModel.getCurrentUser().observe(this, this::updateUserStats);
    }
    
    private void updateTaskStats(List<Task> tasks) {
        if (tasks == null) return;
        
        int total = tasks.size();
        int completed = 0;
        int failed = 0;
        
        // Count by status and category
        Map<String, Integer> categoryCount = new HashMap<>();
        int[] completedPerDay = new int[7];
        
        // Get start of current week (Monday)
        Calendar weekStart = Calendar.getInstance();
        weekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        weekStart.set(Calendar.HOUR_OF_DAY, 0);
        weekStart.set(Calendar.MINUTE, 0);
        weekStart.set(Calendar.SECOND, 0);
        weekStart.set(Calendar.MILLISECOND, 0);
        long weekStartMillis = weekStart.getTimeInMillis();
        
        for (Task task : tasks) {
            // Count status
            if (Task.STATUS_COMPLETED.equals(task.getStatus())) {
                completed++;
                
                // Count by category
                String category = task.getCategoryId() != null ? task.getCategoryId() : "Ostalo";
                categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);
                
                // Count by day of week
                long completedDate = task.getCompletedDate();
                if (completedDate > 0 && completedDate >= weekStartMillis) {
                    Calendar taskCal = Calendar.getInstance();
                    taskCal.setTimeInMillis(completedDate);
                    int dayOfWeek = taskCal.get(Calendar.DAY_OF_WEEK);
                    int dayIndex = (dayOfWeek + 5) % 7; // Convert to Monday=0 index
                    if (dayIndex >= 0 && dayIndex < 7) {
                        completedPerDay[dayIndex]++;
                    }
                }
            } else if (Task.STATUS_FAILED.equals(task.getStatus())) {
                failed++;
            }
        }
        
        // Update overview
        textTotalTasks.setText(String.valueOf(total));
        textCompletedTasks.setText(String.valueOf(completed));
        textFailedTasks.setText(String.valueOf(failed));
        
        // Calculate and update success rate
        int finishedTasks = completed + failed;
        int successRate = finishedTasks > 0 ? (completed * 100) / finishedTasks : 0;
        
        textSuccessRate.setText(successRate + "%");
        progressSuccessRate.setProgress(successRate);
        
        // Update description based on rate
        if (successRate >= 90) {
            textSuccessRateDescription.setText("Neverovatno! Ti si šampion! 🏆");
        } else if (successRate >= 70) {
            textSuccessRateDescription.setText("Odlično! Nastavi tako! 💪");
        } else if (successRate >= 50) {
            textSuccessRateDescription.setText("Dobro je, možeš i bolje! 👍");
        } else if (successRate > 0) {
            textSuccessRateDescription.setText("Ne odustaj, napredak je ključ! 🌱");
        } else {
            textSuccessRateDescription.setText("Započni svoje putovanje! 🚀");
        }
        
        // Update weekly activity
        updateWeeklyActivity(tasks);
        
        // Update bar chart
        updateBarChart(completedPerDay);
        
        // Update pie chart
        updatePieChart(categoryCount);
    }
    
    private void updateBarChart(int[] completedPerDay) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        String[] days = {"Pon", "Uto", "Sre", "Čet", "Pet", "Sub", "Ned"};
        
        for (int i = 0; i < 7; i++) {
            entries.add(new BarEntry(i, completedPerDay[i]));
        }
        
        BarDataSet dataSet = new BarDataSet(entries, "Završeni zadaci");
        dataSet.setColor(ContextCompat.getColor(this, R.color.primary));
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.text_primary));
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return value > 0 ? String.valueOf((int) value) : "";
            }
        });
        
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        
        chartTaskCompletion.getXAxis().setValueFormatter(new IndexAxisValueFormatter(days));
        chartTaskCompletion.setData(barData);
        chartTaskCompletion.invalidate();
    }
    
    private void updatePieChart(Map<String, Integer> categoryCount) {
        if (categoryCount.isEmpty()) {
            chartCategoryDistribution.setCenterText("Nema\npodataka");
            chartCategoryDistribution.setData(null);
            chartCategoryDistribution.invalidate();
            return;
        }
        
        ArrayList<PieEntry> entries = new ArrayList<>();
        int total = 0;
        
        for (Map.Entry<String, Integer> entry : categoryCount.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
            total += entry.getValue();
        }
        
        PieDataSet dataSet = new PieDataSet(entries, "");
        
        // Colors for categories
        int[] colors = {
            ContextCompat.getColor(this, R.color.primary),
            ContextCompat.getColor(this, R.color.secondary),
            ContextCompat.getColor(this, R.color.rpg_gold),
            ContextCompat.getColor(this, R.color.status_active),
            ContextCompat.getColor(this, R.color.rpg_xp),
            Color.parseColor("#FF7043"),
            Color.parseColor("#9575CD"),
            Color.parseColor("#4DD0E1")
        };
        
        ArrayList<Integer> colorList = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            colorList.add(colors[i % colors.length]);
        }
        dataSet.setColors(colorList);
        
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(2f);
        
        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });
        
        chartCategoryDistribution.setCenterText(total + "\nzadataka");
        chartCategoryDistribution.setData(pieData);
        chartCategoryDistribution.invalidate();
    }
    
    private void updateUserStats(User user) {
        if (user == null) return;
        
        // XP stats
        textTotalXp.setText(user.getExperiencePoints() + " XP");
        textCurrentLevel.setText("Level " + user.getLevel());
        
        // Streak
        textCurrentStreak.setText(String.valueOf(user.getCurrentStreak()));
        textLongestStreak.setText(String.valueOf(user.getLongestStreak()));
    }
    
    private void updateWeeklyActivity(List<Task> tasks) {
        // Get start of current week (Monday)
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        
        long weekStart = cal.getTimeInMillis();
        
        // Count completed tasks per day
        int[] completedPerDay = new int[7];
        
        for (Task task : tasks) {
            if (!Task.STATUS_COMPLETED.equals(task.getStatus())) continue;
            
            long completedDate = task.getCompletedDate();
            if (completedDate <= 0 || completedDate < weekStart) continue;
            
            Calendar taskCal = Calendar.getInstance();
            taskCal.setTimeInMillis(completedDate);
            
            int dayOfWeek = taskCal.get(Calendar.DAY_OF_WEEK);
            // Convert to 0-based index (Monday = 0)
            int dayIndex = (dayOfWeek + 5) % 7;
            
            if (dayIndex >= 0 && dayIndex < 7) {
                completedPerDay[dayIndex]++;
            }
        }
        
        // Update day indicators
        View[] dayViews = {dayMon, dayTue, dayWed, dayThu, dayFri, daySat, daySun};
        int today = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7;
        
        for (int i = 0; i < 7; i++) {
            updateDayIndicator(dayViews[i], completedPerDay[i], i == today);
        }
    }
    
    private void updateDayIndicator(View view, int completedCount, boolean isToday) {
        GradientDrawable bg = (GradientDrawable) view.getBackground();
        
        int color;
        if (completedCount >= 3) {
            color = ContextCompat.getColor(this, R.color.status_active);
        } else if (completedCount >= 1) {
            color = ContextCompat.getColor(this, R.color.rpg_gold);
        } else if (isToday) {
            color = ContextCompat.getColor(this, R.color.primary);
        } else {
            color = ContextCompat.getColor(this, R.color.card_stroke);
        }
        
        bg.setColor(color);
        
        if (isToday) {
            bg.setStroke(2, ContextCompat.getColor(this, R.color.white));
        } else {
            bg.setStroke(0, 0);
        }
    }
}
