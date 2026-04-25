package com.jom.healthy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jom.healthy.dto.FoodNutritionNeedsDto;
import com.jom.healthy.entity.BmiStandard;
import com.jom.healthy.entity.FoodNutrition;
import com.jom.healthy.enums.BmiStatus;
import com.jom.healthy.mapper.BmiMapper;
import com.jom.healthy.mapper.FoodNutritionMapper;
import com.jom.healthy.model.params.FoodNutritionParam;
import com.jom.healthy.service.FoodNutritionService;
import com.jom.healthy.util.ToolUtil;
import com.jom.healthy.util.response.ResponseData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class FoodNutritionServiceImpl extends ServiceImpl<FoodNutritionMapper, FoodNutrition> implements FoodNutritionService {

    @Autowired
    private BmiMapper bmiMapper;

    @Override
    public void addFood(FoodNutritionParam param) {
        FoodNutrition entity = getEntity(param);
        this.save(entity);
    }

    @Override
    public List<FoodNutrition> queryFood(String name) {
        QueryWrapper<FoodNutrition> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("food_name_combine", name);
        List<FoodNutrition> foodNutritions = this.baseMapper.selectList(queryWrapper);

        return foodNutritions;
    }

    @Override
    public FoodNutritionNeedsDto getFoodNutritionNeeds(Double heightCm, Double weightKg, Integer ageMonths, Integer gender) {

        double heightM = heightCm / 100.0;
        double bmi = weightKg / (heightM * heightM);
        BmiStandard bmiStandard = bmiMapper.findStandardByAgeAndGender(ageMonths, gender);
        if (ObjectUtils.isEmpty(bmiStandard)) {
            throw new RuntimeException("网络错误");
        }

        BmiStatus calBmiStatus = calBmiStatus(bmi, bmiStandard);
        BigDecimal ageYears = BigDecimal.valueOf(ageMonths)
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        BigDecimal tee = calculateChildTee(gender, ageYears, BigDecimal.valueOf(heightCm), BigDecimal.valueOf(weightKg), "");
        BigDecimal targetEnergy  = adjustEnergyForGrowthStatus(tee, calBmiStatus);
        return calculateFoodNutritionNeeds(targetEnergy);
    }

    private FoodNutrition getEntity(FoodNutritionParam param) {
        FoodNutrition entity = new FoodNutrition();
        ToolUtil.copyProperties(param, entity);
        return entity;
    }

    @Override
    public void heartBeatCheck() {
        QueryWrapper<FoodNutrition> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("food_name_original", "beef");
        this.baseMapper.selectList(queryWrapper);
    }

    @Override
    public void updateName() {
        // 1. 获取所有原始数据
        List<FoodNutrition> list = this.baseMapper.selectList(null);

        for (FoodNutrition food : list) {

            String foodNameCn = food.getFoodNameCn();
            String foodNameEn = food.getFoodNameEn();
            String foodNameMs = food.getFoodNameMs();
            food.setFoodNameCombine(foodNameCn+","+foodNameEn+","+foodNameMs);
            this.baseMapper.updateById(food);
        }
    }

    private BmiStatus calBmiStatus(Double bmi, BmiStandard bmiStandard) {
        /**
         * Interpretation of cut-offs
         * Overweight: >+1SD (equivalent to BMI 25 kg/m2 at 19 years)
         * Obesity: >+2SD (equivalent to BMI 30 kg/m2 at 19 years)
         * Thinness: <-2SD
         * Severe thinness: <-3SD
         */
        if (bmi > bmiStandard.getSdPos2()) {
            return BmiStatus.OBESITY;
        }
        if (bmi > bmiStandard.getSdPos1()) {
            return BmiStatus.OVERWEIGHT;
        }
        if (bmi < bmiStandard.getSdNeg3()) {
            return BmiStatus.SEVERE_THINNESS;
        }
        if (bmi < bmiStandard.getSdNeg2()) {
            return BmiStatus.THINNESS;
        }
        return BmiStatus.NORMAL;
    }

    private BigDecimal calculateChildTee(
            Integer gender,
            BigDecimal ageYears,
            BigDecimal heightCm,
            BigDecimal weightKg,
            String activityLevel
    ) {
        boolean male = 1 == gender;

        BigDecimal result;

        if (male) {
            switch (activityLevel) {
                case "inactive":
                    result = bd("-447.51")
                            .add(bd("3.68").multiply(ageYears))
                            .add(bd("13.01").multiply(heightCm))
                            .add(bd("13.15").multiply(weightKg));
                    break;

                case "low_active":
                    result = bd("19.12")
                            .add(bd("3.68").multiply(ageYears))
                            .add(bd("8.62").multiply(heightCm))
                            .add(bd("20.28").multiply(weightKg));
                    break;

                case "active":
                    result = bd("-388.19")
                            .add(bd("3.68").multiply(ageYears))
                            .add(bd("12.66").multiply(heightCm))
                            .add(bd("20.46").multiply(weightKg));
                    break;

                case "very_active":
                    result = bd("-671.75")
                            .add(bd("3.68").multiply(ageYears))
                            .add(bd("15.38").multiply(heightCm))
                            .add(bd("23.25").multiply(weightKg));
                    break;

                default:
                    result = bd("19.12")
                            .add(bd("3.68").multiply(ageYears))
                            .add(bd("8.62").multiply(heightCm))
                            .add(bd("20.28").multiply(weightKg));
                    break;
            }
        } else {
            switch (activityLevel) {
                case "inactive":
                    result = bd("55.59")
                            .add(bd("-22.25").multiply(ageYears))
                            .add(bd("8.43").multiply(heightCm))
                            .add(bd("17.07").multiply(weightKg));
                    break;

                case "low_active":
                    result = bd("-297.54")
                            .add(bd("-22.25").multiply(ageYears))
                            .add(bd("12.77").multiply(heightCm))
                            .add(bd("14.73").multiply(weightKg));
                    break;

                case "active":
                    result = bd("-189.55")
                            .add(bd("-22.25").multiply(ageYears))
                            .add(bd("11.74").multiply(heightCm))
                            .add(bd("18.34").multiply(weightKg));
                    break;

                case "very_active":
                    result = bd("-709.59")
                            .add(bd("-22.25").multiply(ageYears))
                            .add(bd("18.22").multiply(heightCm))
                            .add(bd("14.25").multiply(weightKg));
                    break;

                default:
                    result = bd("-297.54")
                            .add(bd("-22.25").multiply(ageYears))
                            .add(bd("12.77").multiply(heightCm))
                            .add(bd("14.73").multiply(weightKg));
                    break;
            }
        }

        return result.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    public static BigDecimal adjustEnergyForGrowthStatus(
            BigDecimal tee,
            BmiStatus bmiStatus
    ) {
        if (tee == null) {
            return BigDecimal.ZERO.setScale(0, RoundingMode.HALF_UP);
        }

        if (bmiStatus == null) {
            bmiStatus = BmiStatus.NORMAL;
        }

        BigDecimal factor;

        switch (bmiStatus) {
            case SEVERE_THINNESS:
                factor = bd("1.15");
                break;

            case THINNESS:
                factor = bd("1.10");
                break;

            case NORMAL:
                factor = bd("1.00");
                break;

            case OVERWEIGHT:
                factor = bd("0.95");
                break;

            case OBESITY:
                // 儿童肥胖不建议自动大幅减热量
                factor = bd("1.00");
                break;

            default:
                factor = bd("1.00");
                break;
        }

        return tee.multiply(factor).setScale(0, RoundingMode.HALF_UP);
    }

    private FoodNutritionNeedsDto calculateFoodNutritionNeeds(BigDecimal calories) {
        FoodNutritionNeedsDto foodNutritionNeedsDto = new FoodNutritionNeedsDto();
        foodNutritionNeedsDto.setCal(calories.setScale(0, RoundingMode.HALF_UP).intValue());
        foodNutritionNeedsDto.setCarb(calories.multiply(new BigDecimal("0.55").divide(new BigDecimal(4))).setScale(0,RoundingMode.HALF_UP).intValue());
        foodNutritionNeedsDto.setProtein(calories.multiply(new BigDecimal("0.15").divide(new BigDecimal(4))).setScale(0,RoundingMode.HALF_UP).intValue());
        foodNutritionNeedsDto.setFat(calories.multiply(new BigDecimal("0.3").divide(new BigDecimal(9))).setScale(0,RoundingMode.HALF_UP).intValue());
        return foodNutritionNeedsDto;
    }
}