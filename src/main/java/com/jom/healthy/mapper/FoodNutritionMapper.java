package com.jom.healthy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jom.healthy.entity.FoodNutrition;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FoodNutritionMapper extends BaseMapper<FoodNutrition> {
    // 这里可以添加自定义的查询方法
}
