package com.fit2cloud.provider.impl.tencent.entity.request;

import com.fit2cloud.provider.impl.tencent.entity.credential.TencentBillCredential;
import com.tencentcloudapi.billing.v20180709.models.DescribeBillDetailRequest;
import lombok.Data;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2022/10/14  10:51 AM}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class SyncBillRequest extends DescribeBillDetailRequest {
    /**
     * 认证信息
     */
    private TencentBillCredential credential;
    /**
     * 账单信息
     */
    private TencentBill bill;
    /**
     * 云账号idZ
     */
    private String cloudAccountId;

    @Override
    public void setMonth(String Month) {
        super.setMonth(Month);
    }
}
