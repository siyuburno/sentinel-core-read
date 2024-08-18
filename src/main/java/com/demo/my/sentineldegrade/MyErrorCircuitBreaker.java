package com.demo.my.sentineldegrade;

import com.demo.my.sentinel.MyLeapArray;

import java.util.concurrent.atomic.AtomicReference;

public class MyErrorCircuitBreaker implements MyCircuitBreaker {
    /**
     * 熔断阈值
     */
    private double threshold;
    /**
     * 熔断器当前状态
     */
    private AtomicReference<Status> currentStatus;
    /**
     * 熔断器恢复的间隔时间，毫秒
     */
    private long recoveryIntervalTimeMills;
    /**
     * 熔断器下一次可用时间
     */
    private volatile long nextAvailableTimeMills;
    /**
     * 统计器，记录请求时间
     */
    private MyLeapArray statistic;

    public MyErrorCircuitBreaker(long recoveryIntervalTimeMills, long threshold) {
        this.recoveryIntervalTimeMills = recoveryIntervalTimeMills;
        this.threshold = threshold;
        this.statistic = new MyLeapArray(250, 4);
        this.currentStatus = new AtomicReference<>(Status.CLOSE);
    }


    @Override
    public boolean tryPass() {
        if (currentStatus.get() == Status.CLOSE) {
            return true;
        }
        if (currentStatus.get() == Status.OPEN) {
            return arrivePassOneTime() && fromOpen2HalfOpen();
        }
        return false;
    }

    @Override
    public void onSuccess() {
        statistic.getCurrentV2().addTotal(1);
        statistic.getCurrentV2().addSuccess(1);
        if (currentStatus.get() == Status.HALF_OPEN) {
            fromHalfOpen2Close();
        }
    }

    @Override
    public void onError() {
        statistic.getCurrentV2().addTotal(1);
        statistic.getCurrentV2().addException(1);
        if (currentStatus.get() == Status.HALF_OPEN) {
            fromHalfOpen2Open();
            return;
        }
        if (currentStatus.get() == Status.CLOSE) {
            if (statistic.sumAll().getException() > threshold) {
                fromClose2Open();
            }
        }
    }

    @Override
    public void onBlock() {
        statistic.getCurrentV2().addTotal(1);
        statistic.getCurrentV2().addBlock(1);
    }

    /**
     * 是否到了放行一请求的时间
     * @return
     */
    private boolean arrivePassOneTime() {
        return System.currentTimeMillis() > nextAvailableTimeMills;
    }

    /**
     * 刷新熔断器下一次可用时间
     */
    private void refreshNextAvailableTimeMills() {
        this.nextAvailableTimeMills = System.currentTimeMillis() + recoveryIntervalTimeMills;
    }

    /**
     * 将熔断器状态从CLOSE改为OPEN，并刷新熔断器下次可用时间
     * @return
     */
    private boolean fromClose2Open() {
        if (currentStatus.compareAndSet(Status.CLOSE, Status.OPEN)) {
            refreshNextAvailableTimeMills();
            return true;
        }
        return false;
    }

    /**
     * 熔断器状态从OPEN改为HALF_OPEN
     * @return
     */
    private boolean fromOpen2HalfOpen() {
        return currentStatus.compareAndSet(Status.OPEN, Status.HALF_OPEN);
    }

    /**
     * 熔断器状态从HALF_OPEN改为OPEN，并刷新熔断器下次可用时间
     * @return
     */
    private boolean fromHalfOpen2Open(){
        if (currentStatus.compareAndSet(Status.HALF_OPEN, Status.OPEN)) {
            refreshNextAvailableTimeMills();
            return true;
        }
        return false;
    }

    /**
     * 熔断器状态从HALF_OPEN改为CLOSE
     * @return
     */
    private boolean fromHalfOpen2Close() {
        if (currentStatus.compareAndSet(Status.HALF_OPEN, Status.CLOSE)) {
            statistic.reset();
            return true;
        }
        return false;
    }


    enum Status{
        /**
         * 开启状态，表示异常次数已经达到阈值，后续请求需要被快速失败
         */
        OPEN,
        /**
         * 关闭状态，表示异常次数未达到阈值，后续请求可以放行
         */
        CLOSE,
        /**
         * 半开启状态，表示可以放行一个请求
         * 1. 如果请求执行成功，说明系统已恢复，重置接口统计数据，然后将熔断器的状态改为CLOSE。
         * 2. 如果请求执行失败，说明系统未恢复，需要重新将熔断器状态改为OPEN
         */
        HALF_OPEN
    }
}
