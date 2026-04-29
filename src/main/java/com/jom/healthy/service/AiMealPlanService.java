package com.jom.healthy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jom.healthy.dto.MealPlanGenerateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiMealPlanService {

    @Value("${GEMINI_API_KEY:}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> generateMealPlan(MealPlanGenerateRequest request) {
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

            long costMs = System.currentTimeMillis() - startTime;
            System.out.println("Gemini meal plan generation took " + costMs + " ms");

            return result;
        } catch (Exception e) {
            e.printStackTrace();

            long costMs = System.currentTimeMillis() - startTime;
            System.out.println("Gemini meal plan generation failed after " + costMs + " ms, fallback returned.");

            return fallbackMealPlan(request);
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
        prompt.append("6. The total macros should be reasonably close to the nutrition targets.\n\n");

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

        prompt.append("Each meal must include:\n");
        prompt.append("- idMeal\n");
        prompt.append("- strMeal\n");
        prompt.append("- strCategory\n");
        prompt.append("- strArea\n");
        prompt.append("- strInstructions\n");
        prompt.append("- strMealThumb\n");
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
        prompt.append("12. Example: if strMeal is \"Chicken Rice\", strYoutube should be \"https://www.youtube.com/results?search_query=Chicken+Rice+tutorial\".\n\n");

        prompt.append("Return exactly this JSON structure:\n");
        prompt.append("{\n");
        prompt.append("  \"plan\": {\n");
        prompt.append("    \"breakfast\": {\n");
        prompt.append("      \"idMeal\": \"ai-breakfast-1\",\n");
        prompt.append("      \"strMeal\": \"Recipe name\",\n");
        prompt.append("      \"strCategory\": \"Breakfast\",\n");
        prompt.append("      \"strArea\": \"Malaysian or International\",\n");
        prompt.append("      \"strInstructions\": \"Cooking instructions\",\n");
        prompt.append("      \"strMealThumb\": \"real image url or empty string\",\n");
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
        meal.put("strCategory", category);
        meal.put("strArea", "AI Recommended");
        meal.put("strInstructions", "Prepare ingredients, cook safely, and serve in an age-appropriate portion.");
        meal.put("strMealThumb", "");
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

    private String safeString(String value, String fallback) {
        if (value == null || value.trim().length() == 0) {
            return fallback;
        }

        return value.trim();
    }
}