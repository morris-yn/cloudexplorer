package com.fit2cloud.controller.request.cloud_account;

import com.fit2cloud.common.validator.annnotaion.CustomQueryWrapperValidated;
import com.fit2cloud.common.validator.annnotaion.CustomValidated;
import com.fit2cloud.common.validator.group.ValidationGroup;
import com.fit2cloud.common.validator.handler.ExistHandler;
import com.fit2cloud.common.validator.handler.ExistQueryWrapperValidatedHandler;
import com.fit2cloud.dao.mapper.CloudAccountMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
@CustomQueryWrapperValidated(groups = ValidationGroup.UPDATE.class,
        handler = ExistQueryWrapperValidatedHandler.class,
        el = "#getQueryWrapper().ne(\"id\",#this.id).eq(\"name\",#this.name)",
        message = "云账号名称不能重复", exist = true,
        mapper = CloudAccountMapper.class)
public class UpdateAccountNameRequest {
    @Schema(title = "云账号id", description = "云账号id")
    @NotNull(message = "云账号id不能为null")
    @CustomValidated(mapper = CloudAccountMapper.class, field = "id", handler = ExistHandler.class, message = "{i18n.cloud_account.id.is.not.existent}", exist = false)
    private String id;


    @Schema(title = "云账号名称", description = "云账号名称")
    @NotNull(message = "云账号名称不能为null")
    @CustomValidated(mapper = CloudAccountMapper.class, field = "name", handler = ExistHandler.class, message = "{i18n.cloud_account.name.not.repeat}", exist = false)
    private String name;

}
