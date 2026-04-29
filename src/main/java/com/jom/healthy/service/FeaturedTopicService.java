package com.jom.healthy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jom.healthy.dto.FeaturedTopicDto;
import com.jom.healthy.entity.FeaturedTopic;
import java.util.List;

public interface FeaturedTopicService extends IService<FeaturedTopic> {

    // 原有的推荐方法
    List<FeaturedTopicDto> getRecommendedTopics(String healthTag, String lang);

    // 💡 新增：获取所有专题的方法，也要带上 lang
    List<FeaturedTopicDto> getAllTopics(String lang);
}