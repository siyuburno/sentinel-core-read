package com.demo.sentinel.mysentinel;

public class MyMetricBucket {
    /**
     * 总数
     */
    private volatile long total;
    /**
     * 成功数
     */
    private volatile long success;
    /**
     * 阻塞数
     */
    private volatile long block;
    /**
     * 异常数
     */
    private volatile long exception;

    public MyMetricBucket() {
        this.total = 0;
        this.success = 0;
        this.block = 0;
        this.exception = 0;
    }

    public synchronized void addTotal(long count) {
        this.total += count;
    }

    public synchronized void addSuccess(long count) {
        this.success += count;
    }

    public synchronized void addBlock(long count) {
        this.block += count;
    }

    public synchronized void addException(long count) {
        this.exception += count;
    }

    public long getTotal() {
        return total;
    }

    public long getSuccess() {
        return success;
    }

    public long getBlock() {
        return block;
    }

    public long getException() {
        return exception;
    }
}
