package com.demo.my.sentineldegrade.example;

import com.demo.my.sentineldegrade.MyErrorCircuitBreaker;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MyCircuitBreakerTest {
    private static final Executor executor = new ThreadPoolExecutor(3, 5, 1000, TimeUnit.MICROSECONDS, new ArrayBlockingQueue<>(100));

    private static final MyErrorCircuitBreaker circuitBreaker = new MyErrorCircuitBreaker(500, 4);

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                int finalJ = j;
                executor.execute(()->{
                    try {
                        if (circuitBreaker.tryPass()) {
                            userInfo(finalJ);
                            circuitBreaker.onSuccess();
                        }else {
                            circuitBreaker.onBlock();
                            System.out.println("熔断中");
                        }
                    } catch (Exception e) {
                        circuitBreaker.onError();
                        System.out.println(e.getMessage());
                    }
                });
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("-------------------------------------");
        }
    }

    /**
     * 模拟自己的业务逻辑
     */
    public static void userInfo(int userId) {
        if (userId % 2==1) {
            throw new RuntimeException("查询用户信息失败");
        }
        System.out.println("查询用户信息成功");
    }
}
