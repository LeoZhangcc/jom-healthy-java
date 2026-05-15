package com.jom.healthy.dto;

import lombok.Data;

@Data
public class NearbySupermarketDto {

    private String placeId;
    private String name;
    private String address;

    private Double latitude;
    private Double longitude;

    private Double rating;
    private Integer userRatingCount;

    private Boolean openNow;

    private Double distanceKm;
}