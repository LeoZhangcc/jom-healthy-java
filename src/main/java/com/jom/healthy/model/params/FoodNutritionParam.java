package com.jom.healthy.model.params;

import com.jom.healthy.validator.BaseValidatingParam;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "食品参数模型",description = "食品参数模型")
public class FoodNutritionParam implements Serializable, BaseValidatingParam {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    private Integer id;

    private String foodNameOriginal;

    @ApiModelProperty(value="food_group_") // 明确指定数据库字段名，防止末尾下划线被忽略
    private String foodGroup;

    private String foodNameEn;

    private String foodNameCn;

    private String foodNameMs;

    private String picUrl;

    private Double waterG;

    private Integer energyKcal;

    private Double proteinG;

    private Double fatG;

    private Double carbohydrateG;

    private Double fibreG;

    private Double ashG;

    @ApiModelProperty(value="calcium__ca_mg") // 对应双下划线字段
    private Integer calciumCaMg;

    @ApiModelProperty(value="iron__fe_mg")
    private Double ironFeMg;

    @ApiModelProperty(value="phosphorus__p_mg")
    private Integer phosphorusPMg;

    @ApiModelProperty(value="potassium__k_mg")
    private Integer potassiumKMg;

    @ApiModelProperty(value="sodium__na_mg")
    private Integer sodiumNaMg;

    private Double vitaminCMg;

    @ApiModelProperty(value="thiamin__b1_mg")
    private Double thiaminB1Mg;

    @ApiModelProperty(value="riboflavin__b2_mg")
    private Double riboflavinB2Mg;

    @ApiModelProperty(value="niacin__b3_mg")
    private Double niacinB3Mg;

    private Integer retinolG;

    private Integer carotenesG;

    @ApiModelProperty(value="retinol_equivalents__re_g")
    private Integer retinolEquivalentsReG;

    private Double cholesterolMg;

    @ApiModelProperty(value="lauric_c_12_0_")
    private Double lauricC120;

    @ApiModelProperty(value="myristic_c_14_0_")
    private Double myristicC140;

    @ApiModelProperty(value="myristoleic_c_14_1_")
    private Double myristoleicC141;

    @ApiModelProperty(value="palmitic_c_16_0_")
    private Double palmiticC160;

    @ApiModelProperty(value="palmitoleic_c_16_1_")
    private Double palmitoleicC161;

    @ApiModelProperty(value="stearic_c_18_0_")
    private Double stearicC180;

    @ApiModelProperty(value="oleic_c_18_1_")
    private Double oleicC181;

    @ApiModelProperty(value="linoleic_c_18_2_")
    private Double linoleicC182;

    @ApiModelProperty(value="linolenic_c_18_3_")
    private Double linolenicC183;

    @ApiModelProperty(value="arachidic_c_20_0_")
    private Double arachidicC200;

    @ApiModelProperty(value="arachidonic_c_20_4_")
    private Double arachidonicC204;

    @ApiModelProperty(value="polyunsaturated_fatty_acid_")
    private Double polyunsaturatedFattyAcid;

    @ApiModelProperty(value="p_s_ratio_")
    private Double psRatio;

    @ApiModelProperty(value="capric_c_10_0_")
    private Double capricC100;

    @ApiModelProperty(value="unconfirmed_fatty_acids_")
    private Double unconfirmedFattyAcids;

    private Integer caroteneG;

    @ApiModelProperty(value="total_retinol_equivalents__re_g")
    private Double totalRetinolEquivalentsReG;

    @ApiModelProperty(value="total_re_from_retinol_")
    private Integer totalReFromRetinol;

    private Integer luteinG;

    private Integer cryptoG;

    private Integer lycopeneG;

    private Integer othersG;

    private Integer sumCaroteneG;
}
