package com.jom.healthy.dto;


import lombok.Data;

@Data
public class GeminiFoodCandidateDTO {

    private String normalizedName;

    private String foodNameEn;
    private String foodNameCn;
    private String foodNameMs;
    private String foodGroup;

    private Integer energyKcal;
    private Double proteinG;
    private Double fatG;
    private Double carbohydrateG;

    private Double confidenceScore;
    private String reason;
}
