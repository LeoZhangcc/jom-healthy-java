package com.jom.healthy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jom.healthy.dto.MealNutritionDto;
import com.jom.healthy.dto.MealPlanGenerateRequest;
import com.jom.healthy.entity.TheMealDbMeal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class AiMealPlanService {

    @Value("${GEMINI_API_KEY:}")
    private String geminiApiKey;

    @Value("${GROQ_API_KEY:}")
    private String groqApiKey;

    @Value("${GROQ_MODEL:openai/gpt-oss-20b}")
    private String groqModel;

    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TheMealService theMealService;

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
            generationConfig.put("temperature", 0.35);
            generationConfig.put("responseMimeType", "application/json");
            // Multi-day plans are large JSON payloads. Without an explicit high output cap,
            // Gemini may stop mid-object and return incomplete JSON.
            generationConfig.put("maxOutputTokens", 65535);

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

    public Object generateMealPlanByGroqTest(MealPlanGenerateRequest request) {
        Map<String, Object> userMessage = new HashMap<String, Object>();
        userMessage.put("role", "user");
        userMessage.put("content", "Explain the importance of fast language models");

        List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();
        messages.add(userMessage);

        Map<String, Object> body = new HashMap<String, Object>();
        body.put("messages", messages);
        body.put("model", groqModel);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + groqApiKey.trim());
        headers.set("Accept", "application/json");
        headers.set(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"
        );

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<Map<String, Object>>(body, headers);

        log.info("entity:{}", entity);

        String url = "https://api.groq.com/openai/v1/chat/completions";

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
        );

        log.info("Groq response: {}", response.getBody());
        return null;
    }

    public Map<String, Object> generateMealPlanByGroq(MealPlanGenerateRequest request) {
        log.info("generateMealPlanByGroq start=====request:{}", request);
        long startTime = System.currentTimeMillis();

        /*
         * Your current Groq on_demand quota is 8000 TPM.
         * A multi-day full JSON plan reserves too many tokens in one request,
         * especially when max_completion_tokens is large.
         *
         * So:
         * - 1 day: Groq can still be used.
         * - 2+ days: delegate to Gemini's generateMealPlan(...) once,
         *   which already supports multi-day single-request output.
         */
//        int requestedDays = normalizeRequestedDays(request);
//        if (requestedDays > 1) {
//            log.info(
//                    "generateMealPlanByGroq rerouted to Gemini for multi-day request=====days:{}",
//                    requestedDays
//            );
//            return generateMealPlan(request);
//        }

        try {
            if (groqApiKey == null || groqApiKey.trim().length() == 0) {
                throw new RuntimeException("GROQ_API_KEY is empty");
            }

            log.info("generateMealPlanByGroq =====groqModel:{}", groqModel);

            List<TheMealDbMeal> candidateMeals =
                    theMealService.selectRandomMealCandidatesForAi(request, 30);

            if (candidateMeals == null || candidateMeals.isEmpty()) {
                throw new RuntimeException("No safe candidate meals were found in themealdb_meals.");
            }

            String prompt = buildGroqDatabaseMealSelectionPrompt(request, candidateMeals);

            Map<String, Object> systemMessage = new HashMap<String, Object>();
            systemMessage.put("role", "system");
            systemMessage.put(
                    "content",
                    "You are a child nutrition meal planning assistant. " +
                            "Return one strictly valid JSON object only. " +
                            "Do not return markdown, code fences, or extra explanations."
            );

            Map<String, Object> userMessage = new HashMap<String, Object>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();
            messages.add(systemMessage);
            messages.add(userMessage);

            Map<String, Object> body = new HashMap<String, Object>();
            body.put("model", groqModel);
            body.put("messages", messages);
            body.put("temperature", 0.5);
            body.put("max_completion_tokens", 6000);
            body.put("reasoning_effort", "low");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + groqApiKey.trim());
            headers.set("Accept", "application/json");

            // Required in your environment, otherwise Groq was blocked by Cloudflare 1010
            headers.set(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/148.0.0.0 Safari/537.36"
            );

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<Map<String, Object>>(body, headers);

            log.info(
                    "generateMealPlanByGroq request ready=====promptLength:{}, maxCompletionTokens:{}",
                    prompt == null ? 0 : prompt.length(),
                    6000
            );

            String url = "https://api.groq.com/openai/v1/chat/completions";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info(
                    "generateMealPlanByGroq response received=====status:{}",
                    response.getStatusCode()
            );

            Map<String, Object> selection = parseGroqMealSelectionResponse(response.getBody());
            Map<String, Object> result = buildDatabaseMealPlanFromGroqSelection(
                    selection,
                    candidateMeals,
                    request
            );

            ensureMealLanguageFields(result);
            ensureMealIconFields(result);
            ensureYoutubeSearchLinks(result);
            sanitizeMealPlanUrls(result);

            enforceMealPlanTargetLimitsByMacroGroupScaling(result, request);

            long costMs = System.currentTimeMillis() - startTime;
            log.info(
                    "generateMealPlanByGroq end=====took:{}ms, model:{}",
                    costMs,
                    groqModel
            );

            return result;

        } catch (HttpClientErrorException e) {
            long costMs = System.currentTimeMillis() - startTime;

            log.error(
                    "Groq HTTP client error after {}ms, status: {}, response body: {}",
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
                    "Groq HTTP server error after {}ms, status: {}, response body: {}",
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

            log.error(
                    "generateMealPlanByGroq error after {}ms, fallback returned",
                    costMs,
                    e
            );

            Map<String, Object> fallback = fallbackMealPlan(request);
            enforceMealPlanTargetLimits(fallback, request);
            return fallback;
        }
    }

    /**
     * Groq 专用 Prompt：
     * 这里只让 AI 从数据库候选 strMeal 里挑选餐单，
     * 不让 AI 自己编造完整营养、食材或做法。
     * 完整食谱数据随后由后端按 strMeal 回查数据库。
     */
    private String buildGroqDatabaseMealSelectionPrompt(
            MealPlanGenerateRequest request,
            List<TheMealDbMeal> candidateMeals
    ) {
        String childName = safeString(request == null ? null : request.getChildName(), "Guest");
        Integer age = request == null || request.getAge() == null ? 7 : request.getAge();
        String gender = safeString(request == null ? null : request.getGender(), "boy");
        Double heightCm = request == null || request.getHeightCm() == null ? 120.0 : request.getHeightCm();
        Double weightKg = request == null || request.getWeightKg() == null ? 20.0 : request.getWeightKg();

        Double targetCarbs = request == null || request.getTargetCarbs() == null ? 155.0 : request.getTargetCarbs();
        Double targetProtein = request == null || request.getTargetProtein() == null ? 32.0 : request.getTargetProtein();
        Double targetFat = request == null || request.getTargetFat() == null ? 28.0 : request.getTargetFat();

        int requestedDays = getRequestedDays(request);

        String allergies = request == null || request.getAllergies() == null
                ? "[]"
                : request.getAllergies().toString();

        String restrictions = request == null || request.getRestrictions() == null
                ? "{}"
                : request.getRestrictions().toString();

        String mealPreference = request == null ? "" : safeString(request.getMealPreference(), "");

        StringBuilder candidateBuilder = new StringBuilder();
        for (int i = 0; i < candidateMeals.size(); i++) {
            TheMealDbMeal meal = candidateMeals.get(i);
            if (meal == null || meal.getStrMeal() == null || meal.getStrMeal().trim().length() == 0) {
                continue;
            }

            if (candidateBuilder.length() > 0) {
                candidateBuilder.append(" | ");
            }

            candidateBuilder.append(meal.getStrMeal().trim());

            if (meal.getStrCategory() != null && meal.getStrCategory().trim().length() > 0) {
                candidateBuilder.append(" [").append(meal.getStrCategory().trim()).append("]");
            }
        }

        StringBuilder prompt = new StringBuilder();

        prompt.append("You are selecting meal names for a child meal plan. ");
        prompt.append("Return exactly one valid minified JSON object only. ");
        prompt.append("Do not return explanations, markdown, or any text outside JSON. ");
        prompt.append("You must choose meal names ONLY from the Candidate meals list. ");
        prompt.append("Copy each chosen strMeal exactly as written. Do not rename or paraphrase it.\n");

        prompt.append("Child profile: ");
        prompt.append("name=").append(childName).append("; ");
        prompt.append("age=").append(age).append("; ");
        prompt.append("gender=").append(gender).append("; ");
        prompt.append("heightCm=").append(heightCm).append("; ");
        prompt.append("weightKg=").append(weightKg).append("; ");
        prompt.append("allergies=").append(allergies).append("; ");
        prompt.append("restrictions=").append(restrictions).append("; ");
        prompt.append("mealPreference=").append(mealPreference).append(".\n");

        prompt.append("Daily nutrition targets used only to guide sensible meal selection: ");
        prompt.append("carbs=").append(targetCarbs).append("g; ");
        prompt.append("protein=").append(targetProtein).append("g; ");
        prompt.append("fat=").append(targetFat).append("g. ");
        prompt.append("The backend will later adjust portions proportionally per whole meal, so you only select suitable meal names.\n");

        prompt.append("Task: choose meals for ").append(requestedDays).append(" day(s). ");
        prompt.append("Each day must contain breakfast, lunch, dinner, and snack. ");
        prompt.append("Prefer variety across days and avoid repeating the same meal too often. ");
        prompt.append("Meal-size guidance: lunch and dinner should be the two nutritionally larger meals and should be reasonably balanced with each other; ");
        prompt.append("breakfast should be moderate, smaller than lunch and dinner, but clearly larger than snack; ");
        prompt.append("snack should be light. ");
        prompt.append("Use the meal's category as a clue, but select only from the candidate list.\n");

        prompt.append("Candidate meals: ").append(candidateBuilder).append("\n");

        if (requestedDays <= 1) {
            prompt.append("Output format exactly: ");
            prompt.append("{\"plan\":{\"breakfast\":\"Exact strMeal\",\"lunch\":\"Exact strMeal\",\"dinner\":\"Exact strMeal\",\"snack\":\"Exact strMeal\"}}");
        } else {
            prompt.append("Output format exactly: ");
            prompt.append("{\"plans\":[{\"day\":1,\"plan\":{\"breakfast\":\"Exact strMeal\",\"lunch\":\"Exact strMeal\",\"dinner\":\"Exact strMeal\",\"snack\":\"Exact strMeal\"}}]} ");
            prompt.append("The plans array must contain exactly ").append(requestedDays).append(" day objects, day values ascending from 1 to ").append(requestedDays).append(".");
        }

        return prompt.toString();
    }

    private Map<String, Object> parseGroqMealSelectionResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        String text = root
                .path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText();

        if (text == null || text.trim().length() == 0) {
            throw new RuntimeException("Groq returned empty meal-selection text");
        }

        text = cleanJsonText(text);

        if (!looksLikeCompleteJsonObject(text)) {
            throw new RuntimeException("Groq returned an incomplete meal-selection JSON object.");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> selection = objectMapper.readValue(text, Map.class);

        return selection;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildDatabaseMealPlanFromGroqSelection(
            Map<String, Object> selection,
            List<TheMealDbMeal> candidateMeals,
            MealPlanGenerateRequest request
    ) {
        int requestedDays = getRequestedDays(request);
        String[] slots = new String[] {"breakfast", "lunch", "dinner", "snack"};

        if (requestedDays <= 1) {
            Map<String, Object> selectedPlan = extractGroqSelectedPlan(selection, 0);
            Map<String, Object> fullPlan = materializeDatabaseDayPlan(
                    selectedPlan,
                    candidateMeals,
                    slots,
                    0
            );

            Map<String, Object> result = new HashMap<String, Object>();
            result.put("plan", fullPlan);
            return result;
        }

        List<Map<String, Object>> plans = new ArrayList<Map<String, Object>>();

        for (int dayIndex = 0; dayIndex < requestedDays; dayIndex++) {
            Map<String, Object> selectedPlan = extractGroqSelectedPlan(selection, dayIndex);
            Map<String, Object> fullPlan = materializeDatabaseDayPlan(
                    selectedPlan,
                    candidateMeals,
                    slots,
                    dayIndex
            );

            Map<String, Object> dayWrapper = new HashMap<String, Object>();
            dayWrapper.put("day", dayIndex + 1);
            dayWrapper.put("plan", fullPlan);
            plans.add(dayWrapper);
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("plans", plans);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractGroqSelectedPlan(Map<String, Object> selection, int dayIndex) {
        if (selection == null) {
            return new HashMap<String, Object>();
        }

        Object singlePlanObj = selection.get("plan");
        if (dayIndex == 0 && singlePlanObj instanceof Map) {
            return (Map<String, Object>) singlePlanObj;
        }

        Object plansObj = selection.get("plans");
        if (!(plansObj instanceof List)) {
            return new HashMap<String, Object>();
        }

        List<Object> plans = (List<Object>) plansObj;
        if (dayIndex < 0 || dayIndex >= plans.size()) {
            return new HashMap<String, Object>();
        }

        Object dayObj = plans.get(dayIndex);
        if (!(dayObj instanceof Map)) {
            return new HashMap<String, Object>();
        }

        Map<String, Object> dayMap = (Map<String, Object>) dayObj;
        Object planObj = dayMap.get("plan");

        if (planObj instanceof Map) {
            return (Map<String, Object>) planObj;
        }

        return new HashMap<String, Object>();
    }

    private Map<String, Object> materializeDatabaseDayPlan(
            Map<String, Object> selectedPlan,
            List<TheMealDbMeal> candidateMeals,
            String[] slots,
            int dayIndex
    ) {
        Map<String, Object> fullPlan = new LinkedHashMap<String, Object>();

        for (int slotIndex = 0; slotIndex < slots.length; slotIndex++) {
            String slot = slots[slotIndex];
            String selectedMealName = selectedPlan == null
                    ? ""
                    : stringValue(selectedPlan.get(slot), "");

            MealNutritionDto mealDto = findDatabaseMealOrFallbackCandidate(
                    selectedMealName,
                    candidateMeals,
                    dayIndex,
                    slotIndex
            );

            if (mealDto == null) {
                throw new RuntimeException("Unable to load database meal for slot: " + slot);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> mealMap = objectMapper.convertValue(mealDto, Map.class);
            fullPlan.put(slot, mealMap);
        }

        return fullPlan;
    }

    private MealNutritionDto findDatabaseMealOrFallbackCandidate(
            String selectedMealName,
            List<TheMealDbMeal> candidateMeals,
            int dayIndex,
            int slotIndex
    ) {
        MealNutritionDto exact = theMealService.findMealNutritionByExactStrMeal(selectedMealName);
        if (exact != null) {
            return exact;
        }

        if (candidateMeals == null || candidateMeals.isEmpty()) {
            return null;
        }

        int candidateIndex = Math.abs(dayIndex * 4 + slotIndex) % candidateMeals.size();
        TheMealDbMeal fallbackCandidate = candidateMeals.get(candidateIndex);

        if (fallbackCandidate == null || fallbackCandidate.getStrMeal() == null) {
            return null;
        }

        return theMealService.findMealNutritionByExactStrMeal(fallbackCandidate.getStrMeal());
    }

    /**
     * Groq + DB meal plan 专用营养调整逻辑。
     *
     * 新逻辑：
     * 1. 不再按早餐/午餐/晚餐/加餐整体缩放。
     * 2. 把当天所有食材按“碳水主导 / 蛋白主导 / 脂肪主导”分为 3 组。
     * 3. 同一组里的所有食材使用同一个缩放比例，避免只改一种食材。
     * 4. 碳水组、蛋白组、脂肪组可使用不同的缩放比例。
     * 5. 通过 3×3 线性方程，直接求出让全天 carbs / protein / fat 精确命中目标的 3 个系数。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> enforceMealPlanTargetLimitsByMacroGroupScaling(
            Map<String, Object> result,
            MealPlanGenerateRequest request
    ) {
        if (result == null) {
            return result;
        }

        List<Map<String, Object>> plans = collectPlanMaps(result);

        for (int dayIndex = 0; dayIndex < plans.size(); dayIndex++) {
            enforceSingleDayMacroGroupScaling(plans.get(dayIndex), request, dayIndex + 1);
        }

        return result;
    }

    private void enforceSingleDayMacroGroupScaling(
            Map<String, Object> plan,
            MealPlanGenerateRequest request,
            int dayNumber
    ) {
        if (plan == null) {
            return;
        }

        double targetCarbs = normalizeTarget(request == null ? null : request.getTargetCarbs(), 155.0);
        double targetProtein = normalizeTarget(request == null ? null : request.getTargetProtein(), 32.0);
        double targetFat = normalizeTarget(request == null ? null : request.getTargetFat(), 28.0);

        String[] mealKeys = new String[] {"breakfast", "lunch", "dinner", "snack"};
        double[] before = recalculatePlanAndGetTotals(plan, mealKeys);
        logMealNutritionDistribution(
                "AI DB meal plan day " + dayNumber + " selected meals BEFORE scaling",
                plan,
                mealKeys,
                new double[] {targetCarbs, targetProtein, targetFat}
        );

        /*
         * matrix[macroRow][groupColumn]
         * macroRow: 0=carbs, 1=protein, 2=fat
         * groupColumn: 0=carb-dominant ingredients, 1=protein-dominant ingredients, 2=fat-dominant ingredients
         */
        double[][] groupMacroMatrix = collectMacroGroupMatrix(plan, mealKeys);

        double[] targets = new double[] {targetCarbs, targetProtein, targetFat};
        double[] factors = solveThreeByThree(groupMacroMatrix, targets);

        boolean exactMacroSolutionFound =
                isUsableMacroGroupFactorSolution(groupMacroMatrix, factors, targets);

        double[] currentMealShares = exactMacroSolutionFound
                ? calculateProjectedMealNutritionShares(plan, mealKeys, factors, targets)
                : null;

        boolean mealDistributionBalanced =
                exactMacroSolutionFound
                        && isMealDistributionBalanced(currentMealShares);

        if (!exactMacroSolutionFound || !mealDistributionBalanced) {
            if (!exactMacroSolutionFound) {
                log.warn(
                        "AI DB meal plan day {} cannot find a positive exact macro-group scaling solution on first attempt. " +
                                "Before:{}/{}/{} Target:{}/{}/{}. GroupMatrix carbs=[{},{},{}], protein=[{},{},{}], fat=[{},{},{}]. " +
                                "Trying same-category meal replacements with lunch/dinner balance preference.",
                        dayNumber,
                        roundOne(before[0]),
                        roundOne(before[1]),
                        roundOne(before[2]),
                        targetCarbs,
                        targetProtein,
                        targetFat,
                        roundFour(groupMacroMatrix[0][0]),
                        roundFour(groupMacroMatrix[0][1]),
                        roundFour(groupMacroMatrix[0][2]),
                        roundFour(groupMacroMatrix[1][0]),
                        roundFour(groupMacroMatrix[1][1]),
                        roundFour(groupMacroMatrix[1][2]),
                        roundFour(groupMacroMatrix[2][0]),
                        roundFour(groupMacroMatrix[2][1]),
                        roundFour(groupMacroMatrix[2][2])
                );
            } else {
                log.info(
                        "AI DB meal plan day {} has exact daily macros but meal distribution is not ideal. " +
                                "Projected breakfast/lunch/dinner/snack shares={}/{}/{}/{}. " +
                                "Trying same-category replacements for a better daily distribution.",
                        dayNumber,
                        roundFour(currentMealShares[0]),
                        roundFour(currentMealShares[1]),
                        roundFour(currentMealShares[2]),
                        roundFour(currentMealShares[3])
                );
            }

            /*
             * First preference:
             * - exact daily macros
             * - balanced meal distribution:
             *   lunch and dinner larger and close to each other,
             *   breakfast moderate and > snack,
             *   snack light.
             */
            MacroGroupReplacementSolution replacementSolution =
                    findSameCategoryReplacementSolution(
                            plan,
                            mealKeys,
                            request,
                            targets,
                            dayNumber,
                            true
                    );

            /*
             * If the original plan has no exact macro solution at all,
             * allow a second pass that only requires exact macros.
             * This prevents returning an unadjusted plan when a nutritionally exact
             * same-category replacement exists but the ideal meal distribution is impossible.
             */
            if (replacementSolution == null && !exactMacroSolutionFound) {
                replacementSolution =
                        findSameCategoryReplacementSolution(
                                plan,
                                mealKeys,
                                request,
                                targets,
                                dayNumber,
                                false
                        );
            }

            if (replacementSolution != null) {
                replacePlanMealsWithSolution(plan, mealKeys, replacementSolution);
                groupMacroMatrix = collectMacroGroupMatrix(plan, mealKeys);
                factors = replacementSolution.factors;

                log.info(
                        "AI DB meal plan day {} found same-category replacement solution. " +
                                "Replacements:{}, BalancedDistribution:{}, Shares breakfast/lunch/dinner/snack={}/{}/{}/{}, " +
                                "Factors carb/protein/fat={}/{}/{}.",
                        dayNumber,
                        replacementSolution.replacementCount,
                        replacementSolution.balancedDistribution,
                        roundFour(replacementSolution.mealShares[0]),
                        roundFour(replacementSolution.mealShares[1]),
                        roundFour(replacementSolution.mealShares[2]),
                        roundFour(replacementSolution.mealShares[3]),
                        roundFour(factors[0]),
                        roundFour(factors[1]),
                        roundFour(factors[2])
                );
            } else if (!exactMacroSolutionFound) {
                log.warn(
                        "AI DB meal plan day {} still cannot find an exact macro-group solution after same-category replacements. " +
                                "Keeping the original selected meals unchanged.",
                        dayNumber
                );
                return;
            } else {
                log.warn(
                        "AI DB meal plan day {} keeps the exact-macro original meals because no same-category replacement " +
                                "could improve the lunch/dinner-heavy, breakfast-moderate, snack-light distribution.",
                        dayNumber
                );
            }
        }

        logMealNutritionDistribution(
                "AI DB meal plan day " + dayNumber + " meals BEFORE macro-group scaling",
                plan,
                mealKeys,
                new double[] {targetCarbs, targetProtein, targetFat}
        );

        scaleIngredientsByMacroGroups(plan, mealKeys, factors);

        double[] after = recalculatePlanAndGetTotals(plan, mealKeys);
        double[] finalMealShares =
                calculateProjectedMealNutritionShares(
                        plan,
                        mealKeys,
                        new double[] {1.0, 1.0, 1.0},
                        targets
                );

        logMealNutritionDistribution(
                "AI DB meal plan day " + dayNumber + " FINAL meals AFTER macro-group scaling",
                plan,
                mealKeys,
                targets
        );

        log.info(
                "AI DB meal plan day {} macro-group factors carb/protein/fat={}/{}/{}. " +
                        "Before:{}/{}/{} Target:{}/{}/{} After:{}/{}/{} Error:{}/{}/{}. " +
                        "Final shares breakfast/lunch/dinner/snack={}/{}/{}/{}, BalancedDistribution:{}",
                dayNumber,
                roundFour(factors[0]),
                roundFour(factors[1]),
                roundFour(factors[2]),
                roundOne(before[0]),
                roundOne(before[1]),
                roundOne(before[2]),
                targetCarbs,
                targetProtein,
                targetFat,
                roundOne(after[0]),
                roundOne(after[1]),
                roundOne(after[2]),
                roundFour(after[0] - targetCarbs),
                roundFour(after[1] - targetProtein),
                roundFour(after[2] - targetFat),
                roundFour(finalMealShares[0]),
                roundFour(finalMealShares[1]),
                roundFour(finalMealShares[2]),
                roundFour(finalMealShares[3]),
                isMealDistributionBalanced(finalMealShares)
        );
    }

    @SuppressWarnings("unchecked")
    private double[][] collectMacroGroupMatrix(Map<String, Object> plan, String[] mealKeys) {
        double[][] matrix = new double[3][3];

        for (String mealKey : mealKeys) {
            Object mealObj = plan.get(mealKey);

            if (!(mealObj instanceof Map)) {
                continue;
            }

            Map<String, Object> meal = (Map<String, Object>) mealObj;
            Object ingredientsObj = meal.get("ingredients");

            if (!(ingredientsObj instanceof List)) {
                continue;
            }

            List<Object> ingredients = (List<Object>) ingredientsObj;

            for (Object ingredientObj : ingredients) {
                if (!(ingredientObj instanceof Map)) {
                    continue;
                }

                Map<String, Object> ingredient = (Map<String, Object>) ingredientObj;

                double carbs = numberValue(ingredient.get("carbohydrateG"));
                double protein = numberValue(ingredient.get("proteinG"));
                double fat = numberValue(ingredient.get("fatG"));

                if (carbs <= 0.0 && protein <= 0.0 && fat <= 0.0) {
                    continue;
                }

                int group = classifyIngredientMacroGroup(ingredient, carbs, protein, fat);

                matrix[0][group] += carbs;
                matrix[1][group] += protein;
                matrix[2][group] += fat;
            }
        }

        return matrix;
    }

    /**
     * 宏量营养分组：
     * 0 = 碳水组
     * 1 = 蛋白组
     * 2 = 脂肪组
     *
     * 优先看明显的营养主导；
     * 如果是混合型食材，再用 foodGroup / 食材名兜底；
     * 最后按数值最大项分类。
     */
    private int classifyIngredientMacroGroup(
            Map<String, Object> ingredient,
            double carbs,
            double protein,
            double fat
    ) {
        double max = Math.max(carbs, Math.max(protein, fat));

        if (max > 0.0) {
            if (carbs >= protein * 1.15 && carbs >= fat * 1.15) {
                return 0;
            }

            if (protein >= carbs * 1.15 && protein >= fat * 1.15) {
                return 1;
            }

            if (fat >= carbs * 1.15 && fat >= protein * 1.15) {
                return 2;
            }
        }

        String text = (
                stringValue(ingredient.get("foodGroup"), "") + " "
                        + stringValue(ingredient.get("foodNameEn"), "") + " "
                        + stringValue(ingredient.get("ingredientName"), "")
        ).toLowerCase();

        if (containsAny(
                text,
                "oil", "fat", "butter", "cream", "coconut milk", "mayonnaise",
                "peanut butter", "nut", "almond", "cashew", "walnut", "avocado"
        )) {
            return 2;
        }

        if (containsAny(
                text,
                "protein", "meat", "chicken", "beef", "pork", "lamb", "fish",
                "salmon", "tuna", "seafood", "egg", "tofu", "tempeh", "bean",
                "lentil", "prawn", "shrimp", "yogurt", "milk"
        )) {
            return 1;
        }

        if (containsAny(
                text,
                "carb", "grain", "rice", "bread", "noodle", "pasta", "spaghetti",
                "oat", "flour", "cereal", "potato", "sweet potato", "corn",
                "banana", "apple", "fruit", "sugar", "syrup"
        )) {
            return 0;
        }

        if (fat >= protein && fat >= carbs) {
            return 2;
        }

        if (protein >= carbs && protein >= fat) {
            return 1;
        }

        return 0;
    }

    private boolean isUsableMacroGroupFactorSolution(
            double[][] matrix,
            double[] factors,
            double[] targets
    ) {
        if (matrix == null || matrix.length != 3 || factors == null || factors.length != 3) {
            return false;
        }

        /*
         * 允许较宽范围，避免过早判定失败。
         * 因为数据库食谱已有 portionFactor，后续这里更像是“按营养组再校正一次”。
         */
        final double minFactor = 0.01;
        final double maxFactor = 20.0;

        for (double factor : factors) {
            if (Double.isNaN(factor)
                    || Double.isInfinite(factor)
                    || factor < minFactor
                    || factor > maxFactor) {
                return false;
            }
        }

        double[] projected = multiplyMacroGroupsByFactors(matrix, factors);

        return withinExactMacroTolerance(projected, targets, 0.05);
    }

    private double[] multiplyMacroGroupsByFactors(double[][] matrix, double[] factors) {
        double[] totals = new double[] {0.0, 0.0, 0.0};

        for (int macroIndex = 0; macroIndex < 3; macroIndex++) {
            totals[macroIndex] =
                    matrix[macroIndex][0] * factors[0]
                            + matrix[macroIndex][1] * factors[1]
                            + matrix[macroIndex][2] * factors[2];
        }

        return totals;
    }

    @SuppressWarnings("unchecked")
    private void scaleIngredientsByMacroGroups(
            Map<String, Object> plan,
            String[] mealKeys,
            double[] factors
    ) {
        for (String mealKey : mealKeys) {
            Object mealObj = plan.get(mealKey);

            if (!(mealObj instanceof Map)) {
                continue;
            }

            Map<String, Object> meal = (Map<String, Object>) mealObj;
            Object ingredientsObj = meal.get("ingredients");

            if (!(ingredientsObj instanceof List)) {
                continue;
            }

            List<Object> ingredients = (List<Object>) ingredientsObj;

            for (Object ingredientObj : ingredients) {
                if (!(ingredientObj instanceof Map)) {
                    continue;
                }

                Map<String, Object> ingredient = (Map<String, Object>) ingredientObj;

                double carbs = numberValue(ingredient.get("carbohydrateG"));
                double protein = numberValue(ingredient.get("proteinG"));
                double fat = numberValue(ingredient.get("fatG"));

                if (carbs <= 0.0 && protein <= 0.0 && fat <= 0.0) {
                    continue;
                }

                int group = classifyIngredientMacroGroup(ingredient, carbs, protein, fat);
                double factor = factors[group];

                scaleSingleIngredientNutrition(ingredient, factor);
            }

            recalculateMealTotalsFromIngredients(meal);
        }
    }

    private void scaleSingleIngredientNutrition(Map<String, Object> ingredient, double factor) {
        double grams = numberValue(ingredient.get("gramsEstimated"));
        double energy = numberValue(ingredient.get("energyKcal"));
        double protein = numberValue(ingredient.get("proteinG"));
        double carbs = numberValue(ingredient.get("carbohydrateG"));
        double fat = numberValue(ingredient.get("fatG"));

        double nextGrams = roundTwo(grams * factor);

        ingredient.put("gramsEstimated", nextGrams);
        ingredient.put("measure", formatScaledGramMeasure(nextGrams));
        ingredient.put("energyKcal", roundFour(energy * factor));
        ingredient.put("proteinG", roundFour(protein * factor));
        ingredient.put("carbohydrateG", roundFour(carbs * factor));
        ingredient.put("fatG", roundFour(fat * factor));
    }

    /**
     * 当当前 4 餐的宏量营养组合无法被 carb/protein/fat 三组缩放精确求解时：
     * 1. 每个 slot 只找相同 strCategory 的数据库食谱；
     * 2. 组合尝试“原食谱 + 同类别替代食谱”；
     * 3. 优先选择替换数量更少、缩放系数更接近 1 的解。
     */
    @SuppressWarnings("unchecked")
    private MacroGroupReplacementSolution findSameCategoryReplacementSolution(
            Map<String, Object> originalPlan,
            String[] mealKeys,
            MealPlanGenerateRequest request,
            double[] targets,
            int dayNumber,
            boolean requireBalancedDistribution
    ) {
        if (originalPlan == null || mealKeys == null || mealKeys.length != 4) {
            return null;
        }

        List<String> dayMealNames = collectCurrentPlanMealNames(originalPlan, mealKeys);
        List<List<Map<String, Object>>> slotOptions = new ArrayList<List<Map<String, Object>>>();

        final int alternativesPerSlot = 5;

        for (String mealKey : mealKeys) {
            Object mealObj = originalPlan.get(mealKey);

            if (!(mealObj instanceof Map)) {
                return null;
            }

            Map<String, Object> currentMeal = deepCopyMealMap((Map<String, Object>) mealObj);
            List<Map<String, Object>> options = new ArrayList<Map<String, Object>>();
            options.add(currentMeal);

            String category = stringValue(currentMeal.get("strCategory"), "");
            List<String> excludedNames = new ArrayList<String>(dayMealNames);

            List<MealNutritionDto> alternatives =
                    theMealService.findSameCategoryAlternativeMealsForAi(
                            category,
                            request,
                            excludedNames,
                            alternativesPerSlot
                    );

            if (alternatives != null) {
                for (MealNutritionDto alternative : alternatives) {
                    if (alternative == null) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> alternativeMap =
                            objectMapper.convertValue(alternative, Map.class);

                    String alternativeName = stringValue(alternativeMap.get("strMeal"), "");
                    if (alternativeName.trim().length() == 0) {
                        continue;
                    }

                    options.add(alternativeMap);
                    excludedNames.add(alternativeName);
                }
            }

            slotOptions.add(options);
        }

        MacroGroupReplacementSolution[] bestHolder =
                new MacroGroupReplacementSolution[] {null};

        List<Map<String, Object>> selected = new ArrayList<Map<String, Object>>();
        searchSameCategoryReplacementCombinations(
                slotOptions,
                mealKeys,
                targets,
                0,
                selected,
                0,
                bestHolder,
                requireBalancedDistribution
        );

        MacroGroupReplacementSolution best = bestHolder[0];

        if (best == null) {
            log.warn(
                    "AI DB meal plan day {} replacement search did not find a valid same-category combination. " +
                            "RequireBalancedDistribution:{}, Options per slot={}/{}/{}/{}.",
                    dayNumber,
                    requireBalancedDistribution,
                    slotOptions.get(0).size(),
                    slotOptions.get(1).size(),
                    slotOptions.get(2).size(),
                    slotOptions.get(3).size()
            );
        }

        return best;
    }

    private void searchSameCategoryReplacementCombinations(
            List<List<Map<String, Object>>> slotOptions,
            String[] mealKeys,
            double[] targets,
            int depth,
            List<Map<String, Object>> selected,
            int replacementCount,
            MacroGroupReplacementSolution[] bestHolder,
            boolean requireBalancedDistribution
    ) {
        if (slotOptions == null || mealKeys == null || targets == null) {
            return;
        }

        if (depth >= mealKeys.length) {
            Map<String, Object> candidatePlan = new HashMap<String, Object>();

            for (int i = 0; i < mealKeys.length; i++) {
                candidatePlan.put(mealKeys[i], deepCopyMealMap(selected.get(i)));
            }

            double[][] matrix = collectMacroGroupMatrix(candidatePlan, mealKeys);
            double[] factors = solveThreeByThree(matrix, targets);

            if (!isUsableMacroGroupFactorSolution(matrix, factors, targets)) {
                return;
            }

            double[] mealShares =
                    calculateProjectedMealNutritionShares(candidatePlan, mealKeys, factors, targets);

            boolean balancedDistribution = isMealDistributionBalanced(mealShares);

            if (requireBalancedDistribution && !balancedDistribution) {
                return;
            }

            double distributionPenalty = mealDistributionPenalty(mealShares);
            double score =
                    replacementCount * 1000.0
                            + distributionPenalty * 100.0
                            + macroGroupFactorDistanceFromOne(factors);

            if (bestHolder[0] == null || score < bestHolder[0].score) {
                bestHolder[0] = new MacroGroupReplacementSolution(
                        cloneMealSelection(selected),
                        factors,
                        replacementCount,
                        score,
                        mealShares,
                        balancedDistribution
                );
            }

            return;
        }

        List<Map<String, Object>> options = slotOptions.get(depth);

        if (options == null || options.isEmpty()) {
            return;
        }

        for (int optionIndex = 0; optionIndex < options.size(); optionIndex++) {
            /*
             * 选项 0 一定是当前原食谱；后续才是同类别替代食谱。
             */
            int nextReplacementCount = replacementCount + (optionIndex == 0 ? 0 : 1);

            if (bestHolder[0] != null && nextReplacementCount > bestHolder[0].replacementCount) {
                continue;
            }

            selected.add(options.get(optionIndex));

            searchSameCategoryReplacementCombinations(
                    slotOptions,
                    mealKeys,
                    targets,
                    depth + 1,
                    selected,
                    nextReplacementCount,
                    bestHolder,
                    requireBalancedDistribution
            );

            selected.remove(selected.size() - 1);
        }
    }

    private List<Map<String, Object>> cloneMealSelection(List<Map<String, Object>> meals) {
        List<Map<String, Object>> clones = new ArrayList<Map<String, Object>>();

        if (meals == null) {
            return clones;
        }

        for (Map<String, Object> meal : meals) {
            clones.add(deepCopyMealMap(meal));
        }

        return clones;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopyMealMap(Map<String, Object> meal) {
        if (meal == null) {
            return new HashMap<String, Object>();
        }

        return objectMapper.convertValue(meal, Map.class);
    }

    private List<String> collectCurrentPlanMealNames(Map<String, Object> plan, String[] mealKeys) {
        List<String> names = new ArrayList<String>();

        if (plan == null || mealKeys == null) {
            return names;
        }

        for (String mealKey : mealKeys) {
            Object mealObj = plan.get(mealKey);

            if (!(mealObj instanceof Map)) {
                continue;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> meal = (Map<String, Object>) mealObj;
            String mealName = stringValue(meal.get("strMeal"), "");

            if (mealName.trim().length() > 0) {
                names.add(mealName.trim());
            }
        }

        return names;
    }

    private void replacePlanMealsWithSolution(
            Map<String, Object> plan,
            String[] mealKeys,
            MacroGroupReplacementSolution solution
    ) {
        if (plan == null || mealKeys == null || solution == null || solution.meals == null) {
            return;
        }

        for (int i = 0; i < mealKeys.length && i < solution.meals.size(); i++) {
            plan.put(mealKeys[i], deepCopyMealMap(solution.meals.get(i)));
        }
    }

    private double macroGroupFactorDistanceFromOne(double[] factors) {
        if (factors == null) {
            return Double.MAX_VALUE;
        }

        double score = 0.0;

        for (double factor : factors) {
            double diff = factor - 1.0;
            score += diff * diff;
        }

        return score;
    }

    private static class MacroGroupReplacementSolution {
        private final List<Map<String, Object>> meals;
        private final double[] factors;
        private final int replacementCount;
        private final double score;
        private final double[] mealShares;
        private final boolean balancedDistribution;

        private MacroGroupReplacementSolution(
                List<Map<String, Object>> meals,
                double[] factors,
                int replacementCount,
                double score,
                double[] mealShares,
                boolean balancedDistribution
        ) {
            this.meals = meals;
            this.factors = factors;
            this.replacementCount = replacementCount;
            this.score = score;
            this.mealShares = mealShares;
            this.balancedDistribution = balancedDistribution;
        }
    }

    /**
     * 打印 4 餐的营养分布，方便检查：
     * - 每餐食谱名称
     * - 每餐 kcal / carbs / protein / fat
     * - 每餐在全天目标中的平均营养占比 share
     */
    @SuppressWarnings("unchecked")
    private void logMealNutritionDistribution(
            String title,
            Map<String, Object> plan,
            String[] mealKeys,
            double[] targets
    ) {
        if (plan == null || mealKeys == null || targets == null || targets.length < 3) {
            return;
        }

        double safeTargetCarbs = Math.max(targets[0], 1.0);
        double safeTargetProtein = Math.max(targets[1], 1.0);
        double safeTargetFat = Math.max(targets[2], 1.0);

        StringBuilder builder = new StringBuilder();
        builder.append(title).append(" ===== ");

        double totalKcal = 0.0;
        double totalCarbs = 0.0;
        double totalProtein = 0.0;
        double totalFat = 0.0;

        for (int i = 0; i < mealKeys.length; i++) {
            String mealKey = mealKeys[i];
            Object mealObj = plan.get(mealKey);

            if (!(mealObj instanceof Map)) {
                builder.append(mealKey).append("={missing}");
                if (i < mealKeys.length - 1) {
                    builder.append(" | ");
                }
                continue;
            }

            Map<String, Object> meal = (Map<String, Object>) mealObj;
            recalculateMealTotalsFromIngredients(meal);

            String mealName = stringValue(meal.get("strMeal"), "Unknown Meal");
            double kcal = numberValue(meal.get("totalEnergyKcal"));
            double carbs = numberValue(meal.get("totalCarbohydrateG"));
            double protein = numberValue(meal.get("totalProteinG"));
            double fat = numberValue(meal.get("totalFatG"));

            double share = (
                    carbs / safeTargetCarbs
                            + protein / safeTargetProtein
                            + fat / safeTargetFat
            ) / 3.0;

            totalKcal += kcal;
            totalCarbs += carbs;
            totalProtein += protein;
            totalFat += fat;

            builder.append(mealKey)
                    .append("={meal:")
                    .append(mealName)
                    .append(", kcal:")
                    .append(roundOne(kcal))
                    .append(", C:")
                    .append(roundOne(carbs))
                    .append(", P:")
                    .append(roundOne(protein))
                    .append(", F:")
                    .append(roundOne(fat))
                    .append(", share:")
                    .append(roundFour(share))
                    .append("}");

            if (i < mealKeys.length - 1) {
                builder.append(" | ");
            }
        }

        builder.append(" || total={kcal:")
                .append(roundOne(totalKcal))
                .append(", C:")
                .append(roundOne(totalCarbs))
                .append(", P:")
                .append(roundOne(totalProtein))
                .append(", F:")
                .append(roundOne(totalFat))
                .append("}");

        log.info(builder.toString());
    }


    /**
     * 计算在 carb/protein/fat 三组缩放因子应用之后，每个餐次在全天营养中的占比。
     *
     * 占比不是单纯用热量，而是取该餐：
     * - 碳水占全天目标的比例
     * - 蛋白质占全天目标的比例
     * - 脂肪占全天目标的比例
     * 三者的平均值。
     *
     * 因为每日总 carbs/protein/fat 已被精确求解到目标，所以 4 餐 share 之和约等于 1。
     */
    @SuppressWarnings("unchecked")
    private double[] calculateProjectedMealNutritionShares(
            Map<String, Object> plan,
            String[] mealKeys,
            double[] factors,
            double[] targets
    ) {
        double[] shares = new double[] {0.0, 0.0, 0.0, 0.0};

        if (plan == null
                || mealKeys == null
                || mealKeys.length != 4
                || factors == null
                || factors.length != 3
                || targets == null
                || targets.length != 3) {
            return shares;
        }

        double safeTargetCarbs = Math.max(targets[0], 1.0);
        double safeTargetProtein = Math.max(targets[1], 1.0);
        double safeTargetFat = Math.max(targets[2], 1.0);

        for (int mealIndex = 0; mealIndex < mealKeys.length; mealIndex++) {
            Object mealObj = plan.get(mealKeys[mealIndex]);

            if (!(mealObj instanceof Map)) {
                continue;
            }

            Map<String, Object> meal = (Map<String, Object>) mealObj;
            Object ingredientsObj = meal.get("ingredients");

            if (!(ingredientsObj instanceof List)) {
                continue;
            }

            double projectedCarbs = 0.0;
            double projectedProtein = 0.0;
            double projectedFat = 0.0;

            List<Object> ingredients = (List<Object>) ingredientsObj;

            for (Object ingredientObj : ingredients) {
                if (!(ingredientObj instanceof Map)) {
                    continue;
                }

                Map<String, Object> ingredient = (Map<String, Object>) ingredientObj;

                double carbs = numberValue(ingredient.get("carbohydrateG"));
                double protein = numberValue(ingredient.get("proteinG"));
                double fat = numberValue(ingredient.get("fatG"));

                if (carbs <= 0.0 && protein <= 0.0 && fat <= 0.0) {
                    continue;
                }

                int group = classifyIngredientMacroGroup(ingredient, carbs, protein, fat);
                double factor = factors[group];

                projectedCarbs += carbs * factor;
                projectedProtein += protein * factor;
                projectedFat += fat * factor;
            }

            shares[mealIndex] =
                    (
                            projectedCarbs / safeTargetCarbs
                                    + projectedProtein / safeTargetProtein
                                    + projectedFat / safeTargetFat
                    ) / 3.0;
        }

        return shares;
    }

    /**
     * 目标分配：
     * breakfast ≈ 22%
     * lunch     ≈ 33%
     * dinner    ≈ 33%
     * snack     ≈ 12%
     *
     * 允许一定弹性，但必须体现：
     * - lunch / dinner 是较多的两餐
     * - lunch 与 dinner 不应差距过大
     * - breakfast > snack
     * - snack 是最轻的一餐
     */
    private boolean isMealDistributionBalanced(double[] shares) {
        if (shares == null || shares.length < 4) {
            return false;
        }

        double breakfast = shares[0];
        double lunch = shares[1];
        double dinner = shares[2];
        double snack = shares[3];

        boolean lunchAndDinnerLarger =
                lunch >= 0.26
                        && dinner >= 0.26
                        && lunch > breakfast
                        && dinner > breakfast
                        && lunch > snack
                        && dinner > snack;

        boolean lunchDinnerReasonablyBalanced =
                Math.abs(lunch - dinner) <= 0.12;

        boolean breakfastModerate =
                breakfast >= 0.14
                        && breakfast <= 0.30
                        && breakfast > snack;

        boolean snackLight =
                snack >= 0.03
                        && snack <= 0.18;

        return lunchAndDinnerLarger
                && lunchDinnerReasonablyBalanced
                && breakfastModerate
                && snackLight;
    }

    private double mealDistributionPenalty(double[] shares) {
        if (shares == null || shares.length < 4) {
            return Double.MAX_VALUE / 4.0;
        }

        double breakfast = shares[0];
        double lunch = shares[1];
        double dinner = shares[2];
        double snack = shares[3];

        double[] desired = new double[] {0.22, 0.33, 0.33, 0.12};

        double penalty =
                square(breakfast - desired[0])
                        + square(lunch - desired[1])
                        + square(dinner - desired[2])
                        + square(snack - desired[3]);

        // Hard-direction penalties so the ranking strongly prefers the wanted shape.
        if (lunch <= breakfast) {
            penalty += square((breakfast - lunch) + 0.08) * 20.0;
        }

        if (dinner <= breakfast) {
            penalty += square((breakfast - dinner) + 0.08) * 20.0;
        }

        if (breakfast <= snack) {
            penalty += square((snack - breakfast) + 0.06) * 20.0;
        }

        if (snack > 0.18) {
            penalty += square(snack - 0.18) * 20.0;
        }

        if (lunch < 0.26) {
            penalty += square(0.26 - lunch) * 20.0;
        }

        if (dinner < 0.26) {
            penalty += square(0.26 - dinner) * 20.0;
        }

        if (Math.abs(lunch - dinner) > 0.12) {
            penalty += square(Math.abs(lunch - dinner) - 0.12) * 20.0;
        }

        return penalty;
    }

    private double square(double value) {
        return value * value;
    }

    private double[] solveThreeByThree(double[][] matrix, double[] vector) {
        if (matrix == null || vector == null || matrix.length != 3 || vector.length != 3) {
            return null;
        }

        double[][] a = new double[3][4];

        for (int row = 0; row < 3; row++) {
            if (matrix[row] == null || matrix[row].length != 3) {
                return null;
            }

            a[row][0] = matrix[row][0];
            a[row][1] = matrix[row][1];
            a[row][2] = matrix[row][2];
            a[row][3] = vector[row];
        }

        for (int col = 0; col < 3; col++) {
            int pivot = col;

            for (int row = col + 1; row < 3; row++) {
                if (Math.abs(a[row][col]) > Math.abs(a[pivot][col])) {
                    pivot = row;
                }
            }

            if (Math.abs(a[pivot][col]) < 1e-9) {
                return null;
            }

            if (pivot != col) {
                double[] temp = a[pivot];
                a[pivot] = a[col];
                a[col] = temp;
            }

            double divisor = a[col][col];

            for (int k = col; k < 4; k++) {
                a[col][k] = a[col][k] / divisor;
            }

            for (int row = 0; row < 3; row++) {
                if (row == col) {
                    continue;
                }

                double eliminationFactor = a[row][col];

                for (int k = col; k < 4; k++) {
                    a[row][k] = a[row][k] - eliminationFactor * a[col][k];
                }
            }
        }

        return new double[] {a[0][3], a[1][3], a[2][3]};
    }

    private boolean withinExactMacroTolerance(double[] actual, double[] targets, double tolerance) {
        if (actual == null || targets == null || actual.length < 3 || targets.length < 3) {
            return false;
        }

        return Math.abs(actual[0] - targets[0]) <= tolerance
                && Math.abs(actual[1] - targets[1]) <= tolerance
                && Math.abs(actual[2] - targets[2]) <= tolerance;
    }

    private String formatScaledGramMeasure(double grams) {
        double rounded = roundTwo(Math.max(grams, 0.0));

        if (Math.abs(rounded - Math.round(rounded)) < 0.0001) {
            return ((long) Math.round(rounded)) + "g";
        }

        return rounded + "g";
    }

    private double roundTwo(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double roundFour(double value) {
        return Math.round(value * 10000.0) / 10000.0;
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

        int requestedDays = getRequestedDays(request);

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
                    "Try to include these foods when safe and suitable: " + mealPreference + ".";
        } else {
            preferenceInstruction =
                    "No specific food preference was provided. Recommend meals based on the child profile, allergies, restrictions, and nutrition targets.";
        }

        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a child nutrition meal planning assistant.\n");
        prompt.append("Return one strictly valid JSON object only. No markdown, no comments, no extra text.\n\n");

        prompt.append("Task:\n");
        prompt.append("Generate a ").append(requestedDays).append("-day meal plan in ONE JSON response.\n");
        prompt.append("Each day must contain exactly four meals: breakfast, lunch, dinner, and snack.\n");
        prompt.append("Use real, common, child-friendly recipes. Prefer Malaysian or family-friendly meals.\n");
        prompt.append("Avoid allergies and dietary restrictions. Use realistic child-sized portions.\n");
        prompt.append("Across different days, make the meals meaningfully varied instead of repeating the same whole-day plan.\n\n");

        prompt.append("Child profile:\n");
        prompt.append("- name: ").append(childName).append("\n");
        prompt.append("- age: ").append(age).append("\n");
        prompt.append("- gender: ").append(gender).append("\n");
        prompt.append("- heightCm: ").append(heightCm).append("\n");
        prompt.append("- weightKg: ").append(weightKg).append("\n");
        prompt.append("- allergies: ").append(allergies).append("\n");
        prompt.append("- restrictions: ").append(restrictions).append("\n");
        prompt.append("- mealPreference: ").append(preferenceInstruction).append("\n\n");

        prompt.append("Daily macro targets, applied independently to EACH day:\n");
        prompt.append("- carbohydrates: ").append(targetCarbs).append("g\n");
        prompt.append("- protein: ").append(targetProtein).append("g\n");
        prompt.append("- fat: ").append(targetFat).append("g\n\n");

//        prompt.append("Macro balancing rules for EACH day:\n");
//        prompt.append("- For every day, total carbohydrates across its four meals must be between ")
//                .append(roundOne(targetCarbs * 0.98))
//                .append("g and ")
//                .append(targetCarbs)
//                .append("g, and never exceed the target.\n");
//        prompt.append("- For every day, total protein across its four meals must be between ")
//                .append(roundOne(targetProtein * 0.98))
//                .append("g and ")
//                .append(targetProtein)
//                .append("g, and never exceed the target.\n");
//        prompt.append("- For every day, total fat across its four meals must be between ")
//                .append(roundOne(targetFat * 0.98))
//                .append("g and ")
//                .append(targetFat)
//                .append("g, and never exceed the target.\n");
//        prompt.append("- Adjust ingredient gramsEstimated and measure to meet the targets for each day.\n");
//        prompt.append("- After adjustment, recalculate ingredient energyKcal, proteinG, carbohydrateG, fatG.\n");
        prompt.append("- Daily totals across all four meals should be close to the daily targets. Do not force every individual meal to equal the full daily target.\n");
        prompt.append("- Each meal totalEnergyKcal, totalProteinG, totalCarbohydrateG, and totalFatG must equal the sum of its ingredients.\n");
        prompt.append("- Do not return unrealistic portions or all-zero nutrition values.\n");
        prompt.append("- Keep the JSON compact. Avoid unnecessarily long prose, long marketing wording, or repeated explanations.\n\n");
        prompt.append("- Check each day's combined macro totals multiple times before returning. Keep the ingredient nutrition values internally consistent.\n\n");

        prompt.append("Multilingual rules:\n");
        prompt.append("- Each meal must include English, Simplified Chinese, and Malay display fields.\n");
        prompt.append("- strMeal and strMealEn are English; strMealCn is Simplified Chinese; strMealMs is Malay.\n");
        prompt.append("- Apply the same pattern to strCategory, strArea, and strInstructions.\n");
        prompt.append("- Keep strInstructionsEn, strInstructionsCn, and strInstructionsMs concise: at most 3 short sentences each.\n");
        prompt.append("- All En, Cn, and Ms display fields must be natural strings suitable for app UI display.\n\n");

        prompt.append("Media and icon rules:\n");
        prompt.append("- strMealThumb: return a real public HTTPS image URL only if confident it exists; otherwise return an empty string.\n");
        prompt.append("- strYoutube may be an empty string if you are not confident. The server will fill a fallback search URL later.\n");
        prompt.append("- Each meal must include mealIconEmoji, mealIconName, and mealIconPrompt.\n");
        prompt.append("- mealIconEmoji should match the recipe. mealIconName and mealIconPrompt may be empty strings when unsure; the server will fill them later.\n\n");

        prompt.append("Required JSON structure:\n");
        if (requestedDays <= 1) {
            prompt.append("{\n");
            prompt.append("  \"plan\": {\n");
            prompt.append("    \"breakfast\": { ...mealObject },\n");
            prompt.append("    \"lunch\": { ...mealObject },\n");
            prompt.append("    \"dinner\": { ...mealObject },\n");
            prompt.append("    \"snack\": { ...mealObject }\n");
            prompt.append("  }\n");
            prompt.append("}\n\n");
        } else {
            prompt.append("{\n");
            prompt.append("  \"plans\": [\n");
            prompt.append("    {\n");
            prompt.append("      \"day\": 1,\n");
            prompt.append("      \"plan\": {\n");
            prompt.append("        \"breakfast\": { ...mealObject },\n");
            prompt.append("        \"lunch\": { ...mealObject },\n");
            prompt.append("        \"dinner\": { ...mealObject },\n");
            prompt.append("        \"snack\": { ...mealObject }\n");
            prompt.append("      }\n");
            prompt.append("    }\n");
            prompt.append("  ]\n");
            prompt.append("}\n\n");
            prompt.append("The plans array must contain exactly ").append(requestedDays).append(" day objects, in ascending day order from 1 to ").append(requestedDays).append(".\n");
            prompt.append("Each day object must contain day and plan. The day value must be an integer.\n\n");
        }

        prompt.append("Each mealObject must contain exactly these fields:\n");
        prompt.append("idMeal, strMeal, strMealEn, strMealCn, strMealMs, ");
        prompt.append("strCategory, strCategoryEn, strCategoryCn, strCategoryMs, ");
        prompt.append("strArea, strAreaEn, strAreaCn, strAreaMs, ");
        prompt.append("strInstructions, strInstructionsEn, strInstructionsCn, strInstructionsMs, ");
        prompt.append("strMealThumb, mealIconEmoji, mealIconName, mealIconPrompt, strYoutube, ");
        prompt.append("totalEnergyKcal, totalProteinG, totalCarbohydrateG, totalFatG, ingredients.\n\n");

        prompt.append("Each ingredient object must contain exactly these fields:\n");
        prompt.append("ingredientName, measure, gramsEstimated, foodNameEn, foodNameCn, foodNameMs, ");
        prompt.append("foodGroup, energyKcal, proteinG, carbohydrateG, fatG.\n\n");

        prompt.append("JSON validity rules:\n");
        prompt.append("- Use double quotes for all keys and string values.\n");
        prompt.append("- Do not use trailing commas.\n");
        prompt.append("- Do not wrap the response in ```json.\n");
        prompt.append("- Return one complete JSON object that can be parsed directly by Jackson ObjectMapper.\n");

        return prompt.toString();
    }

    private Map<String, Object> parseGeminiResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidate = root.path("candidates").path(0);

        String finishReason = candidate.path("finishReason").asText("");
        if ("MAX_TOKENS".equalsIgnoreCase(finishReason)) {
            throw new RuntimeException(
                    "Gemini stopped because MAX_TOKENS was reached. The multi-day JSON was incomplete. " +
                            "The service now requests maxOutputTokens=65535, but the generated plan may still be too large. " +
                            "Reduce requested days or keep recipe instructions shorter."
            );
        }

        String text = candidate
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asText();

        if (text == null || text.trim().length() == 0) {
            throw new RuntimeException("Gemini returned empty text, finishReason=" + finishReason);
        }

        text = cleanJsonText(text);

        if (!looksLikeCompleteJsonObject(text)) {
            throw new RuntimeException(
                    "Gemini returned an incomplete JSON object, finishReason=" + finishReason +
                            ", textLength=" + text.length()
            );
        }

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

    private Map<String, Object> parseGroqResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        String text = root
                .path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText();

        if (text == null || text.trim().length() == 0) {
            throw new RuntimeException("Groq returned empty text");
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
            System.out.println("First Groq JSON parse failed: " + firstError.getMessage());

            String extractedJson = extractFirstJsonObject(text);

            if (extractedJson == null || extractedJson.trim().length() == 0) {
                System.out.println("Invalid Groq JSON text:");
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
                System.out.println("Second Groq JSON parse failed: " + secondError.getMessage());
                System.out.println("Invalid Groq JSON text:");
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

    private boolean looksLikeCompleteJsonObject(String text) {
        if (text == null) {
            return false;
        }

        String value = text.trim();
        if (!value.startsWith("{") || !value.endsWith("}")) {
            return false;
        }

        int braceCount = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

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
                    if (braceCount < 0) {
                        return false;
                    }
                }
            }
        }

        return braceCount == 0 && !inString;
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
    private int getRequestedDays(MealPlanGenerateRequest request) {
        if (request == null) {
            return 1;
        }

        try {
            Object value = request.getClass().getMethod("getDays").invoke(request);
            int days = (int) Math.round(numberValue(value));

            if (days <= 0) {
                return 1;
            }

            return Math.max(1, Math.min(days, 7));
        } catch (Exception ignored) {
            return 1;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> collectPlanMaps(Map<String, Object> result) {
        List<Map<String, Object>> plans = new ArrayList<Map<String, Object>>();

        if (result == null) {
            return plans;
        }

        Object singlePlanObj = result.get("plan");
        if (singlePlanObj instanceof Map) {
            plans.add((Map<String, Object>) singlePlanObj);
        }

        addPlanMapsFromList(result.get("plans"), plans);
        addPlanMapsFromList(result.get("mealPlans"), plans);
        addPlanMapsFromList(result.get("days"), plans);

        return plans;
    }

    @SuppressWarnings("unchecked")
    private void addPlanMapsFromList(Object listObj, List<Map<String, Object>> plans) {
        if (!(listObj instanceof List) || plans == null) {
            return;
        }

        List<Object> items = (List<Object>) listObj;

        for (Object itemObj : items) {
            if (!(itemObj instanceof Map)) {
                continue;
            }

            Map<String, Object> item = (Map<String, Object>) itemObj;
            Object nestedPlanObj = item.get("plan");

            if (!(nestedPlanObj instanceof Map)) {
                nestedPlanObj = item.get("mealPlan");
            }

            if (nestedPlanObj instanceof Map) {
                plans.add((Map<String, Object>) nestedPlanObj);
            } else if (looksLikeDayPlan(item)) {
                plans.add(item);
            }
        }
    }

    private boolean looksLikeDayPlan(Map<String, Object> map) {
        if (map == null) {
            return false;
        }

        return map.containsKey("breakfast")
                || map.containsKey("lunch")
                || map.containsKey("dinner")
                || map.containsKey("snack");
    }
    @SuppressWarnings("unchecked")
    private void ensureMealLanguageFields(Map<String, Object> result) {
        List<Map<String, Object>> plans = collectPlanMaps(result);

        for (Map<String, Object> plan : plans) {
            ensureMealLanguageField(plan.get("breakfast"));
            ensureMealLanguageField(plan.get("lunch"));
            ensureMealLanguageField(plan.get("dinner"));
            ensureMealLanguageField(plan.get("snack"));
        }
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
        List<Map<String, Object>> plans = collectPlanMaps(result);

        for (Map<String, Object> plan : plans) {
            ensureMealIconField(plan.get("breakfast"));
            ensureMealIconField(plan.get("lunch"));
            ensureMealIconField(plan.get("dinner"));
            ensureMealIconField(plan.get("snack"));
        }
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
        List<Map<String, Object>> plans = collectPlanMaps(result);

        for (Map<String, Object> plan : plans) {
            ensureYoutubeSearchLink(plan.get("breakfast"));
            ensureYoutubeSearchLink(plan.get("lunch"));
            ensureYoutubeSearchLink(plan.get("dinner"));
            ensureYoutubeSearchLink(plan.get("snack"));
        }
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
        List<Map<String, Object>> plans = collectPlanMaps(result);

        for (Map<String, Object> plan : plans) {
            sanitizeMealUrl(plan.get("breakfast"));
            sanitizeMealUrl(plan.get("lunch"));
            sanitizeMealUrl(plan.get("dinner"));
            sanitizeMealUrl(plan.get("snack"));
        }

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

        List<Map<String, Object>> plans = collectPlanMaps(result);

        for (int dayIndex = 0; dayIndex < plans.size(); dayIndex++) {
            enforceSingleDayTargetLimits(plans.get(dayIndex), request, dayIndex + 1);
        }

        return result;
    }

    private void enforceSingleDayTargetLimits(
            Map<String, Object> plan,
            MealPlanGenerateRequest request,
            int dayNumber
    ) {
        if (plan == null) {
            return;
        }

        double targetCarbs = normalizeTarget(request == null ? null : request.getTargetCarbs(), 155.0);
        double targetProtein = normalizeTarget(request == null ? null : request.getTargetProtein(), 32.0);
        double targetFat = normalizeTarget(request == null ? null : request.getTargetFat(), 28.0);

        String[] mealKeys = new String[] {"breakfast", "lunch", "dinner", "snack"};
        double[] before = recalculatePlanAndGetTotals(plan, mealKeys);

        int iterations = optimizeSingleDayMacroTargets(
                plan,
                mealKeys,
                targetCarbs,
                targetProtein,
                targetFat
        );

        double[] after = recalculatePlanAndGetTotals(plan, mealKeys);

        log.info(
                "AI meal plan day {} macros optimized. Before carbs/protein/fat: {}/{}/{}. Target: {}/{}/{}. After: {}/{}/{}. Error: {}/{}/{}. Iterations:{}",
                dayNumber,
                roundOne(before[0]),
                roundOne(before[1]),
                roundOne(before[2]),
                targetCarbs,
                targetProtein,
                targetFat,
                roundOne(after[0]),
                roundOne(after[1]),
                roundOne(after[2]),
                roundOne(after[0] - targetCarbs),
                roundOne(after[1] - targetProtein),
                roundOne(after[2] - targetFat),
                iterations
        );
    }

    /*
     * The old logic scaled the whole day by whichever macro was most over target.
     * That could make fat accurate but push carbs/protein far below target.
     *
     * This optimizer adjusts individual ingredient weights instead.
     * At each step it picks the single gram adjustment that most reduces the
     * normalized squared error of carbs, protein, and fat together.
     */
    @SuppressWarnings("unchecked")
    private int optimizeSingleDayMacroTargets(
            Map<String, Object> plan,
            String[] mealKeys,
            double targetCarbs,
            double targetProtein,
            double targetFat
    ) {
        List<MacroIngredientCandidate> candidates = collectAdjustableMacroIngredients(plan, mealKeys);

        if (candidates.size() == 0) {
            recalculatePlanAndGetTotals(plan, mealKeys);
            return 0;
        }

        final int maxIterations = 800;
        final double toleranceGrams = 0.35;
        final double minImprovement = 0.00000001;

        int iterations = 0;

        for (; iterations < maxIterations; iterations++) {
            double[] totals = recalculatePlanAndGetTotals(plan, mealKeys);

            if (isWithinMacroTolerance(
                    totals,
                    targetCarbs,
                    targetProtein,
                    targetFat,
                    toleranceGrams
            )) {
                break;
            }

            double currentScore = normalizedMacroErrorScore(
                    totals,
                    targetCarbs,
                    targetProtein,
                    targetFat
            );

            MacroIngredientCandidate bestCandidate = null;
            double bestDeltaGrams = 0.0;
            double bestScore = currentScore;

            for (MacroIngredientCandidate candidate : candidates) {
                double rawDelta = calculateBestGramDeltaForCandidate(
                        candidate,
                        totals,
                        targetCarbs,
                        targetProtein,
                        targetFat
                );

                double boundedDelta = boundCandidateDelta(candidate, rawDelta);

                if (Math.abs(boundedDelta) < 0.02) {
                    continue;
                }

                double[] projectedTotals = new double[] {
                        totals[0] + candidate.carbsPerGram * boundedDelta,
                        totals[1] + candidate.proteinPerGram * boundedDelta,
                        totals[2] + candidate.fatPerGram * boundedDelta
                };

                double projectedScore = normalizedMacroErrorScore(
                        projectedTotals,
                        targetCarbs,
                        targetProtein,
                        targetFat
                );

                if (projectedScore + minImprovement < bestScore) {
                    bestCandidate = candidate;
                    bestDeltaGrams = boundedDelta;
                    bestScore = projectedScore;
                }
            }

            if (bestCandidate == null) {
                break;
            }

            applyCandidateDelta(bestCandidate, bestDeltaGrams);
        }

        recalculatePlanAndGetTotals(plan, mealKeys);
        return iterations;
    }

    @SuppressWarnings("unchecked")
    private List<MacroIngredientCandidate> collectAdjustableMacroIngredients(
            Map<String, Object> plan,
            String[] mealKeys
    ) {
        List<MacroIngredientCandidate> candidates = new ArrayList<MacroIngredientCandidate>();

        for (String key : mealKeys) {
            Object mealObj = plan.get(key);

            if (!(mealObj instanceof Map)) {
                continue;
            }

            Map<String, Object> meal = (Map<String, Object>) mealObj;
            Object ingredientsObj = meal.get("ingredients");

            if (!(ingredientsObj instanceof List)) {
                continue;
            }

            List<Object> ingredients = (List<Object>) ingredientsObj;

            for (Object ingredientObj : ingredients) {
                if (!(ingredientObj instanceof Map)) {
                    continue;
                }

                Map<String, Object> ingredient = (Map<String, Object>) ingredientObj;
                double grams = numberValue(ingredient.get("gramsEstimated"));

                if (grams <= 0.0) {
                    continue;
                }

                double carbs = numberValue(ingredient.get("carbohydrateG"));
                double protein = numberValue(ingredient.get("proteinG"));
                double fat = numberValue(ingredient.get("fatG"));
                double kcal = numberValue(ingredient.get("energyKcal"));

                if (carbs <= 0.0 && protein <= 0.0 && fat <= 0.0) {
                    continue;
                }

                double carbsPerGram = carbs / grams;
                double proteinPerGram = protein / grams;
                double fatPerGram = fat / grams;
                double kcalPerGram = kcal / grams;

                if (!isFinitePositiveOrZero(carbsPerGram)
                        || !isFinitePositiveOrZero(proteinPerGram)
                        || !isFinitePositiveOrZero(fatPerGram)
                        || !isFinitePositiveOrZero(kcalPerGram)) {
                    continue;
                }

                candidates.add(new MacroIngredientCandidate(
                        ingredient,
                        grams,
                        carbsPerGram,
                        proteinPerGram,
                        fatPerGram,
                        kcalPerGram
                ));
            }
        }

        return candidates;
    }

    private double calculateBestGramDeltaForCandidate(
            MacroIngredientCandidate candidate,
            double[] totals,
            double targetCarbs,
            double targetProtein,
            double targetFat
    ) {
        double safeTargetCarbs = Math.max(targetCarbs, 1.0);
        double safeTargetProtein = Math.max(targetProtein, 1.0);
        double safeTargetFat = Math.max(targetFat, 1.0);

        double errorCarbs = (targetCarbs - totals[0]) / safeTargetCarbs;
        double errorProtein = (targetProtein - totals[1]) / safeTargetProtein;
        double errorFat = (targetFat - totals[2]) / safeTargetFat;

        double vectorCarbs = candidate.carbsPerGram / safeTargetCarbs;
        double vectorProtein = candidate.proteinPerGram / safeTargetProtein;
        double vectorFat = candidate.fatPerGram / safeTargetFat;

        double denominator =
                vectorCarbs * vectorCarbs
                        + vectorProtein * vectorProtein
                        + vectorFat * vectorFat;

        if (denominator <= 0.0) {
            return 0.0;
        }

        return (
                errorCarbs * vectorCarbs
                        + errorProtein * vectorProtein
                        + errorFat * vectorFat
        ) / denominator;
    }

    private double boundCandidateDelta(MacroIngredientCandidate candidate, double rawDelta) {
        if (Double.isNaN(rawDelta) || Double.isInfinite(rawDelta)) {
            return 0.0;
        }

        double maxStep = Math.max(6.0, Math.min(80.0, candidate.currentGrams * 0.35));
        double delta = Math.max(-maxStep, Math.min(maxStep, rawDelta));

        double minimumGrams = 1.0;
        double maximumGrams = candidate.maximumGrams();

        if (candidate.currentGrams + delta < minimumGrams) {
            delta = minimumGrams - candidate.currentGrams;
        }

        if (candidate.currentGrams + delta > maximumGrams) {
            delta = maximumGrams - candidate.currentGrams;
        }

        return delta;
    }

    private void applyCandidateDelta(MacroIngredientCandidate candidate, double deltaGrams) {
        double nextGrams = candidate.currentGrams + deltaGrams;
        nextGrams = Math.max(1.0, Math.min(nextGrams, candidate.maximumGrams()));
        nextGrams = roundOne(nextGrams);

        candidate.currentGrams = nextGrams;

        Map<String, Object> ingredient = candidate.ingredient;
        ingredient.put("gramsEstimated", nextGrams);
        ingredient.put("measure", formatGramMeasure(nextGrams));
        ingredient.put("energyKcal", roundOne(candidate.kcalPerGram * nextGrams));
        ingredient.put("proteinG", roundOne(candidate.proteinPerGram * nextGrams));
        ingredient.put("carbohydrateG", roundOne(candidate.carbsPerGram * nextGrams));
        ingredient.put("fatG", roundOne(candidate.fatPerGram * nextGrams));
    }

    private boolean isWithinMacroTolerance(
            double[] totals,
            double targetCarbs,
            double targetProtein,
            double targetFat,
            double toleranceGrams
    ) {
        return Math.abs(totals[0] - targetCarbs) <= toleranceGrams
                && Math.abs(totals[1] - targetProtein) <= toleranceGrams
                && Math.abs(totals[2] - targetFat) <= toleranceGrams;
    }

    private double normalizedMacroErrorScore(
            double[] totals,
            double targetCarbs,
            double targetProtein,
            double targetFat
    ) {
        double carbsError = (totals[0] - targetCarbs) / Math.max(targetCarbs, 1.0);
        double proteinError = (totals[1] - targetProtein) / Math.max(targetProtein, 1.0);
        double fatError = (totals[2] - targetFat) / Math.max(targetFat, 1.0);

        return carbsError * carbsError
                + proteinError * proteinError
                + fatError * fatError;
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

    private boolean isFinitePositiveOrZero(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && value >= 0.0;
    }

    private String formatGramMeasure(double grams) {
        double rounded = roundOne(Math.max(grams, 0.0));

        if (Math.abs(rounded - Math.round(rounded)) < 0.0001) {
            return ((long) Math.round(rounded)) + "g";
        }

        return rounded + "g";
    }

    private static class MacroIngredientCandidate {
        private final Map<String, Object> ingredient;
        private final double originalGrams;
        private double currentGrams;
        private final double carbsPerGram;
        private final double proteinPerGram;
        private final double fatPerGram;
        private final double kcalPerGram;

        private MacroIngredientCandidate(
                Map<String, Object> ingredient,
                double grams,
                double carbsPerGram,
                double proteinPerGram,
                double fatPerGram,
                double kcalPerGram
        ) {
            this.ingredient = ingredient;
            this.originalGrams = grams;
            this.currentGrams = grams;
            this.carbsPerGram = carbsPerGram;
            this.proteinPerGram = proteinPerGram;
            this.fatPerGram = fatPerGram;
            this.kcalPerGram = kcalPerGram;
        }

        private double maximumGrams() {
            return Math.max(originalGrams * 5.0, originalGrams + 300.0);
        }
    }

    private int normalizeRequestedDays(MealPlanGenerateRequest request) {
        if (request == null || request.getDays() == null) {
            return 1;
        }

        int days = request.getDays().intValue();
        if (days < 1) {
            return 1;
        }

        return Math.min(days, 7);
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
        int requestedDays = getRequestedDays(request);

        if (requestedDays <= 1) {
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("plan", buildFallbackDayPlan(1));
            return result;
        }

        List<Map<String, Object>> plans = new ArrayList<Map<String, Object>>();

        for (int day = 1; day <= requestedDays; day++) {
            Map<String, Object> dayWrapper = new HashMap<String, Object>();
            dayWrapper.put("day", day);
            dayWrapper.put("plan", buildFallbackDayPlan(day));
            plans.add(dayWrapper);
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("plans", plans);
        return result;
    }

    private Map<String, Object> buildFallbackDayPlan(int dayNumber) {
        String suffix = dayNumber <= 1 ? "" : "-day" + dayNumber;

        Map<String, Object> breakfast = meal(
                "ai-breakfast" + suffix,
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
                "ai-lunch" + suffix,
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
                "ai-dinner" + suffix,
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
                "ai-snack" + suffix,
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

        return plan;
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
