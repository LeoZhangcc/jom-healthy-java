package com.jom.healthy.service;

import com.jom.healthy.entity.BmiStandard;
import com.jom.healthy.mapper.BmiMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

@Service
public class BmiService {

    @Autowired
    private BmiMapper bmiMapper;

    /**
     * 评估儿童/青少年 BMI
     * @param heightCm 身高 (厘米)
     * @param weightKg 体重 (公斤)
     * @param birthDateStr 年龄 (月)
     * @param gender 性别 (1:男, 0:女)
     * @return 评估结果与建议
     */
    /**
     * 核心工具方法：根据出生日期计算精确的月龄 (Age in Months)
     * @param birthDateStr 出生日期，格式必须为 "yyyy-MM-dd"
     * @return 精确的月数
     */

    public int calculateAgeInMonths(String birthDateStr) {
        try {
            // 👉 核心修改：将前端可能传来的斜杠 "/" 全部替换为标准破折号 "-"
            // 这样无论前端传 "2018/01/01" 还是 "2018-01-01"，都会被格式化为 "2018-01-01"
            String normalizedDateStr = birthDateStr.replace("/", "-");

            // 1. 将标准化后的字符串解析为 Java 的本地日期对象
            LocalDate birthDate = LocalDate.parse(normalizedDateStr);

            // 2. 获取服务器当前的绝对精确时间
            LocalDate today = LocalDate.now();

            // 3. 一行代码搞定所有逻辑！(自动处理平年、闰年、天数有没有满)
            long months = ChronoUnit.MONTHS.between(birthDate, today);

            // 4. 防止输入未来日期产生负数，如果小于0则返回0
            return months <= 0 ? 0 : (int) months;

        } catch (DateTimeParseException e) {
            // 防呆处理：如果前端乱传日期格式，统一当作格式错误
            throw new IllegalArgumentException("Date format is incorrect, please use YYYY-MM-DD or YYYY/MM/DD");
        }
    }

    public String evaluateChildBmi(double heightCm, double weightKg, String birthDateStr, int gender) {

        // 👉 1. 在这里！把前端传来的生日字符串，转换成精确的月龄！
        int ageMonths = calculateAgeInMonths(birthDateStr);


        // 1. 防御性拦截：超出19岁（228个月）的数据我们数据库里没有
        if (ageMonths < 0 || ageMonths > 228) {
            return "Only data for 0-19 years old (0-228 months) is supported for evaluation.";
        }

        // 2. 计算当前 BMI (公式：体重 / 身高的平方，身高需转为米)
        double heightM = heightCm / 100.0;
        double currentBmi = weightKg / (heightM * heightM);

        // 3. 去数据库查出标准线
        BmiStandard standard = bmiMapper.findStandardByAgeAndGender(ageMonths, gender);

        if (standard == null) {
            return "System error: No standard data for the corresponding age was found.";
        }

        // 4. 将孩子的 BMI 与标准 Z-score 区间进行严谨比对 (从高到低判断)
        // 注意：这里格式化一下用户的 BMI，保留两位小数更好看
        String prefix = String.format("Your BMI is %.2f. Assessment results:", currentBmi);

        if (currentBmi > standard.getSdPos1()) {
            return prefix + "Overweight - It is recommended to reduce snacks and sugary drinks (such as Milo) and cultivate a regular outdoor activity habit.";
        } else if (currentBmi > standard.getSdNeg1()) {
            return prefix + "Underweight - It is recommended to increase dietary energy density and ensure adequate intake of protein and healthy fats.";
        } else {
            return prefix + "Healthy - Perfect! Please continue to maintain the good Malaysian \"Suku-Suku Separuh\" healthy eating habits!";
        }
    }

}