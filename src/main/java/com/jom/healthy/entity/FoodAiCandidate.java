package com.jom.healthy.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("food_ai_candidate")
public class FoodAiCandidate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String normalizedName;

    private String foodNameEn;
    private String foodNameCn;
    private String foodNameMs;

    @TableField("food_group_")
    private String foodGroup;

    private Integer energyKcal;
    private Double proteinG;
    private Double fatG;
    private Double carbohydrateG;

    private String source;
    private BigDecimal confidenceScore;
    private String reason;

    private Integer reviewed;

    private String rawJson;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}