package com.jom.healthy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jom.healthy.dto.FoodNutritionDto;
import com.jom.healthy.dto.FoodNutritionNeedsDto;
import com.jom.healthy.entity.FoodNutrition;
import com.jom.healthy.model.params.FoodNutritionParam;

import java.util.List;

public interface FoodNutritionService extends IService<FoodNutrition> {

    void addFood(FoodNutritionParam param);

    List<FoodNutritionDto> queryFood(String name);

    void heartBeatCheck();

    void updateName();

    FoodNutritionNeedsDto getFoodNutritionNeeds(Double heightCm, Double weightKg, Integer ageMonths, Integer gender);
}
