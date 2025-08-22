package com.fit2cloud.dao.entity.contant;

public enum LogContants {

    ORDER(0,"订单"),
    SERVER(1,"服务开通");

    private final int code;
    private final String desc;


    LogContants(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }
}
