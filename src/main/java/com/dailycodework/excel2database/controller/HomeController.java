package com.dailycodework.excel2database.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String showHomePage() {
        return "index"; // Loads index.html from /templates
    }

    @GetMapping("/error")
    public String showErrorPage() {
        return "error";
    }
}
