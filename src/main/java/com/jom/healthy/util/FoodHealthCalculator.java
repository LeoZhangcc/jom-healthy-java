package com.jom.healthy.util;

import com.jom.healthy.dto.FoodNutritionDto;
import com.jom.healthy.entity.FoodNutrition;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class FoodHealthCalculator {

    public static FoodNutritionDto calculate(FoodNutrition food) {
        FoodNutritionDto dto = new FoodNutritionDto();
        BeanUtils.copyProperties(food, dto);

        int score = 70;

        List<String> warningKeys = new ArrayList<>();
        List<String> goodKeys = new ArrayList<>();

        BigDecimal energy = n(food.getEnergyKcal());
        BigDecimal protein = n(food.getProteinG());
        BigDecimal fat = n(food.getFatG());
        BigDecimal carb = n(food.getCarbohydrateG());
        BigDecimal fibre = n(food.getFibreG());
        BigDecimal sodium = n(food.getSodiumNaMg());

        BigDecimal potassium = n(food.getPotassiumKMg());
        BigDecimal calcium = n(food.getCalciumCaMg());
        BigDecimal iron = n(food.getIronFeMg());
        BigDecimal vitaminC = n(food.getVitaminCMg());
        BigDecimal cholesterol = n(food.getCholesterolMg());

        String group = food.getFoodGroup() == null ? "" : food.getFoodGroup().toLowerCase();
        String name = food.getFoodNameEn() == null ? "" : food.getFoodNameEn().toLowerCase();

        boolean isOilOrFat = group.contains("oil")
                || group.contains("fat")
                || name.contains("oil")
                || name.contains("butter")
                || name.contains("margarine");

        boolean isCondiment = group.contains("condiment")
                || group.contains("sauce")
                || group.contains("seasoning")
                || name.contains("sauce")
                || name.contains("salt")
                || name.contains("ketchup");

        // 1. Calories
        if (energy.compareTo(bd("500")) > 0) {
            score -= 25;
            warningKeys.add("very_high_calorie");
        } else if (energy.compareTo(bd("300")) > 0) {
            score -= 15;
            warningKeys.add("high_calorie");
        } else if (energy.compareTo(bd("150")) <= 0) {
            score += 5;
            goodKeys.add("low_calorie");
        }

        // 2. Fat
        if (fat.compareTo(bd("30")) > 0) {
            if (isOilOrFat) {
                score -= 10;
                warningKeys.add("oil_small_amount");
            } else {
                score -= 25;
                warningKeys.add("very_high_fat");
            }
        } else if (fat.compareTo(bd("17.5")) > 0) {
            if (isOilOrFat) {
                score -= 5;
                warningKeys.add("oil_small_amount");
            } else {
                score -= 15;
                warningKeys.add("high_fat");
            }
        } else if (fat.compareTo(bd("3")) <= 0) {
            score += 5;
            goodKeys.add("low_fat");
        }

        // 3. Sodium
        if (sodium.compareTo(bd("1000")) > 0) {
            score -= 25;
            warningKeys.add("very_high_sodium");
        } else if (sodium.compareTo(bd("600")) > 0) {
            score -= 15;
            warningKeys.add("high_sodium");
        } else if (sodium.compareTo(bd("120")) <= 0) {
            score += 5;
            goodKeys.add("low_sodium");
        }

        if (isCondiment && sodium.compareTo(bd("600")) > 0) {
            score -= 5;
            warningKeys.add("condiment_limit");
        }

        // 4. Carbs + fibre
        if (carb.compareTo(bd("60")) > 0 && fibre.compareTo(bd("3")) < 0) {
            score -= 10;
            warningKeys.add("high_carb_low_fibre");
        } else if (carb.compareTo(bd("60")) > 0 && fibre.compareTo(bd("6")) >= 0) {
            score += 5;
            goodKeys.add("high_fibre_carb");
        }

        // 5. Protein
        if (protein.compareTo(bd("10")) >= 0) {
            score += 10;
            goodKeys.add("high_protein");
        } else if (protein.compareTo(bd("5")) >= 0) {
            score += 5;
            goodKeys.add("some_protein");
        }

        // 6. Fibre
        if (fibre.compareTo(bd("6")) >= 0) {
            score += 10;
            goodKeys.add("high_fibre");
        } else if (fibre.compareTo(bd("3")) >= 0) {
            score += 5;
            goodKeys.add("some_fibre");
        }

        // 7. Vitamins and minerals
        boolean hasMicronutrient = false;

        if (potassium.compareTo(bd("300")) >= 0) {
            score += 5;
            hasMicronutrient = true;
        }

        if (calcium.compareTo(bd("100")) >= 0) {
            score += 5;
            hasMicronutrient = true;
        }

        if (iron.compareTo(bd("2")) >= 0) {
            score += 5;
            hasMicronutrient = true;
        }

        if (vitaminC.compareTo(bd("20")) >= 0) {
            score += 5;
            hasMicronutrient = true;
        }

        if (hasMicronutrient) {
            goodKeys.add("vitamin_mineral");
        }

        // 8. Cholesterol
        if (cholesterol.compareTo(bd("100")) > 0) {
            score -= 5;
            warningKeys.add("high_cholesterol");
        }

        score = Math.max(0, Math.min(100, score));

        dto.setHealthScore(score);
        dto.setHealthGrade(toGrade(score));
        dto.setHealthLabel(toLabel(score));

        dto.setHealthReasonEn(buildShortReasonEn(score, goodKeys, warningKeys));
        dto.setHealthReasonCn(buildShortReasonCn(score, goodKeys, warningKeys));
        dto.setHealthReasonMs(buildShortReasonMs(score, goodKeys, warningKeys));

        dto.setParentTipsEn(buildTipsEn(score, goodKeys, warningKeys));
        dto.setParentTipsCn(buildTipsCn(score, goodKeys, warningKeys));
        dto.setParentTipsMs(buildTipsMs(score, goodKeys, warningKeys));

        return dto;
    }

    private static String buildShortReasonEn(int score, List<String> good, List<String> warning) {
        if (score >= 85) {
            return "This is a healthy choice for children.";
        }
        if (score >= 70) {
            return "This is a good choice, but portion size still matters.";
        }
        if (score >= 55) {
            return "This food is acceptable, but it should be balanced with healthier foods.";
        }
        if (score >= 40) {
            return "This food is less ideal for frequent intake.";
        }
        return "This food is better eaten only occasionally.";
    }

    private static String buildShortReasonCn(int score, List<String> good, List<String> warning) {
        if (score >= 85) {
            return "这是比较适合儿童的健康选择。";
        }
        if (score >= 70) {
            return "这是不错的选择，但仍然需要注意份量。";
        }
        if (score >= 55) {
            return "这种食物整体可以接受，但建议搭配更健康的食物。";
        }
        if (score >= 40) {
            return "这种食物不太适合经常吃。";
        }
        return "这种食物更适合偶尔吃，不建议经常食用。";
    }

    private static String buildShortReasonMs(int score, List<String> good, List<String> warning) {
        if (score >= 85) {
            return "Ini adalah pilihan yang sihat untuk kanak-kanak.";
        }
        if (score >= 70) {
            return "Ini pilihan yang baik, tetapi saiz hidangan masih perlu dikawal.";
        }
        if (score >= 55) {
            return "Makanan ini boleh diterima, tetapi perlu diseimbangkan dengan makanan yang lebih sihat.";
        }
        if (score >= 40) {
            return "Makanan ini kurang sesuai untuk diambil dengan kerap.";
        }
        return "Makanan ini lebih sesuai dimakan sekali-sekala sahaja.";
    }

    private static List<String> buildTipsEn(int score, List<String> good, List<String> warning) {
        List<String> tips = new ArrayList<>();

        if (warning.contains("very_high_sodium") || warning.contains("high_sodium") || warning.contains("condiment_limit")) {
            tips.add("Avoid adding extra salt, soy sauce, or other salty sauces.");
        }

        if (warning.contains("very_high_fat") || warning.contains("high_fat") || warning.contains("oil_small_amount")) {
            tips.add("Choose grilled, steamed, or boiled versions when possible.");
        }

        if (warning.contains("very_high_calorie") || warning.contains("high_calorie")) {
            tips.add("Keep the portion moderate, especially for younger children.");
        }

        if (warning.contains("high_carb_low_fibre")) {
            tips.add("Pair it with vegetables, fruit, or whole grains for more fibre.");
        }

        if (good.contains("high_protein") || good.contains("some_protein")) {
            tips.add("Pair it with vegetables or whole grains for a more balanced meal.");
        }

        if (good.contains("high_fibre") || good.contains("some_fibre")) {
            tips.add("This can support digestion, but still serve an age-appropriate portion.");
        }

        if (tips.isEmpty()) {
            tips.add("Pair with fresh vegetables or fruit for more nutrients.");
            tips.add("Serve an age-appropriate portion for your child.");
            tips.add("Choose grilled, steamed, or boiled options when possible.");
        }

        return limitTips(tips);
    }

    private static List<String> buildTipsCn(int score, List<String> good, List<String> warning) {
        List<String> tips = new ArrayList<>();

        if (warning.contains("very_high_sodium") || warning.contains("high_sodium") || warning.contains("condiment_limit")) {
            tips.add("避免额外加入盐、酱油或其他高钠酱料。");
        }

        if (warning.contains("very_high_fat") || warning.contains("high_fat") || warning.contains("oil_small_amount")) {
            tips.add("能选择蒸、煮、烤时，尽量少选油炸。");
        }

        if (warning.contains("very_high_calorie") || warning.contains("high_calorie")) {
            tips.add("注意控制份量，尤其是年龄较小的儿童。");
        }

        if (warning.contains("high_carb_low_fibre")) {
            tips.add("建议搭配蔬菜、水果或全谷物，增加膳食纤维。");
        }

        if (good.contains("high_protein") || good.contains("some_protein")) {
            tips.add("可以搭配蔬菜或全谷物，让这一餐更均衡。");
        }

        if (good.contains("high_fibre") || good.contains("some_fibre")) {
            tips.add("有助于增加纤维摄入，但仍要注意儿童适量食用。");
        }

        if (tips.isEmpty()) {
            tips.add("建议搭配新鲜蔬菜或水果，增加营养。");
            tips.add("根据孩子年龄控制合适份量。");
            tips.add("能选择蒸、煮、烤时，尽量少选油炸。");
        }

        return limitTips(tips);
    }

    private static List<String> buildTipsMs(int score, List<String> good, List<String> warning) {
        List<String> tips = new ArrayList<>();

        if (warning.contains("very_high_sodium") || warning.contains("high_sodium") || warning.contains("condiment_limit")) {
            tips.add("Elakkan menambah garam, kicap atau sos yang tinggi natrium.");
        }

        if (warning.contains("very_high_fat") || warning.contains("high_fat") || warning.contains("oil_small_amount")) {
            tips.add("Pilih makanan yang dipanggang, dikukus atau direbus jika boleh.");
        }

        if (warning.contains("very_high_calorie") || warning.contains("high_calorie")) {
            tips.add("Kawal saiz hidangan, terutamanya untuk kanak-kanak yang lebih kecil.");
        }

        if (warning.contains("high_carb_low_fibre")) {
            tips.add("Padankan dengan sayur, buah atau bijirin penuh untuk menambah serat.");
        }

        if (good.contains("high_protein") || good.contains("some_protein")) {
            tips.add("Padankan dengan sayur atau bijirin penuh untuk hidangan yang lebih seimbang.");
        }

        if (good.contains("high_fibre") || good.contains("some_fibre")) {
            tips.add("Ia boleh membantu pengambilan serat, tetapi hidangkan dalam jumlah yang sesuai.");
        }

        if (tips.isEmpty()) {
            tips.add("Padankan dengan sayur atau buah segar untuk lebih nutrien.");
            tips.add("Hidangkan mengikut saiz hidangan yang sesuai untuk umur anak.");
            tips.add("Pilih kaedah panggang, kukus atau rebus jika boleh.");
        }

        return limitTips(tips);
    }

    private static List<String> limitTips(List<String> tips) {
        List<String> result = new ArrayList<>();

        for (String tip : tips) {
            if (!result.contains(tip)) {
                result.add(tip);
            }

            if (result.size() >= 3) {
                break;
            }
        }

        return result;
    }

    private static String toGrade(int score) {
        if (score >= 85) {
            return "A";
        }
        if (score >= 70) {
            return "B";
        }
        if (score >= 55) {
            return "C";
        }
        if (score >= 40) {
            return "D";
        }
        return "E";
    }

    private static String toLabel(int score) {
        if (score >= 85) {
            return "Healthy Choice";
        }
        if (score >= 70) {
            return "Good Choice";
        }
        if (score >= 55) {
            return "Moderate Choice";
        }
        if (score >= 40) {
            return "Limit Often";
        }
        return "Occasional Only";
    }

    private static BigDecimal n(Number value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(value.doubleValue()).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

}