package com.jom.healthy.dto;

public class NutritionTipsDto {

    private String nutrition_tips;

    public NutritionTipsDto() {
    }

    public NutritionTipsDto(String nutrition_tips) {
        this.nutrition_tips = nutrition_tips;
    }

    public String getNutrition_tips() {
        return nutrition_tips;
    }

    public void setNutrition_tips(String nutrition_tips) {
        this.nutrition_tips = nutrition_tips;
    }

    @Override
    public String toString() {
        return "NutritionTipsDto{" +
                "nutrition_tips='" + nutrition_tips + '\'' +
                '}';
    }
}