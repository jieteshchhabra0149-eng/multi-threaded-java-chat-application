package com.chatapp.controller;

import com.chatapp.model.ChatMessage;
import com.chatapp.model.UserSession;
import com.chatapp.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * STOMP WebSocket message controller.
 *
 * Handles all real-time message routing:
 * - User registration on connect
 * - Room join/leave
 * - Message send/receive
 * - Typing indicators
 *
 * Each @MessageMapping invocation runs on a virtual thread,
 * enabling extremely high concurrency (Java 21).
 */
@Controller
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * User connects and registers their session.
     * Called when client subscribes to /app/chat.register
     */
    @MessageMapping("/chat.register")
    public void registerUser(
        @Payload Map<String, String> payload,
        SimpMessageHeaderAccessor headerAccessor
    ) {
        String username = payload.get("username");
        String sessionId = headerAccessor.getSessionId();

        if (username == null || username.isBlank()) {
            username = "User-" + sessionId.substring(0, 6);
        }

        UserSession session = chatService.registerUser(username.trim(), sessionId);

        // Store username in WebSocket session attributes
        Map<String, Object> attrs = headerAccessor.getSessionAttributes();
        if (attrs != null) {
            attrs.put("username", session.getUsername());
            attrs.put("sessionId", sessionId);
        }

        // Auto-join general room
        chatService.joinRoom(sessionId, "general");

        log.info("Registered: {} on thread: {}", username, Thread.currentThread().getName());
    }

    /**
     * User sends a chat message.
     * Routed to /app/chat.send
     */
    @MessageMapping("/chat.send")
    public void sendMessage(
        @Payload ChatMessage message,
        SimpMessageHeaderAccessor headerAccessor
    ) {
        String sessionId = headerAccessor.getSessionId();
        chatService.processMessage(sessionId, message);
    }

    /**
     * User joins a room.
     * Routed to /app/chat.join
     */
    @MessageMapping("/chat.join")
    public void joinRoom(
        @Payload Map<String, String> payload,
        SimpMessageHeaderAccessor headerAccessor
    ) {
        String roomId = payload.get("roomId");
        String sessionId = headerAccessor.getSessionId();

        if (roomId != null && !roomId.isBlank()) {
            chatService.joinRoom(sessionId, roomId);
        }
    }

    /**
     * Typing indicator.
     * Routed to /app/chat.typing
     */
    @MessageMapping("/chat.typing")
    public void handleTyping(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        chatService.broadcastTyping(sessionId);
    }
}
