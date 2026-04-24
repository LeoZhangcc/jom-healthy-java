package com.jom.healthy.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jom.healthy.entity.TheMealDbMeal;
import com.jom.healthy.entity.TheMealDbMealIngredient;
import com.jom.healthy.mapper.TheMealDbMealIngredientMapper;
import com.jom.healthy.mapper.TheMealDbMealMapper;
import com.jom.healthy.service.TheMealDbImportService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class TheMealDbImportServiceImpl extends ServiceImpl<TheMealDbMealMapper, TheMealDbMeal> implements TheMealDbImportService {

    private static final String API_BASE = "https://www.themealdb.com/api/json/v1/1/search.php?f=";

    @Autowired
    private TheMealDbMealMapper mealMapper;

    @Autowired
    private TheMealDbMealIngredientMapper ingredientMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void importAllMeals() throws Exception {
        for (char c = 'a'; c <= 'z'; c++) {
            System.out.println("Fetching meals starting with: " + c);

            String json = fetchJson(API_BASE + c);
            JsonNode root = objectMapper.readTree(json);
            JsonNode meals = root.get("meals");

            if (meals == null || meals.isNull()) {
                continue;
            }

            for (JsonNode mealNode : meals) {
                importOneMeal(mealNode);
            }

            Thread.sleep(300);
        }

        System.out.println("TheMealDB import finished.");
    }

    @Transactional
    public void importOneMeal(JsonNode mealNode) throws Exception {
        String idMeal = text(mealNode, "idMeal");

        if (idMeal == null) {
            return;
        }

        TheMealDbMeal meal = convertToMealEntity(mealNode);

        TheMealDbMeal existing = mealMapper.selectOne(
                new LambdaQueryWrapper<TheMealDbMeal>()
                        .eq(TheMealDbMeal::getIdMeal, idMeal)
                        .last("LIMIT 1")
        );

        if (existing == null) {
            mealMapper.insert(meal);
        } else {
            meal.setId(existing.getId());
            mealMapper.updateById(meal);
        }

        ingredientMapper.delete(
                new LambdaQueryWrapper<TheMealDbMealIngredient>()
                        .eq(TheMealDbMealIngredient::getMealId, idMeal)
        );

        for (int i = 1; i <= 20; i++) {
            String ingredient = text(mealNode, "strIngredient" + i);
            String measure = text(mealNode, "strMeasure" + i);

            if (ingredient == null || StringUtils.isEmpty(ingredient)) {
                continue;
            }

            TheMealDbMealIngredient item = new TheMealDbMealIngredient();
            item.setMealId(idMeal);
            item.setIngredientOrder(i);
            item.setIngredientName(ingredient.trim());
            item.setMeasure(measure == null ? null : measure.trim());
            item.setNormalizedName(normalizeIngredientName(ingredient));

            ingredientMapper.insert(item);
        }

        System.out.println("Imported: " + idMeal + " - " + text(mealNode, "strMeal"));
    }

    private String fetchJson(String urlString) throws Exception {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "JomHealthyApp/1.0");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);

            int statusCode = connection.getResponseCode();

            InputStream inputStream;
            if (statusCode >= 200 && statusCode < 300) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }

            String responseBody = readStream(inputStream);

            if (statusCode < 200 || statusCode >= 300) {
                throw new RuntimeException("HTTP Error: " + statusCode + ", body: " + responseBody);
            }

            return responseBody;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
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


    private TheMealDbMeal convertToMealEntity(JsonNode node) throws Exception {
        TheMealDbMeal meal = new TheMealDbMeal();

        meal.setIdMeal(text(node, "idMeal"));

        meal.setStrMeal(text(node, "strMeal"));
        meal.setStrMealAlternate(text(node, "strMealAlternate"));
        meal.setStrCategory(text(node, "strCategory"));
        meal.setStrArea(text(node, "strArea"));

        meal.setStrInstructions(text(node, "strInstructions"));
        meal.setStrMealThumb(text(node, "strMealThumb"));
        meal.setStrTags(text(node, "strTags"));
        meal.setStrYoutube(text(node, "strYoutube"));

        meal.setStrIngredient1(text(node, "strIngredient1"));
        meal.setStrIngredient2(text(node, "strIngredient2"));
        meal.setStrIngredient3(text(node, "strIngredient3"));
        meal.setStrIngredient4(text(node, "strIngredient4"));
        meal.setStrIngredient5(text(node, "strIngredient5"));
        meal.setStrIngredient6(text(node, "strIngredient6"));
        meal.setStrIngredient7(text(node, "strIngredient7"));
        meal.setStrIngredient8(text(node, "strIngredient8"));
        meal.setStrIngredient9(text(node, "strIngredient9"));
        meal.setStrIngredient10(text(node, "strIngredient10"));
        meal.setStrIngredient11(text(node, "strIngredient11"));
        meal.setStrIngredient12(text(node, "strIngredient12"));
        meal.setStrIngredient13(text(node, "strIngredient13"));
        meal.setStrIngredient14(text(node, "strIngredient14"));
        meal.setStrIngredient15(text(node, "strIngredient15"));
        meal.setStrIngredient16(text(node, "strIngredient16"));
        meal.setStrIngredient17(text(node, "strIngredient17"));
        meal.setStrIngredient18(text(node, "strIngredient18"));
        meal.setStrIngredient19(text(node, "strIngredient19"));
        meal.setStrIngredient20(text(node, "strIngredient20"));

        meal.setStrMeasure1(text(node, "strMeasure1"));
        meal.setStrMeasure2(text(node, "strMeasure2"));
        meal.setStrMeasure3(text(node, "strMeasure3"));
        meal.setStrMeasure4(text(node, "strMeasure4"));
        meal.setStrMeasure5(text(node, "strMeasure5"));
        meal.setStrMeasure6(text(node, "strMeasure6"));
        meal.setStrMeasure7(text(node, "strMeasure7"));
        meal.setStrMeasure8(text(node, "strMeasure8"));
        meal.setStrMeasure9(text(node, "strMeasure9"));
        meal.setStrMeasure10(text(node, "strMeasure10"));
        meal.setStrMeasure11(text(node, "strMeasure11"));
        meal.setStrMeasure12(text(node, "strMeasure12"));
        meal.setStrMeasure13(text(node, "strMeasure13"));
        meal.setStrMeasure14(text(node, "strMeasure14"));
        meal.setStrMeasure15(text(node, "strMeasure15"));
        meal.setStrMeasure16(text(node, "strMeasure16"));
        meal.setStrMeasure17(text(node, "strMeasure17"));
        meal.setStrMeasure18(text(node, "strMeasure18"));
        meal.setStrMeasure19(text(node, "strMeasure19"));
        meal.setStrMeasure20(text(node, "strMeasure20"));

        meal.setStrSource(text(node, "strSource"));
        meal.setStrImageSource(text(node, "strImageSource"));
        meal.setStrCreativeCommonsConfirmed(text(node, "strCreativeCommonsConfirmed"));

        meal.setDateModified(parseDateTime(text(node, "dateModified")));

        meal.setRawJson(objectMapper.writeValueAsString(node));

        return meal;
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);

        if (value == null || value.isNull()) {
            return null;
        }

        String str = value.asText();

        if (str == null) {
            return null;
        }

        str = str.trim();

        if (str.isEmpty()) {
            return null;
        }

        return str;
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || StringUtils.isEmpty(value)) {
            return null;
        }

        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeIngredientName(String name) {
        if (name == null) {
            return null;
        }

        return name.trim()
                .toLowerCase()
                .replaceAll("\\s+", " ");
    }
}
