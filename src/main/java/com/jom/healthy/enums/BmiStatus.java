package com.jom.healthy.enums;

public enum BmiStatus {
    SEVERE_THINNESS("严重消瘦", -3),
    THINNESS("消瘦", -2),
    NORMAL("正常", 0), // 正常范围的基准点
    OVERWEIGHT("超重", 1),
    OBESITY("肥胖", 2);

    private String description;
    private Integer threshold;

    BmiStatus(String description, Integer threshold) {
        this.description = description;
        this.threshold = threshold;
    }

    public String getDescription() {
        return description;
    }

    public Integer getThreshold() {
        return threshold;
    }

    /**
     * 根据 Z-Score 判断当前的 BMI 状态
     * @param zScore 计算得出的标准差值
     */
    public static BmiStatus evaluate(double zScore) {
        if (zScore < -3) return SEVERE_THINNESS;
        if (zScore < -2) return THINNESS;
        if (zScore > 2) return OBESITY;
        if (zScore > 1) return OVERWEIGHT;
        return NORMAL;
    }
}
