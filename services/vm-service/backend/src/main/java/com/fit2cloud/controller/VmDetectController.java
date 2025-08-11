package com.fit2cloud.controller;

import com.fit2cloud.controller.handler.ResultHolder;
import com.fit2cloud.dao.entity.ConfrimPayment;
import com.fit2cloud.dao.entity.GoodsToCart;
import com.fit2cloud.dao.entity.LiveGoods;
import com.fit2cloud.service.IGoodsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/vmControl")
@Validated
@Tag(name = "默认配置")
public class VmDetectController {

    @Resource
    private IGoodsService goodsService;


    @Operation(summary = "", description = "心跳检测")
    @PostMapping("/sendHeart")
    public ResultHolder<List<LiveGoods>> list(@RequestBody LiveGoods goods) {
        return ResultHolder.success(goodsService.getList(goods));
    }



}
