package com.jom.healthy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jom.healthy.entity.AppDownload;

public interface AppDownLoadService extends IService<AppDownload> {

    String getAppNewVersionUrl();
}
