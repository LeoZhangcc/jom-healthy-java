package com.jom.healthy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jom.healthy.dto.MealNutritionDto;
import com.jom.healthy.dto.MealPlanGenerateRequest;
import com.jom.healthy.entity.TheMealDbMeal;

import java.util.List;

public interface TheMealService extends IService<TheMealDbMeal> {

    void importAllMeals() throws Exception;

    List<MealNutritionDto> searchMealsByNamePrefix(String keyword);

    /**
     * 为 AI 选餐准备候选食谱：
     * 1. 尽量覆盖不同 str_category
     * 2. 根据 allergies / restrictions 排除不适合的类别与食材
     * 3. 随机返回指定数量
     */
    List<TheMealDbMeal> selectRandomMealCandidatesForAi(MealPlanGenerateRequest request, int limit);

    /**
     * 根据 AI 返回的 strMeal 精确查回数据库食谱和完整营养信息。
     */
    MealNutritionDto findMealNutritionByExactStrMeal(String strMeal);

    /**
     * 当某一天的宏量营养无法通过分组缩放精确求解时，
     * 按相同 str_category 随机找安全的候选食谱作为替换项。
     */
    List<MealNutritionDto> findSameCategoryAlternativeMealsForAi(
            String category,
            MealPlanGenerateRequest request,
            List<String> excludedMealNames,
            int limit
    );
}
