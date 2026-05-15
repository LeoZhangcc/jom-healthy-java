package com.jom.healthy.controller;

import com.jom.healthy.dto.NearbySupermarketDto;
import com.jom.healthy.service.NearbySupermarketService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/map")
@Api(value = "地图接口", tags = {"附近地点"})
public class NearbySupermarketController {

    @Resource
    private NearbySupermarketService nearbySupermarketService;

    @GetMapping("/nearbySupermarkets")
    @ApiOperation("查询附近超市")
    public List<NearbySupermarketDto> nearbySupermarkets(
            @RequestParam("lat") Double latitude,
            @RequestParam("lng") Double longitude,
            @RequestParam(value = "radius", defaultValue = "3000") Integer radiusMeters
    ) throws Exception {

        return nearbySupermarketService.findNearbySupermarkets(
                latitude,
                longitude,
                radiusMeters
        );
    }
}