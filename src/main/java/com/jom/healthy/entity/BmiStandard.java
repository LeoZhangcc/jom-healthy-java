package com.jom.healthy.entity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data // 如果你用了 Lombok，直接加这个注解；没用的话自己生成 Getter/Setter
@TableName("bmi_standard_reference")
public class BmiStandard {
    private Integer id;
    private Integer gender;      // 1:男, 0:女
    private Integer ageMonths;   // 年龄(月)
    private Double sdNeg3;
    private Double sdNeg2;
    private Double sdNeg1;
    @TableField("sd_0")
    private Double sd0;
    private Double sdPos1;
    private Double sdPos2;
    private Double sdPos3;
}
