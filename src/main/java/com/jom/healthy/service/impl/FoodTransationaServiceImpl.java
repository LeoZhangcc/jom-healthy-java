package com.jom.healthy.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jom.healthy.entity.FoodNutrition;
import com.jom.healthy.service.FoodNutritionService;
import com.jom.healthy.service.FoodTransationaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FoodTransationaServiceImpl implements FoodTransationaService {


    private final String apiKey = "";

    private final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent?key=";

    @Resource
    private FoodNutritionService foodService; // MyBatis-Plus Service

    private final RestTemplate restTemplate = new RestTemplate();


    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateFoodCandidatesJson(String prompt) throws Exception {
        String urlString = GEMINI_URL + apiKey;

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(90000);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setDoOutput(true);

        String requestBody = buildRequestBody(prompt);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int code = connection.getResponseCode();

        InputStream inputStream = code >= 200 && code < 300
                ? connection.getInputStream()
                : connection.getErrorStream();

        String response = readStream(inputStream);

        if (code < 200 || code >= 300) {
            throw new RuntimeException("Gemini API error: " + code + ", body: " + response);
        }

        return extractText(response);
    }

    private String buildRequestBody(String prompt) throws Exception {
        String escapedPrompt = objectMapper.writeValueAsString(prompt);

        return "{"
                + "\"contents\":[{"
                + "\"parts\":[{\"text\":" + escapedPrompt + "}]"
                + "}],"
                + "\"generationConfig\":{"
                + "\"temperature\":0.1,"
                + "\"responseMimeType\":\"application/json\","
                + "\"responseSchema\":{"
                + "\"type\":\"ARRAY\","
                + "\"items\":{"
                + "\"type\":\"OBJECT\","
                + "\"properties\":{"
                + "\"normalizedName\":{\"type\":\"STRING\"},"
                + "\"foodNameEn\":{\"type\":\"STRING\"},"
                + "\"foodNameCn\":{\"type\":\"STRING\"},"
                + "\"foodNameMs\":{\"type\":\"STRING\"},"
                + "\"foodGroup\":{\"type\":\"STRING\"},"
                + "\"energyKcal\":{\"type\":\"INTEGER\"},"
                + "\"proteinG\":{\"type\":\"NUMBER\"},"
                + "\"fatG\":{\"type\":\"NUMBER\"},"
                + "\"carbohydrateG\":{\"type\":\"NUMBER\"},"
                + "\"confidenceScore\":{\"type\":\"NUMBER\"},"
                + "\"reason\":{\"type\":\"STRING\"}"
                + "},"
                + "\"required\":["
                + "\"normalizedName\","
                + "\"foodNameEn\","
                + "\"foodNameCn\","
                + "\"foodNameMs\","
                + "\"foodGroup\","
                + "\"energyKcal\","
                + "\"proteinG\","
                + "\"fatG\","
                + "\"carbohydrateG\","
                + "\"confidenceScore\","
                + "\"reason\""
                + "]"
                + "}"
                + "}"
                + "}"
                + "}";
    }

    private String extractText(String responseJson) throws Exception {
        JsonNode root = objectMapper.readTree(responseJson);

        JsonNode textNode = root.path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text");

        if (textNode.isMissingNode() || textNode.isNull()) {
            throw new RuntimeException("No text found in Gemini response: " + responseJson);
        }

        return textNode.asText();
    }

    private String readStream(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        return sb.toString();
    }


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
