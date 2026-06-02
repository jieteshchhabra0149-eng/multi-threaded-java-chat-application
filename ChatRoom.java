package com.chatapp.model;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a chat room with thread-safe member tracking.
 */
public class ChatRoom {

    private final String id;
    private final String name;
    private final String description;
    private final String icon;
    private final Set<String> activeUsers;
    private volatile LocalDateTime lastActivity;
    private volatile int messageCount;

    public ChatRoom(String id, String name, String description, String icon) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.activeUsers = ConcurrentHashMap.newKeySet();
        this.lastActivity = LocalDateTime.now();
        this.messageCount = 0;
    }

    public void addUser(String username) {
        activeUsers.add(username);
        lastActivity = LocalDateTime.now();
    }

    public void removeUser(String username) {
        activeUsers.remove(username);
    }

    public void incrementMessages() {
        messageCount++;
        lastActivity = LocalDateTime.now();
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
    public Set<String> getActiveUsers() { return activeUsers; }
    public int getActiveUserCount() { return activeUsers.size(); }
    public LocalDateTime getLastActivity() { return lastActivity; }
    public int getMessageCount() { return messageCount; }
}
