package com.chatapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the main chat UI page via Thymeleaf.
 * The entire frontend is rendered server-side by Java (Thymeleaf)
 * with client-side WebSocket interactivity.
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("appName", "NexusChat");
        model.addAttribute("version", "1.0.0");
        model.addAttribute("javaVersion", System.getProperty("java.version"));
        return "index";
    }
}
