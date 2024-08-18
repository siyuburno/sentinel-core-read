package com.demo.my.sentinel;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

public class MyLeapArray {
    /**
     * 样例时长
     */
    private long intervalInMs;
    /**
     * 采样数
     */
    private int sampleCount;
    /**
     * 数据数组
     */
    private AtomicReferenceArray<MyWindowWrap<MyMetricBucket>> array;

    private ReentrantLock updateLock = new ReentrantLock();

    public MyLeapArray(long intervalInMs, int windowNum) {
        this.intervalInMs = intervalInMs;
        this.sampleCount = windowNum;
        this.array = new AtomicReferenceArray<>(windowNum);
    }

    /**
     * 获取当前时间的滑动窗口（性能优化版本）
     * @return 当前时间的指标桶
     */
    public MyMetricBucket getCurrentV2() {
        long currentTimeMills = System.currentTimeMillis();
        long currentWindowStart = currentTimeMills - (currentTimeMills % intervalInMs);
        int idx = (int) ((currentTimeMills / intervalInMs) % sampleCount);
        while (true) {
            MyWindowWrap<MyMetricBucket> old = array.get(idx);
            if (old == null) {
                MyWindowWrap<MyMetricBucket> newMyWindowWrap = new MyWindowWrap<>(currentTimeMills, intervalInMs, new MyMetricBucket());
                if (array.compareAndSet(idx, null, newMyWindowWrap)) {
                    return newMyWindowWrap.getData();
                } else {
                    Thread.yield();
                }
            } else if (currentWindowStart == old.getWindowStart()) {
                return old.getData();
            } else if (currentWindowStart > old.getWindowStart()) {
                if (updateLock.tryLock()) {
                    old.setWindowStart(currentWindowStart);
                    old.getData().reset();
                    return old.getData();
                }else {
                    Thread.yield();
                }
            } else {
                throw new RuntimeException("获取当前时间的滑动窗口数据异常，currentWindowStart < old.getWindowStart()");
            }
        }
    }

    /**
     * 获取当前滑动窗口（synchronized版本）
     * @return  当前时间的指标桶
     */
    public synchronized MyMetricBucket getCurrentV1() {
        long currentTimeMillis = System.currentTimeMillis();
        int index = (int) ((currentTimeMillis / intervalInMs) % sampleCount);
        long windowStart = currentTimeMillis - (currentTimeMillis % intervalInMs);
        MyWindowWrap<MyMetricBucket> myWindowWrap = array.get(index);
        if (myWindowWrap != null && myWindowWrap.getWindowStart() == windowStart) {
            return myWindowWrap.getData();
        }

        MyMetricBucket newMetricBucket = new MyMetricBucket();
        MyWindowWrap<MyMetricBucket> newMyWindowWrap = new MyWindowWrap<>(currentTimeMillis, intervalInMs, newMetricBucket);
        array.set(index, newMyWindowWrap);
        return newMetricBucket;
    }

    /**
     * 返回
     * @return
     */
    public MyMetricBucket sumAll() {
        MyMetricBucket sumAll = new MyMetricBucket();
        for (int i = 0; i < sampleCount; i++) {
            MyWindowWrap<MyMetricBucket> myWindowWrap = array.get(i);
            if (isAvailable(myWindowWrap) && myWindowWrap.getData() != null) {
                sumAll.addTotal(myWindowWrap.getData().getTotal());
                sumAll.addSuccess(myWindowWrap.getData().getSuccess());
                sumAll.addBlock(myWindowWrap.getData().getBlock());
                sumAll.addException(myWindowWrap.getData().getException());
            }
        }
        return sumAll;
    }

    /**
     * 重置数据
     */
    public void reset() {
        for (int i = 0; i < array.length(); i++) {
            MyWindowWrap<MyMetricBucket> item = array.get(i);
            if (Objects.nonNull(item) && Objects.nonNull(item.getData())) {
                item.getData().reset();
            }
        }
    }

    /**
     * 判断传入的窗口是否可用（未过期）
     * @param myWindowWrap
     * @return
     */
    private boolean isAvailable(MyWindowWrap<MyMetricBucket> myWindowWrap) {
        return myWindowWrap != null && myWindowWrap.getWindowStart() >= ( System.currentTimeMillis() - (sampleCount * intervalInMs));
    }
}
