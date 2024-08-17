package com.demo.my.sentinel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MyContextUtil {
    private static ThreadLocal<MyContext> myContextHolder = new ThreadLocal<>();
    private static Map<String, MyLeapArray> statisticMap = new HashMap<>();
    private static Lock lock = new ReentrantLock();

    public static void entry(String contextName, String resourceName, long qps) throws Exception {
        MyContext myContext = myContextHolder.get();
        if (myContext == null) {
            myContext = new MyContext(contextName, resourceName);
            myContextHolder.set(myContext);
        }

        String key = contextName + "_" + resourceName;
        MyLeapArray myLeapArray = statisticMap.get(key);
        if (myLeapArray == null) {
            lock.lock();
            try {
                myLeapArray = statisticMap.get(key);
                if (myLeapArray == null) {
                    myLeapArray = new MyLeapArray(500, 2);
                    statisticMap.put(key, myLeapArray);
                }
            } catch (Exception e) {
                System.out.println(e);
            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }

        // 获取当前时间窗口
        MyMetricBucket currentMB = myLeapArray.getCurrentV2();
        // 总请求数+1
        currentMB.addTotal(1);
        // 计算整个滑动时间窗口的总数据
        MyMetricBucket summedAll = myLeapArray.sumAll();
        // 当前的qps > 我们设置的阈值
        if (summedAll.getTotal() > qps) {
            // block数+1，并抛出异常
            currentMB.addBlock(1);
            throw new Exception("QPS >" + qps);
        }
        // 请求pass数+1
        currentMB.addSuccess(1);
    }

    public static void exit() {
        MyContext myContext = myContextHolder.get();
        if (myContext != null) {
            myContextHolder.set(null);
        }
    }

}
