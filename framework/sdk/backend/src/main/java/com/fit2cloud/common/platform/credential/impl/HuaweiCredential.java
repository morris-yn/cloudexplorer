package com.fit2cloud.common.platform.credential.impl;

import com.fit2cloud.common.exception.Fit2cloudException;
import com.fit2cloud.common.form.annotaion.Form;
import com.fit2cloud.common.form.constants.InputType;
import com.fit2cloud.common.platform.credential.Credential;
import com.huaweicloud.sdk.core.auth.GlobalCredentials;
import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.iam.v3.IamClient;
import com.huaweicloud.sdk.iam.v3.model.KeystoneListRegionsRequest;
import com.huaweicloud.sdk.iam.v3.model.KeystoneListRegionsResponse;
import com.huaweicloud.sdk.iam.v3.region.IamRegion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author:张少虎
 * @Date: 2022/9/6  3:24 PM
 * @Version 1.0
 * @注释:
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HuaweiCredential implements Credential {
    /**
     * AccessKey 访问令牌
     */
    @Form(inputType = InputType.Text, label = "访问密钥ID（AK）", hint = "{\"title\": \"子账号权限要求\",\"content\": \"授权范围：所有资源\\n授权策略：Server Administrator\\n\\tTenant Guest\\n\\tBSS Administrator\"}")
    private String ak;
    /**
     * SecretKey 密钥Key
     */
    @Form(inputType = InputType.Password, label = "秘密访问密钥（SK）", extraInfo = "{\"text\":\"如何获取访问密钥？\",\"url\":\"https://support.huaweicloud.com/usermanual-ca/ca_01_0003.html\"}")
    private String sk;

    @Override
    public boolean verification() {
        try {
            regions();
        } catch (Exception e) {
            throw new Fit2cloudException(100001, "云账号校验失败:" + e.getMessage());
        }
        return true;
    }

    @Override
    public List<Region> regions() {
        try {
            ICredential auth = new GlobalCredentials()
                    .withAk(ak)
                    .withSk(sk);
            IamClient client = IamClient.newBuilder()
                    .withCredential(auth)
                    .withRegion(IamRegion.valueOf("cn-north-1"))
                    .build();
            KeystoneListRegionsRequest request = new KeystoneListRegionsRequest();
            KeystoneListRegionsResponse keystoneListRegionsResponse = client.keystoneListRegions(request);
            List<com.huaweicloud.sdk.iam.v3.model.Region> regions = keystoneListRegionsResponse.getRegions();
            return regions.stream().map(r -> {
                Region region = new Region();
                region.setRegionId(r.getId());
                region.setName(r.getLocales().getZhCn());
                return region;
            }).toList();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

    }
}
