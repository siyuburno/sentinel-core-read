package com.demo.my.sentinel.example;


import com.demo.my.sentinel.MyContextUtil;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MySentinelTest {
    private static final String USER_INFO_API = "/sentinelTest/user/info";
    private static final String APP_A = "app_A";
    private static final String APP_B = "app_B";

    private static final Executor EXECUTOR_FOR_HTTP = new ThreadPoolExecutor(10, 20, 1000, TimeUnit.MICROSECONDS, new ArrayBlockingQueue<>(180));
    private static final String INVOKE_TYPE_HTTP = "HTTP";

    private static final Executor EXECUTOR_FOR_RPC = new ThreadPoolExecutor(3, 5, 1000, TimeUnit.MICROSECONDS, new ArrayBlockingQueue<>(100));
    private static final String INVOKE_TYPE_RPC = "RPC";


    public static void main(String[] args) {
        AtomicInteger COUNTER_FRO_APP_A = new AtomicInteger(0);
        AtomicInteger COUNTER_FRO_APP_B = new AtomicInteger(0);
        int i = 0;
        while (i++ < 100) {
            // 模拟A应用通过http的方式调用userInfo接口
            EXECUTOR_FOR_HTTP.execute(() -> userInfo(COUNTER_FRO_APP_A, INVOKE_TYPE_HTTP, APP_A));
            // 模拟B应用通过http的方式调用userInfo接口
            EXECUTOR_FOR_HTTP.execute(() -> userInfo(COUNTER_FRO_APP_B, INVOKE_TYPE_HTTP, APP_B));
            // 因为userInfo方法内部限流10qps，预期结果：A应用和B应用的pass请求数大概是10
        }
    }

    /**
     * 模拟用户影响查询接口，内部限流10qps
     * @param counter 计数器，作用是便于观察限流是否生效，可忽略
     * @param contextName 上下文名称，作用是区分调用场景
     * @param origin 来源，作用是区分调用来源
     * @return
     */
    public static String userInfo(AtomicInteger counter, String contextName, String origin) {
        try {
            MyContextUtil.entry(contextName, USER_INFO_API, 10);
            // 被保护的逻辑
            System.out.printf("请求次数[%s],应用[%s]通过[%s]调用资源[%s],pass%n", counter.addAndGet(1), origin, contextName, USER_INFO_API);
        } catch (Exception ex) {
            // 处理被流控的逻辑
            System.out.printf("请求次数[%s],应用[%s]通过[%s]调用资源[%s],block%n", counter.addAndGet(1), origin, contextName, USER_INFO_API);
        }
        MyContextUtil.exit();
        return "user";
    }
}
