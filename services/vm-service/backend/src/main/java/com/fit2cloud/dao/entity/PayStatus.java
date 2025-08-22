package com.fit2cloud.dao.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayStatus {
    private String orderSn;
    private Boolean isPaid;
    private String goodsId;
    private String goodsBrief;
}
