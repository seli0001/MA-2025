package com.example.rpghabittracker.ui.adapters;

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
 * Adapter for shop items
 */
public class ShopItemAdapter extends RecyclerView.Adapter<ShopItemAdapter.ViewHolder> {

    private List<ShopItem> items = new ArrayList<>();
    private int userCoins = 0;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onBuyClick(ShopItem item);
    }

    public ShopItemAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<ShopItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void setUserCoins(int coins) {
        this.userCoins = coins;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shop, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShopItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageItem;
        private final TextView textItemName;
        private final TextView textItemDescription;
        private final TextView textOwned;
        private final TextView textPrice;
        private final MaterialButton buttonBuy;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageItem = itemView.findViewById(R.id.imageItem);
            textItemName = itemView.findViewById(R.id.textItemName);
            textItemDescription = itemView.findViewById(R.id.textItemDescription);
            textOwned = itemView.findViewById(R.id.textOwned);
            textPrice = itemView.findViewById(R.id.textPrice);
            buttonBuy = itemView.findViewById(R.id.buttonBuy);
        }

        void bind(ShopItem item) {
            textItemName.setText(item.name);
            textItemDescription.setText(item.description);
            textPrice.setText(String.valueOf(item.price));
            
            // Set icon based on type
            int iconRes = getIconForType(item.type);
            imageItem.setImageResource(iconRes);
            
            // Set icon tint based on type
            int tintColor = getTintForType(item.type);
            imageItem.setColorFilter(itemView.getContext().getColor(tintColor));
            
            // Show owned count if applicable
            if (item.ownedCount > 0) {
                textOwned.setVisibility(View.VISIBLE);
                textOwned.setText("U posedu: " + item.ownedCount);
            } else {
                textOwned.setVisibility(View.GONE);
            }
            
            // Update button state
            boolean canAfford = userCoins >= item.price;
            buttonBuy.setEnabled(canAfford);
            buttonBuy.setText(canAfford ? "Kupi" : "Skup");
            buttonBuy.setAlpha(canAfford ? 1.0f : 0.5f);
            
            buttonBuy.setOnClickListener(v -> {
                if (listener != null && canAfford) {
                    listener.onBuyClick(item);
                }
            });
        }

        private int getIconForType(String type) {
            switch (type) {
                case "POTION":
                    return R.drawable.ic_potion;
                case "CLOTHING":
                    return R.drawable.ic_profile; // Using person icon for clothing
                case "WEAPON":
                    return R.drawable.ic_power;
                default:
                    return R.drawable.ic_shop;
            }
        }

        private int getTintForType(String type) {
            switch (type) {
                case "POTION":
                    return R.color.rpg_health;
                case "CLOTHING":
                    return R.color.primary;
                case "WEAPON":
                    return R.color.rpg_gold;
                default:
                    return R.color.text_secondary;
            }
        }
    }

    // Shop item data class
    public static class ShopItem {
        public final String id;
        public final String name;
        public final String description;
        public final String type;
        public final int price;
        public final String effect;
        public final double effectValue;
        public int ownedCount;

        public ShopItem(String id, String name, String description, String type, int price, String effect, double effectValue) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.type = type;
            this.price = price;
            this.effect = effect;
            this.effectValue = effectValue;
            this.ownedCount = 0;
        }
    }
}
