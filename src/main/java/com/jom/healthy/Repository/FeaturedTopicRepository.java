package com.jom.healthy.Repository;

import com.jom.healthy.entity.FeaturedTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeaturedTopicRepository extends JpaRepository<FeaturedTopic, Long> {

    // 自动按时间倒序查询所有文章
    List<FeaturedTopic> findAllByOrderByCreatedAtDesc();

    // 如果以后想按分类查：
    List<FeaturedTopic> findByCategory(String category);
}