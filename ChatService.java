package com.chatapp.service;

import com.chatapp.model.ChatMessage;
import com.chatapp.model.ChatRoom;
import com.chatapp.model.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core chat service — fully thread-safe.
 *
 * Thread safety strategy:
 * - ConcurrentHashMap for all shared state
 * - CopyOnWriteArrayList for message history (read-heavy)
 * - AtomicInteger for counters
 * - Volatile for single-writer fields
 * - Virtual threads (Java 21) for async broadcast
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final int MAX_HISTORY_PER_ROOM = 100;

    private final SimpMessagingTemplate messagingTemplate;

    // Thread-safe collections
    private final ConcurrentHashMap<String, UserSession> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ChatRoom> chatRooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<ChatMessage>> messageHistory = new ConcurrentHashMap<>();
    private final AtomicInteger userCounter = new AtomicInteger(0);

    // Virtual thread executor for async broadcasts
    private final ExecutorService broadcastExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public ChatService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        initializeDefaultRooms();
    }

    /**
     * Initialize default chat rooms.
     */
    private void initializeDefaultRooms() {
        createRoom("general", "General", "The main hangout for everyone", "💬");
        createRoom("tech", "Tech Talk", "Programming, tools, and all things tech", "💻");
        createRoom("random", "Random", "Off-topic and fun conversations", "🎲");
        createRoom("announcements", "Announcements", "Important updates and news", "📢");
    }

    /**
     * Create a new chat room.
     */
    public ChatRoom createRoom(String id, String name, String description, String icon) {
        ChatRoom room = new ChatRoom(id, name, description, icon);
        chatRooms.put(id, room);
        messageHistory.put(id, new CopyOnWriteArrayList<>());
        log.info("Created room: {} ({})", name, id);
        return room;
    }

    /**
     * Register a new user session.
     * Thread-safe via ConcurrentHashMap.putIfAbsent pattern.
     */
    public UserSession registerUser(String username, String sessionId) {
        int index = userCounter.getAndIncrement();
        UserSession session = new UserSession(username, sessionId, index);

        UserSession existing = activeSessions.putIfAbsent(sessionId, session);
        if (existing != null) {
            return existing; // Already registered
        }

        log.info("User registered: {} [{}] thread={}", username, sessionId,
                Thread.currentThread().getName());
        return session;
    }

    /**
     * Remove a user session on disconnect.
     */
    public Optional<UserSession> removeUser(String sessionId) {
        UserSession session = activeSessions.remove(sessionId);
        if (session != null) {
            ChatRoom room = chatRooms.get(session.getCurrentRoom());
            if (room != null) {
                room.removeUser(session.getUsername());
            }
            session.setOnline(false);
            log.info("User disconnected: {}", session.getUsername());
        }
        return Optional.ofNullable(session);
    }

    /**
     * Handle a user joining a room.
     * Async broadcast using virtual threads.
     */
    @Async("virtualThreadExecutor")
    public void joinRoom(String sessionId, String roomId) {
        UserSession user = activeSessions.get(sessionId);
        ChatRoom room = chatRooms.get(roomId);

        if (user == null || room == null) return;

        // Leave previous room
        String prevRoom = user.getCurrentRoom();
        if (prevRoom != null && !prevRoom.equals(roomId)) {
            ChatRoom prev = chatRooms.get(prevRoom);
            if (prev != null) prev.removeUser(user.getUsername());
        }

        user.setCurrentRoom(roomId);
        room.addUser(user.getUsername());

        // Broadcast join event
        ChatMessage joinMsg = ChatMessage.event(
            user.getUsername(), user.getAvatar(), user.getColor(),
            ChatMessage.MessageType.JOIN, roomId
        );

        broadcastToRoom(roomId, joinMsg);

        // Send room history to the joining user
        sendHistoryToUser(sessionId, roomId);

        log.info("User {} joined room {} on thread {}", user.getUsername(), roomId,
                Thread.currentThread().getName());
    }

    /**
     * Process and broadcast a chat message.
     * Thread-safe: CopyOnWriteArrayList for history, virtual thread for broadcast.
     */
    @Async("virtualThreadExecutor")
    public void processMessage(String sessionId, ChatMessage message) {
        UserSession user = activeSessions.get(sessionId);
        if (user == null) return;

        user.updateLastSeen();
        String roomId = user.getCurrentRoom();
        ChatRoom room = chatRooms.get(roomId);
        if (room == null) return;

        room.incrementMessages();

        // Store in history (thread-safe via CopyOnWriteArrayList)
        CopyOnWriteArrayList<ChatMessage> history = messageHistory.get(roomId);
        if (history != null) {
            history.add(message);
            // Trim history if needed
            if (history.size() > MAX_HISTORY_PER_ROOM) {
                history.remove(0);
            }
        }

        // Async broadcast to all room subscribers
        broadcastExecutor.submit(() -> {
            broadcastToRoom(roomId, message);
            log.debug("Message from {} broadcast on thread {}", user.getUsername(),
                    Thread.currentThread().getName());
        });
    }

    /**
     * Broadcast typing indicator (no history storage).
     */
    @Async("virtualThreadExecutor")
    public void broadcastTyping(String sessionId) {
        UserSession user = activeSessions.get(sessionId);
        if (user == null) return;

        ChatMessage typingMsg = ChatMessage.typing(user.getUsername(), user.getCurrentRoom());
        messagingTemplate.convertAndSend("/topic/room/" + user.getCurrentRoom() + "/typing", typingMsg);
    }

    /**
     * Broadcast a message to all subscribers of a room topic.
     */
    private void broadcastToRoom(String roomId, ChatMessage message) {
        messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
    }

    /**
     * Send message history to a specific user on join.
     */
    private void sendHistoryToUser(String sessionId, String roomId) {
        List<ChatMessage> history = messageHistory.getOrDefault(roomId, new CopyOnWriteArrayList<>());
        messagingTemplate.convertAndSendToUser(
            sessionId,
            "/queue/history",
            history
        );
    }

    // ── Getters ────────────────────────────────────────────────────────────────

    public Collection<ChatRoom> getAllRooms() {
        return chatRooms.values();
    }

    public Optional<ChatRoom> getRoom(String roomId) {
        return Optional.ofNullable(chatRooms.get(roomId));
    }

    public Collection<UserSession> getActiveUsers() {
        return activeSessions.values();
    }

    public int getOnlineUserCount() {
        return activeSessions.size();
    }

    public List<ChatMessage> getRoomHistory(String roomId) {
        return messageHistory.getOrDefault(roomId, new CopyOnWriteArrayList<>());
    }

    public Optional<UserSession> getUserBySession(String sessionId) {
        return Optional.ofNullable(activeSessions.get(sessionId));
    }
}
