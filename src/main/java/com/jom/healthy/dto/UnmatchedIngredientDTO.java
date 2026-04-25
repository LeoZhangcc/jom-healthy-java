package com.jom.healthy.dto;

import lombok.Data;

@Data
public class UnmatchedIngredientDTO {
    private String normalizedName;
    private Integer useCount;
}