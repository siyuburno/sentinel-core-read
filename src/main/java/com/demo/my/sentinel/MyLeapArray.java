package com.demo.my.sentinel;

import java.util.concurrent.atomic.AtomicReferenceArray;

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

    public MyLeapArray(long intervalInMs, int windowNum) {
        this.intervalInMs = intervalInMs;
        this.sampleCount = windowNum;
        this.array = new AtomicReferenceArray<>(windowNum);
    }

    /**
     * 获取当前滑动窗口
     * @return
     */
    public synchronized MyMetricBucket getCurrent() {
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
     * 判断传入的窗口是否可用（未过期）
     * @param myWindowWrap
     * @return
     */
    private boolean isAvailable(MyWindowWrap<MyMetricBucket> myWindowWrap) {
        return myWindowWrap != null && myWindowWrap.getWindowStart() >= ( System.currentTimeMillis() - (sampleCount * intervalInMs));
    }
}
