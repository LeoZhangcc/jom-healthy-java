package com.jom.healthy.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MealPlanGenerateRequest {

    private String childName;

    private Integer age;

    /**
     * boy / girl
     */
    private String gender;

    private Double heightCm;

    private Double weightKg;

    private List<String> allergies;

    private Map<String, Object> restrictions;

    private Double targetCarbs;

    private Double targetProtein;

    private Double targetFat;

    private Integer days;

    /**
     * 用户输入想吃什么。
     * 例如: chicken rice, pasta, egg, banana
     * 如果为空，后端会按用户 profile 推荐。
     */
    private String mealPreference;
}