package com.fit2cloud.controller.request.images;

import com.fit2cloud.request.pub.OrderRequest;
import com.fit2cloud.request.pub.PageOrderRequestInterface;
import com.fit2cloud.request.pub.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.util.List;

/**
 * @author jianneng
 * @date 2022/9/27 14:45
 **/
@Data
public class PageVmCloudImageRequest extends PageRequest implements PageOrderRequestInterface {

    @Serial
    private static final long serialVersionUID = -5292015506261363029L;

    @Schema(title = "名称")
    private String imageName;

    @Schema(title = "云账号名称")
    private String accountName;

    @Size(min = 2, max = 2, message = "{i18n.request.date.message}")

    @Schema(title = "创建时间", example = "createTime[]=2121&createTime[]=21212")
    private List<Long> createTime;

    @Size(min = 2, max = 2, message = "{i18n.request.date.message}")
    @Schema(title = "到期时间", example = "updateTime[]=2121&updateTime[]=21212")
    private List<Long> expireTime;

    @Schema(title = "排序", example = " {\"column\":\"createTime\",\"asc\":false}")
    private OrderRequest order;
}
