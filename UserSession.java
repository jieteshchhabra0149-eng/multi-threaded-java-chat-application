package com.chatapp.model;

import java.time.LocalDateTime;

/**
 * Represents a connected chat user session.
 */
public class UserSession {

    // Curated set of emoji avatars
    private static final String[] AVATARS = {
        "🦊", "🐺", "🦁", "🐯", "🦋", "🐉", "🦄", "🐙",
        "🦝", "🐧", "🦜", "🐬", "🦩", "🦚", "🐺", "🦔"
    };

    // Distinctive user colors
    private static final String[] COLORS = {
        "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4",
        "#FFEAA7", "#DDA0DD", "#98D8C8", "#F7DC6F",
        "#BB8FCE", "#85C1E9", "#82E0AA", "#F8C471",
        "#F1948A", "#AED6F1", "#A9DFBF", "#FAD7A0"
    };

    private final String username;
    private final String sessionId;
    private final String avatar;
    private final String color;
    private volatile String currentRoom;
    private final LocalDateTime connectedAt;
    private volatile LocalDateTime lastSeen;
    private volatile boolean online;

    public UserSession(String username, String sessionId, int colorIndex) {
        this.username = username;
        this.sessionId = sessionId;
        this.avatar = AVATARS[colorIndex % AVATARS.length];
        this.color = COLORS[colorIndex % COLORS.length];
        this.connectedAt = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
        this.online = true;
        this.currentRoom = "general";
    }

    public void updateLastSeen() {
        this.lastSeen = LocalDateTime.now();
    }

    // Getters and setters
    public String getUsername() { return username; }
    public String getSessionId() { return sessionId; }
    public String getAvatar() { return avatar; }
    public String getColor() { return color; }
    public String getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(String room) { this.currentRoom = room; }
    public LocalDateTime getConnectedAt() { return connectedAt; }
    public LocalDateTime getLastSeen() { return lastSeen; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
}
