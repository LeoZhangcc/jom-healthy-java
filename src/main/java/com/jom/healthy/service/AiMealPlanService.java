package com.jom.healthy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jom.healthy.dto.MealPlanGenerateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AiMealPlanService {

    @Value("${GEMINI_API_KEY:}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> generateMealPlan(MealPlanGenerateRequest request) {
        log.info("generateMealPlan start=====request:{}", request);
        long startTime = System.currentTimeMillis();

        try {
            if (geminiApiKey == null || geminiApiKey.trim().length() == 0) {
                throw new RuntimeException("GEMINI_API_KEY is empty");
            }

            String prompt = buildPrompt(request);

            Map<String, Object> textPart = new HashMap<String, Object>();
            textPart.put("text", prompt);

            List<Map<String, Object>> parts = new ArrayList<Map<String, Object>>();
            parts.add(textPart);

            Map<String, Object> content = new HashMap<String, Object>();
            content.put("parts", parts);

            List<Map<String, Object>> contents = new ArrayList<Map<String, Object>>();
            contents.add(content);

            Map<String, Object> generationConfig = new HashMap<String, Object>();
            generationConfig.put("temperature", 0.5);
            generationConfig.put("responseMimeType", "application/json");

            Map<String, Object> body = new HashMap<String, Object>();
            body.put("contents", contents);
            body.put("generationConfig", generationConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", geminiApiKey);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<Map<String, Object>>(body, headers);

            String url =
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            Map<String, Object> result = parseGeminiResponse(response.getBody());
            enforceMealPlanTargetLimits(result, request);

            long costMs = System.currentTimeMillis() - startTime;
            log.info("generateMealPlan end=====took:{}ms", costMs);
            return result;
        } catch (HttpClientErrorException e) {
            long costMs = System.currentTimeMillis() - startTime;

            log.error(
                    "Gemini HTTP client error after {}ms, status: {}, response body: {}",
                    costMs,
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e
            );

            Map<String, Object> fallback = fallbackMealPlan(request);
            enforceMealPlanTargetLimits(fallback, request);
            return fallback;
        } catch (HttpServerErrorException e) {
            long costMs = System.currentTimeMillis() - startTime;

            log.error(
                    "Gemini HTTP server error after {}ms, status: {}, response body: {}",
                    costMs,
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e
            );

            Map<String, Object> fallback = fallbackMealPlan(request);
            enforceMealPlanTargetLimits(fallback, request);
            return fallback;
        } catch (Exception e) {
            long costMs = System.currentTimeMillis() - startTime;

            log.error("generateMealPlan error after {}ms, fallback returned", costMs, e);

            Map<String, Object> fallback = fallbackMealPlan(request);
            enforceMealPlanTargetLimits(fallback, request);
            return fallback;
        }
    }

    private String buildPrompt(MealPlanGenerateRequest request) {
        String childName = safeString(request.getChildName(), "Guest");
        Integer age = request.getAge() == null ? 7 : request.getAge();
        String gender = safeString(request.getGender(), "boy");
        Double heightCm = request.getHeightCm() == null ? 120.0 : request.getHeightCm();
        Double weightKg = request.getWeightKg() == null ? 20.0 : request.getWeightKg();

        Double targetCarbs = request.getTargetCarbs() == null ? 155.0 : request.getTargetCarbs();
        Double targetProtein = request.getTargetProtein() == null ? 32.0 : request.getTargetProtein();
        Double targetFat = request.getTargetFat() == null ? 28.0 : request.getTargetFat();

        String allergies = request.getAllergies() == null
                ? "[]"
                : request.getAllergies().toString();

        String restrictions = request.getRestrictions() == null
                ? "{}"
                : request.getRestrictions().toString();

        String mealPreference = safeString(request.getMealPreference(), "");

        String preferenceInstruction;

        if (mealPreference.trim().length() > 0) {
            preferenceInstruction =
                    "The user wants to eat or include these foods: "
                            + mealPreference
                            + ". Try to include them if they are safe and suitable.";
        } else {
            preferenceInstruction =
                    "The user did not enter any food preference. Recommend meals based on the child profile, allergies, restrictions, and nutrition targets.";
        }

        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a child nutrition meal planning assistant.\n\n");

        prompt.append("Generate a 1-day meal plan for a child.\n\n");

        prompt.append("Child profile:\n");
        prompt.append("- name: ").append(childName).append("\n");
        prompt.append("- age: ").append(age).append("\n");
        prompt.append("- gender: ").append(gender).append("\n");
        prompt.append("- heightCm: ").append(heightCm).append("\n");
        prompt.append("- weightKg: ").append(weightKg).append("\n");
        prompt.append("- allergies: ").append(allergies).append("\n");
        prompt.append("- restrictions: ").append(restrictions).append("\n\n");

        prompt.append("User meal preference:\n");
        prompt.append(preferenceInstruction).append("\n\n");

        prompt.append("Daily nutrition targets:\n");
        prompt.append("- carbs: ").append(targetCarbs).append("g\n");
        prompt.append("- protein: ").append(targetProtein).append("g\n");
        prompt.append("- fat: ").append(targetFat).append("g\n\n");

        prompt.append("Recipe selection rules:\n");
        prompt.append("1. Use real, common recipe names.\n");
        prompt.append("2. Prefer Malaysian or family-friendly meals.\n");
        prompt.append("3. Avoid vague names like Healthy Breakfast Bowl unless it is a well-known recipe.\n");
        prompt.append("4. Avoid allergies and dietary restrictions.\n");
        prompt.append("5. Make meals suitable for children.\n");
        prompt.append("6. The full-day macro totals should be almost equal to the daily targets after portion adjustment.\n\n");

        prompt.append("Daily macro target matching rules:\n");
        prompt.append("1. targetCarbs = ").append(targetCarbs).append("g, targetProtein = ").append(targetProtein).append("g, targetFat = ").append(targetFat).append("g.\n");
        prompt.append("2. The sum of totalCarbohydrateG from breakfast + lunch + dinner + snack should be between 98% and 100% of targetCarbs, and must never exceed targetCarbs.\n");
        prompt.append("3. The sum of totalProteinG from breakfast + lunch + dinner + snack should be between 98% and 100% of targetProtein, and must never exceed targetProtein.\n");
        prompt.append("4. The sum of totalFatG from breakfast + lunch + dinner + snack should be between 98% and 100% of targetFat, and must never exceed targetFat.\n");
        prompt.append("5. Do not intentionally make the plan too low. A plan that is far below the targets is incorrect.\n");
        prompt.append("6. If any macro is too low, increase suitable ingredient gramsEstimated and measure portions, then recalculate all ingredient and meal totals.\n");
        prompt.append("7. If any macro is too high, reduce suitable ingredient gramsEstimated and measure portions, then recalculate all ingredient and meal totals.\n");
        prompt.append("8. Use carb-rich foods like rice, oats, bread, potato, pasta, fruit to adjust carbs. Use chicken, fish, egg, tofu, beef, beans, yogurt to adjust protein. Use oil, egg yolk, avocado, nuts, milk, yogurt, fish to adjust fat.\n");
        prompt.append("9. After every portion adjustment, update every ingredient's gramsEstimated, measure, energyKcal, proteinG, carbohydrateG, fatG.\n");
        prompt.append("10. After every portion adjustment, update each meal's totalEnergyKcal, totalProteinG, totalCarbohydrateG, totalFatG so they equal the sum of its ingredients.\n");
        prompt.append("11. Return realistic child-sized portions. Do not use very large portions like 1000g banana or 500g rice.\n");
        prompt.append("12. Prefer changing portion weights of existing ingredients instead of adding unusual ingredients.\n\n");

        prompt.append("Multilingual meal field requirements:\n");
        prompt.append("1. Every meal must include English, Simplified Chinese, and Malay versions of the main display fields.\n");
        prompt.append("2. strMeal should be English. strMealEn must equal the English name. strMealCn must be Simplified Chinese. strMealMs must be Malay.\n");
        prompt.append("3. strCategory should be English. strCategoryEn must equal the English category. strCategoryCn must be Simplified Chinese. strCategoryMs must be Malay.\n");
        prompt.append("4. strArea should be English. strAreaEn must equal the English area/cuisine. strAreaCn must be Simplified Chinese. strAreaMs must be Malay.\n");
        prompt.append("5. strInstructions should be English. strInstructionsEn must equal the English cooking instructions. strInstructionsCn must be Simplified Chinese. strInstructionsMs must be Malay.\n");
        prompt.append("6. Do not leave strMealCn, strMealMs, strCategoryCn, strCategoryMs, strAreaCn, strAreaMs, strInstructionsCn, or strInstructionsMs empty.\n");
        prompt.append("7. Keep translated names natural and short for app UI display.\n\n");

        prompt.append("URL requirements:\n");
        prompt.append("1. Do not invent fake URLs.\n");
        prompt.append("2. Do not use example.com.\n");
        prompt.append("3. Do not use placeholder URLs.\n");
        prompt.append("4. Do not create fake themealdb image links.\n");
        prompt.append("5. Do not create fake YouTube video IDs.\n");
        prompt.append("6. For strMealThumb, only return a real public HTTPS image URL if you are confident it exists.\n");
        prompt.append("7. If you are not confident the image URL exists, return an empty string for strMealThumb.\n");
        prompt.append("8. For strYoutube, do not return a direct YouTube video URL unless you are confident the video exists and matches the recipe.\n");
        prompt.append("9. If you are not confident about a direct YouTube video URL, return a YouTube search URL instead.\n");
        prompt.append("10. YouTube search URL format must be:\n");
        prompt.append("    https://www.youtube.com/results?search_query=RECIPE_NAME+tutorial\n");
        prompt.append("11. Replace spaces in RECIPE_NAME with +.\n");
        prompt.append("12. The YouTube search query must use the exact strMeal value plus the word tutorial.\n");
        prompt.append("13. strYoutube must never be empty.\n");
        prompt.append("14. If no direct video is known, use the YouTube search URL format.\n");
        prompt.append("15. strMealThumb can be empty, but strYoutube must be a valid YouTube search URL or a verified direct YouTube video URL.\n\n");

        prompt.append("Meal icon requirements:\n");
        prompt.append("1. Every meal must include mealIconEmoji, mealIconName, and mealIconPrompt.\n");
        prompt.append("2. mealIconEmoji must be a food emoji that matches the recipe.\n");
        prompt.append("3. Use specific emojis when possible, for example: 🍚 rice, 🍛 curry or mixed rice, 🍜 noodles, 🍲 soup, 🥗 salad, 🥪 sandwich or toast, 🍗 chicken, 🐟 fish, 🥚 eggs, 🥣 yogurt/oats/porridge, 🍌 banana, 🍎 fruit, 🥘 stew, 🌮 wrap, 🍝 pasta, 🥞 pancakes.\n");
        prompt.append("4. mealIconName should be a short English keyword like rice, rice-bowl, curry, noodle, soup, salad, sandwich, chicken, fish, egg, fruit, porridge, pasta.\n");
        prompt.append("5. mealIconPrompt should describe a cute flat food icon matching the recipe, app illustration style, white background.\n");
        prompt.append("6. If strMealThumb is empty, mealIconEmoji is required and should not be empty.\n\n");

        prompt.append("Each meal must include:\n");
        prompt.append("- idMeal\n");
        prompt.append("- strMeal\n");
        prompt.append("- strMealEn\n");
        prompt.append("- strMealCn\n");
        prompt.append("- strMealMs\n");
        prompt.append("- strCategory\n");
        prompt.append("- strCategoryEn\n");
        prompt.append("- strCategoryCn\n");
        prompt.append("- strCategoryMs\n");
        prompt.append("- strArea\n");
        prompt.append("- strAreaEn\n");
        prompt.append("- strAreaCn\n");
        prompt.append("- strAreaMs\n");
        prompt.append("- strInstructions\n");
        prompt.append("- strInstructionsEn\n");
        prompt.append("- strInstructionsCn\n");
        prompt.append("- strInstructionsMs\n");
        prompt.append("- strMealThumb\n");
        prompt.append("- mealIconEmoji\n");
        prompt.append("- mealIconName\n");
        prompt.append("- mealIconPrompt\n");
        prompt.append("- strYoutube\n");
        prompt.append("- totalEnergyKcal\n");
        prompt.append("- totalProteinG\n");
        prompt.append("- totalCarbohydrateG\n");
        prompt.append("- totalFatG\n");
        prompt.append("- ingredients\n\n");

        prompt.append("Each ingredient must include:\n");
        prompt.append("- ingredientName\n");
        prompt.append("- measure\n");
        prompt.append("- gramsEstimated\n");
        prompt.append("- foodNameEn\n");
        prompt.append("- foodNameCn\n");
        prompt.append("- foodNameMs\n");
        prompt.append("- foodGroup\n");
        prompt.append("- energyKcal\n");
        prompt.append("- proteinG\n");
        prompt.append("- carbohydrateG\n");
        prompt.append("- fatG\n\n");

        prompt.append("Strict JSON rules:\n");
        prompt.append("1. Return JSON only. Do not return markdown. Do not use ```json.\n");
        prompt.append("2. The JSON must be strictly valid and parseable by Jackson ObjectMapper.\n");
        prompt.append("3. Every object key must be wrapped in double quotes.\n");
        prompt.append("4. Every string value must be wrapped in double quotes.\n");
        prompt.append("5. Do not add comments.\n");
        prompt.append("6. Do not add trailing commas.\n");
        prompt.append("7. Do not return multiple JSON objects.\n");
        prompt.append("8. Do not add extra text before or after the JSON object.\n");
        prompt.append("9. Use realistic nutrition estimates. Do not make all values zero.\n");
        prompt.append("10. For strMealThumb, return empty string if you cannot provide a real working image URL.\n");
        prompt.append("11. For strYoutube, prefer a valid YouTube search URL using the exact recipe name.\n");
        prompt.append("12. Example: if strMeal is \"Chicken Rice\", strYoutube should be \"https://www.youtube.com/results?search_query=Chicken+Rice+tutorial\".\n");
        prompt.append("13. mealIconEmoji must be a valid emoji string and must not be empty.\n");
        prompt.append("14. mealIconName and mealIconPrompt must not be empty.\n");
        prompt.append("15. All multilingual fields ending with En, Cn, and Ms must be valid strings and must not be empty.\n");
        prompt.append("16. Use Simplified Chinese for fields ending with Cn and Malay for fields ending with Ms.\n");
        prompt.append("17. Daily totalCarbohydrateG should be between ").append(roundOne(targetCarbs * 0.98)).append(" and ").append(targetCarbs).append(".\n");
        prompt.append("18. Daily totalProteinG should be between ").append(roundOne(targetProtein * 0.98)).append(" and ").append(targetProtein).append(".\n");
        prompt.append("19. Daily totalFatG should be between ").append(roundOne(targetFat * 0.98)).append(" and ").append(targetFat).append(".\n");
        prompt.append("20. If daily totals are below or above those ranges, you must adjust ingredient weights and recalculate the affected meal totals before returning JSON.\n\n");

        prompt.append("Return exactly this JSON structure:\n");
        prompt.append("{\n");
        prompt.append("  \"plan\": {\n");
        prompt.append("    \"breakfast\": {\n");
        prompt.append("      \"idMeal\": \"ai-breakfast-1\",\n");
        prompt.append("      \"strMeal\": \"Recipe name\",\n");
        prompt.append("      \"strMealEn\": \"Recipe name\",\n");
        prompt.append("      \"strMealCn\": \"食谱名称\",\n");
        prompt.append("      \"strMealMs\": \"Nama resipi\",\n");
        prompt.append("      \"strCategory\": \"Breakfast\",\n");
        prompt.append("      \"strCategoryEn\": \"Breakfast\",\n");
        prompt.append("      \"strCategoryCn\": \"早餐\",\n");
        prompt.append("      \"strCategoryMs\": \"Sarapan\",\n");
        prompt.append("      \"strArea\": \"Malaysian or International\",\n");
        prompt.append("      \"strAreaEn\": \"Malaysian or International\",\n");
        prompt.append("      \"strAreaCn\": \"马来西亚或国际风味\",\n");
        prompt.append("      \"strAreaMs\": \"Malaysia atau Antarabangsa\",\n");
        prompt.append("      \"strInstructions\": \"Cooking instructions\",\n");
        prompt.append("      \"strInstructionsEn\": \"Cooking instructions\",\n");
        prompt.append("      \"strInstructionsCn\": \"烹饪步骤\",\n");
        prompt.append("      \"strInstructionsMs\": \"Arahan memasak\",\n");
        prompt.append("      \"strMealThumb\": \"real image url or empty string\",\n");
        prompt.append("      \"mealIconEmoji\": \"🍛\",\n");
        prompt.append("      \"mealIconName\": \"rice-bowl\",\n");
        prompt.append("      \"mealIconPrompt\": \"A cute flat food icon of this recipe, colorful, minimal, rounded, app illustration style, white background\",\n");
        prompt.append("      \"strYoutube\": \"https://www.youtube.com/results?search_query=Recipe+name+tutorial\",\n");
        prompt.append("      \"totalEnergyKcal\": 0,\n");
        prompt.append("      \"totalProteinG\": 0,\n");
        prompt.append("      \"totalCarbohydrateG\": 0,\n");
        prompt.append("      \"totalFatG\": 0,\n");
        prompt.append("      \"ingredients\": [\n");
        prompt.append("        {\n");
        prompt.append("          \"ingredientName\": \"Egg\",\n");
        prompt.append("          \"measure\": \"2 large\",\n");
        prompt.append("          \"gramsEstimated\": 100,\n");
        prompt.append("          \"foodNameEn\": \"Egg\",\n");
        prompt.append("          \"foodNameCn\": \"鸡蛋\",\n");
        prompt.append("          \"foodNameMs\": \"Telur\",\n");
        prompt.append("          \"foodGroup\": \"protein\",\n");
        prompt.append("          \"energyKcal\": 140,\n");
        prompt.append("          \"proteinG\": 12,\n");
        prompt.append("          \"carbohydrateG\": 1,\n");
        prompt.append("          \"fatG\": 10\n");
        prompt.append("        }\n");
        prompt.append("      ]\n");
        prompt.append("    },\n");
        prompt.append("    \"lunch\": {},\n");
        prompt.append("    \"dinner\": {},\n");
        prompt.append("    \"snack\": {}\n");
        prompt.append("  }\n");
        prompt.append("}\n");

        return prompt.toString();
    }

    private Map<String, Object> parseGeminiResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        String text = root
                .path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asText();

        if (text == null || text.trim().length() == 0) {
            throw new RuntimeException("Gemini returned empty text");
        }

        text = cleanJsonText(text);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(text, Map.class);

            ensureMealLanguageFields(result);
            ensureMealIconFields(result);
            ensureYoutubeSearchLinks(result);
            sanitizeMealPlanUrls(result);

            return result;
        } catch (Exception firstError) {
            System.out.println("First JSON parse failed: " + firstError.getMessage());

            String extractedJson = extractFirstJsonObject(text);

            if (extractedJson == null || extractedJson.trim().length() == 0) {
                System.out.println("Invalid Gemini JSON text:");
                System.out.println(text);
                throw firstError;
            }

            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = objectMapper.readValue(extractedJson, Map.class);

                ensureMealLanguageFields(result);
                ensureMealIconFields(result);
                ensureYoutubeSearchLinks(result);
                sanitizeMealPlanUrls(result);

                return result;
            } catch (Exception secondError) {
                System.out.println("Second JSON parse failed: " + secondError.getMessage());
                System.out.println("Invalid Gemini JSON text:");
                System.out.println(text);

                throw secondError;
            }
        }
    }

    private String cleanJsonText(String text) {
        if (text == null) {
            return "";
        }

        String cleaned = text.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        }

        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        cleaned = cleaned.trim();

        String extracted = extractFirstJsonObject(cleaned);

        if (extracted != null && extracted.trim().length() > 0) {
            return extracted.trim();
        }

        return cleaned;
    }

    private String extractFirstJsonObject(String text) {
        if (text == null) {
            return null;
        }

        int start = text.indexOf("{");

        if (start < 0) {
            return null;
        }

        int braceCount = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;

                    if (braceCount == 0) {
                        return text.substring(start, i + 1);
                    }
                }
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private void ensureMealLanguageFields(Map<String, Object> result) {
        if (result == null) {
            return;
        }

        Object planObj = result.get("plan");

        if (!(planObj instanceof Map)) {
            return;
        }

        Map<String, Object> plan = (Map<String, Object>) planObj;

        ensureMealLanguageField(plan.get("breakfast"));
        ensureMealLanguageField(plan.get("lunch"));
        ensureMealLanguageField(plan.get("dinner"));
        ensureMealLanguageField(plan.get("snack"));
    }

    @SuppressWarnings("unchecked")
    private void ensureMealLanguageField(Object mealObj) {
        if (!(mealObj instanceof Map)) {
            return;
        }

        Map<String, Object> meal = (Map<String, Object>) mealObj;

        String mealName = stringValue(meal.get("strMeal"), stringValue(meal.get("name"), "Meal"));
        String category = stringValue(meal.get("strCategory"), stringValue(meal.get("category"), "Meal"));
        String area = stringValue(meal.get("strArea"), stringValue(meal.get("area"), "AI Recommended"));
        String instructions = stringValue(meal.get("strInstructions"), stringValue(meal.get("instructions"), "Prepare ingredients, cook safely, and serve in an age-appropriate portion."));

        putIfBlank(meal, "strMealEn", mealName);
        putIfBlank(meal, "strMealCn", mealName);
        putIfBlank(meal, "strMealMs", mealName);

        putIfBlank(meal, "strCategoryEn", category);
        putIfBlank(meal, "strCategoryCn", category);
        putIfBlank(meal, "strCategoryMs", category);

        putIfBlank(meal, "strAreaEn", area);
        putIfBlank(meal, "strAreaCn", area);
        putIfBlank(meal, "strAreaMs", area);

        putIfBlank(meal, "strInstructionsEn", instructions);
        putIfBlank(meal, "strInstructionsCn", instructions);
        putIfBlank(meal, "strInstructionsMs", instructions);
    }

    private void putIfBlank(Map<String, Object> map, String key, String value) {
        Object existing = map.get(key);

        if (existing == null || String.valueOf(existing).trim().length() == 0) {
            map.put(key, value);
        }
    }

    private String stringValue(Object value, String fallback) {
        if (value == null || String.valueOf(value).trim().length() == 0) {
            return fallback;
        }

        return String.valueOf(value).trim();
    }

    @SuppressWarnings("unchecked")
    private void ensureMealIconFields(Map<String, Object> result) {
        if (result == null) {
            return;
        }

        Object planObj = result.get("plan");

        if (!(planObj instanceof Map)) {
            return;
        }

        Map<String, Object> plan = (Map<String, Object>) planObj;

        ensureMealIconField(plan.get("breakfast"));
        ensureMealIconField(plan.get("lunch"));
        ensureMealIconField(plan.get("dinner"));
        ensureMealIconField(plan.get("snack"));
    }

    @SuppressWarnings("unchecked")
    private void ensureMealIconField(Object mealObj) {
        if (!(mealObj instanceof Map)) {
            return;
        }

        Map<String, Object> meal = (Map<String, Object>) mealObj;

        String mealName = meal.get("strMeal") == null
                ? "meal"
                : String.valueOf(meal.get("strMeal")).trim();

        String category = meal.get("strCategory") == null
                ? ""
                : String.valueOf(meal.get("strCategory")).trim();

        String emoji = meal.get("mealIconEmoji") == null
                ? ""
                : String.valueOf(meal.get("mealIconEmoji")).trim();

        String iconName = meal.get("mealIconName") == null
                ? ""
                : String.valueOf(meal.get("mealIconName")).trim();

        String iconPrompt = meal.get("mealIconPrompt") == null
                ? ""
                : String.valueOf(meal.get("mealIconPrompt")).trim();

        if (emoji.length() == 0) {
            meal.put("mealIconEmoji", guessMealEmoji(mealName, category));
        }

        if (iconName.length() == 0) {
            meal.put("mealIconName", guessMealIconName(mealName, category));
        }

        if (iconPrompt.length() == 0) {
            meal.put("mealIconPrompt", buildMealIconPrompt(mealName));
        }
    }

    @SuppressWarnings("unchecked")
    private void ensureYoutubeSearchLinks(Map<String, Object> result) {
        if (result == null) {
            return;
        }

        Object planObj = result.get("plan");

        if (!(planObj instanceof Map)) {
            return;
        }

        Map<String, Object> plan = (Map<String, Object>) planObj;

        ensureYoutubeSearchLink(plan.get("breakfast"));
        ensureYoutubeSearchLink(plan.get("lunch"));
        ensureYoutubeSearchLink(plan.get("dinner"));
        ensureYoutubeSearchLink(plan.get("snack"));
    }

    @SuppressWarnings("unchecked")
    private void ensureYoutubeSearchLink(Object mealObj) {
        if (!(mealObj instanceof Map)) {
            return;
        }

        Map<String, Object> meal = (Map<String, Object>) mealObj;

        String youtubeUrl = meal.get("strYoutube") == null
                ? ""
                : String.valueOf(meal.get("strYoutube")).trim();

        if (isValidYoutubeUrl(youtubeUrl)) {
            return;
        }

        String mealName = meal.get("strMeal") == null
                ? "recipe"
                : String.valueOf(meal.get("strMeal")).trim();

        if (mealName.length() == 0) {
            mealName = "recipe";
        }

        meal.put("strYoutube", buildYoutubeSearchUrl(mealName));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sanitizeMealPlanUrls(Map<String, Object> result) {
        if (result == null) {
            return result;
        }

        Object planObj = result.get("plan");

        if (!(planObj instanceof Map)) {
            return result;
        }

        Map<String, Object> plan = (Map<String, Object>) planObj;

        sanitizeMealUrl(plan.get("breakfast"));
        sanitizeMealUrl(plan.get("lunch"));
        sanitizeMealUrl(plan.get("dinner"));
        sanitizeMealUrl(plan.get("snack"));

        return result;
    }

    @SuppressWarnings("unchecked")
    private void sanitizeMealUrl(Object mealObj) {
        if (!(mealObj instanceof Map)) {
            return;
        }

        Map<String, Object> meal = (Map<String, Object>) mealObj;

        String imageUrl = meal.get("strMealThumb") == null
                ? ""
                : String.valueOf(meal.get("strMealThumb")).trim();

        String youtubeUrl = meal.get("strYoutube") == null
                ? ""
                : String.valueOf(meal.get("strYoutube")).trim();

        if (!isValidImageUrl(imageUrl)) {
            meal.put("strMealThumb", "");
        }

        if (!isValidYoutubeUrl(youtubeUrl)) {
            String mealName = meal.get("strMeal") == null
                    ? "recipe"
                    : String.valueOf(meal.get("strMeal")).trim();

            meal.put("strYoutube", buildYoutubeSearchUrl(mealName));
        }
    }

    private boolean isValidImageUrl(String url) {
        if (url == null || url.trim().length() == 0) {
            return false;
        }

        String lower = url.toLowerCase();

        if (!lower.startsWith("https://")) {
            return false;
        }

        if (lower.contains("example.com")) {
            return false;
        }

        if (lower.contains("placeholder")) {
            return false;
        }

        if (lower.contains("chicken-rice.jpg")) {
            return false;
        }

        return lower.contains(".jpg")
                || lower.contains(".jpeg")
                || lower.contains(".png")
                || lower.contains(".webp");
    }

    private boolean isValidYoutubeUrl(String url) {
        if (url == null || url.trim().length() == 0) {
            return false;
        }

        String lower = url.toLowerCase();

        if (!lower.startsWith("https://")) {
            return false;
        }

        if (lower.contains("example")) {
            return false;
        }

        return lower.contains("youtube.com/watch")
                || lower.contains("youtu.be/")
                || lower.contains("youtube.com/results?search_query=");
    }

    private String buildYoutubeSearchUrl(String mealName) {
        try {
            String query = safeString(mealName, "recipe") + " tutorial";
            return "https://www.youtube.com/results?search_query="
                    + URLEncoder.encode(query, "UTF-8");
        } catch (Exception e) {
            return "https://www.youtube.com/results?search_query=recipe+tutorial";
        }
    }


    @SuppressWarnings("unchecked")
    private Map<String, Object> enforceMealPlanTargetLimits(Map<String, Object> result, MealPlanGenerateRequest request) {
        if (result == null) {
            return result;
        }

        Object planObj = result.get("plan");

        if (!(planObj instanceof Map)) {
            return result;
        }

        Map<String, Object> plan = (Map<String, Object>) planObj;

        double targetCarbs = normalizeTarget(request == null ? null : request.getTargetCarbs(), 155.0);
        double targetProtein = normalizeTarget(request == null ? null : request.getTargetProtein(), 32.0);
        double targetFat = normalizeTarget(request == null ? null : request.getTargetFat(), 28.0);

        String[] mealKeys = new String[] {"breakfast", "lunch", "dinner", "snack"};
        double[] before = recalculatePlanAndGetTotals(plan, mealKeys);

        // The ideal result is close to the target, but still not above it.
        // 99.5% gives a small rounding buffer while looking equal in the app UI.
        double targetFillRatio = 0.995;
        double maxRatio = 0.999;

        // First, reduce if Gemini returned a plan that is over any target.
        reducePlanIfOverTargets(plan, mealKeys, targetCarbs, targetProtein, targetFat, maxRatio);

        // Then fill missing macros by increasing the most suitable existing ingredients.
        // Repeat because increasing one macro can slightly affect the others.
        for (int i = 0; i < 12; i++) {
            reducePlanIfOverTargets(plan, mealKeys, targetCarbs, targetProtein, targetFat, maxRatio);

            double[] totals = recalculatePlanAndGetTotals(plan, mealKeys);
            int macroToFill = findMostMissingMacro(totals, targetCarbs, targetProtein, targetFat, targetFillRatio);

            if (macroToFill < 0) {
                break;
            }

            boolean changed = increaseDominantMacroIngredients(
                    plan,
                    mealKeys,
                    macroToFill,
                    targetCarbs,
                    targetProtein,
                    targetFat,
                    targetFillRatio,
                    maxRatio
            );

            if (!changed) {
                break;
            }
        }

        // Final safety pass: never let totals exceed the target after rounding.
        reducePlanIfOverTargets(plan, mealKeys, targetCarbs, targetProtein, targetFat, maxRatio);

        double[] after = recalculatePlanAndGetTotals(plan, mealKeys);

        log.info(
                "AI meal plan macros balanced. Before carbs/protein/fat: {}/{}/{}. Target: {}/{}/{}. After: {}/{}/{}",
                roundOne(before[0]),
                roundOne(before[1]),
                roundOne(before[2]),
                targetCarbs,
                targetProtein,
                targetFat,
                roundOne(after[0]),
                roundOne(after[1]),
                roundOne(after[2])
        );

        return result;
    }


    @SuppressWarnings("unchecked")
    private double[] recalculatePlanAndGetTotals(Map<String, Object> plan, String[] mealKeys) {
        double carbs = 0.0;
        double protein = 0.0;
        double fat = 0.0;

        for (String key : mealKeys) {
            Object mealObj = plan.get(key);

            if (!(mealObj instanceof Map)) {
                continue;
            }

            Map<String, Object> meal = (Map<String, Object>) mealObj;
            recalculateMealTotalsFromIngredients(meal);

            carbs += numberValue(meal.get("totalCarbohydrateG"));
            protein += numberValue(meal.get("totalProteinG"));
            fat += numberValue(meal.get("totalFatG"));
        }

        return new double[] {carbs, protein, fat};
    }

    @SuppressWarnings("unchecked")
    private void reducePlanIfOverTargets(
            Map<String, Object> plan,
            String[] mealKeys,
            double targetCarbs,
            double targetProtein,
            double targetFat,
            double maxRatio
    ) {
        double[] totals = recalculatePlanAndGetTotals(plan, mealKeys);

        double scale = 1.0;

        if (totals[0] > targetCarbs && totals[0] > 0) {
            scale = Math.min(scale, (targetCarbs * maxRatio) / totals[0]);
        }

        if (totals[1] > targetProtein && totals[1] > 0) {
            scale = Math.min(scale, (targetProtein * maxRatio) / totals[1]);
        }

        if (totals[2] > targetFat && totals[2] > 0) {
            scale = Math.min(scale, (targetFat * maxRatio) / totals[2]);
        }

        if (scale >= 0.9999) {
            return;
        }

        scale = Math.max(0.05, Math.min(1.0, scale));

        for (String key : mealKeys) {
            Object mealObj = plan.get(key);

            if (mealObj instanceof Map) {
                scaleMealNutrition((Map<String, Object>) mealObj, scale);
            }
        }
    }

    private int findMostMissingMacro(
            double[] totals,
            double targetCarbs,
            double targetProtein,
            double targetFat,
            double targetFillRatio
    ) {
        double carbsRatio = targetCarbs > 0 ? totals[0] / targetCarbs : 1.0;
        double proteinRatio = targetProtein > 0 ? totals[1] / targetProtein : 1.0;
        double fatRatio = targetFat > 0 ? totals[2] / targetFat : 1.0;

        int macroIndex = -1;
        double lowestRatio = targetFillRatio;

        if (carbsRatio < lowestRatio) {
            lowestRatio = carbsRatio;
            macroIndex = 0;
        }

        if (proteinRatio < lowestRatio) {
            lowestRatio = proteinRatio;
            macroIndex = 1;
        }

        if (fatRatio < lowestRatio) {
            macroIndex = 2;
        }

        return macroIndex;
    }

    @SuppressWarnings("unchecked")
    private boolean increaseDominantMacroIngredients(
            Map<String, Object> plan,
            String[] mealKeys,
            int macroIndex,
            double targetCarbs,
            double targetProtein,
            double targetFat,
            double targetFillRatio,
            double maxRatio
    ) {
        double[] totals = recalculatePlanAndGetTotals(plan, mealKeys);
        double[] targets = new double[] {targetCarbs, targetProtein, targetFat};
        double desiredMacroTotal = targets[macroIndex] * targetFillRatio;
        double missing = desiredMacroTotal - totals[macroIndex];

        if (missing <= 0) {
            return false;
        }

        List<Map<String, Object>> candidates = collectDominantMacroIngredients(plan, mealKeys, macroIndex, true);

        if (candidates.size() == 0) {
            candidates = collectDominantMacroIngredients(plan, mealKeys, macroIndex, false);
        }

        if (candidates.size() == 0) {
            return false;
        }

        double[] candidateTotals = ingredientListMacroTotals(candidates);

        if (candidateTotals[macroIndex] <= 0) {
            return false;
        }

        double desiredFactor = 1.0 + (missing / candidateTotals[macroIndex]);
        double maxFactor = desiredFactor;

        for (int i = 0; i < 3; i++) {
            if (candidateTotals[i] <= 0) {
                continue;
            }

            double allowedTotal = targets[i] * maxRatio;
            double availableIncrease = allowedTotal - totals[i];

            if (availableIncrease <= 0) {
                return false;
            }

            maxFactor = Math.min(maxFactor, 1.0 + availableIncrease / candidateTotals[i]);
        }

        double factor = Math.min(desiredFactor, maxFactor);

        // Avoid many tiny adjustments that cause noisy grams.
        if (factor <= 1.005) {
            return false;
        }

        // Do not suddenly double a child portion in one pass. The loop can adjust again.
        factor = Math.min(factor, 1.35);

        for (Map<String, Object> ingredient : candidates) {
            scaleIngredientNutrition(ingredient, factor);
        }

        recalculatePlanAndGetTotals(plan, mealKeys);
        return true;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> collectDominantMacroIngredients(
            Map<String, Object> plan,
            String[] mealKeys,
            int macroIndex,
            boolean strict
    ) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

        for (String key : mealKeys) {
            Object mealObj = plan.get(key);

            if (!(mealObj instanceof Map)) {
                continue;
            }

            Object ingredientsObj = ((Map<String, Object>) mealObj).get("ingredients");

            if (!(ingredientsObj instanceof List)) {
                continue;
            }

            List<Object> ingredients = (List<Object>) ingredientsObj;

            for (Object ingredientObj : ingredients) {
                if (!(ingredientObj instanceof Map)) {
                    continue;
                }

                Map<String, Object> ingredient = (Map<String, Object>) ingredientObj;

                if (isMacroCandidate(ingredient, macroIndex, strict)) {
                    result.add(ingredient);
                }
            }
        }

        return result;
    }

    private boolean isMacroCandidate(Map<String, Object> ingredient, int macroIndex, boolean strict) {
        double carbs = numberValue(ingredient.get("carbohydrateG"));
        double protein = numberValue(ingredient.get("proteinG"));
        double fat = numberValue(ingredient.get("fatG"));

        String text = (
                stringValue(ingredient.get("foodGroup"), "") + " "
                        + stringValue(ingredient.get("foodNameEn"), "") + " "
                        + stringValue(ingredient.get("ingredientName"), "")
        ).toLowerCase();

        if (macroIndex == 0) {
            if (carbs <= 0) return false;

            if (containsAny(text, "carb", "grain", "rice", "bread", "noodle", "pasta", "oat", "potato", "fruit", "banana", "apple", "flour", "cereal")) {
                return true;
            }

            return !strict && carbs >= protein && carbs >= fat;
        }

        if (macroIndex == 1) {
            if (protein <= 0) return false;

            if (containsAny(text, "protein", "meat", "chicken", "fish", "egg", "beef", "tofu", "bean", "lentil", "yogurt", "milk", "seafood", "prawn", "shrimp")) {
                return true;
            }

            return !strict && protein >= carbs && protein >= fat;
        }

        if (fat <= 0) return false;

        if (containsAny(text, "fat", "oil", "butter", "avocado", "nut", "peanut", "almond", "cashew", "egg", "milk", "yogurt", "cheese", "salmon", "fish")) {
            return true;
        }

        return !strict && fat >= carbs && fat >= protein;
    }

    private double[] ingredientListMacroTotals(List<Map<String, Object>> ingredients) {
        double carbs = 0.0;
        double protein = 0.0;
        double fat = 0.0;

        for (Map<String, Object> ingredient : ingredients) {
            carbs += numberValue(ingredient.get("carbohydrateG"));
            protein += numberValue(ingredient.get("proteinG"));
            fat += numberValue(ingredient.get("fatG"));
        }

        return new double[] {carbs, protein, fat};
    }

    private double normalizeTarget(Double value, double fallback) {
        if (value == null || value.doubleValue() <= 0 || Double.isNaN(value.doubleValue()) || Double.isInfinite(value.doubleValue())) {
            return fallback;
        }

        return value.doubleValue();
    }

    @SuppressWarnings("unchecked")
    private void scaleMealNutrition(Map<String, Object> meal, double scale) {
        Object ingredientsObj = meal.get("ingredients");

        if (ingredientsObj instanceof List) {
            List<Object> ingredients = (List<Object>) ingredientsObj;

            for (Object ingredientObj : ingredients) {
                if (ingredientObj instanceof Map) {
                    scaleIngredientNutrition((Map<String, Object>) ingredientObj, scale);
                }
            }

            recalculateMealTotalsFromIngredients(meal);
            return;
        }

        meal.put("totalEnergyKcal", roundOne(numberValue(meal.get("totalEnergyKcal")) * scale));
        meal.put("totalProteinG", roundOne(numberValue(meal.get("totalProteinG")) * scale));
        meal.put("totalCarbohydrateG", roundOne(numberValue(meal.get("totalCarbohydrateG")) * scale));
        meal.put("totalFatG", roundOne(numberValue(meal.get("totalFatG")) * scale));
    }

    private void scaleIngredientNutrition(Map<String, Object> ingredient, double scale) {
        double originalGrams = numberValue(ingredient.get("gramsEstimated"));
        int nextGrams = originalGrams > 0 ? Math.max(1, (int) Math.round(originalGrams * scale)) : 0;

        if (nextGrams > 0) {
            ingredient.put("gramsEstimated", nextGrams);
            ingredient.put("measure", nextGrams + "g");
        }

        ingredient.put("energyKcal", roundOne(numberValue(ingredient.get("energyKcal")) * scale));
        ingredient.put("proteinG", roundOne(numberValue(ingredient.get("proteinG")) * scale));
        ingredient.put("carbohydrateG", roundOne(numberValue(ingredient.get("carbohydrateG")) * scale));
        ingredient.put("fatG", roundOne(numberValue(ingredient.get("fatG")) * scale));
    }

    @SuppressWarnings("unchecked")
    private void recalculateMealTotalsFromIngredients(Map<String, Object> meal) {
        Object ingredientsObj = meal.get("ingredients");

        if (!(ingredientsObj instanceof List)) {
            return;
        }

        double kcal = 0.0;
        double protein = 0.0;
        double carbs = 0.0;
        double fat = 0.0;
        boolean hasIngredient = false;

        List<Object> ingredients = (List<Object>) ingredientsObj;

        for (Object ingredientObj : ingredients) {
            if (!(ingredientObj instanceof Map)) {
                continue;
            }

            Map<String, Object> ingredient = (Map<String, Object>) ingredientObj;
            kcal += numberValue(ingredient.get("energyKcal"));
            protein += numberValue(ingredient.get("proteinG"));
            carbs += numberValue(ingredient.get("carbohydrateG"));
            fat += numberValue(ingredient.get("fatG"));
            hasIngredient = true;
        }

        if (!hasIngredient) {
            return;
        }

        meal.put("totalEnergyKcal", roundOne(kcal));
        meal.put("totalProteinG", roundOne(protein));
        meal.put("totalCarbohydrateG", roundOne(carbs));
        meal.put("totalFatG", roundOne(fat));
    }

    private double numberValue(Object value) {
        if (value == null) {
            return 0.0;
        }

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private Map<String, Object> fallbackMealPlan(MealPlanGenerateRequest request) {
        Map<String, Object> breakfast = meal(
                "ai-breakfast",
                "Scrambled Eggs with Toast",
                "Breakfast",
                320,
                18,
                36,
                11,
                ingredients(
                        ingredient("Eggs", "2 eggs", 100, "Eggs", "鸡蛋", "Telur", "protein", 140, 12, 1, 10),
                        ingredient("Whole wheat bread", "2 slices", 60, "Whole wheat bread", "全麦面包", "Roti gandum", "carbs", 160, 6, 30, 2),
                        ingredient("Tomato", "1 small", 80, "Tomato", "番茄", "Tomato", "vegetables", 20, 1, 5, 0)
                )
        );

        Map<String, Object> lunch = meal(
                "ai-lunch",
                "Chicken Rice with Vegetables",
                "Lunch",
                520,
                32,
                60,
                14,
                ingredients(
                        ingredient("Chicken breast", "120g", 120, "Chicken breast", "鸡胸肉", "Dada ayam", "protein", 198, 30, 0, 5),
                        ingredient("Rice", "1 bowl", 180, "Rice", "米饭", "Nasi", "carbs", 230, 4, 52, 1),
                        ingredient("Broccoli", "1 cup", 90, "Broccoli", "西兰花", "Brokoli", "vegetables", 35, 3, 7, 0)
                )
        );

        Map<String, Object> dinner = meal(
                "ai-dinner",
                "Fish Soup with Rice",
                "Dinner",
                430,
                28,
                45,
                10,
                ingredients(
                        ingredient("Fish fillet", "120g", 120, "Fish fillet", "鱼片", "Isi ikan", "protein", 160, 25, 0, 5),
                        ingredient("Rice", "1/2 bowl", 100, "Rice", "米饭", "Nasi", "carbs", 130, 2, 29, 0),
                        ingredient("Mushroom", "1/2 cup", 60, "Mushroom", "蘑菇", "Cendawan", "vegetables", 25, 2, 4, 0)
                )
        );

        Map<String, Object> snack = meal(
                "ai-snack",
                "Yogurt with Banana",
                "Snack",
                210,
                8,
                34,
                4,
                ingredients(
                        ingredient("Yogurt", "1 cup", 150, "Yogurt", "酸奶", "Yogurt", "others", 120, 6, 14, 3),
                        ingredient("Banana", "1 piece", 100, "Banana", "香蕉", "Pisang", "fruit", 90, 1, 20, 1)
                )
        );

        Map<String, Object> plan = new HashMap<String, Object>();
        plan.put("breakfast", breakfast);
        plan.put("lunch", lunch);
        plan.put("dinner", dinner);
        plan.put("snack", snack);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("plan", plan);

        return result;
    }

    private Map<String, Object> meal(
            String id,
            String name,
            String category,
            int kcal,
            int protein,
            int carbs,
            int fat,
            List<Map<String, Object>> ingredients
    ) {
        Map<String, Object> meal = new HashMap<String, Object>();

        meal.put("idMeal", id);
        meal.put("strMeal", name);
        meal.put("strMealEn", name);
        meal.put("strMealCn", fallbackMealNameCn(name));
        meal.put("strMealMs", fallbackMealNameMs(name));
        meal.put("strCategory", category);
        meal.put("strCategoryEn", category);
        meal.put("strCategoryCn", fallbackCategoryCn(category));
        meal.put("strCategoryMs", fallbackCategoryMs(category));
        meal.put("strArea", "AI Recommended");
        meal.put("strAreaEn", "AI Recommended");
        meal.put("strAreaCn", "AI 推荐");
        meal.put("strAreaMs", "Cadangan AI");
        meal.put("strInstructions", "Prepare ingredients, cook safely, and serve in an age-appropriate portion.");
        meal.put("strInstructionsEn", "Prepare ingredients, cook safely, and serve in an age-appropriate portion.");
        meal.put("strInstructionsCn", "准备食材，安全烹饪，并按孩子年龄提供合适份量。");
        meal.put("strInstructionsMs", "Sediakan bahan, masak dengan selamat, dan hidangkan mengikut saiz sesuai umur kanak-kanak.");
        meal.put("strMealThumb", "");
        meal.put("mealIconEmoji", guessMealEmoji(name, category));
        meal.put("mealIconName", guessMealIconName(name, category));
        meal.put("mealIconPrompt", buildMealIconPrompt(name));
        meal.put("strYoutube", buildYoutubeSearchUrl(name));
        meal.put("totalEnergyKcal", kcal);
        meal.put("totalProteinG", protein);
        meal.put("totalCarbohydrateG", carbs);
        meal.put("totalFatG", fat);
        meal.put("ingredients", ingredients);

        return meal;
    }

    private List<Map<String, Object>> ingredients(Map<String, Object>... items) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        if (items != null) {
            for (Map<String, Object> item : items) {
                list.add(item);
            }
        }

        return list;
    }

    private Map<String, Object> ingredient(
            String ingredientName,
            String measure,
            int grams,
            String foodNameEn,
            String foodNameCn,
            String foodNameMs,
            String foodGroup,
            int kcal,
            int protein,
            int carbs,
            int fat
    ) {
        Map<String, Object> ingredient = new HashMap<String, Object>();

        ingredient.put("ingredientName", ingredientName);
        ingredient.put("measure", measure);
        ingredient.put("gramsEstimated", grams);
        ingredient.put("foodNameEn", foodNameEn);
        ingredient.put("foodNameCn", foodNameCn);
        ingredient.put("foodNameMs", foodNameMs);
        ingredient.put("foodGroup", foodGroup);
        ingredient.put("energyKcal", kcal);
        ingredient.put("proteinG", protein);
        ingredient.put("carbohydrateG", carbs);
        ingredient.put("fatG", fat);

        return ingredient;
    }

    private String fallbackMealNameCn(String name) {
        String text = safeString(name, "").toLowerCase();

        if (text.contains("scrambled eggs")) return "炒蛋配吐司";
        if (text.contains("chicken rice")) return "鸡肉饭配蔬菜";
        if (text.contains("fish soup")) return "鱼汤配米饭";
        if (text.contains("yogurt")) return "酸奶配香蕉";

        return safeString(name, "餐食");
    }

    private String fallbackMealNameMs(String name) {
        String text = safeString(name, "").toLowerCase();

        if (text.contains("scrambled eggs")) return "Telur hancur dengan roti bakar";
        if (text.contains("chicken rice")) return "Nasi ayam dengan sayur";
        if (text.contains("fish soup")) return "Sup ikan dengan nasi";
        if (text.contains("yogurt")) return "Yogurt dengan pisang";

        return safeString(name, "Hidangan");
    }

    private String fallbackCategoryCn(String category) {
        String text = safeString(category, "").toLowerCase();

        if (text.contains("breakfast")) return "早餐";
        if (text.contains("lunch")) return "午餐";
        if (text.contains("dinner")) return "晚餐";
        if (text.contains("snack")) return "加餐";

        return safeString(category, "餐食");
    }

    private String fallbackCategoryMs(String category) {
        String text = safeString(category, "").toLowerCase();

        if (text.contains("breakfast")) return "Sarapan";
        if (text.contains("lunch")) return "Makan Tengah Hari";
        if (text.contains("dinner")) return "Makan Malam";
        if (text.contains("snack")) return "Snek";

        return safeString(category, "Hidangan");
    }

    private String guessMealEmoji(String name, String category) {
        String text = (safeString(name, "") + " " + safeString(category, "")).toLowerCase();

        if (containsAny(text, "nasi lemak", "coconut rice")) return "🍛";
        if (containsAny(text, "chicken rice", "rice bowl", "fried rice", "nasi", "rice")) return "🍚";
        if (containsAny(text, "curry", "rendang", "laksa", "biryani", "briyani", "korma")) return "🍛";
        if (containsAny(text, "noodle", "mee", "mie", "ramen", "udon", "vermicelli", "bee hoon", "kuey teow", "laksa")) return "🍜";
        if (containsAny(text, "pasta", "spaghetti", "macaroni", "lasagna", "lasagne")) return "🍝";
        if (containsAny(text, "soup", "porridge", "congee", "broth")) return "🍲";
        if (containsAny(text, "stew", "hotpot", "claypot")) return "🥘";
        if (containsAny(text, "salad", "vegetable bowl", "greens")) return "🥗";
        if (containsAny(text, "sandwich", "toast", "bread", "burger", "wrap", "roti", "chapati", "tortilla")) return "🥪";
        if (containsAny(text, "pancake", "waffle", "crepe")) return "🥞";
        if (containsAny(text, "pizza")) return "🍕";
        if (containsAny(text, "sushi", "maki")) return "🍣";
        if (containsAny(text, "dumpling", "gyoza", "wonton")) return "🥟";
        if (containsAny(text, "taco")) return "🌮";
        if (containsAny(text, "burrito")) return "🌯";
        if (containsAny(text, "chicken", "ayam", "drumstick", "wing")) return "🍗";
        if (containsAny(text, "fish", "salmon", "tuna", "sardine", "ikan")) return "🐟";
        if (containsAny(text, "shrimp", "prawn", "seafood")) return "🍤";
        if (containsAny(text, "egg", "omelette", "scrambled")) return "🥚";
        if (containsAny(text, "beef", "steak", "meatball")) return "🥩";
        if (containsAny(text, "tofu", "tempeh", "bean", "lentil", "chickpea", "dal")) return "🫘";
        if (containsAny(text, "cheese")) return "🧀";
        if (containsAny(text, "milk")) return "🥛";
        if (containsAny(text, "yogurt", "oat", "granola", "cereal", "muesli")) return "🥣";
        if (containsAny(text, "banana")) return "🍌";
        if (containsAny(text, "apple")) return "🍎";
        if (containsAny(text, "orange", "tangerine")) return "🍊";
        if (containsAny(text, "mango")) return "🥭";
        if (containsAny(text, "pineapple")) return "🍍";
        if (containsAny(text, "watermelon")) return "🍉";
        if (containsAny(text, "strawberry", "berry", "blueberry")) return "🍓";
        if (containsAny(text, "grape")) return "🍇";
        if (containsAny(text, "avocado")) return "🥑";
        if (containsAny(text, "carrot")) return "🥕";
        if (containsAny(text, "corn")) return "🌽";
        if (containsAny(text, "potato", "sweet potato")) return "🥔";
        if (containsAny(text, "broccoli")) return "🥦";
        if (containsAny(text, "tomato")) return "🍅";
        if (containsAny(text, "mushroom")) return "🍄";
        if (containsAny(text, "peanut", "nut", "almond", "cashew")) return "🥜";
        if (containsAny(text, "cake", "muffin", "cupcake")) return "🧁";
        if (containsAny(text, "cookie", "biscuit")) return "🍪";
        if (containsAny(text, "smoothie", "juice")) return "🥤";
        if (containsAny(text, "tea")) return "🍵";

        return "🍽️";
    }

    private String guessMealIconName(String name, String category) {
        String text = (safeString(name, "") + " " + safeString(category, "")).toLowerCase();

        if (containsAny(text, "nasi lemak", "chicken rice", "fried rice", "rice bowl", "nasi", "rice")) return "rice";
        if (containsAny(text, "curry", "rendang", "laksa", "biryani", "briyani", "korma")) return "curry";
        if (containsAny(text, "noodle", "mee", "mie", "ramen", "udon", "vermicelli", "bee hoon", "kuey teow")) return "noodle";
        if (containsAny(text, "pasta", "spaghetti", "macaroni", "lasagna", "lasagne")) return "pasta";
        if (containsAny(text, "soup", "porridge", "congee", "broth")) return "soup";
        if (containsAny(text, "stew", "hotpot", "claypot")) return "stew";
        if (containsAny(text, "salad", "vegetable bowl", "greens")) return "salad";
        if (containsAny(text, "sandwich", "toast", "bread", "burger", "wrap", "roti", "chapati", "tortilla")) return "sandwich";
        if (containsAny(text, "pancake", "waffle", "crepe")) return "pancake";
        if (containsAny(text, "pizza")) return "pizza";
        if (containsAny(text, "sushi", "maki")) return "sushi";
        if (containsAny(text, "dumpling", "gyoza", "wonton")) return "dumpling";
        if (containsAny(text, "taco")) return "taco";
        if (containsAny(text, "burrito")) return "burrito";
        if (containsAny(text, "chicken", "ayam", "drumstick", "wing")) return "chicken";
        if (containsAny(text, "fish", "salmon", "tuna", "sardine", "ikan")) return "fish";
        if (containsAny(text, "shrimp", "prawn", "seafood")) return "seafood";
        if (containsAny(text, "egg", "omelette", "scrambled")) return "egg";
        if (containsAny(text, "beef", "steak", "meatball")) return "beef";
        if (containsAny(text, "tofu", "tempeh", "bean", "lentil", "chickpea", "dal")) return "beans";
        if (containsAny(text, "cheese")) return "cheese";
        if (containsAny(text, "milk")) return "milk";
        if (containsAny(text, "yogurt", "oat", "granola", "cereal", "muesli")) return "bowl";
        if (containsAny(text, "banana", "apple", "orange", "mango", "pineapple", "watermelon", "strawberry", "berry", "grape", "fruit")) return "fruit";
        if (containsAny(text, "vegetable", "carrot", "corn", "potato", "broccoli", "tomato", "mushroom")) return "vegetable";
        if (containsAny(text, "cake", "muffin", "cupcake", "cookie", "biscuit")) return "snack";
        if (containsAny(text, "smoothie", "juice", "tea")) return "drink";

        return "meal";
    }

    private String buildMealIconPrompt(String mealName) {
        return "A cute flat food icon of "
                + safeString(mealName, "meal")
                + ", colorful, minimal, rounded, app illustration style, white background";
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null || keywords == null) {
            return false;
        }

        for (String keyword : keywords) {
            if (keyword != null && text.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    private String safeString(String value, String fallback) {
        if (value == null || value.trim().length() == 0) {
            return fallback;
        }

        return value.trim();
    }
}
