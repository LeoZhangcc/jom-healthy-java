package com.jom.healthy.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jom.healthy.dto.MealIngredientNutritionDto;
import com.jom.healthy.dto.MealNutritionDto;
import com.jom.healthy.dto.MealNutritionRowDto;
import com.jom.healthy.entity.TheMealDbMeal;
import com.jom.healthy.entity.TheMealDbMealIngredient;
import com.jom.healthy.mapper.TheMealDbMealIngredientMapper;
import com.jom.healthy.mapper.TheMealDbMealMapper;
import com.jom.healthy.service.TheMealService;
import com.jom.healthy.util.MeasureToGramConverter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TheMealServiceImpl extends ServiceImpl<TheMealDbMealMapper, TheMealDbMeal> implements TheMealService {

    private static final String API_BASE = "https://www.themealdb.com/api/json/v1/1/search.php?f=";

    @Autowired
    private TheMealDbMealMapper mealMapper;

    @Autowired
    private TheMealDbMealIngredientMapper ingredientMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();



    @Override
    public List<MealNutritionDto> searchMealsByNamePrefix(String keyword) {
        if (keyword == null) {
            keyword = "";
        }

        keyword = keyword.trim();

        List<MealNutritionRowDto> rows = mealMapper.searchMealNutritionByPrefix(keyword);

        Map<String, MealNutritionDto> mealMap = new LinkedHashMap<>();

        for (MealNutritionRowDto row : rows) {
            MealNutritionDto mealDto = mealMap.get(row.getIdMeal());

            if (mealDto == null) {
                mealDto = buildMealDto(row);
                mealMap.put(row.getIdMeal(), mealDto);
            }

            if (row.getIngredientId() != null) {
                MealIngredientNutritionDto ingredientDto = buildIngredientDto(row);
                mealDto.getIngredients().add(ingredientDto);

                mealDto.setTotalEnergyKcal(
                        mealDto.getTotalEnergyKcal().add(ingredientDto.getEnergyKcal())
                );
                mealDto.setTotalProteinG(
                        mealDto.getTotalProteinG().add(ingredientDto.getProteinG())
                );
                mealDto.setTotalCarbohydrateG(
                        mealDto.getTotalCarbohydrateG().add(ingredientDto.getCarbohydrateG())
                );
                mealDto.setTotalFatG(
                        mealDto.getTotalFatG().add(ingredientDto.getFatG())
                );
            }
        }

        for (MealNutritionDto dto : mealMap.values()) {
            dto.setTotalEnergyKcal(round(dto.getTotalEnergyKcal()));
            dto.setTotalProteinG(round(dto.getTotalProteinG()));
            dto.setTotalCarbohydrateG(round(dto.getTotalCarbohydrateG()));
            dto.setTotalFatG(round(dto.getTotalFatG()));
        }

        return new ArrayList<>(mealMap.values());
    }

    private MealNutritionDto buildMealDto(MealNutritionRowDto row) {
        MealNutritionDto dto = new MealNutritionDto();

        dto.setId(row.getMealDbId());
        dto.setIdMeal(row.getIdMeal());

        dto.setStrMeal(row.getStrMeal());
        dto.setStrMealAlternate(row.getStrMealAlternate());
        dto.setStrCategory(row.getStrCategory());
        dto.setStrArea(row.getStrArea());
        dto.setStrInstructions(row.getStrInstructions());
        dto.setStrMealThumb(row.getStrMealThumb());
        dto.setStrTags(row.getStrTags());
        dto.setStrYoutube(row.getStrYoutube());

        dto.setStrIngredient1(row.getStrIngredient1());
        dto.setStrIngredient2(row.getStrIngredient2());
        dto.setStrIngredient3(row.getStrIngredient3());
        dto.setStrIngredient4(row.getStrIngredient4());
        dto.setStrIngredient5(row.getStrIngredient5());
        dto.setStrIngredient6(row.getStrIngredient6());
        dto.setStrIngredient7(row.getStrIngredient7());
        dto.setStrIngredient8(row.getStrIngredient8());
        dto.setStrIngredient9(row.getStrIngredient9());
        dto.setStrIngredient10(row.getStrIngredient10());
        dto.setStrIngredient11(row.getStrIngredient11());
        dto.setStrIngredient12(row.getStrIngredient12());
        dto.setStrIngredient13(row.getStrIngredient13());
        dto.setStrIngredient14(row.getStrIngredient14());
        dto.setStrIngredient15(row.getStrIngredient15());
        dto.setStrIngredient16(row.getStrIngredient16());
        dto.setStrIngredient17(row.getStrIngredient17());
        dto.setStrIngredient18(row.getStrIngredient18());
        dto.setStrIngredient19(row.getStrIngredient19());
        dto.setStrIngredient20(row.getStrIngredient20());

        dto.setStrMeasure1(row.getStrMeasure1());
        dto.setStrMeasure2(row.getStrMeasure2());
        dto.setStrMeasure3(row.getStrMeasure3());
        dto.setStrMeasure4(row.getStrMeasure4());
        dto.setStrMeasure5(row.getStrMeasure5());
        dto.setStrMeasure6(row.getStrMeasure6());
        dto.setStrMeasure7(row.getStrMeasure7());
        dto.setStrMeasure8(row.getStrMeasure8());
        dto.setStrMeasure9(row.getStrMeasure9());
        dto.setStrMeasure10(row.getStrMeasure10());
        dto.setStrMeasure11(row.getStrMeasure11());
        dto.setStrMeasure12(row.getStrMeasure12());
        dto.setStrMeasure13(row.getStrMeasure13());
        dto.setStrMeasure14(row.getStrMeasure14());
        dto.setStrMeasure15(row.getStrMeasure15());
        dto.setStrMeasure16(row.getStrMeasure16());
        dto.setStrMeasure17(row.getStrMeasure17());
        dto.setStrMeasure18(row.getStrMeasure18());
        dto.setStrMeasure19(row.getStrMeasure19());
        dto.setStrMeasure20(row.getStrMeasure20());

        dto.setStrSource(row.getStrSource());
        dto.setStrImageSource(row.getStrImageSource());
        dto.setStrCreativeCommonsConfirmed(row.getStrCreativeCommonsConfirmed());

        dto.setDateModified(row.getDateModified());
        dto.setCreatedAt(row.getCreatedAt());
        dto.setUpdatedAt(row.getUpdatedAt());

        dto.setTotalEnergyKcal(BigDecimal.ZERO);
        dto.setTotalProteinG(BigDecimal.ZERO);
        dto.setTotalCarbohydrateG(BigDecimal.ZERO);
        dto.setTotalFatG(BigDecimal.ZERO);

        dto.setIngredients(new ArrayList<>());

        return dto;
    }

    private MealIngredientNutritionDto buildIngredientDto(MealNutritionRowDto row) {
        MealIngredientNutritionDto dto = new MealIngredientNutritionDto();

        dto.setIngredientId(row.getIngredientId());
        dto.setMealId(row.getMealId());
        dto.setIngredientOrder(row.getIngredientOrder());
        dto.setIngredientName(row.getIngredientName());
        dto.setMeasure(row.getMeasure());
        dto.setNormalizedName(row.getNormalizedName());
        dto.setMyfcdFoodId(row.getMyfcdFoodId());
        dto.setMyfcdFoodName(row.getMyfcdFoodName());
        dto.setGramsEstimated(row.getGramsEstimated());
        dto.setMappingConfidence(row.getMappingConfidence());

        dto.setFoodId(row.getFoodId());
        dto.setFoodNameEn(row.getFoodNameEn());
        dto.setFoodNameCn(row.getFoodNameCn());
        dto.setFoodNameMs(row.getFoodNameMs());
        dto.setFoodGroup(row.getFoodGroup());
        dto.setPicUrl(row.getPicUrl());

        dto.setEnergyKcalPer100g(row.getEnergyKcalPer100g());
        dto.setProteinGPer100g(row.getProteinGPer100g());
        dto.setCarbohydrateGPer100g(row.getCarbohydrateGPer100g());
        dto.setFatGPer100g(row.getFatGPer100g());

        BigDecimal grams = row.getGramsEstimated();

        if (grams == null || grams.compareTo(BigDecimal.ZERO) <= 0) {
            grams = MeasureToGramConverter.convertToGram(
                    row.getMeasure(),
                    row.getIngredientName(),
                    row.getNormalizedName()
            );
        }

        dto.setGramsEstimated(grams);

        dto.setEnergyKcal(calculateByGram(row.getEnergyKcalPer100g(), grams));
        dto.setProteinG(calculateByGram(row.getProteinGPer100g(), grams));
        dto.setCarbohydrateG(calculateByGram(row.getCarbohydrateGPer100g(), grams));
        dto.setFatG(calculateByGram(row.getFatGPer100g(), grams));

        return dto;
    }

    private BigDecimal calculateByGram(Number valuePer100g, BigDecimal grams) {
        if (valuePer100g == null || grams == null) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(valuePer100g.doubleValue())
                .multiply(grams)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
    }



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

    private BigDecimal round(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
