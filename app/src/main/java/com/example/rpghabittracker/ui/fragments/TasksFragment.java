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
import com.example.rpghabittracker.utils.AllianceMissionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fragment for displaying and managing tasks.
 *
 * One-time tab  -> shows non-recurring tasks
 * Recurring tab -> shows daily occurrence rows (parentTaskId != null);
 *                  templates are hidden from the list but drive generation
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

        view.findViewById(R.id.btnCalendar).setOnClickListener(v ->
                startActivity(new Intent(requireContext(),
                        com.example.rpghabittracker.ui.calendar.CalendarActivity.class)));
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            viewModel.setUserId(user.getUid());
            userViewModel.setUserId(user.getUid());
        }

        // Generate today's recurring occurrences as soon as we have a userId
        viewModel.generateTodayOccurrences();
        viewModel.processExpiredTasks();

        // Observe all tasks (one-time + templates + occurrences)
        viewModel.getAllTasks().observe(getViewLifecycleOwner(), tasks -> {
            allTasks = tasks != null ? tasks : new ArrayList<>();
            applyFilters();
            progressBar.setVisibility(View.GONE);
        });

        userViewModel.getXpGainEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null) userViewModel.clearXpGainEvent();
        });

        userViewModel.getLevelUpEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                showLevelUpDialog(event);
                userViewModel.clearLevelUpEvent();
            }
        });
    }

    private void showLevelUpDialog(UserViewModel.LevelUpEvent event) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("LEVEL UP!")
                .setMessage("Cestitamo! Dostigli ste nivo " + event.newLevel + "!\n\n" +
                        "Nova titula: " + event.newTitle + "\n" +
                        "Bonus PP: +" + event.ppReward)
                .setPositiveButton("Odlicno!", null)
                .show();
    }

    private void setupRecyclerView() {
        adapter = new TaskAdapter(this);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerTasks.setAdapter(adapter);

        recyclerTasks.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) fabAddTask.shrink();
                else if (dy < 0) fabAddTask.extend();
            }
        });
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Jednokratni"));
        tabLayout.addTab(tabLayout.newTab().setText("Ponavljajuci"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showRecurring = tab.getPosition() == 1;
                applyFilters();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
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
                    boolean typeMatch;
                    if (showRecurring) {
                        // Show only daily occurrences (have a parent), not the template row
                        String pid = task.getParentTaskId();
                        typeMatch = task.isRecurring() && pid != null && !pid.isEmpty();
                    } else {
                        // Show only one-time tasks
                        typeMatch = !task.isRecurring();
                    }

                    boolean statusMatch = "ALL".equals(currentFilter)
                            || currentFilter.equals(task.getStatus());

                    return typeMatch && statusMatch;
                })
                .collect(Collectors.toList());

        adapter.submitList(filtered);

        emptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerTasks.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onTaskClick(Task task) {
        Intent intent = new Intent(requireContext(), TaskDetailsActivity.class);
        intent.putExtra(TaskDetailsActivity.EXTRA_TASK_ID, task.getId());
        startActivity(intent);
    }

    @Override
    public void onTaskComplete(Task task, boolean isChecked) {
        if (!isChecked) return;
        if (!Task.STATUS_ACTIVE.equals(task.getStatus())) {
            Toast.makeText(requireContext(), "Zadatak nije aktivan", Toast.LENGTH_SHORT).show();
            return;
        }

        int xpGained = task.getTotalXp();

        viewModel.completeTask(task, () -> requireActivity().runOnUiThread(() -> {
            userViewModel.addXpFromTask(xpGained);

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                AllianceMissionManager.recordTaskCompletion(
                        FirebaseFirestore.getInstance(),
                        currentUser.getUid(),
                        task,
                        null
                );
            }

            Snackbar.make(requireView(), "+" + xpGained + " XP zaradjeno!",
                    Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getResources().getColor(R.color.secondary, null))
                    .setTextColor(getResources().getColor(R.color.white, null))
                    .show();
        }));
    }

    @Override
    public void onTaskLongClick(Task task) {
        showTaskOptionsDialog(task);
    }

    private void showTaskOptionsDialog(Task task) {
        boolean isOccurrence = task.isRecurring()
                && task.getParentTaskId() != null
                && !task.getParentTaskId().isEmpty();

        if (Task.STATUS_ACTIVE.equals(task.getStatus())) {
            if (isOccurrence) {
                // For an occurrence: cancel this instance OR pause/delete the whole series
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle(task.getName())
                        .setItems(new String[]{
                                "Otkazi ovaj zadatak",
                                "Pauziraj seriju",
                                "Obrisi seriju"
                        }, (dialog, which) -> {
                            switch (which) {
                                case 0:
                                    viewModel.updateStatus(task.getId(), Task.STATUS_CANCELLED);
                                    Toast.makeText(requireContext(), "Zadatak otkazan", Toast.LENGTH_SHORT).show();
                                    break;
                                case 1:
                                    viewModel.pauseRecurringSeries(task.getParentTaskId());
                                    Toast.makeText(requireContext(), "Serija pauzirana", Toast.LENGTH_SHORT).show();
                                    break;
                                case 2:
                                    viewModel.deleteRecurringSeries(task.getParentTaskId());
                                    Toast.makeText(requireContext(), "Serija obrisana", Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        })
                        .show();
            } else {
                // One-time active task
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle(task.getName())
                        .setItems(new String[]{"Otkazi zadatak", "Obrisi"}, (dialog, which) -> {
                            switch (which) {
                                case 0:
                                    viewModel.updateStatus(task.getId(), Task.STATUS_CANCELLED);
                                    Toast.makeText(requireContext(), "Zadatak otkazan", Toast.LENGTH_SHORT).show();
                                    break;
                                case 1:
                                    viewModel.delete(task);
                                    Toast.makeText(requireContext(), "Zadatak obrisan", Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        })
                        .show();
            }
        } else {
            // Non-active task: only delete is allowed (cancelled/failed can't be modified per spec)
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle(task.getName())
                    .setMessage("Zadatak nije aktivan i ne moze se menjati.")
                    .setPositiveButton("U redu", null)
                    .show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Regenerate occurrences and expire stale tasks every time user returns
        viewModel.generateTodayOccurrences();
        viewModel.processExpiredTasks();
    }
}
