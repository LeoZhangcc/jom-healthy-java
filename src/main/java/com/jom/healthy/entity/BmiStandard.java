package com.jom.healthy.entity;

import lombok.Data;

@Data // 如果你用了 Lombok，直接加这个注解；没用的话自己生成 Getter/Setter
public class BmiStandard {
    private Integer id;
    private Integer gender;      // 1:男, 0:女
    private Integer ageMonths;   // 年龄(月)
    private Double sdNeg3;
    private Double sdNeg2;
    private Double sdNeg1;
    private Double sd0;
    private Double sdPos1;
    private Double sdPos2;
    private Double sdPos3;
}
