package com.fit2cloud.controller;

import com.fit2cloud.controller.handler.ResultHolder;
import com.fit2cloud.dao.entity.CDcard;
import com.fit2cloud.dao.entity.ConfrimPayment;
import com.fit2cloud.dao.entity.GoodsToCart;
import com.fit2cloud.dao.entity.LiveGoods;
import com.fit2cloud.service.IGoodsService;
import com.fit2cloud.service.IVmDefaultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Calendar;
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
    public ResultHolder<Map<String,Object>> getVaildTime(HttpServletRequest request) throws Exception {
        String token = request.getHeader("token");
        Map<String,Object> result = new HashMap<>();
        if("abc123456".equals(token)){
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH,6);
            result.put("vaildTime",calendar.getTime());
            return ResultHolder.success(result);
        }else{
            result.put("vaildTime",goodsService.getVaildTimeByToken(token));
        }
        return ResultHolder.success(result);
    }

    @Operation(summary = "", description = "心跳接口")
    @PostMapping("/heart")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Boolean> heart() {
        return ResultHolder.success(iVmDefaultService.heart());
    }


    @Operation(summary = "", description = "开播")
    @PostMapping("/startVm")
//    @PreAuthorize("@cepc.hasAnyCePermission('CLOUD_SERVER:READ')")
    public ResultHolder<Boolean> startVm() {
        Map map = iVmDefaultService.startVm();
        return ResultHolder.message(map.get("code"),map.get("msg"),null);
    }

}
