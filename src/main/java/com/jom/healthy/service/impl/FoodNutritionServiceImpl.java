package com.jom.healthy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jom.healthy.dto.FoodNutritionDto;
import com.jom.healthy.dto.FoodNutritionNeedsDto;
import com.jom.healthy.entity.FoodNutrition;
import com.jom.healthy.mapper.FoodNutritionMapper;
import com.jom.healthy.model.params.FoodNutritionParam;
import com.jom.healthy.service.FoodNutritionService;
import com.jom.healthy.util.FoodHealthCalculator;
import com.jom.healthy.util.ToolUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FoodNutritionServiceImpl extends ServiceImpl<FoodNutritionMapper, FoodNutrition>
        implements FoodNutritionService {

    private static final int KCAL_PER_GRAM_CARBOHYDRATE = 4;
    private static final int KCAL_PER_GRAM_PROTEIN = 4;
    private static final int KCAL_PER_GRAM_FAT = 9;

    /**
     * RNI 2017 carbohydrate recommendation:
     * Carbohydrate should comprise 50% - 65% of total energy intake.
     *
     * For infants, the service does not force this range because infant feeding
     * recommendations are handled differently in the RNI fat chapter and breast-milk
     * energy composition does not always fit the same carbohydrate range logic.
     */
    private static final double CARBOHYDRATE_MIN_TEI_RATIO = 0.50D;
    private static final double CARBOHYDRATE_MAX_TEI_RATIO = 0.65D;
    private static final double CARBOHYDRATE_TARGET_TEI_RATIO = 0.575D;

    private static final double EPSILON = 0.0000001D;

    @Override
    public void addFood(FoodNutritionParam param) {
        FoodNutrition entity = getEntity(param);
        this.save(entity);
    }

    @Override
    public List<FoodNutritionDto> queryFood(String name) {
        QueryWrapper<FoodNutrition> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("food_name_combine", name);
        List<FoodNutrition> foodNutritions = this.baseMapper.selectList(queryWrapper);
        List<FoodNutritionDto> result = new ArrayList<>();

        for (FoodNutrition food : foodNutritions) {
            FoodNutritionDto dto = FoodHealthCalculator.calculate(food);
            result.add(dto);
        }
        return result;
    }

    /**
     * Calculate daily nutrition planning targets according to
     * Recommended Nutrient Intakes (RNI) for Malaysia 2017.
     *
     * Important:
     * 1. The existing API signature is kept unchanged for frontend/backend compatibility.
     * 2. RNI energy and protein recommendations are population reference values selected by
     *    age group and gender, so heightCm and weightKg are not used to change the RNI target.
     * 3. Energy defaults follow the general-population RNI recommendation:
     *    - 1 - 6 years: PAL 1.4
     *    - 7 years and above: PAL 1.6
     * 4. Protein is returned directly from the RNI 2017 summary table, not from a fixed % of calories.
     * 5. Fat is selected inside the official RNI 2017 fat g/day range for the matching age/gender group.
     * 6. Carbohydrate is then calculated as an integer gram target so that:
     *    - the total calories are matched as closely as possible, preferably exactly;
     *    - for non-infant groups, carbohydrate stays within 50% - 65% TEI.
     *
     * Example:
     * ageMonths=60, gender=1 -> boys 4-6 years
     * output: cal=1300, carb=210, protein=16, fat=44
     */
    @Override
    public FoodNutritionNeedsDto getFoodNutritionNeeds(
            Double heightCm,
            Double weightKg,
            Integer ageMonths,
            Integer gender
    ) {
        validateAgeMonths(ageMonths);
        validateGender(gender);

        RniDailyTarget target = resolveRniDailyTarget(ageMonths, gender);
        return calculateFoodNutritionNeeds(target);
    }

    private FoodNutrition getEntity(FoodNutritionParam param) {
        FoodNutrition entity = new FoodNutrition();
        ToolUtil.copyProperties(param, entity);
        return entity;
    }

    @Override
    public void heartBeatCheck() {
        QueryWrapper<FoodNutrition> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("food_name_original", "beef");
        this.baseMapper.selectList(queryWrapper);
    }

    @Override
    public void updateName() {
        List<FoodNutrition> list = this.baseMapper.selectList(null);

        for (FoodNutrition food : list) {
            String foodNameCn = food.getFoodNameCn();
            String foodNameEn = food.getFoodNameEn();
            String foodNameMs = food.getFoodNameMs();
            food.setFoodNameCombine(foodNameCn + "," + foodNameEn + "," + foodNameMs);
            this.baseMapper.updateById(food);
        }
    }

    private void validateAgeMonths(Integer ageMonths) {
        if (ageMonths == null) {
            throw new IllegalArgumentException("ageMonths is required");
        }

        if (ageMonths < 0) {
            throw new IllegalArgumentException("ageMonths must be greater than or equal to 0");
        }
    }

    /**
     * Existing project convention:
     * 1 = male / boy
     * 2 = female / girl
     */
    private void validateGender(Integer gender) {
        if (gender == null) {
            throw new IllegalArgumentException("gender is required");
        }

        if (gender != 1 && gender != 2) {
            throw new IllegalArgumentException("gender must be 1 for male or 2 for female");
        }
    }

    private RniDailyTarget resolveRniDailyTarget(Integer ageMonths, Integer gender) {
        boolean male = gender == 1;

        /*
         * Infants
         * Energy and protein come from RNI Malaysia 2017 Summary Table.
         * Fat g/day ranges come from Appendix 3.5.
         *
         * Carbohydrate TEI range is not force-enforced for infants in this service.
         */
        if (ageMonths <= 2) {
            return male
                    ? new RniDailyTarget(470, 8, 21, 31, false)
                    : new RniDailyTarget(420, 8, 19, 28, false);
        }

        if (ageMonths <= 5) {
            return male
                    ? new RniDailyTarget(540, 8, 24, 36, false)
                    : new RniDailyTarget(500, 8, 22, 33, false);
        }

        if (ageMonths <= 8) {
            return male
                    ? new RniDailyTarget(630, 10, 21, 28, false)
                    : new RniDailyTarget(570, 10, 19, 25, false);
        }

        if (ageMonths <= 11) {
            return male
                    ? new RniDailyTarget(720, 10, 24, 32, false)
                    : new RniDailyTarget(660, 10, 22, 29, false);
        }

        /*
         * Children
         * 1 - 3 years  = 12 - 47 months
         * 4 - 6 years  = 48 - 83 months
         * 7 - 9 years  = 84 - 119 months
         */
        if (ageMonths < 48) {
            return male
                    ? new RniDailyTarget(980, 12, 27, 38, true)
                    : new RniDailyTarget(900, 12, 25, 35, true);
        }

        if (ageMonths < 84) {
            return male
                    ? new RniDailyTarget(1300, 16, 36, 51, true)
                    : new RniDailyTarget(1210, 16, 34, 47, true);
        }

        if (ageMonths < 120) {
            return male
                    ? new RniDailyTarget(1750, 23, 49, 68, true)
                    : new RniDailyTarget(1610, 23, 45, 63, true);
        }

        /*
         * Adolescents
         * 10 - 12 years = 120 - 155 months
         * 13 - 15 years = 156 - 191 months
         * 16 - <18 years = 192 - 215 months
         */
        if (ageMonths < 156) {
            return male
                    ? new RniDailyTarget(1930, 30, 54, 75, true)
                    : new RniDailyTarget(1710, 31, 48, 67, true);
        }

        if (ageMonths < 192) {
            return male
                    ? new RniDailyTarget(2210, 45, 61, 86, true)
                    : new RniDailyTarget(1810, 42, 50, 70, true);
        }

        if (ageMonths < 216) {
            return male
                    ? new RniDailyTarget(2340, 51, 65, 91, true)
                    : new RniDailyTarget(1890, 42, 53, 74, true);
        }

        /*
         * Adults are kept for robustness if the endpoint is reused outside child profiles.
         * Energy uses the PAL 1.6 population recommendation.
         */
        if (ageMonths < 360) {
            return male
                    ? new RniDailyTarget(2240, 62, 62, 75, true)
                    : new RniDailyTarget(1840, 53, 51, 61, true);
        }

        if (ageMonths < 720) {
            return male
                    ? new RniDailyTarget(2190, 61, 61, 73, true)
                    : new RniDailyTarget(1900, 52, 53, 63, true);
        }

        return male
                ? new RniDailyTarget(2030, 58, 56, 68, true)
                : new RniDailyTarget(1770, 50, 49, 59, true);
    }

    private FoodNutritionNeedsDto calculateFoodNutritionNeeds(RniDailyTarget target) {
        MacroTargetCandidate candidate = selectBestMacroTarget(target);

        FoodNutritionNeedsDto dto = new FoodNutritionNeedsDto();
        dto.setCal(target.calories);
        dto.setProtein(target.proteinGrams);
        dto.setFat(candidate.fatGrams);
        dto.setCarb(candidate.carbohydrateGrams);
        return dto;
    }

    /**
     * Pick an integer fat/carb combination that:
     * 1. keeps fat inside the official RNI fat g/day range;
     * 2. keeps carbohydrate inside 50% - 65% TEI where applicable;
     * 3. makes total energy match the RNI calorie target as closely as possible;
     * 4. prefers a fat value near the midpoint of the official fat gram range.
     */
    private MacroTargetCandidate selectBestMacroTarget(RniDailyTarget target) {
        MacroTargetCandidate bestCandidate = searchBestMacroTarget(target, target.enforceCarbohydrateTeiRange);

        if (bestCandidate != null) {
            return bestCandidate;
        }

        /*
         * This fallback is defensive only. With the current RNI target table,
         * children, adolescents and adults should find a valid candidate inside the carb range.
         */
        log.warn(
                "No macro candidate satisfied carbohydrate TEI range for RNI target. " +
                        "Falling back to fat-range + calorie-closest candidate. calories:{}, protein:{}, fatRange:{}-{}",
                target.calories,
                target.proteinGrams,
                target.fatMinGrams,
                target.fatMaxGrams
        );

        bestCandidate = searchBestMacroTarget(target, false);

        if (bestCandidate == null) {
            throw new IllegalStateException(
                    "Unable to calculate macro target for calories="
                            + target.calories
                            + ", protein="
                            + target.proteinGrams
                            + ", fatRange="
                            + target.fatMinGrams
                            + "-"
                            + target.fatMaxGrams
            );
        }

        return bestCandidate;
    }

    private MacroTargetCandidate searchBestMacroTarget(
            RniDailyTarget target,
            boolean enforceCarbohydrateTeiRange
    ) {
        MacroTargetCandidate bestCandidate = null;

        for (int fatGrams = target.fatMinGrams; fatGrams <= target.fatMaxGrams; fatGrams++) {
            int proteinEnergy = target.proteinGrams * KCAL_PER_GRAM_PROTEIN;
            int fatEnergy = fatGrams * KCAL_PER_GRAM_FAT;
            int remainingEnergyForCarbohydrate = target.calories - proteinEnergy - fatEnergy;

            if (remainingEnergyForCarbohydrate < 0) {
                continue;
            }

            int carbohydrateFloor = remainingEnergyForCarbohydrate / KCAL_PER_GRAM_CARBOHYDRATE;
            int carbohydrateCeiling =
                    (remainingEnergyForCarbohydrate + KCAL_PER_GRAM_CARBOHYDRATE - 1)
                            / KCAL_PER_GRAM_CARBOHYDRATE;

            bestCandidate = evaluateCandidate(
                    target,
                    fatGrams,
                    carbohydrateFloor,
                    enforceCarbohydrateTeiRange,
                    bestCandidate
            );

            if (carbohydrateCeiling != carbohydrateFloor) {
                bestCandidate = evaluateCandidate(
                        target,
                        fatGrams,
                        carbohydrateCeiling,
                        enforceCarbohydrateTeiRange,
                        bestCandidate
                );
            }
        }

        return bestCandidate;
    }

    private MacroTargetCandidate evaluateCandidate(
            RniDailyTarget target,
            int fatGrams,
            int carbohydrateGrams,
            boolean enforceCarbohydrateTeiRange,
            MacroTargetCandidate currentBest
    ) {
        if (carbohydrateGrams < 0) {
            return currentBest;
        }

        int carbohydrateEnergy = carbohydrateGrams * KCAL_PER_GRAM_CARBOHYDRATE;
        int proteinEnergy = target.proteinGrams * KCAL_PER_GRAM_PROTEIN;
        int fatEnergy = fatGrams * KCAL_PER_GRAM_FAT;
        int calculatedEnergy = carbohydrateEnergy + proteinEnergy + fatEnergy;

        double carbohydrateTeiRatio = carbohydrateEnergy / (double) target.calories;

        if (enforceCarbohydrateTeiRange
                && (carbohydrateTeiRatio + EPSILON < CARBOHYDRATE_MIN_TEI_RATIO
                || carbohydrateTeiRatio - EPSILON > CARBOHYDRATE_MAX_TEI_RATIO)) {
            return currentBest;
        }

        MacroTargetCandidate candidate = new MacroTargetCandidate(
                carbohydrateGrams,
                fatGrams,
                Math.abs(calculatedEnergy - target.calories),
                Math.abs(fatGrams - target.getFatMidpoint()),
                Math.abs(carbohydrateTeiRatio - CARBOHYDRATE_TARGET_TEI_RATIO)
        );

        if (isBetterCandidate(candidate, currentBest)) {
            return candidate;
        }

        return currentBest;
    }

    private boolean isBetterCandidate(
            MacroTargetCandidate candidate,
            MacroTargetCandidate currentBest
    ) {
        if (candidate == null) {
            return false;
        }

        if (currentBest == null) {
            return true;
        }

        if (candidate.energyGap != currentBest.energyGap) {
            return candidate.energyGap < currentBest.energyGap;
        }

        int fatMidpointCompare = Double.compare(
                candidate.fatMidpointDistance,
                currentBest.fatMidpointDistance
        );
        if (fatMidpointCompare != 0) {
            return fatMidpointCompare < 0;
        }

        int carbohydrateTargetCompare = Double.compare(
                candidate.carbohydrateTargetDistance,
                currentBest.carbohydrateTargetDistance
        );
        if (carbohydrateTargetCompare != 0) {
            return carbohydrateTargetCompare < 0;
        }

        if (candidate.fatGrams != currentBest.fatGrams) {
            return candidate.fatGrams < currentBest.fatGrams;
        }

        return candidate.carbohydrateGrams < currentBest.carbohydrateGrams;
    }

    private static class RniDailyTarget {
        private final int calories;
        private final int proteinGrams;
        private final int fatMinGrams;
        private final int fatMaxGrams;
        private final boolean enforceCarbohydrateTeiRange;

        private RniDailyTarget(
                int calories,
                int proteinGrams,
                int fatMinGrams,
                int fatMaxGrams,
                boolean enforceCarbohydrateTeiRange
        ) {
            this.calories = calories;
            this.proteinGrams = proteinGrams;
            this.fatMinGrams = fatMinGrams;
            this.fatMaxGrams = fatMaxGrams;
            this.enforceCarbohydrateTeiRange = enforceCarbohydrateTeiRange;
        }

        private double getFatMidpoint() {
            return (fatMinGrams + fatMaxGrams) / 2.0D;
        }
    }

    private static class MacroTargetCandidate {
        private final int carbohydrateGrams;
        private final int fatGrams;
        private final int energyGap;
        private final double fatMidpointDistance;
        private final double carbohydrateTargetDistance;

        private MacroTargetCandidate(
                int carbohydrateGrams,
                int fatGrams,
                int energyGap,
                double fatMidpointDistance,
                double carbohydrateTargetDistance
        ) {
            this.carbohydrateGrams = carbohydrateGrams;
            this.fatGrams = fatGrams;
            this.energyGap = energyGap;
            this.fatMidpointDistance = fatMidpointDistance;
            this.carbohydrateTargetDistance = carbohydrateTargetDistance;
        }
    }
}
