package com.openclassrooms.tourguide;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;

@Tag("performance")
public class TestPerformance {

	/*
	 * A note on performance improvements:
	 * 
	 * The number of users generated for the high volume tests can be easily
	 * adjusted via this method:
	 * 
	 * InternalTestHelper.setInternalUserNumber(100000);
	 * 
	 * 
	 * These tests can be modified to suit new solutions, just as long as the
	 * performance metrics at the end of the tests remains consistent.
	 * 
	 * These are performance metrics that we are trying to hit:
	 * 
	 * highVolumeTrackLocation: 100,000 users within 15 minutes:
	 * assertTrue(TimeUnit.MINUTES.toSeconds(15) >=
	 * TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	 *
	 * highVolumeGetRewards: 100,000 users within 20 minutes:
	 * assertTrue(TimeUnit.MINUTES.toSeconds(20) >=
	 * TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	 */

	private GpsUtil gpsUtil;
	private ExecutorService executor;
	private RewardsService rewardsService;

	private final int INTERNAL_USER_NUMBER = 100000;

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
	public void highVolumeTrackLocation() {
		// Users should be incremented up to 100,000, and test finishes within 15 minutes
		InternalTestHelper.setInternalUserNumber(INTERNAL_USER_NUMBER);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService, executor);

		List<User> allUsers;
		allUsers = tourGuideService.getAllUsers();

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		// allUsers.forEach(tourGuideService::trackUserLocation);
		tourGuideService.trackUserLocationForAll(allUsers);

		stopWatch.stop();
		tourGuideService.tracker.stopTracking();

		System.out.println("highVolumeTrackLocation: Time Elapsed: "
				+ TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
		assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}

	@Test
	public void highVolumeGetRewards() {
		// Users should be incremented up to 100,000, and test finishes within 20 minutes
		InternalTestHelper.setInternalUserNumber(INTERNAL_USER_NUMBER);
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService, executor);

		Attraction attraction = gpsUtil.getAttractions().getFirst();
		List<User> allUsers;
		allUsers = tourGuideService.getAllUsers();
		allUsers.forEach(u -> u.addToVisitedLocations(new VisitedLocation(u.getUserId(), attraction, new Date())));

		// allUsers.forEach(rewardsService::calculateRewards);
		rewardsService.calculateRewardsForAll(allUsers);

		for (User user : allUsers) {
            assertFalse(user.getUserRewards().isEmpty());
		}
		stopWatch.stop();
		tourGuideService.tracker.stopTracking();

		System.out.println("highVolumeGetRewards: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime())
				+ " seconds.");
		assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}
}
