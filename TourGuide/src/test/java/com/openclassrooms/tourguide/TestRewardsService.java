package com.openclassrooms.tourguide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

public class TestRewardsService {

	private GpsUtil gpsUtil;
	private ExecutorService executor;
	private RewardsService rewardsService;

	@BeforeEach
	public void setUp() {
		executor = Executors.newVirtualThreadPerTaskExecutor();

		gpsUtil = new GpsUtil();
		rewardsService = new RewardsService(gpsUtil, new RewardCentral(), executor);
	}

	@AfterEach
	public void tearDown() {
		executor.shutdown();
	}

	@Test
	public void userGetRewards() {
		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService, executor);

		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
		Attraction attraction = gpsUtil.getAttractions().getFirst();
		user.addToVisitedLocations(new VisitedLocation(user.getUserId(), attraction, new Date()));
		tourGuideService.trackUserLocation(user);
		List<UserReward> userRewards = user.getUserRewards();
		tourGuideService.tracker.stopTracking();
        assertEquals(1, userRewards.size());
	}

	@Test
	public void isWithinAttractionProximity() {
		Attraction attraction = gpsUtil.getAttractions().getFirst();
		assertTrue(rewardsService.isWithinAttractionProximity(attraction, attraction));
	}

	// Fixed - no more throw of ConcurrentModificationException
	@Test
	public void nearAllAttractions() {
		rewardsService.setProximityBuffer(Integer.MAX_VALUE);

		InternalTestHelper.setInternalUserNumber(1);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService, executor);

		rewardsService.calculateRewards(tourGuideService.getAllUsers().getFirst());
		List<UserReward> userRewards = tourGuideService.getUserRewards(tourGuideService.getAllUsers().getFirst());
		tourGuideService.tracker.stopTracking();

		assertEquals(gpsUtil.getAttractions().size(), userRewards.size());
	}

}
