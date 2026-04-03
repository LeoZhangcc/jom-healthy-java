package com.jom.healthy.controller;

import com.jom.healthy.service.FoodNutritionService;
import com.jom.healthy.util.response.ResponseData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController(value = "/food")
@Api(value = "食品接口",tags = {"食品增删改查"})
public class FoodNutritionController {


    @Resource
    private FoodNutritionService foodNutritionService;


    @ResponseBody
    @ApiOperation("查询食物")
    @PostMapping("/getFoodNutrition")
    public ResponseData getFoodNutrition(@RequestParam("name") String name) {
        return ResponseData.success(foodNutritionService.queryFood(name));
    }

    @ApiOperation("更新名称")
    @PostMapping("/updateName")
    public void updateName() {
        foodNutritionService.updateName();
    }

}
