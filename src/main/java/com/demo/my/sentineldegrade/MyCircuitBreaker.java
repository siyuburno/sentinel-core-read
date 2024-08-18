package com.demo.my.sentineldegrade;

public interface MyCircuitBreaker {
    /**
     * 尝试通过
     */
    boolean tryPass();

    /**
     * 通过成功的回调
     */
    void onSuccess();

    /**
     * 通过失败的回调
     */
    void onBlock();

    /**
     * 执行失败的回调
     */
    void onError();
}
