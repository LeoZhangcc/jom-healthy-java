package com.jom.healthy.controller;

import com.jom.healthy.dto.MealNutritionDto;
import com.jom.healthy.dto.MealPlanGenerateRequest;
import com.jom.healthy.service.AiMealPlanService;
import com.jom.healthy.service.TheMealService;
import com.jom.healthy.util.response.ResponseData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/meal")
@Api(value = "食谱接口",tags = {"食谱增删改查"})
public class MealController {


    @Resource
    private TheMealService mealService;

    @Resource
    private AiMealPlanService aiMealPlanService;

    @ResponseBody
    @ApiOperation("查询食谱")
    @PostMapping("/search")
    public ResponseData searchMeals(@RequestParam String keyword) {
        return ResponseData.success(mealService.searchMealsByNamePrefix(keyword));
    }

    @ResponseBody
    @ApiOperation("AI生成食谱计划")
    @PostMapping("/generatePlan")
    public ResponseData generateMealPlan(@RequestBody MealPlanGenerateRequest request) {
        return ResponseData.success(aiMealPlanService.generateMealPlanByGroq(request));
    }

}
