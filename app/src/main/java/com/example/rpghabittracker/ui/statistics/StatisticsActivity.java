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
import com.example.rpghabittracker.data.model.Category;
import com.example.rpghabittracker.data.model.Task;
import com.example.rpghabittracker.data.model.User;
import com.example.rpghabittracker.ui.viewmodel.CategoryViewModel;
import com.example.rpghabittracker.ui.viewmodel.TaskViewModel;
import com.example.rpghabittracker.ui.viewmodel.UserViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Activity showing user statistics and progress with charts
 */
public class StatisticsActivity extends AppCompatActivity {

    private TaskViewModel taskViewModel;
    private UserViewModel userViewModel;
    private CategoryViewModel categoryViewModel;
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
    private TextView textActiveDays;
    
    // Streak
    private TextView textCurrentStreak, textLongestStreak;

    // Special mission stats
    private TextView textSpecialMissionsStarted, textSpecialMissionsCompleted;
    
    // Weekly activity
    private View dayMon, dayTue, dayWed, dayThu, dayFri, daySat, daySun;
    
    // Charts
    private BarChart chartTaskCompletion;
    private PieChart chartCategoryDistribution;
    private LineChart chartDifficultyTrend;
    private LineChart chartXpLast7Days;

    private static final long DAY_MS = 24L * 60 * 60 * 1000;
    private final Map<String, String> categoryNameById = new HashMap<>();
    private final Map<String, String> fallbackCategoryNames = new HashMap<>();
    private List<Task> cachedTasks = new ArrayList<>();

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
        textActiveDays = findViewById(R.id.textActiveDays);
        
        // Streak
        textCurrentStreak = findViewById(R.id.textCurrentStreak);
        textLongestStreak = findViewById(R.id.textLongestStreak);

