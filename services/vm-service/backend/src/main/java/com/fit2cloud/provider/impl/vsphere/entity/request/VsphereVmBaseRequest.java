package com.fit2cloud.provider.impl.vsphere.entity.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fit2cloud.common.platform.credential.impl.VsphereCredential;
import com.fit2cloud.common.provider.impl.vsphere.entity.request.VsphereBaseRequest;
import com.fit2cloud.provider.impl.vsphere.util.VsphereVmClient;

/**
 * Author: LiuDi
 * Date: 2022/9/22 12:00 PM
 */
public class VsphereVmBaseRequest extends VsphereBaseRequest {

    @JsonIgnore
    public VsphereVmClient getVsphereVmClient() {
        VsphereCredential vCredential = getVsphereCredential();
        if (vCredential == null) {
            throw new RuntimeException("Credential is null");
        }
        return new VsphereVmClient(vCredential, getRegionId(), getLanguage());
    }

}
