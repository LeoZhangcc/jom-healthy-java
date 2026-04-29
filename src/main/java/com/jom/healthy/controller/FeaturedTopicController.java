package com.jom.healthy.controller;

import com.jom.healthy.dto.FeaturedTopicDto;
import com.jom.healthy.service.FeaturedTopicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
@CrossOrigin(origins = "*")
public class FeaturedTopicController {

    @Autowired
    private FeaturedTopicService topicService;

    /**
     * 获取所有专题（支持多语言转换）
     * 请求示例：/api/topics/all?lang=zh
     */
    @GetMapping("/all")
    public List<FeaturedTopicDto> getAllTopics(
            @RequestParam(required = false, defaultValue = "en") String lang
    ) {
        // 为了保持一致性，建议在 Service 里也写一个支持 lang 的 getAll 方法
        // 或者这里暂时传入一个特殊的 tag 来获取全部，但最标准的是调用 service 的多语言处理逻辑
        return topicService.getAllTopics(lang);
    }

    /**
     * 获取推荐专题（根据健康标签和语言）
     * 请求示例：/api/topics/recommend?status=OVERWEIGHT&lang=zh
     */
    @GetMapping("/recommend")
    public List<FeaturedTopicDto> getRecommendedTopics(
            @RequestParam(defaultValue = "OVERWEIGHT", required = false) String status,
            @RequestParam(required = false, defaultValue = "en") String lang // 💡 新增：接收语言参数
    ) {
        String safeStatus = status.toUpperCase();

        // 💡 这里的调用要匹配你修改后的 Service 方法签名
        return topicService.getRecommendedTopics(safeStatus, lang);
    }
}