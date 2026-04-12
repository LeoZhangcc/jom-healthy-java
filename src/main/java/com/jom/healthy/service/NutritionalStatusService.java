package com.jom.healthy.service;

import com.jom.healthy.dto.NutritionDto;
import com.jom.healthy.mapper.NutritionalStatusMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NutritionalStatusService {

    private final NutritionalStatusMapper nutritionalStatusMapper;

    public NutritionalStatusService(NutritionalStatusMapper nutritionalStatusMapper) {
        this.nutritionalStatusMapper = nutritionalStatusMapper;
    }

    public List<NutritionDto> findAll() {
        return nutritionalStatusMapper.findAll();
    }
}
