package com.fit2cloud.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fit2cloud.controller.handler.ResultHolder;
import com.fit2cloud.controller.request.vm.CreateServerRequest;
import com.fit2cloud.dao.entity.*;
import com.fit2cloud.service.IGoodsService;
import com.fit2cloud.service.IVmDefaultService;
import com.fit2cloud.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/goods")
@Validated
@Tag(name = "默认配置")
public class GoodsController {

    @Resource
    private IGoodsService goodsService;

    @Resource
    private IVmDefaultService iVmDefaultService;

//    @Operation(summary = "", description = "分页查询默认配置列表")
//    @GetMapping("/list")
////    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
//    public ResultHolder<IPage<CreateServerRequest>> list(@Validated PageVmCloudServerRequest pageVmCloudServerRequest) {
//        return ResultHolder.success();
//    }

    @Operation(summary = "", description = "获取商品列表")
    @PostMapping("/list")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<List<LiveGoods>> list(@RequestBody LiveGoods goods) {
        return ResultHolder.success(goodsService.getList(goods));
    }

    @Operation(summary = "", description = "获取支付二维码")
    @PostMapping("/getNativeQR")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Map> getNativeQR(@RequestBody GoodsToCart goods) throws Exception {

        return ResultHolder.success(goodsService.getNativeQR(goods));
    }

    @Operation(summary = "", description = "支付验证接口")
    @PostMapping("/confirmPayment")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Map> confirmPayment(@RequestBody ConfrimPayment payment) throws Exception {
        Map<String,Object> result = new HashMap<>();
        if(goodsService.confirmPayment(payment)){
            result.put("message","已支付");
            result.put("status",true);
        }else {
            result.put("message","未支付");
            result.put("status",false);
        }
        return ResultHolder.success(result);
    }

    @Operation(summary = "", description = "卡密兑换")
    @PostMapping("/CardKeyExchange")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Object> CardKeyExchange(@RequestBody CDcard card) throws Exception {
        Map result = goodsService.writeoff(card);
        return ResultHolder.message(result.get("code"),result.get("msg"),null);
    }

    @Operation(summary = "", description = "查询当前账号有效时间")
    @PostMapping("/getVaildTime")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Map<String,Object>> getVaildTime() throws Exception {
        Map<String,Object> result = new HashMap<>();
        result.put("vaildTime",goodsService.getVaildTimeByToken(UserContext.getToken()));
        return ResultHolder.success(result);
    }

    @Operation(summary = "", description = "心跳接口")
    @PostMapping("/heart")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Boolean> heart(HttpServletRequest request) {
        return ResultHolder.success(iVmDefaultService.heart(request));
    }


    @Operation(summary = "", description = "开播")
    @PostMapping("/startVm")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Boolean> startVm() {
        Map map = iVmDefaultService.startVm();
        return ResultHolder.message(map.get("code"),map.get("msg"),null);
    }


    @Operation(summary = "", description = "服务列表")
    @PostMapping("/serviceList")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Object> serviceList() {
        return ResultHolder.message(200,"ok",iVmDefaultService.getServiceList());
    }


    @Operation(summary = "", description = "设备列表")
    @PostMapping("/equipmentList")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Object> equipmentList() {
        return ResultHolder.message(200,"ok",iVmDefaultService.getEquipmentList());
    }

    @Operation(summary = "", description = "设备详情")
    @GetMapping("/getEquipDetail")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Object> equipmentDetail(@RequestParam("uid") String uid) {
        return ResultHolder.message(200,"ok",iVmDefaultService.getEquipmentDetail(uid));
    }


    @Operation(summary = "", description = "获取账号二维码")
    @GetMapping("/getUserQR")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Object> getUserQR() {
        return ResultHolder.message(200,"ok",iVmDefaultService.getUserQR());
    }


    @Operation(summary = "", description = "扫二维码添加子账号")
    @GetMapping("/addSubUser")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Object> addSubUser(@RequestParam("cecode") String info) {
        return ResultHolder.message(200,"ok",iVmDefaultService.addSubUser(info));
    }
}
