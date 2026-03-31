package com.trendradar.trending.controller;

import com.trendradar.common.response.ApiResponse;
import com.trendradar.trending.dto.BriefingResponse;
import com.trendradar.trending.service.BriefingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BriefingController {

    private final BriefingService briefingService;

    @GetMapping("/briefing")
    public ApiResponse<BriefingResponse> getBriefing(@RequestParam String country) {
        return ApiResponse.of(briefingService.generateBriefing(country));
    }
}
