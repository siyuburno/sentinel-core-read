package com.demo.sentinel.mysentinel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ContextUtil {
    private static ThreadLocal<MyContext> myContextHolder = new ThreadLocal<>();
    private static Map<String, MyLeapArray> statisticMap = new HashMap<>();
    private static Lock lock = new ReentrantLock();

    public static void entry(String contextName, String resourceName) throws Exception {
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
                myLeapArray= statisticMap.get(key);
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

        MyMetricBucket summedAll = myLeapArray.sumAll();
        if (summedAll.getTotal() > 10) {
            myLeapArray.getCurrent().addTotal(1);
            myLeapArray.getCurrent().addBlock(1);
            throw new Exception("QPS > 10");
        }
        myLeapArray.getCurrent().addTotal(1);
        myLeapArray.getCurrent().addSuccess(1);
    }

    public static void exit() {
        MyContext myContext = myContextHolder.get();
        if (myContext != null) {
            myContextHolder.set(null);
        }
    }

}
