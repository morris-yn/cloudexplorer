package com.fit2cloud.dao.entity;


import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@TableName("vm_user")
public class VmUser {

  private long id;
  private String vmServerId;
  private long userId;


}
