package com.jom.healthy.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MeasureToGramConverter {

    private MeasureToGramConverter() {
    }

    public static BigDecimal convertToGram(String measure, String ingredientName, String normalizedName) {
        if (measure == null || measure.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        String raw = measure.trim();
        String text = normalizeText(raw);
        String ingredient = normalizeText(
                normalizedName != null && !normalizedName.trim().isEmpty()
                        ? normalizedName
                        : ingredientName
        );

        if (isNonWeightMeasure(text)) {
            return BigDecimal.ZERO;
        }

        // 1. 处理 "2 x 400g"、"2 (460g)"、"1 x 300ml"
        BigDecimal multiPack = parseMultiPack(text);
        if (multiPack.compareTo(BigDecimal.ZERO) > 0) {
            return multiPack;
        }

        // 2. 优先解析括号中的 gram，例如 "8 ounces (230 grams)"
        BigDecimal gramInBracket = parseGramInBracket(text);
        if (gramInBracket.compareTo(BigDecimal.ZERO) > 0) {
            return gramInBracket;
        }

        // 3. 处理 "175g/6oz" 这种，优先取 g
        BigDecimal directMetric = parseDirectMetric(text);
        if (directMetric.compareTo(BigDecimal.ZERO) > 0) {
            return directMetric;
        }

        // 4. 处理 imperial 单位：lb / pound / oz / ounce / pint / quart
        BigDecimal imperial = parseImperial(text);
        if (imperial.compareTo(BigDecimal.ZERO) > 0) {
            return imperial;
        }

        // 5. 处理 cup / tbsp / tsp
        BigDecimal spoonCup = parseSpoonCup(text, ingredient);
        if (spoonCup.compareTo(BigDecimal.ZERO) > 0) {
            return spoonCup;
        }

        // 6. 处理 clove / slice / piece / can / packet / jar / bottle / bunch / handful 等
        BigDecimal itemBased = parseItemBased(text, ingredient);
        if (itemBased.compareTo(BigDecimal.ZERO) > 0) {
            return itemBased;
        }

        // 7. 纯数字，例如 "1", "2", "12"
        BigDecimal plainNumber = parsePlainNumber(text, ingredient);
        if (plainNumber.compareTo(BigDecimal.ZERO) > 0) {
            return plainNumber;
        }

        return BigDecimal.ZERO;
    }

    private static String normalizeText(String text) {
        if (text == null) {
            return "";
        }

        return text.toLowerCase(Locale.ROOT)
                .replace("½", "1/2")
                .replace("¼", "1/4")
                .replace("¾", "3/4")
                .replace("⅓", "1/3")
                .replace("⅔", "2/3")
                .replace("–", "-")
                .replace("—", "-")
                .replace(",", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean isNonWeightMeasure(String text) {
        return text.contains("to serve")
                || text.contains("to taste")
                || text.contains("for brushing")
                || text.contains("for frying")
                || text.contains("for greasing")
                || text.contains("garnish")
                || text.contains("dusting")
                || text.contains("sprinkling")
                || text.contains("sprinking")
                || text.contains("topping")
                || text.contains("as required")
                || text.equals("white")
                || text.equals("ground")
                || text.equals("grated")
                || text.equals("sliced")
                || text.equals("boiled")
                || text.equals("beaten")
                || text.equals("crushed")
                || text.equals("minced")
                || text.equals("steamed")
                || text.equals("shaved")
                || text.equals("fry")
                || text.equals("top");
    }

    /**
     * 2 x 400g tins -> 800g
     * 2 x 400g -> 800g
     * 1 x 300ml -> 300g
     * 2 (460g) -> 920g
     */
    private static BigDecimal parseMultiPack(String text) {
        Pattern p1 = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*x\\s*(\\d+(?:\\.\\d+)?)\\s*(g|gram|grams|ml|milliliter|milliliters|l|litre|liter|litres|liters)");
        Matcher m1 = p1.matcher(text);
        if (m1.find()) {
            BigDecimal count = new BigDecimal(m1.group(1));
            BigDecimal value = new BigDecimal(m1.group(2));
            String unit = m1.group(3);
            return count.multiply(convertUnitToGram(value, unit)).setScale(2, RoundingMode.HALF_UP);
        }

        Pattern p2 = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*\\(\\s*(\\d+(?:\\.\\d+)?)\\s*g\\s*\\)");
        Matcher m2 = p2.matcher(text);
        if (m2.find()) {
            BigDecimal count = new BigDecimal(m2.group(1));
            BigDecimal value = new BigDecimal(m2.group(2));
            return count.multiply(value).setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO;
    }

    private static BigDecimal parseGramInBracket(String text) {
        Pattern p = Pattern.compile("\\((\\d+(?:\\.\\d+)?)\\s*(g|gram|grams)\\)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return new BigDecimal(m.group(1)).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private static BigDecimal parseDirectMetric(String text) {
        Pattern p = Pattern.compile("(\\d+(?:\\.\\d+)?(?:\\s+\\d+/\\d+)?|\\d+/\\d+)\\s*(kg|g|gram|grams|ml|milliliter|milliliters|l|litre|liter|litres|liters)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            BigDecimal value = parseMixedNumber(m.group(1));
            String unit = m.group(2);
            return convertUnitToGram(value, unit).setScale(2, RoundingMode.HALF_UP);
        }

        // 处理 "200 g" 这种
        Pattern p2 = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s+g\\b");
        Matcher m2 = p2.matcher(text);
        if (m2.find()) {
            return new BigDecimal(m2.group(1)).setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO;
    }

    private static BigDecimal convertUnitToGram(BigDecimal value, String unit) {
        if (unit == null) {
            return BigDecimal.ZERO;
        }

        if (unit.equals("kg")) {
            return value.multiply(new BigDecimal("1000"));
        }

        if (unit.equals("g") || unit.equals("gram") || unit.equals("grams")) {
            return value;
        }

        // 默认水密度近似，ml ≈ g。油/酱料会有误差，但用于估算可接受
        if (unit.equals("ml") || unit.equals("milliliter") || unit.equals("milliliters")) {
            return value;
        }

        if (unit.equals("l") || unit.equals("litre") || unit.equals("liter")
                || unit.equals("litres") || unit.equals("liters")) {
            return value.multiply(new BigDecimal("1000"));
        }

        return BigDecimal.ZERO;
    }

    private static BigDecimal parseImperial(String text) {
        Pattern p = Pattern.compile("(\\d+(?:\\.\\d+)?(?:\\s+\\d+/\\d+)?|\\d+/\\d+)\\s*(lb|lbs|pound|pounds|oz|ounce|ounces|pint|pints|quart|quarts|qt)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            BigDecimal value = parseMixedNumber(m.group(1));
            String unit = m.group(2);

            if (unit.equals("lb") || unit.equals("lbs") || unit.equals("pound") || unit.equals("pounds")) {
                return value.multiply(new BigDecimal("453.592")).setScale(2, RoundingMode.HALF_UP);
            }

            if (unit.equals("oz") || unit.equals("ounce") || unit.equals("ounces")) {
                return value.multiply(new BigDecimal("28.3495")).setScale(2, RoundingMode.HALF_UP);
            }

            if (unit.equals("pint") || unit.equals("pints")) {
                return value.multiply(new BigDecimal("473")).setScale(2, RoundingMode.HALF_UP);
            }

            if (unit.equals("quart") || unit.equals("quarts") || unit.equals("qt")) {
                return value.multiply(new BigDecimal("946")).setScale(2, RoundingMode.HALF_UP);
            }
        }

        return BigDecimal.ZERO;
    }

    private static BigDecimal parseSpoonCup(String text, String ingredient) {
        BigDecimal quantity = extractFirstNumber(text);
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (text.contains("tablespoon") || text.contains("tbsp") || text.contains("tblsp")
                || text.contains("tbls") || text.contains("tbs") || text.contains("tbsp")) {
            return quantity.multiply(gramsPerTablespoon(ingredient)).setScale(2, RoundingMode.HALF_UP);
        }

        if (text.contains("teaspoon") || text.contains("tsp")) {
            return quantity.multiply(gramsPerTeaspoon(ingredient)).setScale(2, RoundingMode.HALF_UP);
        }

        if (text.contains("cup") || text.contains("cups")) {
            return quantity.multiply(gramsPerCup(ingredient)).setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO;
    }

    private static BigDecimal parseItemBased(String text, String ingredient) {
        BigDecimal quantity = extractFirstNumber(text);
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            quantity = BigDecimal.ONE;
        }

        if (text.contains("pinch") || text.contains("pinches")) {
            return quantity.multiply(new BigDecimal("0.36")).setScale(2, RoundingMode.HALF_UP);
        }

        if (text.contains("dash")) {
            return quantity.multiply(new BigDecimal("0.6")).setScale(2, RoundingMode.HALF_UP);
        }

        if (text.contains("splash")) {
            return quantity.multiply(new BigDecimal("15")).setScale(2, RoundingMode.HALF_UP);
        }

        if (text.contains("shot")) {
            return quantity.multiply(new BigDecimal("44")).setScale(2, RoundingMode.HALF_UP);
        }

        if (text.contains("clove") || text.contains("cloves")) {
            return quantity.multiply(gramsPerClove(ingredient)).setScale(2, RoundingMode.HALF_UP);
        }

        if (text.contains("slice") || text.contains("slices")) {
            return quantity.multiply(gramsPerSlice(ingredient)).setScale(2, RoundingMode.HALF_UP);
        }

        if (text.contains("piece") || text.contains("pieces")) {
            return quantity.multiply(gramsPerPiece(ingredient)).setScale(2, RoundingMode.HALF_UP);
        }

        if (text.contains("medium")) {
            return quantity.multiply(gramsPerSize(ingredient, "medium")).setScale(2, RoundingMode.HALF_UP);
        }

        if (text.contains("large")) {
            return quantity.multiply(gramsPerSize(ingredient, "large")).setScale(2, RoundingMode.HALF_UP);
        }

        if (text.contains("small")) {
            return quantity.multiply(gramsPerSize(ingredient, "small")).setScale(2, RoundingMode.HALF_UP);
        }

        if (text.contains("can") || text.contains("tin")) {
            return quantity.multiply(new BigDecimal("400")).setScale(2, RoundingMode.HALF_UP);
        }

        if (text.contains("jar")) {
            return quantity.multiply(new BigDecimal("350")).setScale(2, RoundingMode.HALF_UP);
        }

        if (text.contains("packet") || text.contains("pack") || text.contains("package")) {
            return quantity.multiply(new BigDecimal("250")).setScale(2, RoundingMode.HALF_UP);
        }

        if (text.contains("bottle")) {
            return quantity.multiply(new BigDecimal("500")).setScale(2, RoundingMode.HALF_UP);
        }

        if (text.contains("bunch")) {
            return quantity.multiply(new BigDecimal("80")).setScale(2, RoundingMode.HALF_UP);
        }

        if (text.contains("handful") || text.contains("handfull")) {
            return quantity.multiply(new BigDecimal("30")).setScale(2, RoundingMode.HALF_UP);
        }

        if (text.contains("leaf") || text.contains("leaves")) {
            return quantity.multiply(new BigDecimal("1")).setScale(2, RoundingMode.HALF_UP);
        }

        if (text.contains("sprig") || text.contains("sprigs")) {
            return quantity.multiply(new BigDecimal("2")).setScale(2, RoundingMode.HALF_UP);
        }

        if (text.contains("head") || text.contains("bulb")) {
            return quantity.multiply(gramsPerHeadOrBulb(ingredient)).setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO;
    }

    private static BigDecimal parsePlainNumber(String text, String ingredient) {
        if (!text.matches("\\d+(?:\\.\\d+)?|\\d+/\\d+")) {
            return BigDecimal.ZERO;
        }

        BigDecimal quantity = parseMixedNumber(text);

        if (ingredient.contains("egg")) {
            return quantity.multiply(new BigDecimal("50")).setScale(2, RoundingMode.HALF_UP);
        }

        if (ingredient.contains("pepper")) {
            return quantity.multiply(new BigDecimal("120")).setScale(2, RoundingMode.HALF_UP);
        }

        if (ingredient.contains("onion")) {
            return quantity.multiply(new BigDecimal("110")).setScale(2, RoundingMode.HALF_UP);
        }

        if (ingredient.contains("lemon") || ingredient.contains("lime")) {
            return quantity.multiply(new BigDecimal("60")).setScale(2, RoundingMode.HALF_UP);
        }

        if (ingredient.contains("tomato")) {
            return quantity.multiply(new BigDecimal("120")).setScale(2, RoundingMode.HALF_UP);
        }

        if (ingredient.contains("potato")) {
            return quantity.multiply(new BigDecimal("150")).setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO;
    }

    private static BigDecimal gramsPerTablespoon(String ingredient) {
        if (ingredient.contains("oil")) return new BigDecimal("13.5");
        if (ingredient.contains("butter")) return new BigDecimal("14");
        if (ingredient.contains("flour")) return new BigDecimal("8");
        if (ingredient.contains("sugar")) return new BigDecimal("12.5");
        if (ingredient.contains("salt")) return new BigDecimal("18");
        if (ingredient.contains("soy sauce")) return new BigDecimal("16");
        if (ingredient.contains("sauce")) return new BigDecimal("15");
        if (ingredient.contains("cream")) return new BigDecimal("15");
        if (ingredient.contains("milk")) return new BigDecimal("15");
        if (ingredient.contains("powder")) return new BigDecimal("8");
        if (ingredient.contains("spice") || ingredient.contains("paprika") || ingredient.contains("cumin")
                || ingredient.contains("turmeric") || ingredient.contains("cinnamon")) return new BigDecimal("7");
        return new BigDecimal("15");
    }

    private static BigDecimal gramsPerTeaspoon(String ingredient) {
        if (ingredient.contains("oil")) return new BigDecimal("4.5");
        if (ingredient.contains("salt")) return new BigDecimal("6");
        if (ingredient.contains("sugar")) return new BigDecimal("4");
        if (ingredient.contains("powder")) return new BigDecimal("3");
        if (ingredient.contains("spice") || ingredient.contains("paprika") || ingredient.contains("cumin")
                || ingredient.contains("turmeric") || ingredient.contains("cinnamon")) return new BigDecimal("2.5");
        return new BigDecimal("5");
    }

    private static BigDecimal gramsPerCup(String ingredient) {
        if (ingredient.contains("flour")) return new BigDecimal("120");
        if (ingredient.contains("sugar")) return new BigDecimal("200");
        if (ingredient.contains("rice")) return new BigDecimal("185");
        if (ingredient.contains("milk") || ingredient.contains("water") || ingredient.contains("stock")) return new BigDecimal("240");
        if (ingredient.contains("oil")) return new BigDecimal("216");
        if (ingredient.contains("cheese")) return new BigDecimal("110");
        if (ingredient.contains("cream")) return new BigDecimal("240");
        if (ingredient.contains("chopped")) return new BigDecimal("150");
        return new BigDecimal("240");
    }

    private static BigDecimal gramsPerClove(String ingredient) {
        if (ingredient.contains("garlic")) return new BigDecimal("3");
        return new BigDecimal("3");
    }

    private static BigDecimal gramsPerSlice(String ingredient) {
        if (ingredient.contains("bread")) return new BigDecimal("30");
        if (ingredient.contains("bacon")) return new BigDecimal("25");
        if (ingredient.contains("cheese")) return new BigDecimal("20");
        if (ingredient.contains("ham")) return new BigDecimal("25");
        return new BigDecimal("30");
    }

    private static BigDecimal gramsPerPiece(String ingredient) {
        if (ingredient.contains("chicken")) return new BigDecimal("100");
        if (ingredient.contains("fish")) return new BigDecimal("120");
        if (ingredient.contains("egg")) return new BigDecimal("50");
        return new BigDecimal("50");
    }

    private static BigDecimal gramsPerSize(String ingredient, String size) {
        BigDecimal base;

        if (ingredient.contains("egg")) {
            base = new BigDecimal("50");
        } else if (ingredient.contains("onion")) {
            base = new BigDecimal("110");
        } else if (ingredient.contains("tomato")) {
            base = new BigDecimal("120");
        } else if (ingredient.contains("potato")) {
            base = new BigDecimal("150");
        } else if (ingredient.contains("carrot")) {
            base = new BigDecimal("60");
        } else if (ingredient.contains("pepper")) {
            base = new BigDecimal("120");
        } else if (ingredient.contains("apple")) {
            base = new BigDecimal("180");
        } else if (ingredient.contains("banana")) {
            base = new BigDecimal("118");
        } else if (ingredient.contains("lemon") || ingredient.contains("lime")) {
            base = new BigDecimal("60");
        } else {
            base = new BigDecimal("100");
        }

        if ("small".equals(size)) {
            return base.multiply(new BigDecimal("0.75"));
        }

        if ("large".equals(size)) {
            return base.multiply(new BigDecimal("1.25"));
        }

        return base;
    }

    private static BigDecimal gramsPerHeadOrBulb(String ingredient) {
        if (ingredient.contains("garlic")) return new BigDecimal("50");
        if (ingredient.contains("cabbage")) return new BigDecimal("900");
        if (ingredient.contains("lettuce")) return new BigDecimal("600");
        if (ingredient.contains("cauliflower")) return new BigDecimal("600");
        if (ingredient.contains("broccoli")) return new BigDecimal("500");
        return new BigDecimal("500");
    }

    private static BigDecimal extractFirstNumber(String text) {
        Pattern p = Pattern.compile("(\\d+(?:\\.\\d+)?(?:\\s+\\d+/\\d+)?|\\d+/\\d+)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return parseMixedNumber(m.group(1));
        }

        return BigDecimal.ZERO;
    }

    private static BigDecimal parseMixedNumber(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        value = value.trim();

        if (value.contains(" ")) {
            String[] parts = value.split("\\s+");
            if (parts.length == 2 && parts[1].contains("/")) {
                return new BigDecimal(parts[0]).add(parseFraction(parts[1]));
            }
        }

        if (value.contains("/")) {
            return parseFraction(value);
        }

        return new BigDecimal(value);
    }

    private static BigDecimal parseFraction(String value) {
        String[] parts = value.split("/");
        if (parts.length != 2) {
            return BigDecimal.ZERO;
        }

        BigDecimal numerator = new BigDecimal(parts[0]);
        BigDecimal denominator = new BigDecimal(parts[1]);

        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return numerator.divide(denominator, 6, RoundingMode.HALF_UP);
    }
}
