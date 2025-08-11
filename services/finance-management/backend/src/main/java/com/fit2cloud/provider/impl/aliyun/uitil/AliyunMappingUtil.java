package com.fit2cloud.provider.impl.aliyun.uitil;

import com.aliyun.bssopenapi20171214.models.DescribeInstanceBillResponseBody;
import com.fit2cloud.common.platform.credential.Credential;
import com.fit2cloud.common.provider.util.CommonUtil;
import com.fit2cloud.es.entity.CloudBill;
import com.fit2cloud.provider.constants.BillModeConstants;
import com.fit2cloud.provider.constants.CurrencyConstants;
import com.fit2cloud.provider.impl.aliyun.entity.csv.AliBillCsvModel;
import com.fit2cloud.provider.impl.aliyun.entity.request.SyncBillRequest;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2022/10/14  5:39 PM}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class AliyunMappingUtil {

    /**
     * 将阿里云桶账单数据转换为系统账单对象
     *
     * @param aliBillCsvModel 阿里云桶cvs文件数据
     * @param regions         区域数据
     * @return 系统账单对象
     */
    public static CloudBill toCloudBill(AliBillCsvModel aliBillCsvModel, List<Credential.Region> regions) {
        CloudBill cloudBill = new CloudBill();
        cloudBill.setId(UUID.randomUUID().toString().replace("-", ""));
        cloudBill.setBillMode(toBillModeBucket(aliBillCsvModel.getSubscriptionType()));
        cloudBill.setRegionName(aliBillCsvModel.getRegionName());
        regions.stream().filter(region -> region.getName().equals(aliBillCsvModel.getRegionName())).findFirst().ifPresent(region -> cloudBill.setRegionId(region.getRegionId()));
        cloudBill.setZone(aliBillCsvModel.getZone());
        cloudBill.setProductId(aliBillCsvModel.getProductCode());
        cloudBill.setProductName(aliBillCsvModel.getProductName());
        cloudBill.setProductDetail(aliBillCsvModel.getProductDescName());
        cloudBill.setProjectName(aliBillCsvModel.getFinancialUnit());
        cloudBill.setProjectId(aliBillCsvModel.getFinancialUnit());
        cloudBill.setPayAccountId(aliBillCsvModel.getAccountId());
        cloudBill.setResourceId(aliBillCsvModel.getInstanceId());
        cloudBill.setResourceName(aliBillCsvModel.getRegionName());
        cloudBill.setTags(toTagsMap(aliBillCsvModel.getInstanceTag()));
        cloudBill.setCurrency(getCurrency(aliBillCsvModel.getCurrency()));
        cloudBill.setProvider("fit2cloud_ali_platform");
        cloudBill.setCost(toCost(aliBillCsvModel));
        LocalDateTime deductionDate = CommonUtil.getLocalDateTime(aliBillCsvModel.getBillingCycle());
        if (Objects.nonNull(aliBillCsvModel.getDate())) {
            deductionDate = CommonUtil.getLocalDateTime(aliBillCsvModel.getDate());
        }
        cloudBill.setUsageStartDate(deductionDate);
        cloudBill.setUsageEndDate(deductionDate);
        cloudBill.setDeductionDate(deductionDate);
        cloudBill.setBillingCycle(CommonUtil.getLocalDateTime(aliBillCsvModel.getBillingCycle()));

        return cloudBill;
    }

    /**
     * 将阿里云账单对象转化为系统账单对象
     *
     * @param item            阿里云账单对象
     * @param syncBillRequest 获取阿里云账单请求对象
     * @param regions         区域
     * @return 系统账单对象
     */
    public static CloudBill toCloudBill(DescribeInstanceBillResponseBody.DescribeInstanceBillResponseBodyDataItems item,
                                        SyncBillRequest syncBillRequest,
                                        List<Credential.Region> regions) {
        CloudBill cloudBill = new CloudBill();
        cloudBill.setId(UUID.randomUUID().toString().replace("-", ""));
        cloudBill.setBillMode(toBillMode(item.getSubscriptionType()));
        cloudBill.setRegionName(item.getRegion());
        regions.stream().filter(region -> region.getName().equals(item.getRegion())).findFirst().ifPresent(region -> cloudBill.setRegionId(region.getRegionId()));
        cloudBill.setZone(item.getZone());
        cloudBill.setTags(toTagsMap(item.getTag()));
        cloudBill.setProjectId(item.getCostUnit());
        cloudBill.setProductName(item.getProductName());
        cloudBill.setResourceId(item.getInstanceID());
        cloudBill.setProjectName(item.getCostUnit());
        cloudBill.setProductId(item.getProductCode());
        cloudBill.setProductDetail(item.getProductDetail());
        cloudBill.setResourceName(item.getNickName());
        cloudBill.setCurrency(getCurrency(item.getCurrency()));
        cloudBill.setProvider("fit2cloud_ali_platform");
        cloudBill.setCost(toCost(item));
        if (StringUtils.isNotEmpty(item.getBillingDate())) {
            cloudBill.setUsageStartDate(CommonUtil.getLocalDateTime(item.getBillingDate(), "yyyy-MM-dd"));
            cloudBill.setBillingCycle(CommonUtil.getLocalDateTime(item.getBillingDate(), "yyyy-MM-dd"));
            cloudBill.setUsageEndDate(CommonUtil.getLocalDateTime(item.getBillingDate(), "yyyy-MM-dd"));
            cloudBill.setDeductionDate(CommonUtil.getLocalDateTime(item.getBillingDate(), "yyyy-MM-dd"));
        } else {
            cloudBill.setUsageStartDate(CommonUtil.getLocalDateTime(syncBillRequest.getBillingDate(), "yyyy-MM-dd"));
            cloudBill.setBillingCycle(CommonUtil.getLocalDateTime(syncBillRequest.getBillingDate(), "yyyy-MM-dd"));
            cloudBill.setUsageEndDate(CommonUtil.getLocalDateTime(syncBillRequest.getBillingDate(), "yyyy-MM-dd"));
            cloudBill.setDeductionDate(CommonUtil.getLocalDateTime(syncBillRequest.getBillingDate(), "yyyy-MM-dd"));
        }
        cloudBill.setPayAccountId(item.getBillAccountID());
        return cloudBill;
    }

    private static String toBillModeBucket(String subscriptionType) {
        if (subscriptionType.equals("后付费")) {
            return BillModeConstants.ON_DEMAND.name();
        } else if (subscriptionType.equals("预付费")) {
            return BillModeConstants.MONTHLY.name();
        } else {
            return BillModeConstants.OTHER.name();
        }
    }

    private static CurrencyConstants getCurrency(String currency) {
        return Arrays.stream(CurrencyConstants.values())
                .filter(currencyConstants -> StringUtils.equals(currencyConstants.name(), currency))
                .findFirst().orElse(CurrencyConstants.CNY);
    }

    private static CloudBill.Cost toCost(DescribeInstanceBillResponseBody.DescribeInstanceBillResponseBodyDataItems item) {
        CloudBill.Cost cost = new CloudBill.Cost();
        cost.setCashAmount(Objects.nonNull(item.getCashAmount()) ? BigDecimal.valueOf(item.getCashAmount()) : BigDecimal.ZERO);
        cost.setOfficialAmount(Objects.nonNull(item.getPretaxGrossAmount()) ? BigDecimal.valueOf(item.getPretaxGrossAmount()) : BigDecimal.ZERO);
        cost.setCouponAmount(Objects.nonNull(item.getDeductedByCoupons()) ? BigDecimal.valueOf(item.getDeductedByCoupons()) : BigDecimal.ZERO);
        cost.setPayableAmount(Objects.nonNull(item.getPretaxAmount()) ? BigDecimal.valueOf(item.getPretaxAmount()) : BigDecimal.ZERO);
        return cost;
    }

    private static CloudBill.Cost toCost(AliBillCsvModel item) {
        CloudBill.Cost cost = new CloudBill.Cost();
        cost.setCashAmount(Objects.nonNull(item.getCashPayment()) ? BigDecimal.valueOf(item.getCashPayment()) : BigDecimal.ZERO);
        cost.setOfficialAmount(Objects.nonNull(item.getOfficialWebsitePrice()) ? BigDecimal.valueOf(item.getOfficialWebsitePrice()) : BigDecimal.ZERO);
        cost.setCouponAmount(Objects.nonNull(item.getVoucherDeduction()) ? BigDecimal.valueOf(item.getVoucherDeduction()) : BigDecimal.ZERO);
        cost.setPayableAmount(Objects.nonNull(item.getAmountPayable()) ? BigDecimal.valueOf(item.getAmountPayable()) : BigDecimal.ZERO);
        return cost;
    }

    /**
     * 订阅类型，取值：
     * <p>
     * Subscription：预付费。
     * PayAsYouGo：后付费。
     *
     * @param subscriptionType 阿里云计费模式
     * @return 云管计费模式
     */
    private static String toBillMode(String subscriptionType) {
        if (subscriptionType.equals("Subscription")) {
            return BillModeConstants.MONTHLY.name();
        } else if (subscriptionType.equals("PayAsYouGo")) {
            return BillModeConstants.ON_DEMAND.name();
        } else {
            return BillModeConstants.OTHER.name();
        }
    }

    /**
     * 获取标签map
     *
     * @param tags 标签
     * @return 标签对象
     */
    private static Map<String, Object> toTagsMap(String tags) {
        //  key:testKey value:testValue; key:testKey1 value:testValues1
        if (StringUtils.isNotEmpty(tags) && !tags.equals("-")) {
            String[] split = tags.split(";");
            return Arrays.stream(split).flatMap(item -> {
                String[] s = item.split(" ");
                HashMap<String, Object> paramsTags = new HashMap<>();
                if (s.length == 1) {
                    String key = s[0].replace("key:", "").trim();
                    paramsTags.put(key, "");
                }
                if (s.length == 2) {
                    String key = s[0].replace("key:", "").trim();
                    String value = s[1].replace("value:", "").trim();
                    paramsTags.put(key, value);
                }
                return paramsTags.entrySet().stream();
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            return new HashMap<>();
        }
    }

}
