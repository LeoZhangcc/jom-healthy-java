package com.jom.healthy.service;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jom.healthy.entity.FoodAiCandidate;
import com.jom.healthy.dto.GeminiFoodCandidateDTO;
import com.jom.healthy.dto.UnmatchedIngredientDTO;
import com.jom.healthy.mapper.FoodAiCandidateMapper;
import com.jom.healthy.mapper.TheMealDbMealIngredientMapper;
import com.jom.healthy.service.impl.FoodTransationaServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GeminiFoodCandidateService {

    private final TheMealDbMealIngredientMapper ingredientMapper;
    private final FoodAiCandidateMapper foodAiCandidateMapper;
    private final FoodTransationaServiceImpl geminiClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void generateAllFoodCandidates() throws Exception {
        List<UnmatchedIngredientDTO> all = ingredientMapper.selectUnmatchedIngredientNames();

        int batchSize = 30;

        for (int start = 0; start < all.size(); start += batchSize) {
            int end = Math.min(start + batchSize, all.size());
            List<UnmatchedIngredientDTO> batch = all.subList(start, end);

            System.out.println("Generating food candidates: " + start + " - " + end);

            generateOneBatch(batch);

            Thread.sleep(5000);
        }

        System.out.println("Gemini food candidate generation finished.");
    }

    @Transactional
    public void generateOneBatch(List<UnmatchedIngredientDTO> batch) throws Exception {
        String prompt = buildPrompt(batch);

        String resultJson = geminiClient.generateFoodCandidatesJson(prompt);

        List<GeminiFoodCandidateDTO> results = objectMapper.readValue(
                resultJson,
                new TypeReference<List<GeminiFoodCandidateDTO>>() {}
        );

        for (GeminiFoodCandidateDTO dto : results) {
            if (dto.getNormalizedName() == null || dto.getNormalizedName().trim().isEmpty()) {
                continue;
            }

            saveOrUpdateCandidate(dto, resultJson);
        }
    }

    private String buildPrompt(List<UnmatchedIngredientDTO> batch) {
        StringBuilder sb = new StringBuilder();

        sb.append("Return JSON array only. Estimate nutrition per 100g for each ingredient.\n");
        sb.append("Fields: normalizedName,foodNameEn,foodNameCn,foodNameMs,foodGroup,energyKcal,proteinG,fatG,carbohydrateG,confidenceScore,reason.\n");
        sb.append("Rules: per 100g edible part; realistic common values; no markdown; water/salt=0 kcal/protein/fat/carb; milk=whole milk if generic; eggs=whole chicken egg; vegetable oil=generic vegetable oil.\n");
        sb.append("foodGroup examples: Vegetables,Fruits,Meat,Fish,Dairy,Cereals,Oil,Condiment,Spices,Beverage,Other.\n");
        sb.append("Ingredients:\n");

        for (UnmatchedIngredientDTO item : batch) {
            sb.append(item.getNormalizedName()).append("\n");
        }

        return sb.toString();
    }

    private void saveOrUpdateCandidate(GeminiFoodCandidateDTO dto, String rawJson) {
        FoodAiCandidate existing = foodAiCandidateMapper.selectOne(
                new LambdaQueryWrapper<FoodAiCandidate>()
                        .eq(FoodAiCandidate::getNormalizedName, dto.getNormalizedName())
                        .last("LIMIT 1")
        );

        FoodAiCandidate candidate = existing == null ? new FoodAiCandidate() : existing;

        candidate.setNormalizedName(dto.getNormalizedName());
        candidate.setFoodNameEn(dto.getFoodNameEn());
        candidate.setFoodNameCn(dto.getFoodNameCn());
        candidate.setFoodNameMs(dto.getFoodNameMs());
        candidate.setFoodGroup(dto.getFoodGroup());

        candidate.setEnergyKcal(dto.getEnergyKcal());
        candidate.setProteinG(dto.getProteinG());
        candidate.setFatG(dto.getFatG());
        candidate.setCarbohydrateG(dto.getCarbohydrateG());

        candidate.setSource("gemini");
        candidate.setConfidenceScore(
                dto.getConfidenceScore() == null
                        ? BigDecimal.ZERO
                        : BigDecimal.valueOf(dto.getConfidenceScore())
        );
        candidate.setReason(dto.getReason());
        candidate.setReviewed(0);
        candidate.setRawJson(rawJson);

        if (existing == null) {
            foodAiCandidateMapper.insert(candidate);
        } else {
            candidate.setId(existing.getId());
            foodAiCandidateMapper.updateById(candidate);
        }
    }
}
