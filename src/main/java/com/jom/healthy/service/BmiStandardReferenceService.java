package com.jom.healthy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jom.healthy.entity.BmiStandard;
import com.jom.healthy.mapper.BmiStandardReferenceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BmiStandardReferenceService {

    @Autowired
    private BmiStandardReferenceMapper mapper;

    public Map<String, Object> getWhoChartData(String type, int gender) {
        LambdaQueryWrapper<BmiStandard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BmiStandard::getGender, gender)
                .orderByAsc(BmiStandard::getAgeMonths);

        List<BmiStandard> rawData = mapper.selectList(wrapper);

        Map<String, Object> chartData = new HashMap<>();
        // 💡 修改 1：在这里初始化 sd1
        List<Map<String, Object>> sd3 = new ArrayList<>(),
                sd2 = new ArrayList<>(),
                sd1 = new ArrayList<>(), // 👈 新增
                sd0 = new ArrayList<>(),
                neg1 = new ArrayList<>(),
                neg2 = new ArrayList<>(),
                neg3 = new ArrayList<>();

        for (BmiStandard ref : rawData) {
            int monthVal = ref.getAgeMonths();
            String displayLabel = "";

            if ("MONTH".equals(type)) {
                if (monthVal % 12 == 0) {
                    displayLabel = monthVal + "m";
                }
            } else if ("YEAR".equals(type)) {
                if (monthVal % 24 == 0) {
                    displayLabel = (monthVal / 12) + "y";
                }
            }

            // 💡 修改 2：在这里填充 sd1 数据
            sd3.add(makePoint(displayLabel, ref.getSdPos3()));
            sd2.add(makePoint(displayLabel, ref.getSdPos2()));
            sd1.add(makePoint(displayLabel, ref.getSdPos1())); // 👈 新增
            sd0.add(makePoint(displayLabel, ref.getSd0()));
            neg1.add(makePoint(displayLabel, ref.getSdNeg1()));
            neg2.add(makePoint(displayLabel, ref.getSdNeg2()));
            neg3.add(makePoint(displayLabel, ref.getSdNeg3()));
        }

        // 💡 修改 3：在这里把 sd1 放进 Map 返回给前端
        chartData.put("sd3", sd3);
        chartData.put("sd2", sd2);
        chartData.put("sd1", sd1); // 👈 新增
        chartData.put("sd0", sd0);
        chartData.put("neg1", neg1);
        chartData.put("neg2", neg2);
        chartData.put("neg3", neg3);

        return chartData;
    }

    private Map<String, Object> makePoint(String label, Double value) {
        Map<String, Object> p = new HashMap<>();
        p.put("label", label);
        p.put("value", value);
        return p;
    }
}