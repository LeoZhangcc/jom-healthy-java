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

    private final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=";

    @Resource
    private FoodNutritionService foodService; // MyBatis-Plus Service

    private final RestTemplate restTemplate = new RestTemplate();


    public void executeBatchTranslation(Integer id, Integer size) {
// 1. 查询前 200 条：只要 en/cn/ms 其中一个为空，就查出来
        List<FoodNutrition> targetList = foodService.list(
                new LambdaQueryWrapper<FoodNutrition>()
                        .isNotNull(FoodNutrition::getFoodNameOriginal)
                        .gt(FoodNutrition::getId, id)
                        .last("limit " + size) // 锁定 200 条
        );

        if (targetList.isEmpty()) {
            log.info("没有需要翻译的数据。");
            return;
        }

        // 2. 提取原始名称列表
        List<String> originalNames = targetList.stream()
                .map(FoodNutrition::getFoodNameOriginal)
                .distinct()
                .collect(Collectors.toList());

        // 3. 调用 Gemini (一次性翻译所有语言)
        Map<String, JSONObject> translationMap = callGeminiApiForTriple(originalNames);

        // 4. 数据回填
        for (FoodNutrition food : targetList) {
            JSONObject trans = translationMap.get(food.getFoodNameOriginal());
            if (trans != null) {
                if (trans.containsKey("en")) food.setFoodNameEn(trans.getString("en"));
                if (trans.containsKey("cn")) food.setFoodNameCn(trans.getString("cn"));
                if (trans.containsKey("ms")) food.setFoodNameMs(trans.getString("ms"));
            }
        }

        // 5. 批量更新到数据库
        foodService.updateBatchById(targetList);
        log.info("成功处理并更新 {} 条食物数据", targetList.size());
    }

    private Map<String, JSONObject> callGeminiApiForTriple(List<String> names) {
        // 极简 Prompt 节省 Token
        String prompt = "Role: Nutrition Expert. Task: Translate the following food names into English (en), Chinese (cn), and Malay (ms). " +
                "Format: JSON object where Key is the original name, Value is {en:\"\", cn:\"\", ms:\"\"}. " +
                "No extra text. Data: " + JSON.toJSONString(names);

        Map<String, Object> requestBody = ImmutableMap.of(
                "contents", ImmutableList.of(
                        ImmutableMap.of("parts", ImmutableList.of(ImmutableMap.of("text", prompt)))
                )
        );

        try {
            String responseStr = restTemplate.postForObject(GEMINI_URL + apiKey, requestBody, String.class);
            JSONObject jsonResponse = JSON.parseObject(responseStr);

            // 提取 AI 文本内容
            String aiText = jsonResponse.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

            // 清理 Markdown 标签
            aiText = aiText.replaceAll("```json|```", "").trim();

            // 解析结果映射
            JSONObject resultMap = JSON.parseObject(aiText);
            Map<String, JSONObject> finalMap = new HashMap<>();
            for (String key : resultMap.keySet()) {
                finalMap.put(key, resultMap.getJSONObject(key));
            }
            return finalMap;

        } catch (Exception e) {
            log.error("Gemini API 调用异常: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
