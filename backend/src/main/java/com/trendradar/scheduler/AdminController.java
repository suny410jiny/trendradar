package com.trendradar.scheduler;

import com.trendradar.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TrendingScheduler trendingScheduler;

    @PostMapping("/collect")
    public ApiResponse<String> triggerCollect() {
        trendingScheduler.collectAllCountries();
        return ApiResponse.of("수집 완료");
    }
}
