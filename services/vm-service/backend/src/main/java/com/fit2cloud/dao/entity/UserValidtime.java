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

  private LocalDateTime serverAVt;

  private LocalDateTime serverBVt;

  private Boolean first;

    public Boolean getFirst() {
        return first;
    }

    public void setFirst(Boolean first) {
        this.first = first;
    }

    public LocalDateTime getServerAVt() {
        return serverAVt;
    }

    public void setServerAVt(LocalDateTime serverAVt) {
        this.serverAVt = serverAVt;
    }

    public LocalDateTime getServerBVt() {
        return serverBVt;
    }

    public void setServerBVt(LocalDateTime serverBVt) {
        this.serverBVt = serverBVt;
    }

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
