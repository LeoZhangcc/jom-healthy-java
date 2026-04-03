package com.jom.healthy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jom.healthy.entity.FoodNutrition;
import com.jom.healthy.mapper.FoodNutritionMapper;
import com.jom.healthy.service.FoodNutritionService;
import org.springframework.stereotype.Service;

@Service
public class FoodNutritionServiceImpl extends ServiceImpl<FoodNutritionMapper, FoodNutrition> implements FoodNutritionService {

}