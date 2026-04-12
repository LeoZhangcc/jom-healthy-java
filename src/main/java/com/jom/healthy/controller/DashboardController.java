package com.jom.healthy.controller;


import com.jom.healthy.service.FoodNutritionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/dashboard")
@Api(value = "Dashboard")
public class DashboardController {


    @Resource
    private FoodNutritionService foodNutritionService;

    @ApiOperation("心跳检测")
    @GetMapping("/heartBeatCheck")
    public String heartBeatCheck() {
        foodNutritionService.heartBeatCheck();
        return "ok";
    }
}
