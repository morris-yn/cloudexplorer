package com.fit2cloud.provider.impl.aliyun.entity.request;

import com.aliyun.ecs20140526.models.DescribeInstancesRequest;
import com.fit2cloud.provider.impl.aliyun.entity.credential.AliSecurityComplianceCredential;
import lombok.Data;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2022/12/6  11:30}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class ListEcsInstancesRequest extends DescribeInstancesRequest {
    /**
     * 认证信息
     */
    private AliSecurityComplianceCredential credential;


}
