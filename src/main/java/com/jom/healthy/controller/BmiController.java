package com.jom.healthy.controller;

import com.jom.healthy.service.BmiService;
import com.jom.healthy.service.BmiStandardReferenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController // 告诉 Spring 这是一个接收 Web 请求的接待员
@RequestMapping("/api/bmi") // 设定这个类的基础路由地址
@CrossOrigin(origins = "*") // 允许前端(比如手机APP或网页)跨域访问
public class BmiController {

    @Autowired
    private BmiService bmiService;

    @Autowired
    private BmiStandardReferenceService bmiStandardReferenceService; // 🚀 注入我们新写的 WHO 标尺服务

    /**
     * 1. 评估接口：计算并返回健康建议
     * 访问路径：GET /api/bmi/evaluate
     */
    @GetMapping("/evaluate")
    public String evaluate(
            @RequestParam double heightCm,
            @RequestParam double weightKg,
            @RequestParam String birthDateStr,
            @RequestParam int gender) {

        // 调用 BmiService 逻辑
        return bmiService.evaluateChildBmi(heightCm, weightKg, birthDateStr, gender);
    }

    /**
     * 2. 标尺接口：为前端 Growth 页面提供 WHO 历史对比线数据
     * 访问路径：GET /api/bmi/who-standards
     */
    @GetMapping("/who-standards")
    public ResponseEntity<Map<String, Object>> getWhoStandards(
            @RequestParam String type,   // 前端传过来的 "MONTH" 或 "YEAR"
            @RequestParam int gender) {  // 前端传过来的性别 1 或 0

        // 调用 WhoStandardService 从数据库查真实数据
        Map<String, Object> data = bmiStandardReferenceService.getWhoChartData(type, gender);

        return ResponseEntity.ok(data);
    }
}