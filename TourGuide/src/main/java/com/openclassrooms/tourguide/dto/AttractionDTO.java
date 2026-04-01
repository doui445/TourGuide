package com.openclassrooms.tourguide.dto;

public record AttractionDTO(
        String attractionName,
        double attractionLatitude,
        double attractionLongitude,
        double distanceInMiles,
        int rewardPoints
) {
}
