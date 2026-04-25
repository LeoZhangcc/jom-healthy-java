package com.jom.healthy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jom.healthy.entity.TheMealDbMealIngredient;
import com.jom.healthy.dto.UnmatchedIngredientDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TheMealDbMealIngredientMapper extends BaseMapper<TheMealDbMealIngredient> {

    @Select("SELECT i.normalized_name AS normalizedName, COUNT(*) AS useCount " +
            "FROM themealdb_meal_ingredients i " +
            "LEFT JOIN food_ai_candidate c " +
            "ON i.normalized_name = c.normalized_name " +
            "WHERE i.myfcd_food_id IS NULL " +
            "AND i.normalized_name IS NOT NULL " +
            "AND i.normalized_name <> '' " +
            "AND c.normalized_name IS NULL " +
            "GROUP BY i.normalized_name " +
            "ORDER BY useCount DESC, i.normalized_name")
    List<UnmatchedIngredientDTO> selectUnmatchedIngredientNames();
}
