# NexusChat — Multithreaded Java Chat Application

A **full-stack Java** real-time chat application built with Spring Boot, STOMP WebSocket, and Java 21 Virtual Threads. The frontend is rendered entirely by Java (Thymeleaf server-side templates) with WebSocket-driven real-time updates.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        FRONTEND (Java/Thymeleaf)                │
│  - Thymeleaf templates rendered by Spring MVC (Java)            │
│  - STOMP WebSocket client (SockJS fallback)                     │
│  - Vanilla JS for DOM updates only (no framework)               │
│  - REST API calls for initial data (rooms, users, stats)        │
└──────────────────────┬──────────────────────────────────────────┘
                       │  STOMP over WebSocket / HTTP
┌──────────────────────▼──────────────────────────────────────────┐
│                        BACKEND (Java / Spring Boot 3.x)         │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  WebSocket Layer                                          │   │
│  │  ├── WebSocketConfig  (STOMP broker, endpoints)          │   │
│  │  ├── ChatController   (@MessageMapping handlers)         │   │
│  │  └── WebSocketEventHandler (connect/disconnect events)   │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Service Layer                                            │   │
│  │  └── ChatService (thread-safe, virtual threads)          │   │
│  │       ├── ConcurrentHashMap<sessionId, UserSession>      │   │
│  │       ├── ConcurrentHashMap<roomId, ChatRoom>            │   │
│  │       ├── CopyOnWriteArrayList<ChatMessage> per room     │   │
│  │       └── ExecutorService (virtual threads)              │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  REST API (ApiController)                                 │   │
│  │  ├── GET  /api/rooms          - list rooms                │   │
│  │  ├── GET  /api/rooms/{id}/history                         │   │
│  │  ├── POST /api/rooms          - create room               │   │
│  │  ├── GET  /api/users          - online users              │   │
│  │  └── GET  /api/stats          - server stats              │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Threading Model (Java 21)                                │   │
│  │  ├── Virtual thread per WebSocket message (inbound)      │   │
│  │  ├── Virtual thread per broadcast (outbound)             │   │
│  │  ├── Async @Service methods on virtual thread executor   │   │
│  │  └── Bounded pool for CPU-intensive tasks                │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## Thread Safety Strategy

| Component | Strategy |
|-----------|----------|
| User sessions | `ConcurrentHashMap` (lock-free reads) |
| Chat rooms | `ConcurrentHashMap` (lock-free reads) |
| Message history | `CopyOnWriteArrayList` (read-optimized) |
| User counters | `AtomicInteger` (CAS operations) |
| Async broadcast | `Executors.newVirtualThreadPerTaskExecutor()` |
| Room membership | `ConcurrentHashMap.newKeySet()` |

## Message Flow

```
Client types → Enter
      │
      ▼
WebSocket frame → STOMP /app/chat.send
      │
      ▼
ChatController.sendMessage() [Virtual Thread N]
      │
      ▼
ChatService.processMessage() [@Async, Virtual Thread M]
      │
      ├──► History stored in CopyOnWriteArrayList
      │
      └──► BroadcastExecutor.submit() [Virtual Thread K]
                │
                ▼
          SimpMessagingTemplate.convertAndSend("/topic/room/general")
                │
                ▼
          All subscribed clients receive the message
```

## WebSocket Topics

| Topic | Description |
|-------|-------------|
| `/topic/room/{id}` | Messages for a specific room |
| `/topic/room/{id}/typing` | Typing indicators for a room |
| `/topic/users` | User list updates |
| `/user/queue/history` | Private history delivery on join |

## STOMP Endpoints (Client → Server)

| Destination | Payload | Description |
|-------------|---------|-------------|
| `/app/chat.register` | `{username}` | Register username |
| `/app/chat.send` | `ChatMessage` | Send a message |
| `/app/chat.join` | `{roomId}` | Join a room |
| `/app/chat.typing` | `{}` | Typing indicator |

## Project Structure

```
src/main/java/com/chatapp/
├── ChatApplication.java              ← Entry point, enables virtual threads
├── config/
│   ├── WebSocketConfig.java          ← STOMP broker, endpoints, thread pools
│   ├── SecurityConfig.java           ← Spring Security (permits all for demo)
│   └── ThreadingConfig.java          ← Java 21 virtual thread executors
├── model/
│   ├── ChatMessage.java              ← Immutable record (id, content, sender...)
│   ├── ChatRoom.java                 ← Room with ConcurrentHashSet members
│   └── UserSession.java              ← User with avatar/color assignment
├── service/
│   └── ChatService.java              ← Core logic, thread-safe, @Async methods
├── controller/
│   ├── ChatController.java           ← @MessageMapping WebSocket handlers
│   ├── ApiController.java            ← REST API (@RestController)
│   └── PageController.java           ← Thymeleaf page serving
└── handler/
    └── WebSocketEventHandler.java    ← Connect/disconnect lifecycle events

src/main/resources/
├── application.properties            ← Spring config
└── templates/
    └── index.html                    ← Thymeleaf template (full UI)
```

## Prerequisites

- **Java 21+** (for virtual threads)
- **Maven 3.8+**

## Run the Application

```bash
# Clone / navigate to project
cd chat-app

# Build and run
mvn spring-boot:run

# Or build JAR first
mvn clean package
java -jar target/multithreaded-chat-1.0.0.jar
```

Open: **http://localhost:8080**

## Key Features

- **Real-time messaging** via STOMP WebSocket with SockJS fallback
- **Multithreaded** — Java 21 Virtual Threads handle all I/O concurrently
- **Multiple rooms** — General, Tech Talk, Random, Announcements
- **Typing indicators** — debounced, auto-clears after 3s
- **Message history** — last 100 messages per room, delivered on join
- **User avatars** — emoji + color auto-assigned per user
- **Online presence** — join/leave events broadcast to room
- **Thread-safe** — ConcurrentHashMap, CopyOnWriteArrayList, AtomicInteger
- **Auto-reconnect** — client reconnects on WebSocket error
- **REST API** — rooms, users, stats endpoints
- **Java 21 features** — Records, sealed patterns, virtual threads

## Extending

**Add persistence (PostgreSQL):**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

**Add authentication (JWT):**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

**Add Redis pub/sub for multi-node:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

## Performance

With Java 21 Virtual Threads:
- Each WebSocket connection → 1 virtual thread (~1KB heap)
- 10,000 concurrent users → ~10MB thread overhead
- vs traditional threads → would need ~10GB at 1MB/thread

---

*Built with Spring Boot 3.x · Java 21 · STOMP WebSocket · Thymeleaf*
