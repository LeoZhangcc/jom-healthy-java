package com.jom.healthy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jom.healthy.entity.PhysicalActivityItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PhysicalActivityItemMapper extends BaseMapper<PhysicalActivityItem> {
    // 继承了 BaseMapper，MyBatis-Plus 会自动帮你写好增删改查的底层逻辑，这里一行代码都不用写！
}