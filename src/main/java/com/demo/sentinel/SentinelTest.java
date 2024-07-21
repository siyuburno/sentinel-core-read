package com.demo.sentinel;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SentinelTest {
    private static final String USER_INFO_API = "/sentinelTest/user/info";
    private static final String APP_A = "app_A";
    private static final String APP_B = "app_B";

    private static final Executor EXECUTOR_FOR_HTTP = new ThreadPoolExecutor(10, 20, 1000, TimeUnit.MICROSECONDS, new ArrayBlockingQueue<>(180));
    private static final String INVOKE_TYPE_HTTP = "HTTP";

    private static final Executor EXECUTOR_FOR_RPC = new ThreadPoolExecutor(3, 5, 1000, TimeUnit.MICROSECONDS, new ArrayBlockingQueue<>(100));
    private static final String INVOKE_TYPE_RPC = "RPC";

    private static void initFlowRules(){
        List<FlowRule> rules = new ArrayList<>();
        FlowRule rule = new FlowRule();
        rule.setLimitApp(APP_A);
        rule.setResource(USER_INFO_API);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        // Set limit QPS to 20.
        rule.setCount(20);
        rules.add(rule);
        FlowRuleManager.loadRules(rules);
    }

    public static void main(String[] args) {
        // 配置规则.
        initFlowRules();
        int i = 0;

        AtomicInteger COUNTER_FRO_APP_A = new AtomicInteger(0);
        AtomicInteger COUNTER_FRO_APP_B = new AtomicInteger(0);
        while (i++ < 100) {
            EXECUTOR_FOR_HTTP.execute(() -> userInfo(COUNTER_FRO_APP_A, INVOKE_TYPE_HTTP, APP_A));
            EXECUTOR_FOR_HTTP.execute(() -> userInfo(COUNTER_FRO_APP_B, INVOKE_TYPE_HTTP, APP_B));
        }
    }

    public static String userInfo(AtomicInteger counter, String contextName, String origin) {
        ContextUtil.enter(contextName, origin);
        try (Entry entry = SphU.entry(USER_INFO_API)) {
            // 被保护的逻辑
            System.out.printf("请求次数[%s],应用[%s]通过[%s]调用资源[%s],pass%n", counter.addAndGet(1), origin, contextName, USER_INFO_API);
        } catch (BlockException ex) {
            // 处理被流控的逻辑
            System.out.printf("请求次数[%s],应用[%s]通过[%s]调用资源[%s],block%n", counter.addAndGet(1), origin, contextName, USER_INFO_API);
        }
        ContextUtil.exit();
        return "user";
    }

}
