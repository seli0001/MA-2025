package com.example.rpghabittracker.ui.badges;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpghabittracker.R;
import com.example.rpghabittracker.data.model.Badge;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying badges in a grid
 */
public class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder> {
    
    private List<Badge> badges = new ArrayList<>();
    private final OnBadgeClickListener listener;
    
    public interface OnBadgeClickListener {
        void onBadgeClick(Badge badge);
    }
    
    public BadgeAdapter(OnBadgeClickListener listener) {
        this.listener = listener;
    }
    
    public void setBadges(List<Badge> badges) {
        this.badges = badges;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_badge, parent, false);
        return new BadgeViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
        holder.bind(badges.get(position));
    }
    
    @Override
    public int getItemCount() {
        return badges.size();
    }
    
    class BadgeViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardBadge;
        private final TextView textIcon;
        private final TextView textName;
        private final View lockOverlay;
        
        BadgeViewHolder(@NonNull View itemView) {
            super(itemView);
            cardBadge = itemView.findViewById(R.id.cardBadge);
            textIcon = itemView.findViewById(R.id.textIcon);
            textName = itemView.findViewById(R.id.textName);
            lockOverlay = itemView.findViewById(R.id.lockOverlay);
        }
        
        void bind(Badge badge) {
            textIcon.setText(badge.getIcon());
            textName.setText(badge.getName());
            
            if (badge.isUnlocked()) {
                lockOverlay.setVisibility(View.GONE);
                cardBadge.setAlpha(1.0f);
            } else {
                lockOverlay.setVisibility(View.VISIBLE);
                cardBadge.setAlpha(0.5f);
            }
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBadgeClick(badge);
                }
            });
        }
    }
}
