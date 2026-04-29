package com.jom.healthy.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class FeaturedTopicDto implements Serializable {

    private Long id;
    private String category;
    private String healthTag;
    private String imageUrl;
    private String sourceUrl;

    // 💡 重点：这是给前端展示的“最终版”字段
    // 不管数据库存了多少种语言，返回给前端的永远只有这三个
    private String title;
    private String summary;
    private String content;
}