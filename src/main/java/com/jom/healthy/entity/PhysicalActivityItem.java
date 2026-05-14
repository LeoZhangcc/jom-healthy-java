package com.jom.healthy.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("physical_activity_item") // MyBatis-Plus 指定表名的注解
public class PhysicalActivityItem {

    @TableId(type = IdType.AUTO) // MyBatis-Plus 指定主键自增的注解
    private Integer id;

    private String categoryKey;
    private String activityKey;

    private String nameEn;
    private String nameCn;
    private String nameMs;

    private String descEn;
    private String descCn;
    private String descMs;

    private String videoUrl;
    private String imageUrl;
    // 增加 MET 值字段
    private Double metValue;
}