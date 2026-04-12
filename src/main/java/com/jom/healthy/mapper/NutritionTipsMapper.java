package com.jom.healthy.mapper;

import com.jom.healthy.dto.NutritionTipsDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Result;

import java.util.List;

@Mapper
public interface NutritionTipsMapper {

    @Results({
            @Result(property = "nutrition_tips", column = "nutrition_tips")
        })
    @Select(
        "SELECT nutrition_tips FROM nutrition_tips"
)
    List<NutritionTipsDto> findAll();
}
