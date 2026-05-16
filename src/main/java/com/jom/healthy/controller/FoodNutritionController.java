package com.jom.healthy.controller;

import com.jom.healthy.service.FoodNutritionService;
import com.jom.healthy.util.response.ResponseData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/food")
@Api(value = "食品接口",tags = {"食品增删改查"})
public class FoodNutritionController {


    @Resource
    private FoodNutritionService foodNutritionService;


    /**
     * https://www.who.int/news-room/fact-sheets/detail/healthy-diet
     * https://www.nutrition.org.uk/creating-a-healthy-diet/food-labelling
     * 健康评分相关文章
     */
    @ResponseBody
    @ApiOperation("查询食物")
    @PostMapping("/getFoodNutrition")
    public ResponseData getFoodNutrition(@RequestParam("name") String name) {
        return ResponseData.success(foodNutritionService.queryFood(name));
    }

    /**
     * https://www.who.int/tools/growth-reference-data-for-5to19-years/indicators/bmi-for-age 参考文献
     * https://odphp.health.gov/sites/default/files/2019-09/Appendix-E3-1-Table-A4.pdf 碳水蛋白质脂肪
     * RNI 2017
     */
    @ResponseBody
    @ApiOperation("获取小孩所需要的营养")
    @PostMapping("/getFoodNutritionNeeds")
    public ResponseData getFoodNutritionNeeds(@RequestParam("heightCm") Double heightCm,
                                              @RequestParam("weightKg") Double weightKg,
                                              @RequestParam("ageMonths") Integer ageMonths,
                                              @RequestParam("gender") Integer gender) {

        return ResponseData.success(foodNutritionService.getFoodNutritionNeeds(heightCm, weightKg, ageMonths, gender));
    }

    @ApiOperation("更新名称")
    @PostMapping("/updateName")
    public void updateName() {
        foodNutritionService.updateName();
    }

}
