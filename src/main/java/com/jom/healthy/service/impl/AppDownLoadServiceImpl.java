package com.jom.healthy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jom.healthy.entity.AppDownload;
import com.jom.healthy.mapper.AppDownloadMapper;
import com.jom.healthy.service.AppDownLoadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class AppDownLoadServiceImpl extends ServiceImpl<AppDownloadMapper, AppDownload> implements AppDownLoadService {
    @Override
    public String getAppNewVersionUrl() {
        return this.baseMapper.selectOne(
                new QueryWrapper<AppDownload>()
                        .orderByDesc("id")
                        .last("limit 1")).getUrl();
    }

}
