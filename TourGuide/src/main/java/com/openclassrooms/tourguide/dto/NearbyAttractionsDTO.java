package com.openclassrooms.tourguide.dto;

import java.util.List;

public record NearbyAttractionsDTO(
        double userLatitude,
        double userLongitude,
        List<AttractionDTO> attractions
) {
}
