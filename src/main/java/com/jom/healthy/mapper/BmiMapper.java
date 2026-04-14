package com.jom.healthy.mapper;

import com.jom.healthy.entity.BmiStandard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BmiMapper {

    /**
     * 根据年龄和性别查询BMI标准参考值
     * @param ageMonths 年龄（以月为单位）
     * @param gender 性别（通常用数字表示，如1代表男性，2代表女性等）
     * @return 返回匹配条件的BmiStandard对象，包含BMI标准相关信息
     */
    @Select("SELECT * FROM bmi_standard_reference WHERE gender = #{gender} AND age_months = #{ageMonths}")
    BmiStandard findStandardByAgeAndGender(@Param("ageMonths") Integer ageMonths, @Param("gender") Integer gender);

}

