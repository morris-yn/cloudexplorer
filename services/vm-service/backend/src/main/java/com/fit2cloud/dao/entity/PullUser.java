package com.fit2cloud.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("pull_user")
public class PullUser {
    @TableId(value = "id", type = IdType.AUTO)
  private String id;
  private String pullGroups;
  private String location;
  private String netOperator;
  private String remoteIp;
  private Boolean enable;


    public String getPullGroups() {
        return pullGroups;
    }

    public void setPullGroups(String pullGroups) {
        this.pullGroups = pullGroups;
    }

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
