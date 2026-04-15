package com.jom.healthy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.jom.healthy.entity.BmiStandard;

@Mapper
public interface BmiStandardReferenceMapper extends BaseMapper<BmiStandard> {
    // 继承了 BaseMapper 后，不需要写任何 SQL
}