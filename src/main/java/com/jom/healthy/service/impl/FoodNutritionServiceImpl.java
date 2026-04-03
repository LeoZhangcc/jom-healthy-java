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
        queryWrapper.like("food_name_original", name);
        List<FoodNutrition> foodNutritions = this.baseMapper.selectList(queryWrapper);

        return foodNutritions;
    }

    private FoodNutrition getEntity(FoodNutritionParam param) {
        FoodNutrition entity = new FoodNutrition();
        ToolUtil.copyProperties(param, entity);
        return entity;
    }
}