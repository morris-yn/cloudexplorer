package com.fit2cloud.provider.impl.aliyun.entity.request;

import com.aliyun.dds20151201.models.DescribeDBInstancesRequest;
import com.fit2cloud.provider.impl.aliyun.entity.credential.AliSecurityComplianceCredential;
import lombok.Data;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2023/1/5  11:01}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class ListMongoDBRequest extends DescribeDBInstancesRequest {
    /**
     * 认证信息
     */
    private AliSecurityComplianceCredential credential;


}
