package com.jom.healthy.controller;

import com.jom.healthy.dto.NutritionDto;
import com.jom.healthy.service.NutritionalStatusService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/nutritionalstatus")
public class NutritionalStatusController {

    private final NutritionalStatusService nutritionalStatusService;

    public NutritionalStatusController(NutritionalStatusService nutritionalStatusService) {
        this.nutritionalStatusService = nutritionalStatusService;
    }

    @GetMapping
    public List<NutritionDto> getNutritionalStatusData() {
        return nutritionalStatusService.findAll();
    }
}
