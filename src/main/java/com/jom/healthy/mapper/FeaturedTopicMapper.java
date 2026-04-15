package com.jom.healthy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jom.healthy.entity.FeaturedTopic;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FeaturedTopicMapper extends BaseMapper<FeaturedTopic> {
    // 继承 BaseMapper 后，普通的增删改查已经自动写好了
}