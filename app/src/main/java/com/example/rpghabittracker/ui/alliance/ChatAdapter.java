package com.example.rpghabittracker.ui.alliance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rpghabittracker.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for chat messages
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private final List<AllianceChatActivity.ChatMessage> messages;
    private final String currentUserId;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ChatAdapter(List<AllianceChatActivity.ChatMessage> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AllianceChatActivity.ChatMessage message = messages.get(position);
        boolean isOwnMessage = message.senderId != null && message.senderId.equals(currentUserId);

        // Show sender name for others' messages
        if (!isOwnMessage) {
            holder.textSender.setVisibility(View.VISIBLE);
            holder.textSender.setText(message.senderName);
        } else {
            holder.textSender.setVisibility(View.GONE);
        }

        holder.textMessage.setText(message.text);

        // Format timestamp
        if (message.timestamp != null) {
            Date date = message.timestamp.toDate();
            holder.textTime.setText(timeFormat.format(date));
        } else {
            holder.textTime.setText("");
        }

        // Align message based on sender
        ViewGroup.LayoutParams rawParams = holder.cardMessage.getLayoutParams();
        if (rawParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) rawParams;
            if (isOwnMessage) {
                params.setMargins(100, 4, 16, 4);
                holder.cardMessage.setBackgroundResource(R.drawable.bg_message_sent);
            } else {
                params.setMargins(16, 4, 100, 4);
                holder.cardMessage.setBackgroundResource(R.drawable.bg_message_received);
            }
            holder.cardMessage.setLayoutParams(params);
        } else if (isOwnMessage) {
            holder.cardMessage.setBackgroundResource(R.drawable.bg_message_sent);
        } else {
            holder.cardMessage.setBackgroundResource(R.drawable.bg_message_received);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View cardMessage;
        TextView textSender;
        TextView textMessage;
        TextView textTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardMessage = itemView.findViewById(R.id.cardMessage);
            textSender = itemView.findViewById(R.id.textSender);
            textMessage = itemView.findViewById(R.id.textMessage);
            textTime = itemView.findViewById(R.id.textTime);
        }
    }
}
