package com.jom.healthy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jom.healthy.dto.MealNutritionDto;
import com.jom.healthy.entity.TheMealDbMeal;

import java.util.List;

public interface TheMealService extends IService<TheMealDbMeal> {

    void importAllMeals() throws Exception;

    List<MealNutritionDto> searchMealsByNamePrefix(String keyword);
}
