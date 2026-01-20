package com.example.rpghabittracker.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.rpghabittracker.R;
import com.example.rpghabittracker.ui.battle.BattleActivity;
import com.google.android.material.button.MaterialButton;

/**
 * Fragment for Battle menu - launches BattleActivity
 */
public class BattleFragment extends Fragment {
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(getResources().getColor(R.color.md_theme_light_background, null));
        layout.setPadding(48, 48, 48, 48);
        
        // Boss Icon
        TextView bossIcon = new TextView(requireContext());
        bossIcon.setText("👹");
        bossIcon.setTextSize(80);
        bossIcon.setGravity(Gravity.CENTER);
        layout.addView(bossIcon);
        
        // Title
        TextView title = new TextView(requireContext());
        title.setText("Boss Battle");
        title.setTextSize(28);
        title.setTextColor(getResources().getColor(R.color.text_primary, null));
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.topMargin = 32;
        title.setLayoutParams(titleParams);
        layout.addView(title);
        
        // Description
        TextView desc = new TextView(requireContext());
        desc.setText("Use Power Points to attack the boss!\nShake your phone or tap to deal damage.");
        desc.setTextSize(14);
        desc.setTextColor(getResources().getColor(R.color.text_secondary, null));
        desc.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        descParams.topMargin = 16;
        desc.setLayoutParams(descParams);
        layout.addView(desc);
        
        // Start Battle Button
        MaterialButton battleButton = new MaterialButton(requireContext());
        battleButton.setText("⚔️ Start Battle");
        battleButton.setTextSize(16);
        battleButton.setCornerRadius(56);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                140
        );
        btnParams.topMargin = 48;
        battleButton.setLayoutParams(btnParams);
        battleButton.setPadding(64, 0, 64, 0);
        battleButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), BattleActivity.class);
            // Do not force boss level here; BattleActivity loads the persisted value.
            startActivity(intent);
        });
        layout.addView(battleButton);
        
        return layout;
    }
}
