package com.openclassrooms.tourguide.service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
    private final int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;
	private final ExecutorService executor;
	
	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral, ExecutorService executor) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
        this.executor = executor;
    }
	
	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}
	
	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}

	public void calculateRewardsForAll(List<User> users) {
		// On lance tous les calculs en parallèle et on récupère les "tickets" (futures)
		CompletableFuture<?>[] futures = users.stream()
				.map(user -> CompletableFuture.runAsync(() -> calculateRewards(user), executor))
				.toArray(CompletableFuture[]::new);

		// On bloque jusqu'à ce que tous les users soient traités (join).
		CompletableFuture.allOf(futures).join();
	}
	
	public void calculateRewards(User user) {
		List<VisitedLocation> userLocations = user.getVisitedLocations();
		List<Attraction> attractions = gpsUtil.getAttractions();

		// On récupère les récompenses déjà obtenues
		Set<String> rewardedAttractions = user.getUserRewards().stream()
				.map(userReward -> userReward.attraction.attractionName)
				.collect(Collectors.toSet()); // Set = 1 seule occurrence par attraction

		for (Attraction attraction : attractions) {
			if (!rewardedAttractions.contains(attraction.attractionName)) {
				// On fait le calcul seulement sur les attractions restantes
				for (VisitedLocation location : userLocations) {
					if (nearAttraction(location, attraction)) {
						user.addUserReward(new UserReward(location, attraction, getRewardPoints(attraction, user)));
						break; // Si on trouve une correspondance, on arrête de chercher et on passe à l'attraction suiante
					}
				}
			}
		}
	}
	
	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return !(getDistance(attraction, location) > attractionProximityRange);
	}
	
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return !(getDistance(attraction, visitedLocation.location) > proximityBuffer);
	}
	
	private int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}
	
	public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        return STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
	}

}
