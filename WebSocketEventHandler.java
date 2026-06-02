package com.chatapp.handler;

import com.chatapp.service.ChatService;
import com.chatapp.model.ChatMessage;
import com.chatapp.model.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Optional;

/**
 * Handles WebSocket lifecycle events.
 *
 * SessionConnectedEvent → user connected
 * SessionDisconnectEvent → user disconnected, cleanup & broadcast
 *
 * Each event listener runs on a Spring-managed thread.
 * Disconnect cleanup is critical for accurate online user counts.
 */
@Component
public class WebSocketEventHandler {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventHandler.class);

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventHandler(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Fired when a WebSocket connection is established.
     */
    @EventListener
    public void handleWebSocketConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.info("WebSocket connected: session={} thread={}",
                accessor.getSessionId(), Thread.currentThread().getName());
    }

    /**
     * Fired when a WebSocket session disconnects.
     * Removes user from active sessions and broadcasts leave event.
     */
    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        log.info("WebSocket disconnected: session={}", sessionId);

        Optional<UserSession> userOpt = chatService.removeUser(sessionId);

        userOpt.ifPresent(user -> {
            // Broadcast leave event to the room they were in
            String roomId = user.getCurrentRoom();
            if (roomId != null) {
                ChatMessage leaveMsg = ChatMessage.event(
                    user.getUsername(), user.getAvatar(), user.getColor(),
                    ChatMessage.MessageType.LEAVE, roomId
                );
                messagingTemplate.convertAndSend("/topic/room/" + roomId, leaveMsg);
            }

            // Broadcast updated user list
            messagingTemplate.convertAndSend("/topic/users",
                chatService.getActiveUsers());

            log.info("User {} disconnected and cleaned up", user.getUsername());
        });
    }
}
