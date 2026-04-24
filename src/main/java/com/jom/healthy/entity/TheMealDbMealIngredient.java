package com.jom.healthy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("themealdb_meal_ingredients")
public class TheMealDbMealIngredient {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String mealId;

    private Integer ingredientOrder;

    private String ingredientName;

    private String measure;

    private String normalizedName;

    private String myfcdFoodId;

    private String myfcdFoodName;

    private BigDecimal gramsEstimated;

    private BigDecimal mappingConfidence;

    private LocalDateTime createdAt;
}
