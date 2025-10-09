package com.fit2cloud.dao.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.experimental.Accessors;

@TableName("vm_logs")
@Accessors(chain = true)
public class VmLogs {

  private long id;
  private long type;
  private long info;
  private java.sql.Timestamp createTime;
  private long createUser;


  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }


  public long getType() {
    return type;
  }

  public void setType(long type) {
    this.type = type;
  }


  public long getInfo() {
    return info;
  }

  public void setInfo(long info) {
    this.info = info;
  }


  public java.sql.Timestamp getCreateTime() {
    return createTime;
  }

  public void setCreateTime(java.sql.Timestamp createTime) {
    this.createTime = createTime;
  }


  public long getCreateUser() {
    return createUser;
  }

  public void setCreateUser(long createUser) {
    this.createUser = createUser;
  }

}
