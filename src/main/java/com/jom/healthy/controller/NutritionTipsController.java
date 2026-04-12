package com.jom.healthy.controller;

import com.jom.healthy.dto.NutritionTipsDto;
import com.jom.healthy.service.NutritionTipsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/nutritiontips")
public class NutritionTipsController {

    private final NutritionTipsService nutritionTipsService;

    public NutritionTipsController(NutritionTipsService nutritionTipsService) {
        this.nutritionTipsService = nutritionTipsService;
    }

    @GetMapping
    public List<NutritionTipsDto> getNutritionTipsData() {
        return nutritionTipsService.findAll();
    }
}