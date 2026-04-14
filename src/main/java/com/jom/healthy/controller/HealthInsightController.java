package com.jom.healthy.controller;

import com.jom.healthy.entity.HealthInsight;
import com.jom.healthy.entity.InsightChartData;
import com.jom.healthy.mapper.HealthInsightMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/insights")
@CrossOrigin(origins = "*") // 允许前端 Next.js 跨域调用这个接口
public class HealthInsightController {

    @Autowired
    private HealthInsightMapper healthInsightMapper;

    // 前端调用 GET /api/insights 时，会触发这个方法
    @GetMapping
    public List<HealthInsight> getAllInsights() {
        return healthInsightMapper.findAll();
    }

    // 前端调用 GET /api/insights/1 时，会触发这个方法
    @GetMapping("/{id}")
    public HealthInsight getInsightDetail(@PathVariable Integer id) {
        return healthInsightMapper.findById(id);
    }

    @GetMapping("/{id}/chart")
    public List<InsightChartData> getChartData(@PathVariable Integer id) {
        return healthInsightMapper.findChartDataByInsightId(id);
    }
}