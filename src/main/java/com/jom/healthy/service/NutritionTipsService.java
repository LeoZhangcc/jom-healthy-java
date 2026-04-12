package com.jom.healthy.service;

import com.jom.healthy.dto.NutritionTipsDto;
import com.jom.healthy.mapper.NutritionTipsMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NutritionTipsService {

    private final NutritionTipsMapper nutritionTipsMapper;

    public NutritionTipsService(NutritionTipsMapper nutritionTipsMapper) {
        this.nutritionTipsMapper = nutritionTipsMapper;
    }

    public List<NutritionTipsDto> findAll() {
        return nutritionTipsMapper.findAll();
    }
}