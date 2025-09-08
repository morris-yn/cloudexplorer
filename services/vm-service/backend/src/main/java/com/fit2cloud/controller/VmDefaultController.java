package com.fit2cloud.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fit2cloud.controller.handler.ResultHolder;
import com.fit2cloud.controller.request.vm.CreateServerRequest;
import com.fit2cloud.dao.entity.DefaultVmConfig;
import com.fit2cloud.dao.entity.LiveUser;
import com.fit2cloud.dao.entity.UserValidtime;
import com.fit2cloud.service.IVmDefaultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/default")
@Validated
@Tag(name = "默认配置")
public class VmDefaultController {

    @Resource
    private IVmDefaultService iVmDefaultService;

//    @Operation(summary = "", description = "分页查询默认配置列表")
//    @GetMapping("/list")
////    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
//    public ResultHolder<IPage<CreateServerRequest>> list(@Validated PageVmCloudServerRequest pageVmCloudServerRequest) {
//        return ResultHolder.success();
//    }


    @Operation(summary = "", description = "默认配置存储")
    @PostMapping("/save")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Boolean> save(@RequestBody CreateServerRequest request) {
        iVmDefaultService.save(request);
        return ResultHolder.success(Boolean.TRUE);
    }

    @Operation(summary = "", description = "设置默认配置")
    @PostMapping("/set")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Boolean> set(@RequestBody DefaultVmConfig request) {
        iVmDefaultService.set(request);
        return ResultHolder.success(Boolean.TRUE);
    }

    @Operation(summary = "", description = "删除默认配置")
    @PostMapping("/del")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Boolean> del(@RequestBody DefaultVmConfig request) {
        iVmDefaultService.del(request);
        return ResultHolder.success(Boolean.TRUE);
    }

    @Operation(summary = "", description = "获取指定人列表")
    @GetMapping("/getDesignators")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<List<UserValidtime>> getDesignators() {

        return ResultHolder.success(iVmDefaultService.getDesignators());
    }


    @Operation(summary = "", description = "默认配置存储列表")
    @GetMapping("/list")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<IPage<DefaultVmConfig>> list(@ModelAttribute CreateServerRequest request) {
        return ResultHolder.success(iVmDefaultService.pageDefaultConfig(request));
    }


    @Operation(summary = "", description = "推拉流信息列表")
    @GetMapping("/infoList")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<IPage<LiveUser>> infoList(@ModelAttribute CreateServerRequest request) {
        return ResultHolder.of(200,"ok",iVmDefaultService.getInfoList(request));
    }

}
