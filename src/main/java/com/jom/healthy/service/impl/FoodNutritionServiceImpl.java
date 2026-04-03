package com.jom.healthy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jom.healthy.entity.FoodNutrition;
import com.jom.healthy.mapper.FoodNutritionMapper;
import com.jom.healthy.model.params.FoodNutritionParam;
import com.jom.healthy.service.FoodNutritionService;
import com.jom.healthy.util.response.ResponseData;
import org.springframework.stereotype.Service;

@Service
public class FoodNutritionServiceImpl extends ServiceImpl<FoodNutritionMapper, FoodNutrition> implements FoodNutritionService {

    @Override
    public ResponseData addFood(FoodNutritionParam param) {
        return this.addFood(param);
    }
}