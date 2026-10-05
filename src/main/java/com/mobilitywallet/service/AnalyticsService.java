package com.mobilitywallet.service;

import com.mobilitywallet.dto.response.AdminAnalyticsDto;
import com.mobilitywallet.entity.Transaction;

import java.util.List;

public interface AnalyticsService {

    AdminAnalyticsDto getAdminAnalytics();

    List<Transaction> getRecentTransactions(int limit);
}
