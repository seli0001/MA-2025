package com.example.rpghabittracker.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpghabittracker.R;
import com.example.rpghabittracker.data.model.Task;
import com.example.rpghabittracker.ui.adapters.TaskAdapter;
import com.example.rpghabittracker.ui.tasks.AddTaskActivity;
import com.example.rpghabittracker.ui.tasks.TaskDetailsActivity;
import com.example.rpghabittracker.ui.viewmodel.TaskViewModel;
import com.example.rpghabittracker.ui.viewmodel.UserViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fragment for displaying and managing tasks
 */
public class TasksFragment extends Fragment implements TaskAdapter.TaskClickListener {
    
    private RecyclerView recyclerTasks;
    private LinearLayout emptyState;
    private ProgressBar progressBar;
    private TabLayout tabLayout;
    private ChipGroup filterChipGroup;
    private Chip chipAll, chipActive, chipCompleted, chipFailed;
    private ExtendedFloatingActionButton fabAddTask;
    
    private TaskAdapter adapter;
    private TaskViewModel viewModel;
    private UserViewModel userViewModel;
    
    private boolean showRecurring = false;
    private String currentFilter = "ALL";
    private List<Task> allTasks = new ArrayList<>();
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tasks, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupViewModel();
        setupRecyclerView();
        setupTabs();
        setupFilters();
        setupFab();
    }
    
    private void initViews(View view) {
        recyclerTasks = view.findViewById(R.id.recyclerTasks);
        emptyState = view.findViewById(R.id.emptyState);
        progressBar = view.findViewById(R.id.progressBar);
        tabLayout = view.findViewById(R.id.tabLayout);
        filterChipGroup = view.findViewById(R.id.filterChipGroup);
        chipAll = view.findViewById(R.id.chipAll);
        chipActive = view.findViewById(R.id.chipActive);
        chipCompleted = view.findViewById(R.id.chipCompleted);
        chipFailed = view.findViewById(R.id.chipFailed);
        fabAddTask = view.findViewById(R.id.fabAddTask);
        
        // Calendar button
        view.findViewById(R.id.btnCalendar).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), 
                    com.example.rpghabittracker.ui.calendar.CalendarActivity.class));
        });
    }
    
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        // Use requireActivity() to share UserViewModel with MainActivity
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            viewModel.setUserId(user.getUid());
            userViewModel.setUserId(user.getUid());
        }
        
        // Observe all tasks
        viewModel.getAllTasks().observe(getViewLifecycleOwner(), tasks -> {
            allTasks = tasks != null ? tasks : new ArrayList<>();
            applyFilters();
            progressBar.setVisibility(View.GONE);
        });
        
        // Observe XP gain events
        userViewModel.getXpGainEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                // XP gain is handled in onTaskComplete
                userViewModel.clearXpGainEvent();
            }
        });
        
        // Observe level up events
        userViewModel.getLevelUpEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                showLevelUpDialog(event);
                userViewModel.clearLevelUpEvent();
            }
        });
        
        // Process expired tasks on load
        viewModel.processExpiredTasks();
    }
    
    private void showLevelUpDialog(UserViewModel.LevelUpEvent event) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("🎊 LEVEL UP!")
                .setMessage("Čestitamo! Dostigli ste nivo " + event.newLevel + "!\n\n" +
                        "Nova titula: " + event.newTitle + "\n" +
                        "Bonus PP: +" + event.ppReward)
                .setPositiveButton("Odlično!", null)
                .show();
    }
    
    private void setupRecyclerView() {
        adapter = new TaskAdapter(this);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerTasks.setAdapter(adapter);
        
        // Shrink FAB on scroll
        recyclerTasks.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    fabAddTask.shrink();
                } else if (dy < 0) {
                    fabAddTask.extend();
                }
            }
        });
    }
    
    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("One-time"));
        tabLayout.addTab(tabLayout.newTab().setText("Recurring"));
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showRecurring = tab.getPosition() == 1;
                applyFilters();
            }
            
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
    
    private void setupFilters() {
        chipAll.setChecked(true);
        
        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipAll)) {
                currentFilter = "ALL";
            } else if (checkedIds.contains(R.id.chipActive)) {
                currentFilter = Task.STATUS_ACTIVE;
            } else if (checkedIds.contains(R.id.chipCompleted)) {
                currentFilter = Task.STATUS_COMPLETED;
            } else if (checkedIds.contains(R.id.chipFailed)) {
                currentFilter = Task.STATUS_FAILED;
            } else {
                currentFilter = "ALL";
                chipAll.setChecked(true);
            }
            applyFilters();
        });
    }
    
    private void setupFab() {
        fabAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddTaskActivity.class);
            intent.putExtra("isRecurring", showRecurring);
            startActivity(intent);
        });
    }
    
    private void applyFilters() {
        List<Task> filtered = allTasks.stream()
                .filter(task -> {
                    // Filter by type (one-time vs recurring)
                    boolean typeMatch = showRecurring == task.isRecurring();
                    
                    // Filter by status
                    boolean statusMatch = "ALL".equals(currentFilter) 
                            || currentFilter.equals(task.getStatus());
                    
                    return typeMatch && statusMatch;
                })
                .collect(Collectors.toList());
        
        adapter.submitList(filtered);
        
        // Show/hide empty state
        if (filtered.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerTasks.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerTasks.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    public void onTaskClick(Task task) {
        Intent intent = new Intent(requireContext(), TaskDetailsActivity.class);
        intent.putExtra(TaskDetailsActivity.EXTRA_TASK_ID, task.getId());
        startActivity(intent);
    }
    
    @Override
    public void onTaskComplete(Task task, boolean isChecked) {
        if (isChecked) {
            int xpGained = task.getTotalXp();
            
            android.util.Log.d("TasksFragment", "Task complete: " + task.getName() + ", XP to add: " + xpGained);
            
            // Mark task as complete in database
            viewModel.completeTask(task, () -> {
                requireActivity().runOnUiThread(() -> {
                    android.util.Log.d("TasksFragment", "Task marked complete, now adding XP: " + xpGained);
                    
                    // Add XP to user (this will trigger level up if needed)
                    userViewModel.addXpFromTask(xpGained);
                    
                    Snackbar.make(requireView(), 
                            "+" + xpGained + " XP zarađeno!", 
                            Snackbar.LENGTH_SHORT)
                            .setBackgroundTint(getResources().getColor(R.color.secondary, null))
                            .setTextColor(getResources().getColor(R.color.white, null))
                            .show();
                });
            });
        }
    }
    
    @Override
    public void onTaskLongClick(Task task) {
        // Show options dialog
        showTaskOptionsDialog(task);
    }
    
    private void showTaskOptionsDialog(Task task) {
        String[] options;
        if (Task.STATUS_ACTIVE.equals(task.getStatus())) {
            options = new String[]{"Edit", "Cancel Task", "Pause Task", "Delete"};
        } else {
            options = new String[]{"Delete"};
        }
        
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(task.getName())
                .setItems(options, (dialog, which) -> {
                    if (Task.STATUS_ACTIVE.equals(task.getStatus())) {
                        switch (which) {
                            case 0: // Edit
                                // TODO: Open edit activity
                                break;
                            case 1: // Cancel
                                viewModel.updateStatus(task.getId(), Task.STATUS_CANCELLED);
                                Toast.makeText(requireContext(), "Task cancelled", Toast.LENGTH_SHORT).show();
                                break;
                            case 2: // Pause (recurring only)
                                if (task.isRecurring()) {
                                    viewModel.updateStatus(task.getId(), Task.STATUS_PAUSED);
                                    Toast.makeText(requireContext(), "Task paused", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case 3: // Delete
                                viewModel.delete(task);
                                Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show();
                                break;
                        }
                    } else {
                        // Only delete option for non-active tasks
                        viewModel.delete(task);
                        Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to this fragment
        viewModel.processExpiredTasks();
    }
}
