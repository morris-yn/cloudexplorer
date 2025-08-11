package com.fit2cloud.autoconfigure;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Author:张少虎
 * @Date: 2022/9/19  9:44 AM
 * @Version 1.0
 * @注释:
 */
@Component
public class ThreadPoolConfig {

    /**
     * 工作线程池
     *
     * @return 工作线程池
     */
    @Bean(name = "workThreadPool")
    public ThreadPoolExecutor workThreadPool() {
        //todo 核心线程4个,最大线程16个,活跃时间30秒(当线程池已经到最大线程池后30秒后清除不活跃线程),活跃时间单位秒,阻塞线程10个,线程生产工厂:默认的线程工厂,拒绝策略:当线程超过最大线程+阻塞队列后会抛出错误RejectedExecutionException
        return new ThreadPoolExecutor(4, 100, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
    }

}
