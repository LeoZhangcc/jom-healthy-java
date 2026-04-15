package com.jom.healthy.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jom.healthy.entity.FeaturedTopic;
import com.jom.healthy.mapper.FeaturedTopicMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
@CrossOrigin(origins = "*")
public class FeaturedTopicController {

    @Autowired
    private FeaturedTopicMapper topicMapper; // 注入 Mapper 代替 Repository

    @GetMapping("/all")
    public List<FeaturedTopic> getAllTopics() {
        // 使用 LambdaQueryWrapper 实现逻辑：SELECT * FROM featured_topics ORDER BY created_at DESC
        LambdaQueryWrapper<FeaturedTopic> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(FeaturedTopic::getCreatedAt);

        return topicMapper.selectList(wrapper);
    }
}