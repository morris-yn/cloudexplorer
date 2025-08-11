package com.fit2cloud.es.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fit2cloud.common.annotaion.BillField;
import com.fit2cloud.common.child_key.impl.OrgTreeChildKey;
import com.fit2cloud.common.child_key.impl.TagChildKey;
import com.fit2cloud.common.conver.impl.*;
import com.fit2cloud.provider.constants.CurrencyConstants;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2022/10/14  3:35 PM}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Document(indexName = "ce-cloud-bill")
@Data
public class CloudBill {
    @Id
    private String id;

    @BillField(label = "组织层级树", group = true, conver = OrganizationConvert.class, childKey = OrgTreeChildKey.class)
    @Field(type = FieldType.Object)
    private Map<String, Object> orgTree;

    @BillField(label = "工作空间", group = true, conver = WorkSpaceConvert.class)
    @Field(type = FieldType.Keyword)
    private String workspaceId;

    @BillField(label = "组织", group = true, conver = OrganizationConvert.class)
    @Field(type = FieldType.Keyword)
    private String organizationId;

    @BillField(label = "资源id")
    @Field(type = FieldType.Keyword)
    private String resourceId;

    @BillField(label = "资源名称", group = true)
    @MultiField(mainField = @Field(type = FieldType.Text),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword))
    private String resourceName;

    @BillField(label = "企业项目Id")
    @Field(type = FieldType.Keyword)
    private String projectId;

    @BillField(label = "企业项目", group = true, authorize = true)
    @MultiField(mainField = @Field(type = FieldType.Text),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword))
    private String projectName;

    @BillField(label = "付款账号")
    @Field(type = FieldType.Keyword)
    private String payAccountId;

    @BillField(label = "账期")
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime billingCycle;

    @BillField(label = "扣费时间")
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime deductionDate;

    @BillField(label = "账单开始时间")
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime usageStartDate;

    @BillField(label = "账单结束时间")
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime usageEndDate;

    @BillField(label = "云平台", group = true, authorize = true, conver = ProviderConvert.class)
    @Field(type = FieldType.Keyword)
    private String provider;

    @BillField(label = "产品id")
    @Field(type = FieldType.Keyword)
    private String productId;

    @BillField(label = "产品名称", group = true, authorize = true)
    @MultiField(mainField = @Field(type = FieldType.Text),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword))
    private String productName;

    @BillField(label = "产品明细", group = true, authorize = true)
    @MultiField(mainField = @Field(type = FieldType.Text),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword))
    private String productDetail;

    @BillField(label = "计费模式", group = true, conver = BillModeConvert.class)
    @Field(type = FieldType.Keyword)
    private String billMode;

    @BillField(label = "区域Id")
    @Field(type = FieldType.Keyword)
    private String regionId;

    @BillField(label = "区域", group = true)
    @MultiField(mainField = @Field(type = FieldType.Text),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword))
    private String regionName;
    @BillField(label = "可用区")
    @Field(type = FieldType.Keyword)
    private String zone;

    @BillField(label = "云账号", group = true, authorize = true, conver = CloudAccountConvert.class)
    @Field(type = FieldType.Keyword)
    private String cloudAccountId;

    @BillField(label = "标签", authorize = true, group = true, childKey = TagChildKey.class)
    @Field(type = FieldType.Object)
    private Map<String, Object> tags;

    @BillField(label = "价格")
    @Field(type = FieldType.Object)
    private Cost cost;

    @BillField(label = "币种")
    @Field(type = FieldType.Keyword)
    private CurrencyConstants currency;

    /**
     * 使用runtime_mappings聚合查询太慢, 将聚合数据冗余起来
     */
    @BillField(label = "聚合字段")
    @Field(type = FieldType.Object)
    private Map<String, String> aggregations;

    /**
     * 冗余数据
     * 其他数据都加这里
     */
    @BillField(label = "元数据")
    @Field(type = FieldType.Object)
    private Map<String, Object> meta;


    public static class Cost extends HashMap<String, BigDecimal> {
        /**
         * 现金支付
         *
         * @param cashAmount 现金支付
         */
        public void setCashAmount(BigDecimal cashAmount) {
            put("cashAmount", cashAmount);
        }

        /**
         * @param couponAmount 代金券支付
         */
        public void setCouponAmount(BigDecimal couponAmount) {
            put("couponAmount", couponAmount);
        }

        /**
         * @param officialAmount 官网价钱
         */
        public void setOfficialAmount(BigDecimal officialAmount) {
            put("officialAmount", officialAmount);
        }

        /**
         * @param payableAmount 应付金额
         */
        public void setPayableAmount(BigDecimal payableAmount) {
            put("payableAmount", payableAmount);
        }

    }


}
