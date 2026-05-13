package com.jom.healthy.dto;

import lombok.Data;

@Data
public class PhysicalActivityItemDto {
    private String categoryKey;
    private String activityKey;

    // 把三语都传给前端，前端自行根据 LanguageContext 决定显示哪个
    private String nameEn;
    private String nameCn;
    private String nameMs;

    private String descEn;
    private String descCn;
    private String descMs;

    private String videoUrl;
    private String imageUrl;
}