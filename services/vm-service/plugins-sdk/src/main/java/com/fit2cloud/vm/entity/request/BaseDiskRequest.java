package com.fit2cloud.vm.entity.request;

import lombok.Data;

/**
 * Author: LiuDi
 * Date: 2022/10/13 9:39 PM
 */
@Data
public class BaseDiskRequest extends BaseRequest {
    /**
     * 磁盘IDS
     */
    String[] diskIds;

    /**
     * 磁盘ID
     */
    String diskId;

    /**
     * 实例ID
     */
    String instanceUuid;
}
