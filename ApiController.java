package com.chatapp.controller;

import com.chatapp.model.ChatMessage;
import com.chatapp.model.ChatRoom;
import com.chatapp.model.UserSession;
import com.chatapp.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API for rooms, users, and stats.
 * Consumed by the frontend for initial data load.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {

    private final ChatService chatService;

    public ApiController(ChatService chatService) {
        this.chatService = chatService;
    }

    /** GET /api/rooms — list all rooms with metadata */
    @GetMapping("/rooms")
    public ResponseEntity<List<Map<String, Object>>> getRooms() {
        List<Map<String, Object>> rooms = chatService.getAllRooms().stream()
            .map(room -> {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("id", room.getId());
                r.put("name", room.getName());
                r.put("description", room.getDescription());
                r.put("icon", room.getIcon());
                r.put("activeUsers", room.getActiveUserCount());
                r.put("messageCount", room.getMessageCount());
                return r;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(rooms);
    }

    /** GET /api/rooms/{id}/history — message history for a room */
    @GetMapping("/rooms/{id}/history")
    public ResponseEntity<List<ChatMessage>> getRoomHistory(@PathVariable String id) {
        return ResponseEntity.ok(chatService.getRoomHistory(id));
    }

    /** GET /api/users — list online users */
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getUsers() {
        List<Map<String, Object>> users = chatService.getActiveUsers().stream()
            .map(u -> {
                Map<String, Object> user = new LinkedHashMap<>();
                user.put("username", u.getUsername());
                user.put("avatar", u.getAvatar());
                user.put("color", u.getColor());
                user.put("currentRoom", u.getCurrentRoom());
                user.put("online", u.isOnline());
                return user;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(users);
    }

    /** GET /api/stats — server stats */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("onlineUsers", chatService.getOnlineUserCount());
        stats.put("totalRooms", chatService.getAllRooms().size());
        stats.put("serverTime", new Date());
        stats.put("javaVersion", System.getProperty("java.version"));
        stats.put("threadModel", "Virtual Threads (Java 21)");
        return ResponseEntity.ok(stats);
    }

    /** POST /api/rooms — create a new room */
    @PostMapping("/rooms")
    public ResponseEntity<ChatRoom> createRoom(@RequestBody Map<String, String> body) {
        String id = body.getOrDefault("id", UUID.randomUUID().toString().substring(0, 8));
        String name = body.getOrDefault("name", "New Room");
        String description = body.getOrDefault("description", "");
        String icon = body.getOrDefault("icon", "💬");

        ChatRoom room = chatService.createRoom(id, name, description, icon);
        return ResponseEntity.ok(room);
    }
}
