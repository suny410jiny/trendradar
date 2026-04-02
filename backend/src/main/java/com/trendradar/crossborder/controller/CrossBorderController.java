package com.trendradar.crossborder.controller;

import com.trendradar.common.response.ApiResponse;
import com.trendradar.crossborder.dto.GlobalLocalResponse;
import com.trendradar.crossborder.dto.OpportunityResponse;
import com.trendradar.crossborder.dto.PropagationResponse;
import com.trendradar.crossborder.service.CrossBorderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/crossborder")
@RequiredArgsConstructor
public class CrossBorderController {

    private final CrossBorderService crossBorderService;

    @GetMapping("/opportunities")
    public ApiResponse<List<OpportunityResponse>> getOpportunities(
            @RequestParam String country) {
        return ApiResponse.of(crossBorderService.findOpportunities(country));
    }

    @GetMapping("/propagation")
    public ApiResponse<PropagationResponse> getPropagation(
            @RequestParam String keyword) {
        return ApiResponse.of(crossBorderService.findPropagation(keyword));
    }

    @GetMapping("/global-vs-local")
    public ApiResponse<GlobalLocalResponse> getGlobalVsLocal(
            @RequestParam String country) {
        return ApiResponse.of(crossBorderService.findGlobalVsLocal(country));
    }
}
