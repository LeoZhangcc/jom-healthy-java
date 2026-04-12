package com.jom.healthy.controller;


import com.jom.healthy.service.FoodTransationaService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/foodtransation")
@Api(value = "食品翻译接口",tags = {"食品翻译(更新操作)"})
public class FoodTransationaController {


    @Resource
    private FoodTransationaService foodTransationaService;


    @PostMapping("/excuteFoodTrsantion")
    @ApiOperation("翻译食品名称(gemini)")
    public void excuteFoodTrsantion(@RequestParam("id") Integer id,
                                    @RequestParam("size")Integer size) {
        foodTransationaService.executeBatchTranslation(id, size);
    }
}
