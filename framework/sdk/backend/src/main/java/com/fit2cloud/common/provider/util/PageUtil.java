package com.fit2cloud.common.provider.util;

import com.fit2cloud.common.provider.exception.ReTryException;
import com.fit2cloud.common.provider.exception.SkipPageException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.*;

/**
 * @Author:张少虎
 * @Date: 2022/9/21  8:55 AM
 * @Version 1.0
 * @注释:
 */
public class PageUtil {
    /**
     * 同步重试次数
     */
    protected static final Integer DefaultReTry = 5;
    /**
     * 默认第一页
     */
    public static final Integer DefaultCurrentPage = 1;
    /**
     * 默认每页100条
     */
    public static final Integer DefaultPageSize = 100;

    /**
     * @param request    请求参数
     * @param exec       执行函数
     * @param getList    获取数据
     * @param hasNext    是否有下一条
     * @param next       下一条
     * @param <T>        返回值类型
     * @param <Request>  请求对象类型
     * @param <Response> 接口返回类型
     * @return 分页查询到到全量数据
     */
    public static <T, Request, Response> List<T> pageNextF(Request request, Function<Request, Response> exec, Function<Response, List<T>> getList, BiFunction<Request, Response, Boolean> hasNext, BiFunction<Request, Response, Request> next) {
        ArrayList<T> collector = new ArrayList<>();
        return page(request, exec, getList, hasNext, next, DefaultReTry, collector);
    }

    /**
     * @param request    请求参数
     * @param exec       执行函数
     * @param getList    获取数据
     * @param hasNext    是否有下一条
     * @param next       下一条
     * @param <T>        返回值类型
     * @param <Request>  请求对象类型
     * @param <Response> 接口返回类型
     * @return 分页查询到到全量数据
     */
    public static <T, Request, Response> List<T> page(Request request, Function<Request, Response> exec, Function<Response, List<T>> getList, BiFunction<Request, Response, Boolean> hasNext, Consumer<Request> next) {
        ArrayList<T> collector = new ArrayList<>();
        return page(request, exec, getList, hasNext, (req, res) -> {
            next.accept(req);
        }, DefaultReTry, collector);
    }

    /**
     * @param request     请求参数
     * @param exec        执行函数
     * @param getList     获取数据
     * @param hasNext     是否有下一条
     * @param next        下一条
     * @param reTryNumber 重试次数
     * @param <T>         返回值类型
     * @param <Request>   请求对象类型
     * @param <Response>  接口返回类型
     * @return 分页查询到到全量数据
     */
    public static <T, Request, Response> List<T> page(Request request, Function<Request, Response> exec, Function<Response, List<T>> getList, BiFunction<Request, Response, Boolean> hasNext, Consumer<Request> next, Integer reTryNumber) {
        ArrayList<T> collector = new ArrayList<>();
        return page(request, exec, getList, hasNext, (req, res) -> {
            next.accept(req);
        }, reTryNumber, collector);
    }

    /**
     * @param request     请求参数
     * @param exec        执行函数
     * @param getList     获取数据
     * @param hasNext     是否有下一条
     * @param next        下一条
     * @param reTryNumber 重试次数
     * @param <T>         返回值类型
     * @param <Request>   请求对象类型
     * @param <Response>  接口返回类型
     * @return 分页查询到到全量数据
     */
    public static <T, Request, Response> List<T> page(Request request, Function<Request, Response> exec, Function<Response, List<T>> getList, BiFunction<Request, Response, Boolean> hasNext, BiConsumer<Request, Response> next, Integer reTryNumber) {
        ArrayList<T> collector = new ArrayList<>();
        return page(request, exec, getList, hasNext, next, reTryNumber, collector);
    }

    /**
     * @param request    请求参数
     * @param exec       执行函数
     * @param getList    获取数据
     * @param hasNext    是否有下一条
     * @param next       下一条
     * @param <T>        返回值类型
     * @param <Request>  请求对象类型
     * @param <Response> 接口返回类型
     * @return 分页查询到到全量数据
     */
    public static <T, Request, Response> List<T> page(Request request, Function<Request, Response> exec, Function<Response, List<T>> getList, BiFunction<Request, Response, Boolean> hasNext, BiConsumer<Request, Response> next) {
        ArrayList<T> collector = new ArrayList<>();
        return page(request, exec, getList, hasNext, next, DefaultReTry, collector);
    }

    /**
     * @param request    请求参数
     * @param exec       执行函数
     * @param getList    获取数据
     * @param hasNext    是否有下一条
     * @param next       下一条
     * @param reTry      重试次数
     * @param collector  数据收集器
     * @param <T>        返回值类型
     * @param <Request>  请求对象类型
     * @param <Response> 接口返回类型
     * @return 分页查询到到全量数据
     */
    private static <T, Request, Response> List<T> page(Request request, Function<Request, Response> exec, Function<Response, List<T>> getList, BiFunction<Request, Response, Boolean> hasNext, BiConsumer<Request, Response> next, Integer reTry, List<T> collector) {
        return page(request, exec, getList, hasNext, (req, res) -> {
            next.accept(req, res);
            return req;
        }, reTry, collector);
    }


    /**
     * @param request    请求参数
     * @param exec       执行函数
     * @param getList    获取数据
     * @param hasNext    是否有下一条
     * @param next       下一条
     * @param reTry      重试次数
     * @param collector  数据收集器
     * @param <T>        返回值类型
     * @param <Request>  请求对象类型
     * @param <Response> 接口返回类型
     * @return 分页查询到到全量数据
     */
    private static <T, Request, Response> List<T> page(Request request, Function<Request, Response> exec, Function<Response, List<T>> getList, BiFunction<Request, Response, Boolean> hasNext, BiFunction<Request, Response, Request> next, Integer reTry, List<T> collector) {
        Response response = null;
        try {
            response = exec.apply(request);
        } catch (Exception e) {
            if (reTry > 0 && e instanceof ReTryException) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
                page(request, exec, getList, hasNext, next, reTry - 1, collector);
            } else if (e instanceof SkipPageException) {
                return collector;
            } else {
                throw e;
            }

        }
        if (response != null) {
            List<T> responseList = getList.apply(response);
            collector.addAll(responseList);
            if (hasNext.apply(request, response)) {
                Request nextReq = next.apply(request, response);
                page(nextReq, exec, getList, hasNext, next, reTry, collector);
            }
        }
        return collector;
    }

    /**
     * @param exec        执行函数
     * @param reTryNumber 重试次数
     * @param <Response>  响应对象
     * @return 响应对象
     */
    public static <Response> Response reTry(Supplier<Response> exec, Integer reTryNumber) {
        try {
            return exec.get();
        } catch (Exception e) {
            if (reTryNumber > 0 && e instanceof ReTryException) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
                return reTry(exec, reTryNumber);
            } else if (e instanceof SkipPageException) {
                return null;
            }
            throw e;
        }
    }
}
