package com.chatapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Multithreaded Chat Application.
 *
 * Architecture:
 * - Spring Boot 3.x backend
 * - STOMP over WebSocket for real-time messaging
 * - Virtual threads (Java 21) for high concurrency
 * - Thread-safe message broadcasting
 * - Thymeleaf + custom CSS/JS frontend (all Java-driven)
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class ChatApplication {

    public static void main(String[] args) {
        // Enable virtual threads for Java 21 high-throughput concurrency
        System.setProperty("spring.threads.virtual.enabled", "true");
        SpringApplication.run(ChatApplication.class, args);
    }
}
