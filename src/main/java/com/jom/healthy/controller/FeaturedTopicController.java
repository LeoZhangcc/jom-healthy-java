package com.jom.healthy.controller;

import com.jom.healthy.Repository.FeaturedTopicRepository;
import com.jom.healthy.entity.FeaturedTopic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
@CrossOrigin(origins = "*") // 允许前端跨域访问
public class FeaturedTopicController {

    @Autowired
    private FeaturedTopicRepository topicRepository;

    @GetMapping("/all")
    public List<FeaturedTopic> getAllTopics() {
        // 直接返回数据库里所有的文章
        return topicRepository.findAllByOrderByCreatedAtDesc();
    }
}