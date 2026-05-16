package com.jom.healthy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jom.healthy.dto.MealNutritionRowDto;
import com.jom.healthy.entity.TheMealDbMeal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface TheMealDbMealMapper extends BaseMapper<TheMealDbMeal> {

    @Select(
            "SELECT " +
                    "m.id AS mealDbId, " +
                    "m.id_meal AS idMeal, " +
                    "m.str_meal AS strMeal, " +
                    "m.str_meal_cn AS strMealCn, " +
                    "m.str_meal_ms AS strMealMs, " +
                    "m.str_meal_alternate AS strMealAlternate, " +

                    "m.str_category AS strCategory, " +
                    "m.str_category_cn AS strCategoryCn, " +
                    "m.str_category_ms AS strCategoryMs, " +

                    "m.str_area AS strArea, " +
                    "m.str_area_cn AS strAreaCn, " +
                    "m.str_area_ms AS strAreaMs, " +

                    "m.str_instructions AS strInstructions, " +
                    "m.str_instructions_cn AS strInstructionsCn, " +
                    "m.str_instructions_ms AS strInstructionsMs, " +

                    "m.str_meal_thumb AS strMealThumb, " +
                    "m.str_tags AS strTags, " +
                    "m.str_youtube AS strYoutube, " +
                    "m.portion_factor AS portionFactor, " +

                    "m.str_ingredient1 AS strIngredient1, " +
                    "m.str_ingredient2 AS strIngredient2, " +
                    "m.str_ingredient3 AS strIngredient3, " +
                    "m.str_ingredient4 AS strIngredient4, " +
                    "m.str_ingredient5 AS strIngredient5, " +
                    "m.str_ingredient6 AS strIngredient6, " +
                    "m.str_ingredient7 AS strIngredient7, " +
                    "m.str_ingredient8 AS strIngredient8, " +
                    "m.str_ingredient9 AS strIngredient9, " +
                    "m.str_ingredient10 AS strIngredient10, " +
                    "m.str_ingredient11 AS strIngredient11, " +
                    "m.str_ingredient12 AS strIngredient12, " +
                    "m.str_ingredient13 AS strIngredient13, " +
                    "m.str_ingredient14 AS strIngredient14, " +
                    "m.str_ingredient15 AS strIngredient15, " +
                    "m.str_ingredient16 AS strIngredient16, " +
                    "m.str_ingredient17 AS strIngredient17, " +
                    "m.str_ingredient18 AS strIngredient18, " +
                    "m.str_ingredient19 AS strIngredient19, " +
                    "m.str_ingredient20 AS strIngredient20, " +

                    "m.str_measure1 AS strMeasure1, " +
                    "m.str_measure2 AS strMeasure2, " +
                    "m.str_measure3 AS strMeasure3, " +
                    "m.str_measure4 AS strMeasure4, " +
                    "m.str_measure5 AS strMeasure5, " +
                    "m.str_measure6 AS strMeasure6, " +
                    "m.str_measure7 AS strMeasure7, " +
                    "m.str_measure8 AS strMeasure8, " +
                    "m.str_measure9 AS strMeasure9, " +
                    "m.str_measure10 AS strMeasure10, " +
                    "m.str_measure11 AS strMeasure11, " +
                    "m.str_measure12 AS strMeasure12, " +
                    "m.str_measure13 AS strMeasure13, " +
                    "m.str_measure14 AS strMeasure14, " +
                    "m.str_measure15 AS strMeasure15, " +
                    "m.str_measure16 AS strMeasure16, " +
                    "m.str_measure17 AS strMeasure17, " +
                    "m.str_measure18 AS strMeasure18, " +
                    "m.str_measure19 AS strMeasure19, " +
                    "m.str_measure20 AS strMeasure20, " +

                    "m.str_source AS strSource, " +
                    "m.str_image_source AS strImageSource, " +
                    "m.str_creative_commons_confirmed AS strCreativeCommonsConfirmed, " +
                    "m.date_modified AS dateModified, " +
                    "m.raw_json AS rawJson, " +
                    "m.created_at AS createdAt, " +
                    "m.updated_at AS updatedAt, " +

                    "i.id AS ingredientId, " +
                    "i.meal_id AS mealId, " +
                    "i.ingredient_order AS ingredientOrder, " +
                    "i.ingredient_name AS ingredientName, " +
                    "i.measure AS measure, " +
                    "i.normalized_name AS normalizedName, " +
                    "i.myfcd_food_id AS myfcdFoodId, " +
                    "i.myfcd_food_name AS myfcdFoodName, " +
                    "i.grams_estimated AS gramsEstimated, " +
                    "i.mapping_confidence AS mappingConfidence, " +

                    "f.id AS foodId, " +
                    "f.food_name_en AS foodNameEn, " +
                    "f.food_name_cn AS foodNameCn, " +
                    "f.food_name_ms AS foodNameMs, " +
                    "f.food_group_ AS foodGroup, " +
                    "f.pic_url AS picUrl, " +
                    "f.energy_kcal AS energyKcalPer100g, " +
                    "f.protein_g AS proteinGPer100g, " +
                    "f.carbohydrate_g AS carbohydrateGPer100g, " +
                    "f.fat_g AS fatGPer100g " +

                    "FROM themealdb_meals m " +
                    "LEFT JOIN themealdb_meal_ingredients i " +
                    "ON m.id_meal = i.meal_id " +
                    "LEFT JOIN food f " +
                    "ON i.myfcd_food_id IS NOT NULL " +
                    "AND i.myfcd_food_id <> '' " +
                    "AND CAST(i.myfcd_food_id AS UNSIGNED) = f.id " +
                    "WHERE m.str_meal LIKE CONCAT(#{keyword}, '%') " +
                    "ORDER BY m.str_meal ASC, i.ingredient_order ASC"
    )
    List<MealNutritionRowDto> searchMealNutritionByPrefix(@Param("keyword") String keyword);


    /**
     * 查询需要翻译 str_meal 的 meal
     * 一次最多由 Service 限制为 100 条
     */
    @Select(
            "SELECT " +
                    "id, " +
                    "str_meal AS strMeal " +
                    "FROM themealdb_meals " +
                    "WHERE id > #{id} " +
                    "AND str_meal IS NOT NULL " +
                    "AND str_meal <> '' " +
                    "AND ( " +
                    "   str_meal_cn IS NULL OR str_meal_cn = '' " +
                    "   OR str_meal_ms IS NULL OR str_meal_ms = '' " +
                    ") " +
                    "ORDER BY id ASC " +
                    "LIMIT #{size}"
    )
    List<TheMealDbMeal> selectMealsNeedMealNameTranslation(
            @Param("id") Long id,
            @Param("size") Integer size
    );


    /**
     * 更新 str_meal 的中文和马来文翻译
     * 只填空，不覆盖已有值
     */
    @Update(
            "UPDATE themealdb_meals SET " +

                    "str_meal_cn = CASE " +
                    "   WHEN str_meal_cn IS NULL OR str_meal_cn = '' " +
                    "   THEN #{strMealCn} ELSE str_meal_cn END, " +

                    "str_meal_ms = CASE " +
                    "   WHEN str_meal_ms IS NULL OR str_meal_ms = '' " +
                    "   THEN #{strMealMs} ELSE str_meal_ms END, " +

                    "updated_at = NOW() " +
                    "WHERE id = #{id}"
    )
    int updateMealNameTranslationFields(TheMealDbMeal meal);


    /**
     * 查询需要翻译的 meal，每次最多 size 条
     */
    @Select(
            "SELECT " +
                    "id, " +
                    "str_category AS strCategory, " +
                    "str_area AS strArea, " +
                    "str_instructions AS strInstructions " +
                    "FROM themealdb_meals " +
                    "WHERE id >= #{id} " +
                    "AND ( " +
                    "   (str_category IS NOT NULL AND str_category <> '' AND " +
                    "       (str_category_cn IS NULL OR str_category_cn = '' " +
                    "        OR str_category_ms IS NULL OR str_category_ms = '')) " +
                    "   OR " +
                    "   (str_area IS NOT NULL AND str_area <> '' AND " +
                    "       (str_area_cn IS NULL OR str_area_cn = '' " +
                    "        OR str_area_ms IS NULL OR str_area_ms = '')) " +
                    "   OR " +
                    "   (str_instructions IS NOT NULL AND str_instructions <> '' AND " +
                    "       (str_instructions_cn IS NULL OR str_instructions_cn = '' " +
                    "        OR str_instructions_ms IS NULL OR str_instructions_ms = '')) " +
                    ") " +
                    "ORDER BY id ASC " +
                    "LIMIT #{size}"
    )
    List<TheMealDbMeal> selectMealsNeedTranslation(@Param("id") Long id,
                                                   @Param("size") Integer size);


    /**
     * 更新翻译字段：
     * 只填充原本为空的字段，不覆盖已有翻译
     */
    @Update(
            "UPDATE themealdb_meals SET " +

                    "str_category_cn = CASE " +
                    "   WHEN str_category_cn IS NULL OR str_category_cn = '' " +
                    "   THEN #{strCategoryCn} ELSE str_category_cn END, " +

                    "str_category_ms = CASE " +
                    "   WHEN str_category_ms IS NULL OR str_category_ms = '' " +
                    "   THEN #{strCategoryMs} ELSE str_category_ms END, " +

                    "str_area_cn = CASE " +
                    "   WHEN str_area_cn IS NULL OR str_area_cn = '' " +
                    "   THEN #{strAreaCn} ELSE str_area_cn END, " +

                    "str_area_ms = CASE " +
                    "   WHEN str_area_ms IS NULL OR str_area_ms = '' " +
                    "   THEN #{strAreaMs} ELSE str_area_ms END, " +

                    "str_instructions_cn = CASE " +
                    "   WHEN str_instructions_cn IS NULL OR str_instructions_cn = '' " +
                    "   THEN #{strInstructionsCn} ELSE str_instructions_cn END, " +

                    "str_instructions_ms = CASE " +
                    "   WHEN str_instructions_ms IS NULL OR str_instructions_ms = '' " +
                    "   THEN #{strInstructionsMs} ELSE str_instructions_ms END, " +

                    "updated_at = NOW() " +
                    "WHERE id = #{id}"
    )
    int updateMealTranslationFields(TheMealDbMeal meal);

    /**
     * 查询需要翻译分类或地区的 meal
     * 一次最多 200 条
     */
    @Select(
            "SELECT " +
                    "id, " +
                    "str_category AS strCategory, " +
                    "str_area AS strArea " +
                    "FROM themealdb_meals " +
                    "WHERE id >= #{id} " +
                    "AND ( " +
                    "   (str_category IS NOT NULL AND str_category <> '' AND " +
                    "       (str_category_cn IS NULL OR str_category_cn = '' " +
                    "        OR str_category_ms IS NULL OR str_category_ms = '')) " +
                    "   OR " +
                    "   (str_area IS NOT NULL AND str_area <> '' AND " +
                    "       (str_area_cn IS NULL OR str_area_cn = '' " +
                    "        OR str_area_ms IS NULL OR str_area_ms = '')) " +
                    ") " +
                    "ORDER BY id ASC " +
                    "LIMIT #{size}"
    )
    List<TheMealDbMeal> selectMealsNeedCategoryAreaTranslation(
            @Param("id") Long id,
            @Param("size") Integer size
    );


    /**
     * 查询需要翻译做法步骤的 meal
     * 一次最多 5 条
     */
    @Select(
            "SELECT " +
                    "id, " +
                    "str_instructions AS strInstructions " +
                    "FROM themealdb_meals " +
                    "WHERE str_instructions IS NOT NULL " +
                    "AND str_instructions_cn IS NULL " +
                    "AND str_instructions <> '' " +
                    "AND ( " +
                    "   str_instructions_cn IS NULL OR str_instructions_cn = '' " +
                    "   OR str_instructions_ms IS NULL OR str_instructions_ms = '' " +
                    ") " +
                    "ORDER BY id ASC " +
                    "LIMIT #{size}"
    )
    List<TheMealDbMeal> selectMealsNeedInstructionsTranslation(
            @Param("size") Integer size
    );


    /**
     * 更新分类和地区翻译
     * 只填空，不覆盖已有值
     */
    @Update(
            "UPDATE themealdb_meals SET " +

                    "str_category_cn = CASE " +
                    "   WHEN str_category_cn IS NULL OR str_category_cn = '' " +
                    "   THEN #{strCategoryCn} ELSE str_category_cn END, " +

                    "str_category_ms = CASE " +
                    "   WHEN str_category_ms IS NULL OR str_category_ms = '' " +
                    "   THEN #{strCategoryMs} ELSE str_category_ms END, " +

                    "str_area_cn = CASE " +
                    "   WHEN str_area_cn IS NULL OR str_area_cn = '' " +
                    "   THEN #{strAreaCn} ELSE str_area_cn END, " +

                    "str_area_ms = CASE " +
                    "   WHEN str_area_ms IS NULL OR str_area_ms = '' " +
                    "   THEN #{strAreaMs} ELSE str_area_ms END, " +

                    "updated_at = NOW() " +
                    "WHERE id = #{id}"
    )
    int updateMealCategoryAreaTranslationFields(TheMealDbMeal meal);


    /**
     * 更新做法步骤翻译
     * 只填空，不覆盖已有值
     */
    @Update(
            "UPDATE themealdb_meals SET " +

                    "str_instructions_cn = CASE " +
                    "   WHEN str_instructions_cn IS NULL OR str_instructions_cn = '' " +
                    "   THEN #{strInstructionsCn} ELSE str_instructions_cn END, " +

                    "str_instructions_ms = CASE " +
                    "   WHEN str_instructions_ms IS NULL OR str_instructions_ms = '' " +
                    "   THEN #{strInstructionsMs} ELSE str_instructions_ms END, " +

                    "updated_at = NOW() " +
                    "WHERE id = #{id}"
    )
    int updateMealInstructionsTranslationFields(TheMealDbMeal meal);
}