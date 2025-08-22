package com.fit2cloud.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fit2cloud.controller.handler.ResultHolder;
import com.fit2cloud.controller.request.vm.CreateServerRequest;
import com.fit2cloud.controller.request.vm.PageVmCloudServerRequest;
import com.fit2cloud.dao.entity.LiveGoods;
import com.fit2cloud.dto.VmCloudServerDTO;
import com.fit2cloud.service.IVmDefaultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
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


    @Operation(summary = "", description = "默认配置存储列表")
    @PostMapping("/list")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Boolean> list(@RequestBody CreateServerRequest request) {
        iVmDefaultService.save(request);
        return ResultHolder.success(Boolean.TRUE);
    }




}
