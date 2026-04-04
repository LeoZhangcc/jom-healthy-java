package com.jom.healthy.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jom.healthy.entity.FoodNutrition;
import com.jom.healthy.service.FoodNutritionService;
import com.jom.healthy.service.FoodTransationaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FoodTransationaServiceImpl implements FoodTransationaService {


    private final String apiKey = "";

    private final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview:generateContent?key=";

    @Resource
    private FoodNutritionService foodService; // MyBatis-Plus Service

    private final RestTemplate restTemplate = new RestTemplate();


    public void executeBatchTranslation() {
        // 1. 找出所有需要翻译的记录 (CN 字段为空)
        List<FoodNutrition> targetList = foodService.list(
                new LambdaQueryWrapper<FoodNutrition>().isNull(FoodNutrition::getFoodNameCn)
        );

        if (targetList.isEmpty()) return;

        // 2. 分批处理（例如每 30 个食物调用一次 API，防止 Prompt 过长或触发限制）
        int batchSize = 30;
        for (int i = 0; i < targetList.size(); i += batchSize) {
            List<FoodNutrition> currentBatch = targetList.subList(i, Math.min(i + batchSize, targetList.size()));

            // 提取英文名列表
            List<String> enNames = currentBatch.stream()
                    .map(FoodNutrition::getFoodNameEn)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (enNames.isEmpty()) continue;

            // 3. 调用 Gemini API
            Map<String, String> translationMap = callGeminiApi(enNames);

            // 4. 将翻译结果回填到对象中
            for (FoodNutrition food : currentBatch) {
                String cnName = translationMap.get(food.getFoodNameEn());
                if (cnName != null) {
                    food.setFoodNameCn(cnName);
                }
            }

            // 5. MyBatis-Plus 批量更新数据库
            foodService.updateBatchById(currentBatch);

            System.out.println("已完成批次: " + (i / batchSize + 1));

            // 免费版 API 建议增加延迟，防止触发 15 RPM 限制
            try { Thread.sleep(15000); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    private Map<String, String> callGeminiApi(List<String> names) {
        String prompt = "你是一个营养学专家。请将以下食物英文名翻译成中文，以JSON格式返回(Key为英文，Value为中文翻译)，不要输出多余文字：" + names.toString();

        // 构造请求体 (根据 Gemini API 标准格式)
        Map<String, Object> requestBody = ImmutableMap.of(
                "contents", ImmutableList.of(
                        ImmutableMap.of("parts", ImmutableList.of(ImmutableMap.of("text", prompt)))
                )
        );

        try {
            String responseStr = restTemplate.postForObject(GEMINI_URL + apiKey, requestBody, String.class);

            // 解析 Gemini 返回的嵌套 JSON 字符串
            JSONObject jsonObject = JSON.parseObject(responseStr);
            String aiText = jsonObject.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

            // 去掉 AI 可能自带的 ```json 标记
            aiText = aiText.replace("```json", "").replace("```", "").trim();

            return JSON.parseObject(aiText, Map.class);
        } catch (Exception e) {
            System.err.println("API 调用失败: " + e.getMessage());
            return Collections.emptyMap();
        }
    }
}
