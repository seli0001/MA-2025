package com.example.rpghabittracker.ui.alliance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpghabittracker.R;

import java.util.List;

/**
 * Adapter for displaying alliance members
 */
public class AllianceMemberAdapter extends RecyclerView.Adapter<AllianceMemberAdapter.ViewHolder> {

    private final List<AllianceActivity.MemberItem> members;

    public AllianceMemberAdapter(List<AllianceActivity.MemberItem> members) {
        this.members = members;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alliance_member, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AllianceActivity.MemberItem member = members.get(position);
        holder.textUsername.setText(member.username);
        holder.textLevel.setText("Level " + member.level);
        
        int avatarRes = getAvatarResource(member.avatar);
        holder.imageAvatar.setImageResource(avatarRes);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    private int getAvatarResource(String avatarId) {
        if (avatarId == null) return R.drawable.ic_avatar_placeholder;
        switch (avatarId) {
            case "avatar_1": return R.drawable.ic_avatar_1;
            case "avatar_2": return R.drawable.ic_avatar_2;
            case "avatar_3": return R.drawable.ic_avatar_3;
            case "avatar_4": return R.drawable.ic_avatar_4;
            case "avatar_5": return R.drawable.ic_avatar_5;
            default: return R.drawable.ic_avatar_placeholder;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageAvatar;
        TextView textUsername;
        TextView textLevel;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageAvatar = itemView.findViewById(R.id.imageAvatar);
            textUsername = itemView.findViewById(R.id.textUsername);
            textLevel = itemView.findViewById(R.id.textLevel);
        }
    }
}
