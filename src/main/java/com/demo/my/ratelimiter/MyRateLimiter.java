package com.demo.my.ratelimiter;

public interface MyRateLimiter {
    /**
     * 获取【许可】
     * @param permits
     * @return
     */
    double acquire(int permits);

    /**
     * 尝试获取【许可】
     * @param permits
     * @param tryAcquireTime
     * @return
     */
    boolean tryAcquire(int permits, long tryAcquireTime);

    void setRate(double permitsPerSecond, double permitsUnexpiredSeconds);
}
