package com.demo.sentinel.mysentinel;

public class WindowWrap<D> {
    private long timestamp;
    private long windowStart;
    private long intervalInMs;
    private D data;

    public WindowWrap(long timestamp, long intervalInMs, D data) {
        this.timestamp = timestamp;
        this.intervalInMs = intervalInMs;
        this.data = data;
        this.windowStart = timestamp - (timestamp % intervalInMs);
    }

    public long getWindowStart() {
        return windowStart;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getIntervalInMs() {
        return intervalInMs;
    }

    public D getData() {
        return data;
    }
}
