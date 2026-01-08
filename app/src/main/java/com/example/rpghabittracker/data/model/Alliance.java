package com.example.rpghabittracker.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an alliance (savez) for special missions
 */
@Entity(tableName = "alliances")
public class Alliance implements Serializable {
    
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_IN_MISSION = "IN_MISSION";
    public static final String STATUS_DISBANDED = "DISBANDED";
    
    @PrimaryKey(autoGenerate = false)
    @NonNull
    private String id;
    
    private String name;
    private String leaderId;
    private String leaderUsername;
    private List<String> memberIds;
    private String status;
    private long createdAt;
    
    // Mission data
    private boolean missionActive;
    private long missionStartTime;
    private long missionEndTime;
    private int missionBossMaxHp;
    private int missionBossCurrentHp;
    
    public Alliance() {
        this.createdAt = System.currentTimeMillis();
        this.status = STATUS_ACTIVE;
        this.memberIds = new ArrayList<>();
        this.missionActive = false;
    }
    
    public Alliance(String id, String name, String leaderId, String leaderUsername) {
        this();
        this.id = id;
        this.name = name;
        this.leaderId = leaderId;
        this.leaderUsername = leaderUsername;
        this.memberIds.add(leaderId);
    }
    
    // Getters and setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getLeaderId() { return leaderId; }
    public void setLeaderId(String leaderId) { this.leaderId = leaderId; }
    
    public String getLeaderUsername() { return leaderUsername; }
    public void setLeaderUsername(String leaderUsername) { this.leaderUsername = leaderUsername; }
    
    public List<String> getMemberIds() { return memberIds; }
    public void setMemberIds(List<String> memberIds) { this.memberIds = memberIds; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public boolean isMissionActive() { return missionActive; }
    public void setMissionActive(boolean missionActive) { this.missionActive = missionActive; }
    
    public long getMissionStartTime() { return missionStartTime; }
    public void setMissionStartTime(long missionStartTime) { this.missionStartTime = missionStartTime; }
    
    public long getMissionEndTime() { return missionEndTime; }
    public void setMissionEndTime(long missionEndTime) { this.missionEndTime = missionEndTime; }
    
    public int getMissionBossMaxHp() { return missionBossMaxHp; }
    public void setMissionBossMaxHp(int missionBossMaxHp) { this.missionBossMaxHp = missionBossMaxHp; }
    
    public int getMissionBossCurrentHp() { return missionBossCurrentHp; }
    public void setMissionBossCurrentHp(int missionBossCurrentHp) { this.missionBossCurrentHp = missionBossCurrentHp; }
    
    // Helper methods
    public void addMember(String memberId) {
        if (!memberIds.contains(memberId)) {
            memberIds.add(memberId);
        }
    }
    
    public void removeMember(String memberId) {
        memberIds.remove(memberId);
    }
    
    public boolean isLeader(String userId) {
        return leaderId != null && leaderId.equals(userId);
    }
    
    public boolean isMember(String userId) {
        return memberIds.contains(userId);
    }
    
    public int getMemberCount() {
        return memberIds.size();
    }
    
    public void startMission() {
        this.missionActive = true;
        this.missionStartTime = System.currentTimeMillis();
        // 2 weeks duration (for demo, use shorter time)
        this.missionEndTime = missionStartTime + (14L * 24 * 60 * 60 * 1000);
        this.missionBossMaxHp = 100 * memberIds.size();
        this.missionBossCurrentHp = missionBossMaxHp;
        this.status = STATUS_IN_MISSION;
    }
    
    public void damageBoss(int damage) {
        this.missionBossCurrentHp = Math.max(0, this.missionBossCurrentHp - damage);
    }
    
    public boolean isMissionDefeated() {
        return missionBossCurrentHp <= 0;
    }
    
    public double getMissionProgress() {
        if (missionBossMaxHp == 0) return 0;
        return 1.0 - ((double) missionBossCurrentHp / missionBossMaxHp);
    }
}
