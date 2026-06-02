package com.chatapp.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable chat message model.
 * Transferred over WebSocket as JSON.
 */
public record ChatMessage(
    String id,
    String content,
    String sender,
    String avatar,       // emoji avatar
    String color,        // user's assigned color
    MessageType type,
    String room,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime timestamp,

    String threadId,     // for threading / reply chains
    String replyTo       // message id being replied to
) {

    public enum MessageType {
        CHAT,       // normal message
        JOIN,       // user joined
        LEAVE,      // user left
        SYSTEM,     // system notification
        TYPING,     // typing indicator
        REACTION    // emoji reaction
    }

    /** Factory: create a standard chat message */
    public static ChatMessage chat(String sender, String avatar, String color,
                                   String content, String room) {
        return new ChatMessage(
            UUID.randomUUID().toString(),
            content, sender, avatar, color,
            MessageType.CHAT, room,
            LocalDateTime.now(),
            null, null
        );
    }

    /** Factory: create a join/leave notification */
    public static ChatMessage event(String sender, String avatar, String color,
                                    MessageType type, String room) {
        String msg = type == MessageType.JOIN
            ? sender + " joined the room"
            : sender + " left the room";
        return new ChatMessage(
            UUID.randomUUID().toString(),
            msg, "System", "🔔", "#888",
            type, room,
            LocalDateTime.now(),
            null, null
        );
    }

    /** Factory: typing indicator */
    public static ChatMessage typing(String sender, String room) {
        return new ChatMessage(
            UUID.randomUUID().toString(),
            sender + " is typing...", sender, "⌨️", "#888",
            MessageType.TYPING, room,
            LocalDateTime.now(),
            null, null
        );
    }
}
