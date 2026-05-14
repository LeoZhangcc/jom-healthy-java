package com.jom.healthy.controller;

import com.jom.healthy.dto.PhysicalActivityItemDto;

import com.jom.healthy.service.PhysicalActivityItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activity")
@CrossOrigin(origins = "*") // 允许前端跨域访问
public class PhysicalActivityItemController {

    @Autowired
    private PhysicalActivityItemService activityService;

    /**
     * 前端调用示例：GET /api/activity/recommend?status=OVERWEIGHT
     */
    @GetMapping("/recommend")
    public ResponseEntity<List<PhysicalActivityItemDto>> getRecommendations(
            @RequestParam(defaultValue = "HEALTHY") String status) {

        List<PhysicalActivityItemDto> recommendations = activityService.getRecommendationsByStatus(status);
        return ResponseEntity.ok(recommendations);
    }
}