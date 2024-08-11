package com.demo.my.ratelimiter;

import com.alibaba.csp.sentinel.util.AssertUtil;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.RateLimiter;

import java.util.concurrent.TimeUnit;

import static com.google.common.primitives.Doubles.min;
import static java.util.concurrent.TimeUnit.SECONDS;

public class MyImplRateLimiter implements MyRateLimiter {
    /**
     *
     */
    private volatile Object mutrix;

    private final Stopwatch stopwatch;

    /**
     * 已存储的【许可】
     */
    double storedPermits;
    /**
     * 最大【许可】容量
     */
    double maxPermits;
    /**
     * 下次可用时间
     */
    long nextFreeTimeMs;
    /**
     * 生成一个【许可】需要的毫秒数
     */
    double onePermitCreateTimeInMs;

    public MyImplRateLimiter() {
        this.mutrix = new Object();
        this.stopwatch = Stopwatch.createStarted();
    }


    @Override
    public double acquire(int permits) {
        AssertUtil.isTrue(permits > 0, "permit必须大于0");
        long waitTime;
        synchronized (mutrix) {
            long currentTimeMs = stopwatch.elapsed(TimeUnit.MICROSECONDS);
            waitTime = Math.max(reSync(currentTimeMs) - currentTimeMs, 0);
            doAcquirePermits(permits);
        }
        try {
            Thread.sleep(TimeUnit.MILLISECONDS.convert(waitTime, TimeUnit.MICROSECONDS));
        } catch (InterruptedException e) {
            System.out.println(e);
            System.out.println("线程被打断休眠");
        }
        return 1.0 * waitTime / SECONDS.toMicros(1L);
    }

    @Override
    public boolean tryAcquire(int permits, long tryAcquireTime) {
        AssertUtil.isTrue(permits > 0, "permits必须大于0");
        AssertUtil.isTrue(tryAcquireTime > 0, "tryAcquireTime必须大于0");
        long waitTime;
        synchronized (mutrix) {
            long currentTimeMillis = stopwatch.elapsed(TimeUnit.MICROSECONDS);
            long availableTime = reSync(currentTimeMillis);
            if (currentTimeMillis + tryAcquireTime >= availableTime) {
                waitTime = Math.max(availableTime - currentTimeMillis, 0);
                doAcquirePermits(permits);
            } else {
                return false;
            }
        }

        try {
            Thread.sleep(TimeUnit.MILLISECONDS.convert(waitTime, TimeUnit.MICROSECONDS));
        } catch (InterruptedException e) {
            System.out.println(e);
            System.out.println("线程被打断休眠");
        }
        return false;
    }

    /**
     * 同步刷新令牌桶数据，返回令牌桶的可用时间
     *
     * @param currentTimeMs 当前时间戳
     * @return
     */
    private long reSync(long currentTimeMs) {
        if (currentTimeMs > this.nextFreeTimeMs) {
            double newPermits = (currentTimeMs - nextFreeTimeMs) / onePermitCreateTimeInMs;
            this.storedPermits = Math.min(maxPermits, this.storedPermits + newPermits);
            this.nextFreeTimeMs = currentTimeMs;
        }
        return this.nextFreeTimeMs;
    }

    /**
     * 执行获取【许可】的动作
     *
     * @param acquiredPermits
     */
    private void doAcquirePermits(double acquiredPermits) {
        double availablePermitsToSpend = min(acquiredPermits, this.storedPermits);
        double needFreshPermits = acquiredPermits - availablePermitsToSpend;
        this.storedPermits -= availablePermitsToSpend;
        this.nextFreeTimeMs += (long) (needFreshPermits * onePermitCreateTimeInMs);
    }

    /**
     * 设置生成【许可】的速率
     *
     * @param permitsPerSecond
     * @param permitsUnexpiredSeconds
     */
    public void setRate(double permitsPerSecond, double permitsUnexpiredSeconds) {
        synchronized (mutrix) {
            onePermitCreateTimeInMs = SECONDS.toMicros(1L) / permitsPerSecond;
            maxPermits = permitsPerSecond * permitsUnexpiredSeconds;
            reSync(stopwatch.elapsed(TimeUnit.MICROSECONDS));
        }
    }
}
