package com.jom.healthy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jom.healthy.entity.FeaturedTopic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FeaturedTopicMapper extends BaseMapper<FeaturedTopic> {

    /**
     * 智能个性化推荐：从四个板块各随机抽取一篇文章
     * 如果 healthTag 是 NORMAL 或为空，则全局随机抽取
     * 如果 healthTag 是 OVERWEIGHT 或 UNDERWEIGHT，则按对应标签精准抽取
     */
    @Select("<script>" +
            "(SELECT * FROM featured_topics WHERE category = 'HABIT' " +
            "<if test=\"healthTag != null and healthTag != 'NORMAL'\"> AND health_tag = #{healthTag} </if> " +
            "ORDER BY RAND() LIMIT 1) " +
            "UNION ALL " +
            "(SELECT * FROM featured_topics WHERE category = 'REPORT' " +
            "<if test=\"healthTag != null and healthTag != 'NORMAL'\"> AND health_tag = #{healthTag} </if> " +
            "ORDER BY RAND() LIMIT 1) " +
            "UNION ALL " +
            "(SELECT * FROM featured_topics WHERE category = 'SPORTS' " +
            "<if test=\"healthTag != null and healthTag != 'NORMAL'\"> AND health_tag = #{healthTag} </if> " +
            "ORDER BY RAND() LIMIT 1) " +
            "UNION ALL " +
            "(SELECT * FROM featured_topics WHERE category = 'DIET' " +
            "<if test=\"healthTag != null and healthTag != 'NORMAL'\"> AND health_tag = #{healthTag} </if> " +
            "ORDER BY RAND() LIMIT 1)" +
            "</script>")
    List<FeaturedTopic> getRandomRecommendedTopics(@Param("healthTag") String healthTag);

}