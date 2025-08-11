package com.fit2cloud.common.provider.impl.vsphere.entity.request;

import com.fit2cloud.common.constants.Language;
import lombok.Data;

@Data
public class BaseRequest {
    private String credential;
    /**
     * 数据中心ID
     */
    private String regionId;
    /**
     * 语言
     */
    private Language language;
}
