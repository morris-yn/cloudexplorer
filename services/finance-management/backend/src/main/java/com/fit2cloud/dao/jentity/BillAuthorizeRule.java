package com.fit2cloud.dao.jentity;

import com.fit2cloud.constants.BillAuthorizeConditionTypeConstants;
import lombok.Data;

import java.util.List;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2022/11/8  9:28 AM}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Data
public class BillAuthorizeRule {
    /**
     * 树
     */
    private List<BillAuthorizeRule> children;
    /*
     * 账单授权条件
     */
    private List<BillAuthorizeRuleCondition> conditions;
    /**
     * 类型
     */
    private BillAuthorizeConditionTypeConstants conditionType;
}
