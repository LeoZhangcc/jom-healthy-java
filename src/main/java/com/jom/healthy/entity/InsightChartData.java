package com.jom.healthy.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InsightChartData {
    private Integer id;
    private Integer insightId;
    private Integer dataYear;
    private String categoryName;
    private BigDecimal percentage;
}