        // Special missions
        textSpecialMissionsStarted = findViewById(R.id.textSpecialMissionsStarted);
        textSpecialMissionsCompleted = findViewById(R.id.textSpecialMissionsCompleted);
        
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
        chartDifficultyTrend = findViewById(R.id.chartDifficultyTrend);
        chartXpLast7Days = findViewById(R.id.chartXpLast7Days);
    }
    
    private void setupCharts() {
        // Category bar chart
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
        xAxis.setLabelRotationAngle(-35f);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
        
        YAxis leftAxis = chartTaskCompletion.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(ContextCompat.getColor(this, R.color.card_stroke));
        leftAxis.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        
        chartTaskCompletion.getAxisRight().setEnabled(false);
        
        // Task status donut
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

        // Line charts
        setupLineChart(chartDifficultyTrend);
        setupLineChart(chartXpLast7Days);
    }

    private void setupLineChart(LineChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.getLegend().setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDoubleTapToZoomEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        leftAxis.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
        leftAxis.setGridColor(ContextCompat.getColor(this, R.color.card_stroke));

        chart.getAxisRight().setEnabled(false);
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
                    Long started = documentSnapshot.getLong("specialMissionsStarted");
                    Long completed = documentSnapshot.getLong("specialMissionsCompleted");
                    
                    textTotalXp.setText((xp != null ? xp : 0) + " XP");
                    textCurrentLevel.setText("Level " + (level != null ? level : 1));
                    textSpecialMissionsStarted.setText(String.valueOf(started != null ? started : 0));
                    textSpecialMissionsCompleted.setText(String.valueOf(completed != null ? completed : 0));
                });
    }
    
    private void setupViewModels() {
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        
        taskViewModel.setUserId(userId);
        userViewModel.setUserId(userId);
        categoryViewModel.setUserId(userId);
        
        // Observe tasks for statistics
        taskViewModel.getAllTasks().observe(this, this::updateTaskStats);
        
        // Observe user for XP and level
        userViewModel.getCurrentUser().observe(this, this::updateUserStats);

        // Observe categories to map categoryId -> category name in charts
        categoryViewModel.getUserCategories().observe(this, categories -> {
            categoryNameById.clear();
            if (categories != null) {
                for (Category category : categories) {
                    if (category != null && category.getId() != null) {
                        categoryNameById.put(category.getId(), category.getName());
                    }
                }
            }
            updateTaskStats(cachedTasks);
        });
    }
    
    private void updateTaskStats(List<Task> tasks) {
        if (tasks == null) {
            tasks = Collections.emptyList();
        }
        cachedTasks = tasks;
        
        int total = tasks.size();
        int completed = 0;
        int failed = 0;
        int cancelled = 0;
        int createdOpen = 0;
        
        // Count by category (completed only)
        Map<String, Integer> categoryCount = new HashMap<>();
        int[] completedPerDayThisWeek = new int[7];
        int[] xpPerLast7Days = new int[7];
        float[] difficultyXpSumPerLast7Days = new float[7];
        int[] difficultyXpCountPerLast7Days = new int[7];
        String[] last7Labels = buildLast7DayLabels();
        Map<Long, Integer> last7IndexByDay = buildLast7IndexMap();
        Set<Long> activeUsageDays = new HashSet<>();
        Map<Long, DayOutcome> dayOutcomes = new TreeMap<>();
        
        // Get start of current week (Monday)
        Calendar weekStart = Calendar.getInstance();
        weekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        weekStart.set(Calendar.HOUR_OF_DAY, 0);
        weekStart.set(Calendar.MINUTE, 0);
        weekStart.set(Calendar.SECOND, 0);
        weekStart.set(Calendar.MILLISECOND, 0);
        long weekStartMillis = weekStart.getTimeInMillis();
        long weekEndMillis = weekStartMillis + (7 * DAY_MS);
        
        for (Task task : tasks) {
            if (task.getCreatedAt() > 0) {
                activeUsageDays.add(getDayStart(task.getCreatedAt()));
            }

            String status = task.getStatus();
            // Count status
            if (Task.STATUS_COMPLETED.equals(status)) {
                completed++;
                
                // Count by category
                String categoryName = resolveCategoryName(task.getCategoryId());
                categoryCount.put(categoryName, categoryCount.getOrDefault(categoryName, 0) + 1);
                
                // Count by day of week
                long completedDate = task.getCompletedDate();
                if (completedDate > 0) {
                    long completedDay = getDayStart(completedDate);
                    activeUsageDays.add(completedDay);

                    Integer last7Index = last7IndexByDay.get(completedDay);
                    if (last7Index != null) {
                        xpPerLast7Days[last7Index] += Math.max(0, task.getTotalXp());
                        difficultyXpSumPerLast7Days[last7Index] += Math.max(0, task.getDifficultyXp());
                        difficultyXpCountPerLast7Days[last7Index]++;
                    }
                }

                if (completedDate > 0 && completedDate >= weekStartMillis && completedDate < weekEndMillis) {
                    Calendar taskCal = Calendar.getInstance();
                    taskCal.setTimeInMillis(completedDate);
                    int dayOfWeek = taskCal.get(Calendar.DAY_OF_WEEK);
                    int dayIndex = (dayOfWeek + 5) % 7; // Convert to Monday=0 index
                    if (dayIndex >= 0 && dayIndex < 7) {
                        completedPerDayThisWeek[dayIndex]++;
                    }
                }

                long streakDay = getTaskEffectiveDay(task);
                if (streakDay > 0) {
                    DayOutcome outcome = dayOutcomes.get(streakDay);
                    if (outcome == null) outcome = new DayOutcome();
                    outcome.completed = true;
                    dayOutcomes.put(streakDay, outcome);
                }
            } else if (Task.STATUS_FAILED.equals(status)) {
                failed++;
                long streakDay = getTaskEffectiveDay(task);
                if (streakDay > 0) {
                    DayOutcome outcome = dayOutcomes.get(streakDay);
                    if (outcome == null) outcome = new DayOutcome();
                    outcome.failed = true;
                    dayOutcomes.put(streakDay, outcome);
                }
            } else if (Task.STATUS_CANCELLED.equals(status)) {
                cancelled++;
            } else if (Task.STATUS_ACTIVE.equals(status) || Task.STATUS_PAUSED.equals(status)) {
                createdOpen++;
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
        
        // Active usage days (total + current consecutive usage streak)
        int totalActiveDays = activeUsageDays.size();
        int activeUsageStreak = getConsecutiveUsageStreak(activeUsageDays);
        textActiveDays.setText(totalActiveDays + " dana (niz: " + activeUsageStreak + ")");

        // Task-completion streak calculated from task outcomes.
        StreakStats streakStats = calculateTaskStreakStats(dayOutcomes);
        textCurrentStreak.setText(String.valueOf(streakStats.current));
        textLongestStreak.setText(String.valueOf(streakStats.longest));

        // Update weekly activity
        updateWeeklyActivity(completedPerDayThisWeek);
        
        // Update category bar chart
        updateCategoryBarChart(categoryCount);
        
        // Update task status donut chart
        updateStatusDonutChart(createdOpen, completed, failed, cancelled);

        // Update line charts
        updateAverageDifficultyLineChart(last7Labels, difficultyXpSumPerLast7Days, difficultyXpCountPerLast7Days);
        updateXpLast7DaysLineChart(last7Labels, xpPerLast7Days);
    }
    
    private void updateCategoryBarChart(Map<String, Integer> categoryCount) {
        if (categoryCount.isEmpty()) {
            chartTaskCompletion.clear();
            chartTaskCompletion.invalidate();
            return;
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(categoryCount.entrySet());
        sorted.sort((a, b) -> {
            int valueCompare = Integer.compare(b.getValue(), a.getValue());
            if (valueCompare != 0) return valueCompare;
            return a.getKey().compareToIgnoreCase(b.getKey());
        });

        ArrayList<BarEntry> entries = new ArrayList<>();
        String[] labels = new String[sorted.size()];
        for (int i = 0; i < sorted.size(); i++) {
            Map.Entry<String, Integer> entry = sorted.get(i);
            labels[i] = entry.getKey();
            entries.add(new BarEntry(i, entry.getValue()));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Završeni zadaci po kategoriji");
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
        
        XAxis xAxis = chartTaskCompletion.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(labels.length);

        chartTaskCompletion.setData(barData);
        chartTaskCompletion.setVisibleXRangeMaximum(Math.max(4, labels.length));
        chartTaskCompletion.invalidate();
    }
    
    private void updateStatusDonutChart(int createdOpen, int completed, int failed, int cancelled) {
        int total = createdOpen + completed + failed + cancelled;
        if (total <= 0) {
            chartCategoryDistribution.setCenterText("Nema\npodataka");
            chartCategoryDistribution.setData(null);
            chartCategoryDistribution.invalidate();
            return;
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        if (createdOpen > 0) entries.add(new PieEntry(createdOpen, "Kreirani"));
        if (completed > 0) entries.add(new PieEntry(completed, "Urađeni"));
        if (failed > 0) entries.add(new PieEntry(failed, "Neurađeni"));
        if (cancelled > 0) entries.add(new PieEntry(cancelled, "Otkazani"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        ArrayList<Integer> colors = new ArrayList<>();
        for (PieEntry entry : entries) {
            String label = entry.getLabel();
            if ("Kreirani".equals(label)) {
                colors.add(ContextCompat.getColor(this, R.color.status_paused));
            } else if ("Urađeni".equals(label)) {
                colors.add(ContextCompat.getColor(this, R.color.status_active));
            } else if ("Neurađeni".equals(label)) {
                colors.add(ContextCompat.getColor(this, R.color.status_failed));
            } else if ("Otkazani".equals(label)) {
                colors.add(ContextCompat.getColor(this, R.color.status_cancelled));
            } else {
                colors.add(ContextCompat.getColor(this, R.color.primary));
            }
        }
        dataSet.setColors(colors);
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

        chartCategoryDistribution.setCenterText("Ukupno\n" + total);
        chartCategoryDistribution.setData(pieData);
        chartCategoryDistribution.invalidate();
    }

    private void updateAverageDifficultyLineChart(String[] labels, float[] sum, int[] count) {
        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            float average = count[i] > 0 ? (sum[i] / count[i]) : 0f;
            entries.add(new Entry(i, average));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Prosečan XP težine");
        dataSet.setColor(ContextCompat.getColor(this, R.color.secondary));
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.secondary));
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.text_primary));
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(3.5f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.secondary_20));

        LineData lineData = new LineData(dataSet);
        chartDifficultyTrend.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartDifficultyTrend.setData(lineData);
        chartDifficultyTrend.invalidate();
    }

    private void updateXpLast7DaysLineChart(String[] labels, int[] xpPerDay) {
        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            entries.add(new Entry(i, xpPerDay[i]));
        }

        LineDataSet dataSet = new LineDataSet(entries, "XP po danu");
        dataSet.setColor(ContextCompat.getColor(this, R.color.rpg_xp));
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.rpg_xp));
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.text_primary));
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(3.5f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.primary_20));

        LineData lineData = new LineData(dataSet);
        chartXpLast7Days.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartXpLast7Days.setData(lineData);
        chartXpLast7Days.invalidate();
    }

    private String resolveCategoryName(String categoryId) {
        if (categoryId == null || categoryId.trim().isEmpty()) {
            return "Ostalo";
        }

        String categoryName = categoryNameById.get(categoryId);
        if (categoryName != null && !categoryName.trim().isEmpty()) {
            return categoryName;
        }

        // Keep names stable per unknown category ID, without exposing raw IDs to the user.
        String fallback = fallbackCategoryNames.get(categoryId);
        if (fallback == null) {
            fallback = "Kategorija " + (fallbackCategoryNames.size() + 1);
            fallbackCategoryNames.put(categoryId, fallback);
        }
        return fallback;
    }

    private String[] buildLast7DayLabels() {
        String[] labels = new String[7];
        long todayStart = getDayStart(System.currentTimeMillis());
        long firstDay = todayStart - (6 * DAY_MS);

        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i < 7; i++) {
            long day = firstDay + (i * DAY_MS);
            calendar.setTimeInMillis(day);
            int d = calendar.get(Calendar.DAY_OF_MONTH);
            int m = calendar.get(Calendar.MONTH) + 1;
            labels[i] = d + "." + m + ".";
        }
        return labels;
    }

    private Map<Long, Integer> buildLast7IndexMap() {
        Map<Long, Integer> indexMap = new HashMap<>();
        long todayStart = getDayStart(System.currentTimeMillis());
        long firstDay = todayStart - (6 * DAY_MS);
        for (int i = 0; i < 7; i++) {
            long day = firstDay + (i * DAY_MS);
            indexMap.put(day, i);
        }
        return indexMap;
    }

    private int getConsecutiveUsageStreak(Set<Long> activeDays) {
        if (activeDays.isEmpty()) return 0;

        int streak = 0;
        long day = getDayStart(System.currentTimeMillis());
        while (activeDays.contains(day)) {
            streak++;
            day -= DAY_MS;
        }
        return streak;
    }

    private StreakStats calculateTaskStreakStats(Map<Long, DayOutcome> dayOutcomes) {
        int current = 0;
        int longest = 0;

        for (Map.Entry<Long, DayOutcome> entry : dayOutcomes.entrySet()) {
            DayOutcome outcome = entry.getValue();
            if (outcome.failed) {
                current = 0;
                continue;
            }
            if (outcome.completed) {
                current++;
                if (current > longest) longest = current;
            }
            // If there were no tasks on a day, that day doesn't exist in this map,
            // so streak is not broken by empty days (as required).
        }

        return new StreakStats(current, longest);
    }

    private long getTaskEffectiveDay(Task task) {
        if (task.getDueDate() > 0) return getDayStart(task.getDueDate());
        if (task.getCompletedDate() > 0) return getDayStart(task.getCompletedDate());
        if (task.getCreatedAt() > 0) return getDayStart(task.getCreatedAt());
        return -1L;
    }

    private long getDayStart(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static final class DayOutcome {
        boolean completed;
        boolean failed;
    }

    private static final class StreakStats {
        final int current;
        final int longest;

        StreakStats(int current, int longest) {
            this.current = current;
            this.longest = longest;
        }
    }

    private void updateUserStats(User user) {
        if (user == null) return;
        
        // XP stats
        textTotalXp.setText(user.getExperiencePoints() + " XP");
        textCurrentLevel.setText("Level " + user.getLevel());
    }

    private void updateWeeklyActivity(int[] completedPerDay) {
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
