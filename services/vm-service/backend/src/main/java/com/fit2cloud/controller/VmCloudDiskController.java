package com.fit2cloud.controller;

import com.alibaba.excel.EasyExcelFactory;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fit2cloud.base.mapper.BaseVmCloudDiskMapper;
import com.fit2cloud.common.form.vo.FormObject;
import com.fit2cloud.common.log.annotation.OperatedLog;
import com.fit2cloud.common.log.constants.OperatedTypeEnum;
import com.fit2cloud.common.log.constants.ResourceTypeEnum;
import com.fit2cloud.common.utils.CustomCellWriteHeightConfig;
import com.fit2cloud.common.utils.CustomCellWriteWidthConfig;
import com.fit2cloud.common.utils.EasyExcelUtils;
import com.fit2cloud.common.validator.annnotaion.CustomValidated;
import com.fit2cloud.common.validator.handler.ExistHandler;
import com.fit2cloud.controller.handler.ResultHolder;
import com.fit2cloud.controller.request.GrantRequest;
import com.fit2cloud.controller.request.disk.*;
import com.fit2cloud.dto.VmCloudDiskDTO;
import com.fit2cloud.dto.VmCloudDiskDownloadDTO;
import com.fit2cloud.dto.VmCloudServerDTO;
import com.fit2cloud.service.IVmCloudDiskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * @author jianneng
 * @date 2022/9/27 14:31
 **/
@RestController
@RequestMapping("/api/disk")
@Validated
@Tag(name = "硬盘相关接口")
public class VmCloudDiskController {
    @Resource
    private IVmCloudDiskService diskService;

    @Operation(summary = "分页查询硬盘", description = "分页查询硬盘")
    @GetMapping("/page")
    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_DISK:READ')")
    public ResultHolder<IPage<VmCloudDiskDTO>> list(@Validated PageVmCloudDiskRequest pageVmCloudDiskRequest) {
        return ResultHolder.success(diskService.pageVmCloudDisk(pageVmCloudDiskRequest));
    }

