package com.example.rpghabittracker.ui.equipment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpghabittracker.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for equipment grid
 */
public class EquipmentAdapter extends RecyclerView.Adapter<EquipmentAdapter.ViewHolder> {

    private List<EquipmentActivity.EquipmentItem> items = new ArrayList<>();
    private final OnEquipmentListener listener;

    public interface OnEquipmentListener {
        void onItemClick(EquipmentActivity.EquipmentItem item);
        void onActivate(EquipmentActivity.EquipmentItem item);
    }

    public EquipmentAdapter(OnEquipmentListener listener) {
        this.listener = listener;
    }

    public void setItems(List<EquipmentActivity.EquipmentItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_equipment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EquipmentActivity.EquipmentItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageIcon;
        private final TextView textName;
        private final TextView textType;
        private final TextView textQuantity;
        private final TextView textBonus;
        private final MaterialButton buttonActivate;
        private final View viewActive;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageIcon = itemView.findViewById(R.id.imageIcon);
            textName = itemView.findViewById(R.id.textName);
            textType = itemView.findViewById(R.id.textType);
            textQuantity = itemView.findViewById(R.id.textQuantity);
            textBonus = itemView.findViewById(R.id.textBonus);
            buttonActivate = itemView.findViewById(R.id.buttonActivate);
            viewActive = itemView.findViewById(R.id.viewActive);
        }

        void bind(EquipmentActivity.EquipmentItem item) {
            textName.setText(item.name);
            textQuantity.setText("x" + item.quantity);
            
            // Set type label
            switch (item.type) {
                case "potion":
                    textType.setText("Napitak");
                    imageIcon.setImageResource(R.drawable.ic_potion);
                    break;
                case "clothing":
                    textType.setText("Odeća");
                    imageIcon.setImageResource(R.drawable.ic_clothing);
                    break;
                case "weapon":
                    textType.setText("Oružje");
                    imageIcon.setImageResource(R.drawable.ic_weapon);
                    break;
                default:
                    textType.setText(item.type);
                    imageIcon.setImageResource(R.drawable.ic_equipment);
            }
            
            // Show bonus
            if (item.bonus > 0) {
                textBonus.setText("+" + item.bonus + " PP");
                textBonus.setVisibility(View.VISIBLE);
            } else {
                textBonus.setVisibility(View.GONE);
            }
            
            // Active indicator
            viewActive.setVisibility(item.active ? View.VISIBLE : View.GONE);
            
            // Button text
            if ("potion".equals(item.type)) {
                buttonActivate.setText("Koristi");
            } else if (item.active) {
                buttonActivate.setText("Aktivno");
            } else {
                buttonActivate.setText("Aktiviraj");
            }
            
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
            
            buttonActivate.setOnClickListener(v -> {
                if (listener != null) listener.onActivate(item);
            });
        }
    }
}
