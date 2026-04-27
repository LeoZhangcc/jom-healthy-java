package com.jom.healthy.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MealIngredientNutritionDto {

    private Long ingredientId;

    private String mealId;

    private Integer ingredientOrder;

    private String ingredientName;

    private String measure;

    private String normalizedName;

    private String myfcdFoodId;

    private String myfcdFoodName;

    private BigDecimal gramsEstimated;

    private BigDecimal mappingConfidence;

    private Integer foodId;

    private String foodNameEn;
    private String foodNameCn;
    private String foodNameMs;
    private String foodGroup;
    private String picUrl;

    private Integer energyKcalPer100g;
    private Double proteinGPer100g;
    private Double carbohydrateGPer100g;
    private Double fatGPer100g;

    private BigDecimal energyKcal;
    private BigDecimal proteinG;
    private BigDecimal carbohydrateG;
    private BigDecimal fatG;
}
