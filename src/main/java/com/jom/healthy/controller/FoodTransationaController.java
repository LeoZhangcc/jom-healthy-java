package com.jom.healthy.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jom.healthy.entity.TheMealDbMeal;
import com.jom.healthy.mapper.TheMealDbMealMapper;
import com.jom.healthy.service.FoodTransationaService;
import com.jom.healthy.service.GeminiFoodCandidateService;
import com.jom.healthy.service.TheMealService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/foodtransation")
@Api(value = "食品翻译接口", tags = {"食品翻译(更新操作)"})
@Slf4j
public class FoodTransationaController {

    @Resource
    private FoodTransationaService foodTransationaService;

    @Resource
    private TheMealService theMealDbImportService;

    @Resource
    private GeminiFoodCandidateService geminiFoodCandidateService;

    @Resource
    private TheMealDbMealMapper theMealDbMealMapper;

    @Value("${GEMINI_API_KEY:}")
    private String geminiApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/excuteFoodTrsantion")
    @ApiOperation("翻译食品名称(gemini)")
    public void excuteFoodTrsantion(@RequestParam("id") Integer id,
                                    @RequestParam("size") Integer size) {
        foodTransationaService.executeBatchTranslation(id, size);
    }

    @PostMapping("/importAllMeals")
    @ApiOperation("导入theMeal")
    public void importAllMeals() throws Exception {
        theMealDbImportService.importAllMeals();
    }

    @PostMapping("/generateAllFoodCandidates")
    @ApiOperation("generateAllFoodCandidates")
    public void generateAllFoodCandidates() throws Exception {
        geminiFoodCandidateService.generateAllFoodCandidates();
    }

    /**
     * 分类 + 地区翻译
     * 一次最多 200 条
     *
     * POST /foodtransation/translateMealCategoryAreaBatch?id=1&size=200
     */
    @PostMapping("/translateMealCategoryAreaBatch")
    @ApiOperation("批量翻译食谱分类和地区，一次最多200条")
    public String translateMealCategoryAreaBatch(
            @RequestParam(value = "id", defaultValue = "1") Long id,
            @RequestParam(value = "size", defaultValue = "200") Integer size
    ) throws Exception {

        if (id == null || id <= 0) {
            id = 1L;
        }

        if (size == null || size <= 0) {
            size = 200;
        }

        if (size > 200) {
            size = 200;
        }

        List<TheMealDbMeal> meals =
                theMealDbMealMapper.selectMealsNeedCategoryAreaTranslation(id, size);

        if (meals == null || meals.isEmpty()) {
            return "No category or area needs translation.";
        }

        String prompt = buildCategoryAreaTranslationPrompt(meals);
        String geminiJson = callGeminiForJson(prompt);
        int updatedCount = parseAndUpdateCategoryAreaTranslations(geminiJson);

        return "Category and area translation finished. Selected: "
                + meals.size()
                + ", Updated: "
                + updatedCount;
    }

    /**
     * 做法步骤翻译
     * 一次最多 5 条
     *
     * POST /foodtransation/translateMealInstructionsBatch?id=1&size=5
     */
    @PostMapping("/translateMealInstructionsBatch")
    @ApiOperation("批量翻译食谱做法步骤，一次最多5条")
    public void translateMealInstructionsBatch(
            @RequestParam(value = "number", defaultValue = "10") Integer number,
            @RequestParam(value = "size", defaultValue = "20") Integer size
    ) throws Exception {

        for (int i = 0; i < size; i++) {
            boolean success = this.updateInstruction(number);
            log.info("更新一条"+ (success ? "success":"fail"));
        }
    }

    private boolean updateInstruction(int size) {
        List<TheMealDbMeal> meals =
                theMealDbMealMapper.selectMealsNeedInstructionsTranslation(size);

        if (meals == null || meals.isEmpty()) {
            return false;
        }

        String prompt = buildInstructionsTranslationPrompt(meals);
        int updatedCount = 0;
        try {
            String geminiJson = callGeminiForJson(prompt);
            updatedCount = parseAndUpdateInstructionsTranslations(geminiJson);
        } catch (Exception e) {
            log.error("callGeminiForJson exception",e);
            return false;
        }
        return updatedCount > 0;
    }

