package com.fit2cloud.provider.entity;

import java.util.Arrays;
import java.util.List;

import static com.fit2cloud.provider.entity.InstanceFieldCompare.*;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2022/12/7  14:28}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public enum InstanceFieldType {
    /**
     * 枚举
     */
    Enum(EQ, NOT_EQ, EXIST, NOT_EXIST),
    /**
     * 字符串
     */
    String(EQ, NOT_EQ, STRING_CONTAIN, INCLUDED_IN, EXIST, NOT_EXIST),
    /**
     * 数字
     */
    Number(GT, GE, LT, LE, EQ, EXIST, NOT_EXIST),
    /**
     * 普通数组字符串
     */
    ArrayString(CONTAIN, NOT_CONTAIN, LENGTH_GE, LENGTH_GT, LENGTH_LE, LENGTH_LT, EXIST, NOT_EXIST),
    /**
     * 普通数组枚举
     */
    ArrayEnum(CONTAIN, NOT_CONTAIN, EXIST, NOT_EXIST),
    /**
     * 普通数组数字
     */
    ArrayNumber(LENGTH_GE, LENGTH_GT, LENGTH_LE, LENGTH_LT, AVG_GE, AVG_GT, AVG_LE, AVG_LT, SUM_GE, SUM_GT, SUM_LE, SUM_LT, EXIST, NOT_EXIST),
    /**
     * 嵌套数组字符串
     */
    NestedArrayString(EQ, EXIST, NOT_EXIST),
    /**
     * 嵌套数组枚举
     */
    NestedArrayEnum(EQ, EXIST, NOT_EXIST),
    /**
     * 嵌套数组数字
     */
    NestedArrayNumber(EQ, GE, GT, LE, LT, EXIST, NOT_EXIST),
    /**
     * 时间
     */
    Date(GE, LE);

    /**
     * 字段对比器
     */
    private final List<InstanceFieldCompare> compares;

    InstanceFieldType(InstanceFieldCompare... compares) {
        this.compares = Arrays.stream(compares).toList();
    }

    public List<InstanceFieldCompare> getCompares() {
        return compares;
    }
}
