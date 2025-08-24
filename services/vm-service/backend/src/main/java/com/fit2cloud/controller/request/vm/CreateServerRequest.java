package com.fit2cloud.controller.request.vm;

import com.fit2cloud.request.pub.OrderRequest;
import com.fit2cloud.request.pub.PageOrderRequestInterface;
import com.fit2cloud.request.pub.PageRequest;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CreateServerRequest extends PageRequest implements PageOrderRequestInterface {

    private String accountId;

    private String createRequest;

    private String fromInfo;

    private OrderRequest order;

    private String configName;

    private String designator;

    public CreateServerRequest copy() {
        return new CreateServerRequest().setCreateRequest(this.createRequest).setFromInfo(this.fromInfo).setAccountId(this.accountId);
    }
}
