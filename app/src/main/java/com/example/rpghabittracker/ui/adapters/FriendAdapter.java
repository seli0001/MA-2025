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
 * Adapter for displaying users (friends, search results, etc.)
 */
public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {

    public static final int MODE_FRIENDS = 0;
    public static final int MODE_SEARCH = 1;
    public static final int MODE_REQUESTS = 2;

    private List<UserItem> users = new ArrayList<>();
    private int mode = MODE_FRIENDS;
    private String currentUserId;
    private OnUserActionListener listener;

    public interface OnUserActionListener {
        void onAddFriend(UserItem user);
        void onRemoveFriend(UserItem user);
        void onAcceptRequest(UserItem user);
        void onRejectRequest(UserItem user);
        void onViewProfile(UserItem user);
    }

    public FriendAdapter(String currentUserId, OnUserActionListener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setUsers(List<UserItem> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    public void setMode(int mode) {
        this.mode = mode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserItem user = users.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageAvatar;
        private final TextView textUsername;
        private final TextView textLevel;
        private final TextView textTitle;
        private final MaterialButton buttonAction;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageAvatar = itemView.findViewById(R.id.imageAvatar);
            textUsername = itemView.findViewById(R.id.textUsername);
            textLevel = itemView.findViewById(R.id.textLevel);
            textTitle = itemView.findViewById(R.id.textTitle);
            buttonAction = itemView.findViewById(R.id.buttonAction);
            
            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onViewProfile(users.get(pos));
                }
            });
        }

        void bind(UserItem user) {
            textUsername.setText(user.username);
            textLevel.setText("LVL " + user.level);
            textTitle.setText(user.title != null ? user.title : "Početnik");
            
            // Set avatar based on avatar ID
            int avatarRes = getAvatarResource(user.avatar);
            imageAvatar.setImageResource(avatarRes);
            
            // Configure action button based on mode
            switch (mode) {
                case MODE_FRIENDS:
                    buttonAction.setText("Profil");
                    buttonAction.setOnClickListener(v -> {
                        if (listener != null) listener.onViewProfile(user);
                    });
                    break;
                    
                case MODE_SEARCH:
                    if (user.isFriend) {
                        buttonAction.setText("Prijatelj");
                        buttonAction.setEnabled(false);
                    } else if (user.requestSent) {
                        buttonAction.setText("Poslato");
                        buttonAction.setEnabled(false);
                    } else {
                        buttonAction.setText("Dodaj");
                        buttonAction.setEnabled(true);
                        buttonAction.setOnClickListener(v -> {
                            if (listener != null) listener.onAddFriend(user);
                        });
                    }
                    break;
                    
                case MODE_REQUESTS:
                    buttonAction.setText("Prihvati");
                    buttonAction.setOnClickListener(v -> {
                        if (listener != null) listener.onAcceptRequest(user);
                    });
                    break;
            }
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
    }

    // User data class
    public static class UserItem {
        public String id;
        public String username;
        public String avatar;
        public int level;
        public String title;
        public int xp;
        public boolean isFriend;
        public boolean requestSent;
        public boolean requestReceived;

        public UserItem(String id, String username, String avatar, int level, String title) {
            this.id = id;
            this.username = username;
            this.avatar = avatar;
            this.level = level;
            this.title = title;
        }
    }
}
