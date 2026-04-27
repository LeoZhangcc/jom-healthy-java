package com.jom.healthy.dto;

import com.jom.healthy.entity.FoodNutrition;
import lombok.Data;

@Data
public class FoodNutritionDto extends FoodNutrition {
    // 新增：健康评分相关字段
    private Integer healthScore;
    private String healthGrade;
    private String healthLabel;
    private String healthReasonEn;
    private String healthReasonCn;
    private String healthReasonMs;
}
