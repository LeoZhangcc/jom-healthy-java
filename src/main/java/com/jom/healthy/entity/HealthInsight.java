package com.jom.healthy.entity;

import lombok.Data;

@Data // Lombok 注解，自动生成 Getter/Setter
public class HealthInsight {
    private Integer id;
    private String themeName;
    private String coverImageUrl;
    private String shortSummary;
    private String detailAnalysis;
    private String sourceLabel;
}