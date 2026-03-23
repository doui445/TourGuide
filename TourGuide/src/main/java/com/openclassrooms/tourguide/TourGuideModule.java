package com.openclassrooms.tourguide;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import gpsUtil.GpsUtil;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.service.RewardsService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class TourGuideModule {

	@Bean
	public ExecutorService virtualThreadExecutor() {
		return Executors.newVirtualThreadPerTaskExecutor(); // instead of newFixedThreadPool(nbOfThreads)
	}
	
	@Bean
	public GpsUtil getGpsUtil() {
		return new GpsUtil();
	}
	
	@Bean
	public RewardsService getRewardsService() {
		return new RewardsService(getGpsUtil(), getRewardCentral(),  virtualThreadExecutor());
	}
	
	@Bean
	public RewardCentral getRewardCentral() {
		return new RewardCentral();
	}
	
}
