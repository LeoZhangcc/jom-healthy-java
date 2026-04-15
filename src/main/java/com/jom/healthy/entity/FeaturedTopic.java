package com.jom.healthy.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("featured_topics") // 替换 @Table
public class FeaturedTopic {

    @TableId(type = IdType.AUTO) // 替换 @Id 和 @GeneratedValue
    private Long id;

    private String title;

    private String category;

    @TableField("image_url") // 对应数据库字段名
    private String imageUrl;

    private String summary;

    private String content;

    @TableField("source_url")
    private String sourceUrl;

    @TableField(value = "created_at", fill = FieldFill.INSERT) // MP 自动填充创建时间
    private LocalDateTime createdAt;
}