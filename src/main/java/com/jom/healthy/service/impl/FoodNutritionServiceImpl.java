package com.jom.healthy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jom.healthy.entity.FoodNutrition;
import com.jom.healthy.mapper.FoodNutritionMapper;
import com.jom.healthy.model.params.FoodNutritionParam;
import com.jom.healthy.service.FoodNutritionService;
import com.jom.healthy.util.ToolUtil;
import com.jom.healthy.util.response.ResponseData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class FoodNutritionServiceImpl extends ServiceImpl<FoodNutritionMapper, FoodNutrition> implements FoodNutritionService {

    @Resource
    private FoodNutritionMapper foodNutritionMapper;

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


}