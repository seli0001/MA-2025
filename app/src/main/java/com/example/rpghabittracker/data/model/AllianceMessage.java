package com.example.rpghabittracker.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

/**
 * Represents a chat message in an alliance
 */
@Entity(tableName = "alliance_messages")
public class AllianceMessage implements Serializable {
    
    @PrimaryKey(autoGenerate = false)
    @NonNull
    private String id;
    
    private String allianceId;
    private String senderId;
    private String senderUsername;
    private String senderAvatar;
    private String message;
    private long timestamp;
    
    public AllianceMessage() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public AllianceMessage(String allianceId, String senderId, String senderUsername, String message) {
        this();
        this.id = allianceId + "_" + System.currentTimeMillis();
        this.allianceId = allianceId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.message = message;
    }
    
    // Getters and setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    
    public String getAllianceId() { return allianceId; }
    public void setAllianceId(String allianceId) { this.allianceId = allianceId; }
    
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    
    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }
    
    public String getSenderAvatar() { return senderAvatar; }
    public void setSenderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
