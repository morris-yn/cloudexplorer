package com.fit2cloud.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Getter
@Setter
@Accessors(chain = true)
@TableName("user_validtime")
public class UserValidtime {

  private long id;
  private String userId;
  private String userName;
  private LocalDateTime vaildTime;


  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }


  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }


  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }


  public LocalDateTime getVaildTime() {
    return vaildTime;
  }

  public void setVaildTime(LocalDateTime vaildTime) {
    this.vaildTime = vaildTime;
  }

}
