package com.fit2cloud.provider.impl.huawei.entity.request;

import com.fit2cloud.provider.impl.huawei.entity.credential.HuaweiBillCredential;
import lombok.Data;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2022/11/29  12:10}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class ListBucketMonthRequest {
    /**
     * 认证数据
     */
    private HuaweiBillCredential credential;
    /**
     * 账单信息
     */
    private HuaweiBill bill;
}
