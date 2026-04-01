package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.dto.AttractionDTO;
import com.openclassrooms.tourguide.dto.NearbyAttractionsDTO;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
public class TourGuideService {
	private final Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;
	private final ExecutorService executor;

	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService, ExecutorService executor) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;
        this.executor = executor;

        Locale.setDefault(Locale.US);

		if (testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}

	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	public VisitedLocation getUserLocation(User user) {
        return (!user.getVisitedLocations().isEmpty()) ? user.getLastVisitedLocation()
                : trackUserLocation(user);
	}

	public User getUser(String userName) {
		User user = internalUserMap.get(userName);
		if (user == null) throw new IllegalArgumentException("User not found: " + userName);
		return user;
	}

	public List<User> getAllUsers() {
		return new ArrayList<>(internalUserMap.values());
	}

	public void addUser(User user) {
		if (!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}

	public List<Provider> getTripDeals(User user) {
		int cumulativeRewardPoints = user.getUserRewards().stream().mapToInt(UserReward::getRewardPoints).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(), cumulativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}

	public void trackUserLocationForAll(List<User> users) {
		// On lance tous les calculs en parallèle et on récupère les "tickets" (futures).
		CompletableFuture<?>[] futures = users.stream()
				.map(user -> CompletableFuture.runAsync(() -> trackUserLocation(user), executor))
				.toArray(CompletableFuture[]::new);

		// On bloque jusqu'à ce que tous les users soient traités (join).
		CompletableFuture.allOf(futures).join();
	}

	public VisitedLocation trackUserLocation(User user) {
		VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
		user.addToVisitedLocations(visitedLocation);
		rewardsService.calculateRewards(user);
		return visitedLocation;
	}

	public NearbyAttractionsDTO getNearbyAttractions(User user) {
		record AttractionDistance(Attraction attraction, double distance) {}
		Location userLocation = getUserLocation(user).location;

		List<CompletableFuture<AttractionDTO>> futures = gpsUtil.getAttractions().stream()
				.map(attraction -> new AttractionDistance(attraction, rewardsService.getDistance(attraction, userLocation)))
				.sorted(Comparator.comparingDouble(AttractionDistance::distance)) // On trie par distance
				.limit(5) // On ne garde que les 5 plus proches
				.map(attractionDistance -> CompletableFuture.supplyAsync(() -> new AttractionDTO( // On transforme les Attractions en AttractionDTO de manière asynchrone
                        attractionDistance.attraction().attractionName,
                        attractionDistance.attraction().latitude,
                        attractionDistance.attraction().longitude,
                        attractionDistance.distance(),
                        rewardsService.getRewardPoints(attractionDistance.attraction(), user)), executor))
				.toList(); // On lance l'exécution

		List<AttractionDTO> nearbyAttractions = futures.stream()
				.map(CompletableFuture::join) // On attend la fin des 5 tickets
				.toList(); // On rassemble le tout

		return new NearbyAttractionsDTO(
				userLocation.latitude,
				userLocation.longitude,
				nearbyAttractions
		);
	}

	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(tracker::stopTracking));
	}

	/**********************************************************************************
	 * <p>
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes
	// internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();

	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);

			internalUserMap.put(userName, user);
		});
        logger.debug("Created {} internal test users.", InternalTestHelper.getInternalUserNumber());
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i -> user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
                new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime())));
	}

	private double generateRandomLongitude() {
		double leftLimit = -180;
		double rightLimit = 180;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}
}
