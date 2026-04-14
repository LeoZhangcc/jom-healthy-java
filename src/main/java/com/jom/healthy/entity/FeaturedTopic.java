package com.jom.healthy.entity;

import javax.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * FeaturedTopic实体类，用于存储特色主题相关信息
 * 使用了Lombok的@Data注解自动生成getter、setter等方法
 * 通过@Entity和@Table注解将其映射到数据库表"featured_topics"
 */
@Data
@Entity
@Table(name = "featured_topics")
public class FeaturedTopic {

    /**
     * 主题ID，作为主键
     * 使用@GeneratedValue注解设置为自增长策略
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 主题标题
     */
    private String title;

    /**
     * 主题分类
     * 分类包括：REPORT(报告), DIET(饮食), SPORTS(运动), HABIT(习惯)
     */
    private String category; // 对应 REPORT, DIET, SPORTS, HABIT

    /**
     * 主题图片URL
     * 映射到数据库的image_url列
     */
    @Column(name = "image_url")
    private String imageUrl;

    /**
     * 主题摘要
     */
    private String summary;

    /**
     * 主题详细内容
     * 使用TEXT类型以支持长文本内容
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * 原文链接
     * 映射到数据库的source_url列
     */
    @Column(name = "source_url")
    private String sourceUrl;

    /**
     * 创建时间
     * 使用@CreationTimestamp注解自动设置为记录创建时的时间
     */
    @CreationTimestamp
    private LocalDateTime createdAt;
}