    /**
     * 分类 + 地区 Prompt
     */
    private String buildCategoryAreaTranslationPrompt(List<TheMealDbMeal> meals) {
        ArrayNode inputArray = objectMapper.createArrayNode();

        for (TheMealDbMeal meal : meals) {
            ObjectNode node = inputArray.addObject();
            node.put("id", meal.getId());
            node.put("strCategory", safeText(meal.getStrCategory()));
            node.put("strArea", safeText(meal.getStrArea()));
        }

        return "You are a professional food database translator.\n"
                + "Translate recipe category and region fields into Simplified Chinese and Malay.\n"
                + "\n"
                + "Rules:\n"
                + "1. Use short, natural, standard labels.\n"
                + "2. Keep the meaning accurate.\n"
                + "3. Do not add explanations.\n"
                + "4. If the original field is empty, return an empty string.\n"
                + "5. Return ONLY valid JSON, no markdown.\n"
                + "\n"
                + "Return this exact JSON structure:\n"
                + "{\n"
                + "  \"items\": [\n"
                + "    {\n"
                + "      \"id\": 1,\n"
                + "      \"strCategoryCn\": \"\",\n"
                + "      \"strCategoryMs\": \"\",\n"
                + "      \"strAreaCn\": \"\",\n"
                + "      \"strAreaMs\": \"\"\n"
                + "    }\n"
                + "  ]\n"
                + "}\n"
                + "\n"
                + "Input records:\n"
                + inputArray.toString();
    }

    /**
     * 做法步骤 Prompt
     */
    private String buildInstructionsTranslationPrompt(List<TheMealDbMeal> meals) {
        ArrayNode inputArray = objectMapper.createArrayNode();

        for (TheMealDbMeal meal : meals) {
            ObjectNode node = inputArray.addObject();
            node.put("id", meal.getId());
            node.put("strInstructions", safeText(meal.getStrInstructions()));
        }

        return "You are a professional recipe translator.\n"
                + "Translate recipe cooking instructions into Simplified Chinese and Malay.\n"
                + "\n"
                + "Rules:\n"
                + "1. Preserve all cooking steps and meanings.\n"
                + "2. Keep the translation natural and easy to read.\n"
                + "3. Do not shorten, summarize, or omit details.\n"
                + "4. If the original field is empty, return an empty string.\n"
                + "5. Return ONLY valid JSON, no markdown.\n"
                + "\n"
                + "Return this exact JSON structure:\n"
                + "{\n"
                + "  \"items\": [\n"
                + "    {\n"
                + "      \"id\": 1,\n"
                + "      \"strInstructionsCn\": \"\",\n"
                + "      \"strInstructionsMs\": \"\"\n"
                + "    }\n"
                + "  ]\n"
                + "}\n"
                + "\n"
                + "Input records:\n"
                + inputArray.toString();
    }

    /**
     * 解析并更新 分类 + 地区
     */
    private int parseAndUpdateCategoryAreaTranslations(String geminiJson) throws Exception {
        JsonNode root = objectMapper.readTree(geminiJson);
        JsonNode items = root.path("items");

        if (!items.isArray()) {
            throw new RuntimeException("Gemini JSON does not contain items array: " + geminiJson);
        }

        int updatedCount = 0;

        for (JsonNode item : items) {
            long id = item.path("id").asLong(0L);

            if (id <= 0) {
                continue;
            }

            TheMealDbMeal update = new TheMealDbMeal();
            update.setId(id);

            update.setStrCategoryCn(trimToNull(item.path("strCategoryCn").asText(null)));
            update.setStrCategoryMs(trimToNull(item.path("strCategoryMs").asText(null)));

            update.setStrAreaCn(trimToNull(item.path("strAreaCn").asText(null)));
            update.setStrAreaMs(trimToNull(item.path("strAreaMs").asText(null)));

            int rows = theMealDbMealMapper.updateMealCategoryAreaTranslationFields(update);
            updatedCount += rows;
        }

        return updatedCount;
    }

