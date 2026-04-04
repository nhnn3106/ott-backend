package iuh.fit.se.analyticservice.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import iuh.fit.se.analyticservice.dto.DailyPostCountResponse;
import iuh.fit.se.analyticservice.dto.MessageTypesResponse;
import iuh.fit.se.analyticservice.dto.OverviewResponse;
import iuh.fit.se.analyticservice.dto.RecentNewUserDTO;
import iuh.fit.se.analyticservice.service.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    // 1) Overview tab
    @GetMapping("/overview")
    public OverviewResponse getOverview() {
        return adminAnalyticsService.getOverview();
    }

    // 2) Users tab
    @GetMapping("/users/recent")
    public List<RecentNewUserDTO> getRecentUsers() {
        return adminAnalyticsService.getRecentUsers();
    }

    // 3) Messaging tab
    @GetMapping("/messages/types")
    public MessageTypesResponse getMessageTypes() {
        return adminAnalyticsService.getMessageTypes();
    }

    // 4) Social tab
    @GetMapping("/social/posts/daily")
    public List<DailyPostCountResponse> getPostsDaily() {
        return adminAnalyticsService.getPostDaily7Days();
    }
}
