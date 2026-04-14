package com.jom.healthy.mapper;

import com.jom.healthy.entity.HealthInsight;
import com.jom.healthy.entity.InsightChartData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface HealthInsightMapper {

    // 查询所有数据（用于前端首页的横向滑动卡片）
    @Select("SELECT id, theme_name, cover_image_url, short_summary, source_label FROM health_insight")
    List<HealthInsight> findAll();

    // 根据ID查询一条详细数据（用于点击卡片后的详情页）
    @Select("SELECT * FROM health_insight WHERE id = #{id}")
    HealthInsight findById(Integer id);

    // 根据主题ID查询其关联的扇形图百分比数据
    @Select("SELECT * FROM insight_chart_data WHERE insight_id = #{insightId} ORDER BY data_year DESC")
    List<InsightChartData> findChartDataByInsightId(Integer insightId);

}

