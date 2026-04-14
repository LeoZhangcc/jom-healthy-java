package com.jom.healthy.controller;

import com.jom.healthy.service.BmiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController // 告诉 Spring 这是一个接收 Web 请求的接待员
@RequestMapping("/api/bmi") // 设定这个类的基础路由地址
@CrossOrigin(origins = "*") // 允许前端(比如手机APP或网页)跨域访问
public class BmiController {

    @Autowired
    private BmiService bmiService;

    /**
     * 测试接口：评估儿童/青少年 BMI
     * 访问路径：GET /api/bmi/evaluate
     */
    @GetMapping("/evaluate")
    public String evaluate(
            @RequestParam double heightCm,
            @RequestParam double weightKg,
            @RequestParam String birthDateStr,
            @RequestParam int gender) {

        // 把前端传进来的数据，直接交给 Service 去计算
        return bmiService.evaluateChildBmi(heightCm, weightKg, birthDateStr, gender);
    }
}