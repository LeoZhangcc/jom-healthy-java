package com.jom.healthy.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jom.healthy.dto.PhysicalActivityItemDto;
import com.jom.healthy.entity.PhysicalActivityItem;
import com.jom.healthy.mapper.PhysicalActivityItemMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PhysicalActivityItemService {

    @Autowired
    private PhysicalActivityItemMapper activityMapper;

    public List<PhysicalActivityItemDto> getRecommendationsByStatus(String status) {
        List<String> targetKeys = new ArrayList<>();
        String safeStatus = (status != null) ? status.toUpperCase() : "HEALTHY";

        // 1. 判断逻辑：根据状态把需要的 category_key 装进篮子
        if (safeStatus.contains("OVERWEIGHT") || safeStatus.contains("OBESE")) {
            targetKeys.add("MODERATE_AEROBIC");
        } else if (safeStatus.contains("UNDERWEIGHT") || safeStatus.contains("THIN")) {
            targetKeys.add("MUSCLE_STRENGTHENING");
            targetKeys.add("MODERATE_AEROBIC");
        } else {
            targetKeys.add("MODERATE_AEROBIC");
            targetKeys.add("VIGOROUS_AEROBIC");
            targetKeys.add("BONE_STRENGTHENING");
        }

        // 2. MyBatis-Plus 的魔法：构建查询条件 WHERE category_key IN (...)
        QueryWrapper<PhysicalActivityItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("category_key", targetKeys);

        List<PhysicalActivityItem> items = activityMapper.selectList(queryWrapper);

        // 3. 转换为 DTO 返回给前端
        return items.stream().map(item -> {
            PhysicalActivityItemDto dto = new PhysicalActivityItemDto();
            BeanUtils.copyProperties(item, dto);
            return dto;
        }).collect(Collectors.toList());
    }
}