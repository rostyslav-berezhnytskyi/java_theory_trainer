package com.els.javatheorytrainer.controller;

import com.els.javatheorytrainer.service.AiUsageLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AiUsageLogService aiUsageLogService;

    @GetMapping("/")
    public String home() {
        return "redirect:/admin";
    }

    @GetMapping("/admin")
    public String adminDashboard(Model model) {
        model.addAttribute("aiUsageSummaries", aiUsageLogService.summarizeByOperationAndModel());
        model.addAttribute("recentAiUsageLogs", aiUsageLogService.findRecentLogs(20));

        return "admin/dashboard";
    }
}
