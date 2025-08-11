package com.fit2cloud.controller.request;

import com.fit2cloud.base.entity.json_entity.BillingField;
import com.fit2cloud.base.entity.json_entity.PackagePriceBillingPolicy;
import com.fit2cloud.base.mapper.BaseBillPolicyMapper;
import com.fit2cloud.common.form.vo.Form;
import com.fit2cloud.common.validator.annnotaion.CustomValidated;
import com.fit2cloud.common.validator.group.ValidationGroup;
import com.fit2cloud.common.validator.handler.ExistHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2023/6/1  15:06}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class BillingPolicyRequest {

    @Schema(title = "策略名称")
    @Length(min = 1, max = 255, message = "计费策略名称长度必须在1-255之间")
    @CustomValidated(groups = ValidationGroup.SAVE.class, mapper = BaseBillPolicyMapper.class, handler = ExistHandler.class, field = "name", message = "策略名称已存在", exist = true)
    @NotNull(message = "策略名称不能为空")
    private String name;

    @Schema(title = "关联云账号列表")
    private List<String> cloudAccountList;

    @Schema(title = "策略详情")
    private List<BillingPolicyDetails> billingPolicyDetailsList;

    @Data
    public static class BillingPolicyDetails {
        /**
         * 资源类型
         */
        private String resourceType;

        /**
         * 单价(按需)计费策略
         */
        private List<BillingField> unitPriceOnDemandBillingPolicy;

        /**
         * 单价(包年包月)计费策略
         */
        private List<BillingField> unitPriceMonthlyBillingPolicy;

        /**
         * 套餐计费策略
         */
        private List<PackagePriceBillingPolicy> packagePriceBillingPolicy;
        private List<? extends Form> globalConfigMetaForms;

        private Object globalConfigMeta;
    }
}
