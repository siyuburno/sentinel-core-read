package com.demo.officail.ratelimiter;

import com.google.common.util.concurrent.RateLimiter;

public class RateLimiterTest {
    private static RateLimiter rateLimiter = RateLimiter.create(1);

    public static void main(String[] args) {
        long currentTimeMs = System.currentTimeMillis();
        for (int i = 1; i <= 10; i++) {
            double waitTime = rateLimiter.acquire(1);
            System.out.printf("i=%s,waitTime=%s\n", i, waitTime);
        }
        long cost = System.currentTimeMillis() - currentTimeMs;
        System.out.println("整体QPS=" + cost / 1000.0 / 10.0);
    }
}
