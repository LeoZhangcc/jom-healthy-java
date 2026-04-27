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

        List<String> goodEn = new ArrayList<>();
        List<String> warningEn = new ArrayList<>();

        List<String> goodCn = new ArrayList<>();
        List<String> warningCn = new ArrayList<>();

        List<String> goodMs = new ArrayList<>();
        List<String> warningMs = new ArrayList<>();

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

        // 1. 热量
        if (energy.compareTo(bd("500")) > 0) {
            score -= 25;
            addWarning(
                    warningEn, warningCn, warningMs,
                    "it is very high in calories",
                    "热量非常高",
                    "kalorinya sangat tinggi"
            );
        } else if (energy.compareTo(bd("300")) > 0) {
            score -= 15;
            addWarning(
                    warningEn, warningCn, warningMs,
                    "it is high in calories",
                    "热量偏高",
                    "kalorinya tinggi"
            );
        } else if (energy.compareTo(bd("150")) <= 0) {
            score += 5;
            addGood(
                    goodEn, goodCn, goodMs,
                    "it is relatively low in calories",
                    "热量较低",
                    "kalorinya lebih rendah"
            );
        }

        // 2. 脂肪
        if (fat.compareTo(bd("30")) > 0) {
            if (isOilOrFat) {
                score -= 10;
                addWarning(
                        warningEn, warningCn, warningMs,
                        "it is an oil or fat-based food, so it should be used in small amounts",
                        "属于油脂类食物，建议控制用量",
                        "ia makanan berasaskan minyak atau lemak, jadi perlu diambil dalam jumlah kecil"
                );
            } else {
                score -= 25;
                addWarning(
                        warningEn, warningCn, warningMs,
                        "it is very high in fat",
                        "脂肪含量非常高",
                        "lemaknya sangat tinggi"
                );
            }
        } else if (fat.compareTo(bd("17.5")) > 0) {
            if (isOilOrFat) {
                score -= 5;
                addWarning(
                        warningEn, warningCn, warningMs,
                        "it is high in fat, so portion size matters",
                        "脂肪较高，需要注意份量",
                        "lemaknya tinggi, jadi saiz hidangan perlu dikawal"
                );
            } else {
                score -= 15;
                addWarning(
                        warningEn, warningCn, warningMs,
                        "it is high in fat",
                        "脂肪含量较高",
                        "lemaknya tinggi"
                );
            }
        } else if (fat.compareTo(bd("3")) <= 0) {
            score += 5;
            addGood(
                    goodEn, goodCn, goodMs,
                    "it is low in fat",
                    "脂肪较低",
                    "rendah lemak"
            );
        }

        // 3. 钠
        if (sodium.compareTo(bd("1000")) > 0) {
            score -= 25;
            addWarning(
                    warningEn, warningCn, warningMs,
                    "it is very high in sodium",
                    "钠含量非常高",
                    "natriumnya sangat tinggi"
            );
        } else if (sodium.compareTo(bd("600")) > 0) {
            score -= 15;
            addWarning(
                    warningEn, warningCn, warningMs,
                    "it is high in sodium",
                    "钠含量较高",
                    "natriumnya tinggi"
            );
        } else if (sodium.compareTo(bd("120")) <= 0) {
            score += 5;
            addGood(
                    goodEn, goodCn, goodMs,
                    "it is low in sodium",
                    "钠含量较低",
                    "rendah natrium"
            );
        }

        if (isCondiment && sodium.compareTo(bd("600")) > 0) {
            score -= 5;
            addWarning(
                    warningEn, warningCn, warningMs,
                    "it is a condiment, so it is better to use only a small amount",
                    "属于调味品类，更适合少量使用",
                    "ia sejenis perasa, jadi lebih baik digunakan dalam jumlah kecil"
            );
        }

        // 4. 碳水 + 纤维组合
        if (carb.compareTo(bd("60")) > 0 && fibre.compareTo(bd("3")) < 0) {
            score -= 10;
            addWarning(
                    warningEn, warningCn, warningMs,
                    "it is high in carbohydrates but low in fibre",
                    "碳水较高但膳食纤维较低",
                    "karbohidratnya tinggi tetapi seratnya rendah"
            );
        } else if (carb.compareTo(bd("60")) > 0 && fibre.compareTo(bd("6")) >= 0) {
            score += 5;
            addGood(
                    goodEn, goodCn, goodMs,
                    "it provides carbohydrates together with a good amount of fibre",
                    "虽然碳水较高，但膳食纤维也较丰富",
                    "ia membekalkan karbohidrat bersama serat yang baik"
            );
        }

        // 5. 蛋白质
        if (protein.compareTo(bd("10")) >= 0) {
            score += 10;
            addGood(
                    goodEn, goodCn, goodMs,
                    "it provides a good amount of protein",
                    "蛋白质含量不错",
                    "ia membekalkan protein yang baik"
            );
        } else if (protein.compareTo(bd("5")) >= 0) {
            score += 5;
            addGood(
                    goodEn, goodCn, goodMs,
                    "it contains some protein",
                    "含有一定蛋白质",
                    "ia mengandungi sedikit protein"
            );
        }

        // 6. 膳食纤维
        if (fibre.compareTo(bd("6")) >= 0) {
            score += 10;
            addGood(
                    goodEn, goodCn, goodMs,
                    "it is rich in dietary fibre",
                    "膳食纤维较丰富",
                    "ia kaya dengan serat diet"
            );
        } else if (fibre.compareTo(bd("3")) >= 0) {
            score += 5;
            addGood(
                    goodEn, goodCn, goodMs,
                    "it contains some dietary fibre",
                    "含有一定膳食纤维",
                    "ia mengandungi sedikit serat diet"
            );
        }

        // 7. 微量营养素
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
            addGood(
                    goodEn, goodCn, goodMs,
                    "it contains useful vitamins or minerals",
                    "含有一定维生素或矿物质",
                    "ia mengandungi vitamin atau mineral yang bermanfaat"
            );
        }

        // 8. 胆固醇
        if (cholesterol.compareTo(bd("100")) > 0) {
            score -= 5;
            addWarning(
                    warningEn, warningCn, warningMs,
                    "it is relatively high in cholesterol",
                    "胆固醇偏高",
                    "kolesterolnya agak tinggi"
            );
        }

        score = Math.max(0, Math.min(100, score));

        dto.setHealthScore(score);
        dto.setHealthGrade(toGrade(score));
        dto.setHealthLabel(toLabel(score));

        dto.setHealthReasonEn(buildReasonEn(score, goodEn, warningEn));
        dto.setHealthReasonCn(buildReasonCn(score, goodCn, warningCn));
        dto.setHealthReasonMs(buildReasonMs(score, goodMs, warningMs));

        return dto;
    }

    private static void addGood(
            List<String> goodEn,
            List<String> goodCn,
            List<String> goodMs,
            String en,
            String cn,
            String ms
    ) {
        goodEn.add(en);
        goodCn.add(cn);
        goodMs.add(ms);
    }

    private static void addWarning(
            List<String> warningEn,
            List<String> warningCn,
            List<String> warningMs,
            String en,
            String cn,
            String ms
    ) {
        warningEn.add(en);
        warningCn.add(cn);
        warningMs.add(ms);
    }

    private static String buildReasonEn(int score, List<String> good, List<String> warning) {
        if (good.isEmpty() && warning.isEmpty()) {
            return defaultReasonEn(score);
        }

        StringBuilder sb = new StringBuilder();

        if (!good.isEmpty()) {
            sb.append("This food has some positive nutrition points because ")
                    .append(joinEn(good))
                    .append(". ");
        }

        if (!warning.isEmpty()) {
            sb.append("However, ")
                    .append(joinEn(warning))
                    .append(", so it is better to eat it in a moderate portion.");
        } else {
            sb.append("It can be included as part of a balanced diet.");
        }

        return sb.toString();
    }

    private static String buildReasonCn(int score, List<String> good, List<String> warning) {
        if (good.isEmpty() && warning.isEmpty()) {
            return defaultReasonCn(score);
        }

        StringBuilder sb = new StringBuilder();

        if (!good.isEmpty()) {
            sb.append("这种食物有一些营养优点，比如")
                    .append(String.join("、", good))
                    .append("。");
        }

        if (!warning.isEmpty()) {
            sb.append("不过")
                    .append(String.join("、", warning))
                    .append("，建议控制份量，不要一次吃太多。");
        } else {
            sb.append("可以作为均衡饮食的一部分。");
        }

        return sb.toString();
    }

    private static String buildReasonMs(int score, List<String> good, List<String> warning) {
        if (good.isEmpty() && warning.isEmpty()) {
            return defaultReasonMs(score);
        }

        StringBuilder sb = new StringBuilder();

        if (!good.isEmpty()) {
            sb.append("Makanan ini mempunyai beberapa kelebihan nutrisi kerana ")
                    .append(joinMs(good))
                    .append(". ");
        }

        if (!warning.isEmpty()) {
            sb.append("Namun, ")
                    .append(joinMs(warning))
                    .append(", jadi lebih baik diambil dalam jumlah sederhana.");
        } else {
            sb.append("Ia boleh dimasukkan sebagai sebahagian daripada diet seimbang.");
        }

        return sb.toString();
    }

    private static String joinEn(List<String> list) {
        if (list.size() == 1) {
            return list.get(0);
        }

        if (list.size() == 2) {
            return list.get(0) + " and " + list.get(1);
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {
            if (i == list.size() - 1) {
                sb.append("and ").append(list.get(i));
            } else {
                sb.append(list.get(i)).append(", ");
            }
        }

        return sb.toString();
    }

    private static String joinMs(List<String> list) {
        if (list.size() == 1) {
            return list.get(0);
        }

        if (list.size() == 2) {
            return list.get(0) + " dan " + list.get(1);
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {
            if (i == list.size() - 1) {
                sb.append("dan ").append(list.get(i));
            } else {
                sb.append(list.get(i)).append(", ");
            }
        }

        return sb.toString();
    }

    private static String defaultReasonEn(int score) {
        if (score >= 85) {
            return "This food looks like a healthy choice and can be included as part of a balanced diet.";
        }
        if (score >= 70) {
            return "This food is generally a good choice, but portion size still matters.";
        }
        if (score >= 55) {
            return "This food is moderate overall. It can be eaten sometimes and should be balanced with healthier foods.";
        }
        if (score >= 40) {
            return "This food is less ideal for frequent intake. Try to keep the portion small and balance it with vegetables, fruits, or protein-rich foods.";
        }
        return "This food is not the best choice for regular intake. It is better to eat it only occasionally.";
    }

    private static String defaultReasonCn(int score) {
        if (score >= 85) {
            return "这种食物整体比较健康，可以作为均衡饮食的一部分。";
        }
        if (score >= 70) {
            return "这种食物整体还不错，但仍然需要注意份量。";
        }
        if (score >= 55) {
            return "这种食物整体属于中等水平，可以偶尔吃，但建议搭配更健康的食物。";
        }
        if (score >= 40) {
            return "这种食物不太适合经常吃，建议减少份量，并搭配蔬菜、水果或优质蛋白。";
        }
        return "这种食物不建议经常吃，更适合作为偶尔食用的食物。";
    }

    private static String defaultReasonMs(int score) {
        if (score >= 85) {
            return "Makanan ini kelihatan sebagai pilihan yang sihat dan boleh dimasukkan dalam diet seimbang.";
        }
        if (score >= 70) {
            return "Makanan ini secara umum adalah pilihan yang baik, tetapi saiz hidangan masih perlu dikawal.";
        }
        if (score >= 55) {
            return "Makanan ini berada pada tahap sederhana. Ia boleh dimakan sekali-sekala dan perlu diseimbangkan dengan makanan yang lebih sihat.";
        }
        if (score >= 40) {
            return "Makanan ini kurang sesuai untuk diambil dengan kerap. Cuba ambil dalam jumlah kecil dan seimbangkan dengan sayur, buah atau makanan tinggi protein.";
        }
        return "Makanan ini kurang sesuai untuk pengambilan harian. Lebih baik dimakan sekali-sekala sahaja.";
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
            return "Healthy";
        }
        if (score >= 70) {
            return "Good";
        }
        if (score >= 55) {
            return "Moderate";
        }
        if (score >= 40) {
            return "Less healthy";
        }
        return "Unhealthy";
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