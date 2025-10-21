package com.fit2cloud.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("groups_info")
public class GroupsInfo {
    @TableId(value = "id", type = IdType.AUTO)
    private String id;

    private String groupName;

    private String pusherId;

}
