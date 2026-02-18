package com.example.rpghabittracker.ui.categories;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpghabittracker.R;
import com.example.rpghabittracker.data.model.Category;
import com.example.rpghabittracker.data.repository.CategoryRepository;
import com.example.rpghabittracker.ui.adapters.CategoryAdapter;
import com.example.rpghabittracker.ui.viewmodel.CategoryViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Activity for managing categories (CRUD operations)
 */
public class CategoriesActivity extends AppCompatActivity implements CategoryAdapter.CategoryClickListener {

    private CategoryViewModel categoryViewModel;
    private CategoryAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyView;
    private String userId;
    
    // Available colors for categories
    private final String[] colorOptions = CategoryRepository.DEFAULT_COLORS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        // Get current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }
        userId = currentUser.getUid();

        setupViews();
        setupViewModel();
    }

    private void setupViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.categoriesRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        FloatingActionButton fabAdd = findViewById(R.id.fabAddCategory);

        adapter = new CategoryAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showAddCategoryDialog());
    }

    private void setupViewModel() {
        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);
        categoryViewModel.startListening(userId);

        categoryViewModel.getUserCategories(userId).observe(this, categories -> {
            if (categories == null || categories.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                adapter.submitList(categories);
            }
        });
    }

    private void showAddCategoryDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null);
        EditText editName = dialogView.findViewById(R.id.editCategoryName);
        GridLayout colorGrid = dialogView.findViewById(R.id.colorGrid);

        final String[] selectedColor = {colorOptions[0]};

        // Add color options
        for (String color : colorOptions) {
            View colorView = createColorOption(color, colorGrid, selectedColor);
            colorGrid.addView(colorView);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Nova kategorija")
                .setView(dialogView)
                .setPositiveButton("Dodaj", (dialog, which) -> {
                    String name = editName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Unesite naziv kategorije", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    Category newCategory = new Category(userId, name, selectedColor[0]);
                    categoryViewModel.insertCategory(newCategory);
                    Toast.makeText(this, "Kategorija dodata", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }

    private View createColorOption(String color, GridLayout parent, String[] selectedColor) {
        View view = new View(this);
        int size = (int) (48 * getResources().getDisplayMetrics().density);
        int margin = (int) (4 * getResources().getDisplayMetrics().density);
        
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = size;
        params.height = size;
        params.setMargins(margin, margin, margin, margin);
        view.setLayoutParams(params);
        
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(Color.parseColor(color));
        drawable.setStroke(color.equals(selectedColor[0]) ? 4 : 0, 
                          getColor(R.color.md_theme_light_primary));
        view.setBackground(drawable);
        
        view.setOnClickListener(v -> {
            selectedColor[0] = color;
            // Update all color views
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                GradientDrawable bg = (GradientDrawable) child.getBackground();
                String childColor = colorOptions[i];
                bg.setStroke(childColor.equals(color) ? 4 : 0, 
                            getColor(R.color.md_theme_light_primary));
            }
        });
        
        return view;
    }

    @Override
    public void onCategoryClick(Category category) {
        showEditCategoryDialog(category);
    }

    @Override
    public void onEditColorClick(Category category) {
        showColorPickerDialog(category);
    }

    @Override
    public void onDeleteClick(Category category) {
        categoryViewModel.isCategoryInUse(category.getId(), isInUse -> runOnUiThread(() -> {
            if (isInUse) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Nije moguće obrisati")
                        .setMessage("Kategorija \"" + category.getName() +
                                "\" se koristi u jednom ili više zadataka i ne može biti obrisana.")
                        .setPositiveButton("U redu", null)
                        .show();
            } else {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Obriši kategoriju")
                        .setMessage("Da li ste sigurni da želite da obrišete kategoriju \"" +
                                category.getName() + "\"?")
                        .setPositiveButton("Obriši", (dialog, which) -> {
                            categoryViewModel.deleteCategory(category);
                            Toast.makeText(this, "Kategorija obrisana", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Otkaži", null)
                        .show();
            }
        }));
    }

    private void showEditCategoryDialog(Category category) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null);
        EditText editName = dialogView.findViewById(R.id.editCategoryName);
        editName.setText(category.getName());
        
        GridLayout colorGrid = dialogView.findViewById(R.id.colorGrid);
        final String[] selectedColor = {category.getColor()};

        for (String color : colorOptions) {
            View colorView = createColorOption(color, colorGrid, selectedColor);
            colorGrid.addView(colorView);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Uredi kategoriju")
                .setView(dialogView)
                .setPositiveButton("Sačuvaj", (dialog, which) -> {
                    String name = editName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Unesite naziv kategorije", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    category.setName(name);
                    category.setColor(selectedColor[0]);
                    categoryViewModel.updateCategory(category);
                    Toast.makeText(this, "Kategorija ažurirana", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }

    private void showColorPickerDialog(Category category) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_color_picker, null);
        GridLayout colorGrid = dialogView.findViewById(R.id.colorGrid);
        final String[] selectedColor = {category.getColor()};

        for (String color : colorOptions) {
            View colorView = createColorOption(color, colorGrid, selectedColor);
            colorGrid.addView(colorView);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Izaberi boju")
                .setView(dialogView)
                .setPositiveButton("Sačuvaj", (dialog, which) -> {
                    categoryViewModel.updateCategoryColor(category.getId(), selectedColor[0], 
                            userId, (success, error) -> {
                        runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(this, "Boja promenjena", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (categoryViewModel != null) {
            categoryViewModel.stopListening();
        }
    }
}
