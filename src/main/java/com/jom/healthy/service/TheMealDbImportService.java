package com.jom.healthy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jom.healthy.entity.TheMealDbMeal;

public interface TheMealDbImportService extends IService<TheMealDbMeal> {

    void importAllMeals() throws Exception;
}