    @Operation(summary = "硬盘明细下载", description = "硬盘明细下载")
    @GetMapping("/download")
    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_DISK:READ')")
    public void hostListDownload(@Validated VmCloudDiskRequest request, HttpServletResponse response) {
        List<VmCloudDiskDTO> list = diskService.listVMCloudDisk(request);
        try {
            EasyExcelFactory.write(response.getOutputStream(), VmCloudDiskDownloadDTO.class)
                    .sheet("硬盘明细列表")
                    .registerWriteHandler(new CustomCellWriteWidthConfig())
                    .registerWriteHandler(new CustomCellWriteHeightConfig())
                    .registerWriteHandler(EasyExcelUtils.getStyleStrategy())
                    .doWrite(list.stream().map(VmCloudDiskDownloadDTO::new).toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Operation(summary = "查询硬盘数量", description = "查询硬盘数量")
    @GetMapping("/count")
    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_DISK:READ')")
    public ResultHolder<Long> count() {
        return ResultHolder.success(diskService.countDisk());
    }

    @Operation(summary = "查询可以挂载磁盘的云主机")
    @GetMapping("/listVm")
    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_DISK:READ')")
    public ResultHolder<List<VmCloudServerDTO>> cloudServerList(ListVmRequest req) {
        return ResultHolder.success(diskService.cloudServerList(req));
    }

    @Operation(summary = "根据ID查询磁盘信息")
    @GetMapping("/showCloudDiskById/{id}")
    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_DISK:READ')")
    public ResultHolder<VmCloudDiskDTO> cloudDisk(@PathVariable("id") String id) {
        return ResultHolder.success(diskService.cloudDisk(id));
    }

    @GetMapping("/createDiskForm/{platform}")
    @Operation(summary = "根据云平台查询创建磁盘的表单数据")
    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_DISK:READ')")
    public ResultHolder<FormObject> findCreateDiskForm(@PathVariable("platform") String platform) {
        return ResultHolder.success(diskService.getCreateDiskForm(platform));
    }

    @Operation(summary = "创建磁盘")
    @PostMapping("createDisk")
    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_DISK:CREATE')")
    @OperatedLog(resourceType = ResourceTypeEnum.CLOUD_DISK, operated = OperatedTypeEnum.ADD,
            content = "'创建磁盘['+#request.diskName+']'",
            joinResourceType = ResourceTypeEnum.CLOUD_SERVER,
            joinResourceId = "#request.instanceUuid",
            param = "#request")
    public ResultHolder<Boolean> createDisk(@RequestBody CreateVmCloudDiskRequest request) {
        return ResultHolder.success(diskService.createDisk(request));
    }

    @Operation(summary = "扩容磁盘")
    @PutMapping("enlarge")
    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_DISK:RESIZE')")
    @OperatedLog(resourceType = ResourceTypeEnum.CLOUD_DISK, operated = OperatedTypeEnum.ENLARGE,
            resourceId = "#req.id",
            joinResourceType = ResourceTypeEnum.CLOUD_SERVER,
            joinResourceId = "#req.instanceUuid",
            content = "'磁盘大小扩容到['+#req.newDiskSize+']GB'",
            param = "#req")
    public ResultHolder<Boolean> enlarge(@RequestBody EnlargeVmCloudDiskRequest req) {
        return ResultHolder.success(diskService.enlarge(req.getId(), req.getNewDiskSize()));
    }

    @Operation(summary = "挂载磁盘")
    @PutMapping("attach")
    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_DISK:ATTACH')")
    @OperatedLog(resourceType = ResourceTypeEnum.CLOUD_DISK,
            operated = OperatedTypeEnum.ATTACH,
            resourceId = "#req.id",
            joinResourceType = ResourceTypeEnum.CLOUD_SERVER,
            joinResourceId = "#req.instanceUuid",
            content = "'磁盘挂载'",
            param = "#req")
    public ResultHolder<Boolean> attach(@RequestBody AttachVmCloudDiskRequest req) {
        return ResultHolder.success(diskService.attach(req.getId(), req.getInstanceUuid(), req.getDeleteWithInstance()));
    }

    @Operation(summary = "卸载磁盘")
    @PutMapping("detach/{id}")
    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_DISK:DETACH')")
    @OperatedLog(resourceType = ResourceTypeEnum.CLOUD_DISK, operated = OperatedTypeEnum.DETACH,
            resourceId = "#id",
            content = "'磁盘卸载'",
            param = "#id")
    public ResultHolder<Boolean> detach(@Parameter(description = "主键 ID")
                                        @NotNull(message = "{i18n.primary.key.cannot.be.null}")
                                        @CustomValidated(mapper = BaseVmCloudDiskMapper.class, handler = ExistHandler.class, message = "{i18n.primary.key.not.exist}", exist = false)
                                        @PathVariable("id") String id) {
        return ResultHolder.success(diskService.detach(id));
    }

    @Operation(summary = "删除磁盘")
    @DeleteMapping("delete/{id}")
    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_DISK:DELETE')")
    @OperatedLog(resourceType = ResourceTypeEnum.CLOUD_DISK, operated = OperatedTypeEnum.DELETE, resourceId = "#id", content = "'删除磁盘'", param = "#id")
    public ResultHolder<Boolean> delete(@Parameter(description = "主键 ID")
                                        @NotNull(message = "{i18n.primary.key.cannot.be.null}")
                                        @CustomValidated(mapper = BaseVmCloudDiskMapper.class, handler = ExistHandler.class, message = "{i18n.primary.key.not.exist}", exist = false)
                                        @PathVariable("id") String id) {
        return ResultHolder.success(diskService.delete(id));
    }

    @Operation(summary = "批量挂载磁盘")
    @PutMapping("batchAttach")
    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_DISK:ATTACH')")
    @OperatedLog(resourceType = ResourceTypeEnum.CLOUD_DISK, operated = OperatedTypeEnum.BATCH_ATTACH, resourceId = "#req.ids",
            joinResourceType = ResourceTypeEnum.CLOUD_SERVER,
            joinResourceId = "#req.instanceUuid",
            content = "'批量挂载磁盘'", param = "#req")
    public ResultHolder<Boolean> batchAttach(@RequestBody BatchAttachVmCloudDiskRequest req) {
        return ResultHolder.success(diskService.batchAttach(req.getIds(), req.getInstanceUuid(), req.getDeleteWithInstance()));
    }

    @Operation(summary = "批量卸载磁盘")
    @PutMapping("batchDetach")
    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_DISK:DETACH')")
    @OperatedLog(resourceType = ResourceTypeEnum.CLOUD_DISK, operated = OperatedTypeEnum.BATCH_DETACH, resourceId = "#ids", content = "'批量卸载磁盘'", param = "#ids")
    public ResultHolder<Boolean> batchDetach(@RequestBody String[] ids) {
        return ResultHolder.success(diskService.batchDetach(ids));
    }

    @Operation(summary = "批量删除磁盘")
    @DeleteMapping("batchDelete")
    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_DISK:DELETE')")
    @OperatedLog(resourceType = ResourceTypeEnum.CLOUD_DISK, operated = OperatedTypeEnum.BATCH_DELETE, content = "'批量删除磁盘'", param = "#ids")
    public ResultHolder<Boolean> batchDelete(@RequestBody String[] ids) {
        return ResultHolder.success(diskService.batchDelete(ids));
    }

    @Operation(summary = "磁盘授权")
    @PostMapping("/grant")
    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_DISK:AUTH')")
    @OperatedLog(resourceType = ResourceTypeEnum.CLOUD_DISK, operated = OperatedTypeEnum.BATCH_AUTHORISATION,
            resourceId = "#grantDiskRequest.ids",
            content = "#grantDiskRequest.grant?'磁盘批量授权':'磁盘批量取消授权'",
            param = "#grantDiskRequest")
    public ResultHolder<Boolean> grant(@RequestBody GrantRequest grantDiskRequest) {
        return ResultHolder.success(diskService.grant(grantDiskRequest));
    }

    @Operation(summary = "批量放入回收站")
    @PutMapping("batchRecycleDisks")
    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_DISK:DELETE')")
    @OperatedLog(resourceType = ResourceTypeEnum.CLOUD_DISK, operated = OperatedTypeEnum.BATCH_RECYCLE,
            resourceId = "#ids",
            content = "'磁盘放入回收站'",
            param = "#ids")
    public ResultHolder<Boolean> batchRecycleDisks(@RequestBody String[] ids) {
        return ResultHolder.success(diskService.batchRecycleDisks(ids));
    }

    @Operation(summary = "放入回收站")
    @PutMapping("recycleDisk/{id}")
    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_DISK:DELETE')")
    @OperatedLog(resourceType = ResourceTypeEnum.CLOUD_DISK, operated = OperatedTypeEnum.RECYCLE, resourceId = "#id", param = "#id")
    public ResultHolder<Boolean> recycleDisk(@Parameter(description = "主键 ID")
                                             @NotNull(message = "{i18n.primary.key.cannot.be.null}")
                                             @CustomValidated(mapper = BaseVmCloudDiskMapper.class, handler = ExistHandler.class, message = "{i18n.primary.key.not.exist}", exist = false)
                                             @PathVariable("id") String id) {
        return ResultHolder.success(diskService.recycleDisk(id));
    }
}
