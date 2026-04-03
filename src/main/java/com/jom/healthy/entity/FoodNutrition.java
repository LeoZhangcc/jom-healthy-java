package com.jom.healthy.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("food") // 如果你改了表名，请在这里替换为实际表名
public class FoodNutrition implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @TableId(value = "id", type = IdType.INPUT) // 因为你的 SQL 里 id 不是自增的
    private Integer id;

    private String foodNameOriginal;

    @TableField("food_group_") // 明确指定数据库字段名，防止末尾下划线被忽略
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

    @TableField("calcium__ca_mg") // 对应双下划线字段
    private Integer calciumCaMg;

    @TableField("iron__fe_mg")
    private Double ironFeMg;

    @TableField("phosphorus__p_mg")
    private Integer phosphorusPMg;

    @TableField("potassium__k_mg")
    private Integer potassiumKMg;

    @TableField("sodium__na_mg")
    private Integer sodiumNaMg;

    private Double vitaminCMg;

    @TableField("thiamin__b1_mg")
    private Double thiaminB1Mg;

    @TableField("riboflavin__b2_mg")
    private Double riboflavinB2Mg;

    @TableField("niacin__b3_mg")
    private Double niacinB3Mg;

    private Integer retinolG;

    private Integer carotenesG;

    @TableField("retinol_equivalents__re_g")
    private Integer retinolEquivalentsReG;

    private Double cholesterolMg;

    @TableField("lauric_c_12_0_")
    private Double lauricC120;

    @TableField("myristic_c_14_0_")
    private Double myristicC140;

    @TableField("myristoleic_c_14_1_")
    private Double myristoleicC141;

    @TableField("palmitic_c_16_0_")
    private Double palmiticC160;

    @TableField("palmitoleic_c_16_1_")
    private Double palmitoleicC161;

    @TableField("stearic_c_18_0_")
    private Double stearicC180;

    @TableField("oleic_c_18_1_")
    private Double oleicC181;

    @TableField("linoleic_c_18_2_")
    private Double linoleicC182;

    @TableField("linolenic_c_18_3_")
    private Double linolenicC183;

    @TableField("arachidic_c_20_0_")
    private Double arachidicC200;

    @TableField("arachidonic_c_20_4_")
    private Double arachidonicC204;

    @TableField("polyunsaturated_fatty_acid_")
    private Double polyunsaturatedFattyAcid;

    @TableField("p_s_ratio_")
    private Double psRatio;

    @TableField("capric_c_10_0_")
    private Double capricC100;

    @TableField("unconfirmed_fatty_acids_")
    private Double unconfirmedFattyAcids;

    private Integer caroteneG;

    @TableField("total_retinol_equivalents__re_g")
    private Double totalRetinolEquivalentsReG;

    @TableField("total_re_from_retinol_")
    private Integer totalReFromRetinol;

    private Integer luteinG;

    private Integer cryptoG;

    private Integer lycopeneG;

    private Integer othersG;

    private Integer sumCaroteneG;
}