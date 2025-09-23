package com.fit2cloud.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("pull_user")
public class PullUser {

  private String id;
  private String location;
  private String netOperator;
  private String remoteIp;
  private Boolean enable;


  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }


  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }


  public String getNetOperator() {
    return netOperator;
  }

  public void setNetOperator(String netOperator) {
    this.netOperator = netOperator;
  }


  public String getRemoteIp() {
    return remoteIp;
  }

  public void setRemoteIp(String remoteIp) {
    this.remoteIp = remoteIp;
  }


  public Boolean getEnable() {
    return enable;
  }

  public void setEnable(Boolean enable) {
    this.enable = enable;
  }

}
