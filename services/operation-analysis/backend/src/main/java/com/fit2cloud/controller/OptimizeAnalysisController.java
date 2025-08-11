package com.fit2cloud.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fit2cloud.controller.handler.ResultHolder;
import com.fit2cloud.controller.request.optimize.PageOptimizationRequest;
import com.fit2cloud.dto.AnalysisServerDTO;
import com.fit2cloud.service.IOptimizeAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

/**
 * 优化建议
 *
 * @author jianneng
 * @date 2022/12/24 11:23
 **/
@RestController
@RequestMapping("/api/optimize_analysis")
@Validated
@Tag(name = "资源优化分析相关接口")
public class OptimizeAnalysisController {
    @Resource
    private IOptimizeAnalysisService iOptimizeAnalysisService;

    @Operation(summary = "资源优化分析", description = "资源优化分析")
    @GetMapping("/server/page")
    @PreAuthorize("@cepc.hasAnyCePermission('SERVER_OPTIMIZATION:READ','OVERVIEW:READ')")
    public ResultHolder<IPage<AnalysisServerDTO>> pageServerList(@Validated PageOptimizationRequest request) {
        return ResultHolder.success(iOptimizeAnalysisService.pageServer(request));
    }

}
