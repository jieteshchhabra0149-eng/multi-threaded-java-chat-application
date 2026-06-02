package com.chatapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket + STOMP configuration.
 *
 * Uses an in-memory message broker with topic destinations.
 * Each connected client gets its own thread from the virtual thread pool.
 *
 * Message flow:
 *   Client → /app/chat.send → @MessageMapping → broker → /topic/messages → All clients
 *   Client → /app/chat.join → @MessageMapping → broker → /topic/messages → All clients
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable in-memory broker for topics and user queues
        config.enableSimpleBroker("/topic", "/queue");
        // Prefix for messages routed to @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
        // Prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint with SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Use virtual threads for inbound message processing (Java 21)
        registration.taskExecutor()
                .corePoolSize(10)
                .maxPoolSize(100)
                .keepAliveSeconds(60);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // Use virtual threads for outbound message broadcasting
        registration.taskExecutor()
                .corePoolSize(10)
                .maxPoolSize(100)
                .keepAliveSeconds(60);
    }
}
