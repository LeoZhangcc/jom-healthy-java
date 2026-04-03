package com.jom.healthy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jom.healthy.entity.FoodNutrition;
import com.jom.healthy.model.params.FoodNutritionParam;
import com.jom.healthy.util.response.ResponseData;

public interface FoodNutritionService extends IService<FoodNutrition> {

    ResponseData addFood(FoodNutritionParam param);

}
