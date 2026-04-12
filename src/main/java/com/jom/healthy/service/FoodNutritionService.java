package com.jom.healthy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jom.healthy.entity.FoodNutrition;
import com.jom.healthy.model.params.FoodNutritionParam;
import com.jom.healthy.util.response.ResponseData;

import java.util.List;

public interface FoodNutritionService extends IService<FoodNutrition> {

    void addFood(FoodNutritionParam param);

    List<FoodNutrition> queryFood(String name);

    void heartBeatCheck();

    void updateName();

}
