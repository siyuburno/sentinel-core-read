package com.demo.sentinel.mysentinel;

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
    private AtomicReferenceArray<WindowWrap<MyMetricBucket>> array;

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
        WindowWrap<MyMetricBucket> windowWrap = array.get(index);
        if (windowWrap != null && windowWrap.getWindowStart() == windowStart) {
            return windowWrap.getData();
        }

        MyMetricBucket newMetricBucket = new MyMetricBucket();
        WindowWrap<MyMetricBucket> newWindowWrap = new WindowWrap<>(currentTimeMillis, intervalInMs, newMetricBucket);
        array.set(index, newWindowWrap);
        return newMetricBucket;
    }

    /**
     * 返回
     * @return
     */
    public MyMetricBucket sumAll() {
        MyMetricBucket sumAll = new MyMetricBucket();
        for (int i = 0; i < sampleCount; i++) {
            WindowWrap<MyMetricBucket> windowWrap = array.get(i);
            if (isAvailable(windowWrap) && windowWrap.getData() != null) {
                sumAll.addTotal(windowWrap.getData().getTotal());
                sumAll.addSuccess(windowWrap.getData().getSuccess());
                sumAll.addBlock(windowWrap.getData().getBlock());
                sumAll.addException(windowWrap.getData().getException());
            }
        }
        return sumAll;
    }

    /**
     * 判断传入的窗口是否可用（未过期）
     * @param windowWrap
     * @return
     */
    private boolean isAvailable(WindowWrap<MyMetricBucket> windowWrap) {
        return windowWrap != null && windowWrap.getWindowStart() >= ( System.currentTimeMillis() - (sampleCount * intervalInMs));
    }
}
