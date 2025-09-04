package com.fit2cloud.dao.entity;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LiveUser {

    private Boolean isOnline;

    private String sendIp;

    private String sendLocation;

    private String sendOperator;

    private String receiveIp;

    private String receiveServer;

    private String receiveStatus;

    private String createTime;

    private String belong;
}
