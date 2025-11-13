package com.dailycodework.excel2database.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/student-search")
@RequiredArgsConstructor
@Slf4j
public class StudentSearchController {

    @GetMapping
    public String showSearchPage(Model model) {
        return "minimal-search";
    }
}
