package com.jom.healthy.service;

import com.jom.healthy.dto.NearbySupermarketDto;

import java.util.List;

public interface NearbySupermarketService {

    List<NearbySupermarketDto> findNearbySupermarkets(
            Double latitude,
            Double longitude,
            Integer radiusMeters
    ) throws Exception;
}