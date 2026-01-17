package com.example.rpghabittracker.ui.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpghabittracker.R;
import com.example.rpghabittracker.data.model.Category;

/**
 * RecyclerView adapter for displaying categories
 */
public class CategoryAdapter extends ListAdapter<Category, CategoryAdapter.CategoryViewHolder> {

    private final CategoryClickListener listener;

    public interface CategoryClickListener {
        void onCategoryClick(Category category);
        void onEditColorClick(Category category);
        void onDeleteClick(Category category);
    }

    public CategoryAdapter(CategoryClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Category> DIFF_CALLBACK = new DiffUtil.ItemCallback<Category>() {
        @Override
        public boolean areItemsTheSame(@NonNull Category oldItem, @NonNull Category newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Category oldItem, @NonNull Category newItem) {
            return oldItem.getName().equals(newItem.getName()) &&
                   oldItem.getColor().equals(newItem.getColor());
        }
    };

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = getItem(position);
        holder.bind(category);
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final View colorIndicator;
        private final TextView categoryName;
        private final TextView taskCount;
        private final ImageButton btnEditColor;
        private final ImageButton btnDelete;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            colorIndicator = itemView.findViewById(R.id.colorIndicator);
            categoryName = itemView.findViewById(R.id.categoryName);
            taskCount = itemView.findViewById(R.id.taskCount);
            btnEditColor = itemView.findViewById(R.id.btnEditColor);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(Category category) {
            categoryName.setText(category.getName());
            
            // Set color indicator
            try {
                GradientDrawable background = (GradientDrawable) colorIndicator.getBackground();
                background.setColor(Color.parseColor(category.getColor()));
            } catch (Exception e) {
                // Default color if parsing fails
                GradientDrawable background = (GradientDrawable) colorIndicator.getBackground();
                background.setColor(Color.GRAY);
            }
            
            // Task count will be updated separately if needed
            taskCount.setText("Tapni za detalje");
            
            // Click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCategoryClick(category);
                }
            });
            
            btnEditColor.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditColorClick(category);
                }
            });
            
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(category);
                }
            });
        }
    }
}