    /**
     * 解析并更新 做法步骤
     */
    private int parseAndUpdateInstructionsTranslations(String geminiJson) throws Exception {
        JsonNode root = objectMapper.readTree(geminiJson);
        JsonNode items = root.path("items");

        if (!items.isArray()) {
            throw new RuntimeException("Gemini JSON does not contain items array: " + geminiJson);
        }

        int updatedCount = 0;

        for (JsonNode item : items) {
            long id = item.path("id").asLong(0L);

            if (id <= 0) {
                continue;
            }

            TheMealDbMeal update = new TheMealDbMeal();
            update.setId(id);

            update.setStrInstructionsCn(trimToNull(item.path("strInstructionsCn").asText(null)));
            update.setStrInstructionsMs(trimToNull(item.path("strInstructionsMs").asText(null)));

            int rows = theMealDbMealMapper.updateMealInstructionsTranslationFields(update);
            updatedCount += rows;
        }

        return updatedCount;
    }

    /**
     * 调用 Gemini
     */
    private String callGeminiForJson(String prompt) throws Exception {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty()) {
            throw new RuntimeException("GEMINI_API_KEY is empty.");
        }

        String urlString =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent";

        HttpURLConnection connection = null;

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("x-goog-api-key", geminiApiKey);
            connection.setConnectTimeout(15000);

            // 做法翻译会比较慢，统一给10分钟
            connection.setReadTimeout(600000);

            connection.setDoOutput(true);

            ObjectNode requestBody = objectMapper.createObjectNode();

            ArrayNode contents = requestBody.putArray("contents");
            ObjectNode content = contents.addObject();
            ArrayNode parts = content.putArray("parts");
            parts.addObject().put("text", prompt);

            ObjectNode generationConfig = requestBody.putObject("generationConfig");
            generationConfig.put("temperature", 0.1);
            generationConfig.put("responseMimeType", "application/json");

            byte[] bodyBytes = requestBody.toString().getBytes(StandardCharsets.UTF_8);

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(bodyBytes);
                outputStream.flush();
            }

            int statusCode = connection.getResponseCode();

            InputStream inputStream;
            if (statusCode >= 200 && statusCode < 300) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }

            String responseBody = readStream(inputStream);

            if (statusCode < 200 || statusCode >= 300) {
                throw new RuntimeException(
                        "Gemini HTTP Error: " + statusCode + ", body: " + responseBody
                );
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");

            if (!candidates.isArray() || candidates.size() == 0) {
                throw new RuntimeException("Gemini response has no candidates: " + responseBody);
            }

            JsonNode partsNode = candidates.get(0).path("content").path("parts");

            if (!partsNode.isArray() || partsNode.size() == 0) {
                throw new RuntimeException("Gemini response has no parts: " + responseBody);
            }

            StringBuilder result = new StringBuilder();

            for (JsonNode part : partsNode) {
                String text = part.path("text").asText();
                if (text != null) {
                    result.append(text);
                }
            }

            return cleanJsonText(result.toString());

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String text = value.trim();

        if (text.isEmpty()) {
            return null;
        }

        return text;
    }

    private String cleanJsonText(String text) {
        if (text == null) {
            return "";
        }

        String result = text.trim();

        if (result.startsWith("```json")) {
            result = result.substring(7).trim();
        } else if (result.startsWith("```")) {
            result = result.substring(3).trim();
        }

        if (result.endsWith("```")) {
            result = result.substring(0, result.length() - 3).trim();
        }

        return result;
    }

    private String readStream(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }

        return result.toString();
    }
}