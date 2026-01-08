package com.example.rpghabittracker.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

/**
 * Represents an alliance invite
 */
@Entity(tableName = "alliance_invites")
public class AllianceInvite implements Serializable {
    
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";
    
    @PrimaryKey(autoGenerate = false)
    @NonNull
    private String id;
    
    private String allianceId;
    private String allianceName;
    private String senderId;
    private String senderUsername;
    private String receiverId;
    private String status;
    private long createdAt;
    
    public AllianceInvite() {
        this.createdAt = System.currentTimeMillis();
        this.status = STATUS_PENDING;
    }
    
    public AllianceInvite(String allianceId, String allianceName, String senderId, 
                          String senderUsername, String receiverId) {
        this();
        this.id = allianceId + "_" + receiverId;
        this.allianceId = allianceId;
        this.allianceName = allianceName;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.receiverId = receiverId;
    }
    
    // Getters and setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    
    public String getAllianceId() { return allianceId; }
    public void setAllianceId(String allianceId) { this.allianceId = allianceId; }
    
    public String getAllianceName() { return allianceName; }
    public void setAllianceName(String allianceName) { this.allianceName = allianceName; }
    
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    
    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }
    
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
