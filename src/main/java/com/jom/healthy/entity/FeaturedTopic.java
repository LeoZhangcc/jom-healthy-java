package com.jom.healthy.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("featured_topics")
public class FeaturedTopic {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分类：REPORT, DIET, SPORTS, HABIT
     */
    private String category;

    /**
     * 适用健康标签：UNDERWEIGHT, HEALTHY, OVERWEIGHT
     */
    @TableField("health_tag")
    private String healthTag;

    @TableField("image_url")
    private String imageUrl;

    @TableField("source_url")
    private String sourceUrl;

    // =================  标题三语  =================
    @TableField("title_en")
    private String titleEn;

    @TableField("title_zh")
    private String titleZh;

    @TableField("title_ms")
    private String titleMs;

    // =================  摘要三语  =================
    @TableField("summary_en")
    private String summaryEn;

    @TableField("summary_zh")
    private String summaryZh;

    @TableField("summary_ms")
    private String summaryMs;

    // =================  正文三语  =================
    @TableField("content_en")
    private String contentEn;

    @TableField("content_zh")
    private String contentZh;

    @TableField("content_ms")
    private String contentMs;

    // =============================================

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}