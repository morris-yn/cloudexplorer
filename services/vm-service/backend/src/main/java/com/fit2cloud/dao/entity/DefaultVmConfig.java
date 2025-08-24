package com.fit2cloud.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@Accessors(chain = true)
@TableName("default_vm_config")
public class DefaultVmConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private long id;

    @TableField(value = "account_id",updateStrategy = FieldStrategy.IGNORED)
    private String accountId;
    @TableField(value = "form_req",updateStrategy = FieldStrategy.IGNORED)
    private String formReq;
    @TableField(value = "create_server_req",updateStrategy = FieldStrategy.IGNORED)
    private String createServerReq;
    @TableField(value = "create_time",updateStrategy = FieldStrategy.IGNORED)
    private java.sql.Timestamp createTime;
    @TableField(value = "designator",updateStrategy = FieldStrategy.IGNORED)
    private String designator;
    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String userId;
    @TableField(exist = false)
    private String name;
    @TableField(value = "config_name",updateStrategy = FieldStrategy.IGNORED)
    private String configName;

    @TableField(value = "is_default",updateStrategy = FieldStrategy.IGNORED)
    private Boolean isDefault;
}
