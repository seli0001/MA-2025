package com.example.rpghabittracker.ui.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpghabittracker.R;
import com.example.rpghabittracker.data.model.Task;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * RecyclerView Adapter for Task items
 */
public class TaskAdapter extends ListAdapter<Task, TaskAdapter.TaskViewHolder> {
    
    private final TaskClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
    private final SimpleDateFormat todayFormat = new SimpleDateFormat("'Today,' h:mm a", Locale.getDefault());
    
    public interface TaskClickListener {
        void onTaskClick(Task task);
        void onTaskComplete(Task task, boolean isChecked);
        void onTaskLongClick(Task task);
    }
    
    public TaskAdapter(TaskClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }
    
    private static final DiffUtil.ItemCallback<Task> DIFF_CALLBACK = new DiffUtil.ItemCallback<Task>() {
        @Override
        public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.getId().equals(newItem.getId());
        }
        
        @Override
        public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.getName().equals(newItem.getName())
                && oldItem.getStatus().equals(newItem.getStatus())
                && oldItem.getTotalXp() == newItem.getTotalXp()
                && oldItem.getDueDate() == newItem.getDueDate();
        }
    };
    
    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = getItem(position);
        holder.bind(task);
    }
    
    class TaskViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardTask;
        private final View categoryIndicator;
        private final TextView textTaskName;
        private final TextView textXp;
        private final TextView textDescription;
        private final TextView textCategory;
        private final ImageView iconDifficulty;
        private final TextView textDifficulty;
        private final LinearLayout layoutDueDate;
        private final TextView textDueDate;
        private final ImageView iconRecurring;
        private final CheckBox checkboxComplete;
        private final TextView textStatus;
        
        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTask = itemView.findViewById(R.id.cardTask);
            categoryIndicator = itemView.findViewById(R.id.categoryIndicator);
            textTaskName = itemView.findViewById(R.id.textTaskName);
            textXp = itemView.findViewById(R.id.textXp);
            textDescription = itemView.findViewById(R.id.textDescription);
            textCategory = itemView.findViewById(R.id.textCategory);
            iconDifficulty = itemView.findViewById(R.id.iconDifficulty);
            textDifficulty = itemView.findViewById(R.id.textDifficulty);
            layoutDueDate = itemView.findViewById(R.id.layoutDueDate);
            textDueDate = itemView.findViewById(R.id.textDueDate);
            iconRecurring = itemView.findViewById(R.id.iconRecurring);
            checkboxComplete = itemView.findViewById(R.id.checkboxComplete);
            textStatus = itemView.findViewById(R.id.textStatus);
        }
        
        void bind(Task task) {
            Context context = itemView.getContext();
            
            // Task name
            textTaskName.setText(task.getName());
            
            // XP value
            textXp.setText(String.valueOf(task.getTotalXp()));
            
            // Description
            if (task.getDescription() != null && !task.getDescription().isEmpty()) {
                textDescription.setVisibility(View.VISIBLE);
                textDescription.setText(task.getDescription());
            } else {
                textDescription.setVisibility(View.GONE);
            }
            
            // Category (placeholder)
            if (task.getCategoryId() != null) {
                textCategory.setVisibility(View.VISIBLE);
                textCategory.setText("Task"); // TODO: Load category name
            } else {
                textCategory.setVisibility(View.GONE);
            }
            
            // Difficulty indicator
            bindDifficulty(task.getDifficulty(), context);
            
            // Due date
            if (task.getDueDate() > 0) {
                layoutDueDate.setVisibility(View.VISIBLE);
                textDueDate.setText(formatDueDate(task.getDueDate()));
                
                // Color based on urgency
                long now = System.currentTimeMillis();
                long diff = task.getDueDate() - now;
                if (diff < 0) {
                    textDueDate.setTextColor(ContextCompat.getColor(context, R.color.status_failed));
                } else if (diff < 24 * 60 * 60 * 1000) {
                    textDueDate.setTextColor(ContextCompat.getColor(context, R.color.status_paused));
                } else {
                    textDueDate.setTextColor(ContextCompat.getColor(context, R.color.text_tertiary));
                }
            } else {
                layoutDueDate.setVisibility(View.GONE);
            }
            
            // Recurring indicator
            iconRecurring.setVisibility(task.isRecurring() ? View.VISIBLE : View.GONE);
            
            // Status handling
            boolean isActive = Task.STATUS_ACTIVE.equals(task.getStatus());
            boolean isCompleted = Task.STATUS_COMPLETED.equals(task.getStatus());
            boolean isFailed = Task.STATUS_FAILED.equals(task.getStatus());
            
            if (isActive) {
                checkboxComplete.setVisibility(View.VISIBLE);
                checkboxComplete.setChecked(false);
                textStatus.setVisibility(View.GONE);
                cardTask.setAlpha(1.0f);
            } else if (isCompleted) {
                checkboxComplete.setVisibility(View.GONE);
                textStatus.setVisibility(View.VISIBLE);
                textStatus.setText("DONE");
                textStatus.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.secondary_20));
                textStatus.setTextColor(ContextCompat.getColor(context, R.color.secondary));
                cardTask.setAlpha(0.7f);
            } else if (isFailed) {
                checkboxComplete.setVisibility(View.GONE);
                textStatus.setVisibility(View.VISIBLE);
                textStatus.setText("FAILED");
                textStatus.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.status_failed));
                textStatus.setTextColor(ContextCompat.getColor(context, R.color.white));
                cardTask.setAlpha(0.5f);
            } else {
                checkboxComplete.setVisibility(View.GONE);
                textStatus.setVisibility(View.VISIBLE);
                textStatus.setText(task.getStatus());
                cardTask.setAlpha(0.7f);
            }
            
            // Category color indicator
            setCategoryColor(task, context);
            
            // Click listeners
            cardTask.setOnClickListener(v -> listener.onTaskClick(task));
            cardTask.setOnLongClickListener(v -> {
                listener.onTaskLongClick(task);
                return true;
            });
            
            checkboxComplete.setOnCheckedChangeListener(null);
            checkboxComplete.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && isActive) {
                    listener.onTaskComplete(task, true);
                }
            });
        }
        
        private void bindDifficulty(String difficulty, Context context) {
            if (difficulty == null) {
                textDifficulty.setText("Normal");
                iconDifficulty.setColorFilter(ContextCompat.getColor(context, R.color.difficulty_2));
                return;
            }
            
            switch (difficulty) {
                case Task.DIFFICULTY_VERY_EASY:
                    textDifficulty.setText("Very Easy");
                    iconDifficulty.setColorFilter(ContextCompat.getColor(context, R.color.difficulty_1));
                    break;
                case Task.DIFFICULTY_EASY:
                    textDifficulty.setText("Easy");
                    iconDifficulty.setColorFilter(ContextCompat.getColor(context, R.color.difficulty_2));
                    break;
                case Task.DIFFICULTY_HARD:
                    textDifficulty.setText("Hard");
                    iconDifficulty.setColorFilter(ContextCompat.getColor(context, R.color.difficulty_4));
                    break;
                case Task.DIFFICULTY_EXTREME:
                    textDifficulty.setText("Extreme");
                    iconDifficulty.setColorFilter(ContextCompat.getColor(context, R.color.difficulty_5));
                    break;
                default:
                    textDifficulty.setText("Normal");
                    iconDifficulty.setColorFilter(ContextCompat.getColor(context, R.color.difficulty_3));
            }
        }
        
        private void setCategoryColor(Task task, Context context) {
            // Default colors based on difficulty for now
            String difficulty = task.getDifficulty();
            int color;
            if (Task.DIFFICULTY_EXTREME.equals(difficulty)) {
                color = ContextCompat.getColor(context, R.color.difficulty_5);
            } else if (Task.DIFFICULTY_HARD.equals(difficulty)) {
                color = ContextCompat.getColor(context, R.color.difficulty_4);
            } else if (Task.DIFFICULTY_EASY.equals(difficulty)) {
                color = ContextCompat.getColor(context, R.color.difficulty_2);
            } else {
                color = ContextCompat.getColor(context, R.color.primary);
            }
            categoryIndicator.setBackgroundColor(color);
        }
        
        private String formatDueDate(long timestamp) {
            Date date = new Date(timestamp);
            Date today = new Date();
            
            // Check if it's today
            SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            if (dayFormat.format(date).equals(dayFormat.format(today))) {
                return todayFormat.format(date);
            }
            
            return dateFormat.format(date);
        }
    }
}
