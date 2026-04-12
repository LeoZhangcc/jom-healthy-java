package com.jom.healthy.dto;
import java.math.BigDecimal;

public class NutritionDto {

    private String nutritional_status;
    private String sociodemographics;
    private String age_range;
    private BigDecimal prevalence_percent;

    public NutritionDto() {
    }

    public NutritionDto(String nutritional_status,
                        String sociodemographics,
                        String age_range,
                        BigDecimal prevalence_percent) {
        this.nutritional_status = nutritional_status;
        this.sociodemographics = sociodemographics;
        this.age_range = age_range;
        this.prevalence_percent = prevalence_percent;
    }

    public String getNutritional_status() {
        return nutritional_status;
    }

    public void setNutritional_status(String nutritional_status) {
        this.nutritional_status = nutritional_status;
    }

    public String getSociodemographics() {
        return sociodemographics;
    }

    public void setSociodemographics(String sociodemographics) {
        this.sociodemographics = sociodemographics;
    }

    public String getAge_range() {
        return age_range;
    }

    public void setAge_range(String age_range) {
        this.age_range = age_range;
    }

    public BigDecimal getPrevalence_percent() {
        return prevalence_percent;
    }

    public void setPrevalence_percent(BigDecimal prevalence_percent) {
        this.prevalence_percent = prevalence_percent;
    }

    @Override
    public String toString() {
        return "NutritionDto{" +
                "nutritional_status='" + nutritional_status + '\'' +
                ", sociodemographics='" + sociodemographics + '\'' +
                ", age_range='" + age_range + '\'' +
                ", prevalence_percent=" + prevalence_percent +
                '}';
    }
}
