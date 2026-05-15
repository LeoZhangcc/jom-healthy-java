package com.jom.healthy.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jom.healthy.dto.NearbySupermarketDto;
import com.jom.healthy.service.NearbySupermarketService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class NearbySupermarketServiceImpl implements NearbySupermarketService {

    private static final String GOOGLE_NEARBY_SEARCH_URL =
            "https://places.googleapis.com/v1/places:searchNearby";

    @Value("${GOOGLE_PLACES_API_KEY}")
    private String googlePlacesApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<NearbySupermarketDto> findNearbySupermarkets(
            Double latitude,
            Double longitude,
            Integer radiusMeters
    ) throws Exception {

        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("latitude and longitude are required");
        }

        int safeRadius = radiusMeters == null ? 3000 : radiusMeters;
        safeRadius = Math.max(500, Math.min(safeRadius, 10000));

        HttpURLConnection connection = null;

        try {
            URL url = new URL(GOOGLE_NEARBY_SEARCH_URL);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("X-Goog-Api-Key", googlePlacesApiKey);

            /*
             * 只要前端展示需要的字段，避免无用字段增加计费和响应体。
             */
            connection.setRequestProperty(
                    "X-Goog-FieldMask",
                    "places.id," +
                            "places.displayName," +
                            "places.formattedAddress," +
                            "places.location," +
                            "places.rating," +
                            "places.userRatingCount," +
                            "places.currentOpeningHours.openNow"
            );

            connection.setConnectTimeout(10000);
            connection.setReadTimeout(20000);
            connection.setDoOutput(true);

            String requestBody = buildRequestBody(latitude, longitude, safeRadius);

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(requestBody.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }

            int statusCode = connection.getResponseCode();

            InputStream inputStream;
            if (statusCode >= 200 && statusCode < 300) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }

            String responseBody = readStream(inputStream);

            if (statusCode < 200 || statusCode >= 300) {
                throw new RuntimeException(
                        "Google Places API error, status: "
                                + statusCode
                                + ", body: "
                                + responseBody
                );
            }

            return parseNearbySupermarkets(responseBody, latitude, longitude);

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String buildRequestBody(
            Double latitude,
            Double longitude,
            Integer radiusMeters
    ) throws Exception {

        String json =
                "{"
                        + "\"includedTypes\":[\"supermarket\",\"grocery_store\"],"
                        + "\"maxResultCount\":20,"
                        + "\"rankPreference\":\"DISTANCE\","
                        + "\"locationRestriction\":{"
                        + "  \"circle\":{"
                        + "    \"center\":{"
                        + "      \"latitude\":" + latitude + ","
                        + "      \"longitude\":" + longitude
                        + "    },"
                        + "    \"radius\":" + radiusMeters
                        + "  }"
                        + "}"
                        + "}";

        return json;
    }

    private List<NearbySupermarketDto> parseNearbySupermarkets(
            String responseBody,
            Double userLatitude,
            Double userLongitude
    ) throws Exception {

        List<NearbySupermarketDto> result = new ArrayList<>();

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode places = root.path("places");

        if (!places.isArray()) {
            return result;
        }

        for (JsonNode place : places) {
            NearbySupermarketDto dto = new NearbySupermarketDto();

            dto.setPlaceId(text(place, "id"));
            dto.setName(place.path("displayName").path("text").asText(""));
            dto.setAddress(text(place, "formattedAddress"));

            Double latitude = doubleValue(place.path("location").path("latitude"));
            Double longitude = doubleValue(place.path("location").path("longitude"));

            dto.setLatitude(latitude);
            dto.setLongitude(longitude);

            dto.setRating(doubleValue(place.path("rating")));
            dto.setUserRatingCount(intValue(place.path("userRatingCount")));

            JsonNode openNowNode = place.path("currentOpeningHours").path("openNow");
            dto.setOpenNow(openNowNode.isMissingNode() ? null : openNowNode.asBoolean());

            if (latitude != null && longitude != null) {
                dto.setDistanceKm(
                        roundToTwo(
                                calculateDistanceKm(
                                        userLatitude,
                                        userLongitude,
                                        latitude,
                                        longitude
                                )
                        )
                );
            }

            result.add(dto);
        }

        return result;
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);

        if (value.isMissingNode() || value.isNull()) {
            return null;
        }

        String text = value.asText();

        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        return text.trim();
    }

    private Double doubleValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        return node.asDouble();
    }

    private Integer intValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        return node.asInt();
    }

    private String readStream(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }

        return result.toString();
    }

    /**
     * Haversine distance
     */
    private double calculateDistanceKm(
            double lat1,
            double lon1,
            double lat2,
            double lon2
    ) {
        final double earthRadiusKm = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2)
                        * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusKm * c;
    }

    private double roundToTwo(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}