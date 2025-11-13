package com.dailycodework.excel2database.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/fixed")
public class FixedController {

    @GetMapping("/search")
    public String showFixedSearchPage() {
        return "fixed-search";
    }
}
