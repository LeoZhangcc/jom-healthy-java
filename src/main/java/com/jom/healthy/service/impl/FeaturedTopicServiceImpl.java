package com.jom.healthy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jom.healthy.dto.FeaturedTopicDto;
import com.jom.healthy.entity.FeaturedTopic;
import com.jom.healthy.mapper.FeaturedTopicMapper;
import com.jom.healthy.service.FeaturedTopicService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FeaturedTopicServiceImpl extends ServiceImpl<FeaturedTopicMapper, FeaturedTopic> implements FeaturedTopicService {

    // --- 实现推荐方法 ---
    @Override
    public List<FeaturedTopicDto> getRecommendedTopics(String healthTag, String lang) {
        List<FeaturedTopic> rawList = baseMapper.getRandomRecommendedTopics(healthTag);
        return rawList.stream()
                .map(topic -> convertToDto(topic, lang))
                .collect(Collectors.toList());
    }

    // --- 💡 重点：实现报错缺失的 getAllTopics 方法 ---
    @Override
    public List<FeaturedTopicDto> getAllTopics(String lang) {
        // 使用 MyBatis-Plus 默认的 list() 获取所有数据
        List<FeaturedTopic> rawList = this.list();
        return rawList.stream()
                .map(topic -> convertToDto(topic, lang))
                .collect(Collectors.toList());
    }

    /**
     * 🛠️ 私有工具方法：统一处理多语言转换逻辑
     */
    private FeaturedTopicDto convertToDto(FeaturedTopic topic, String lang) {
        FeaturedTopicDto dto = new FeaturedTopicDto();
        // 1. 拷贝基础不变量（ID, 图片地址等）
        BeanUtils.copyProperties(topic, dto);

        // 2. 根据 lang 参数给 DTO 的通用字段赋值
        if ("zh".equalsIgnoreCase(lang)) {
            dto.setTitle(topic.getTitleZh());
            dto.setSummary(topic.getSummaryZh());
            dto.setContent(topic.getContentZh());
        } else if ("ms".equalsIgnoreCase(lang)) {
            dto.setTitle(topic.getTitleMs());
            dto.setSummary(topic.getSummaryMs());
            dto.setContent(topic.getContentMs());
        } else {
            // 默认返回英文
            dto.setTitle(topic.getTitleEn());
            dto.setSummary(topic.getSummaryEn());
            dto.setContent(topic.getContentEn());
        }
        return dto;
    }
}