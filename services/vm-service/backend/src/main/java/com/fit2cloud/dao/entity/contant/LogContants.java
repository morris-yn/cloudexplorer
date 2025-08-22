package com.fit2cloud.dao.entity.contant;

import lombok.Getter;

public enum LogContants {

    ORDER(0,"订单"),
    SERVER(1,"服务开通");

    @Getter
    private final int code;
    private final String desc;


    LogContants(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

}
