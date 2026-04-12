package com.jom.healthy.mapper;

import com.jom.healthy.dto.NutritionDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Result;


import java.util.List;

@Mapper
public interface NutritionalStatusMapper {
    
    @Results({
        @Result(property = "nutritional_status", column = "nutritional_status"),
        @Result(property = "sociodemographics", column = "sociodemographics"),
        @Result(property = "age_range", column = "age_range"),
        @Result(property = "prevalence_percent", column = "prevalence_percent")
    })
    @Select(
        "SELECT nutritional_status, sociodemographics, age_range, prevalence_percent " +
        "FROM nutritional_status"
    )
    List<NutritionDto> findAll();

}
