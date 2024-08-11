package com.demo.my.ratelimiter.example;

import com.demo.my.ratelimiter.MyImplRateLimiter;
import com.demo.my.ratelimiter.MyRateLimiter;

public class MyRateLimiterTest {
    private static MyRateLimiter myRateLimiter = new MyImplRateLimiter();

    static {
        myRateLimiter.setRate(1,1);
    }

    public static void main(String[] args) {
        long currentTimeMs = System.currentTimeMillis();
        for (int i = 1; i <= 10; i++) {
            double waitTime = myRateLimiter.acquire(1);
            System.out.printf("i=%s,waitTime=%s\n", i, waitTime);
        }
        long cost = System.currentTimeMillis() - currentTimeMs;
        System.out.println("整体QPS=" + cost / 1000.0 / 10.0);
    }
}
