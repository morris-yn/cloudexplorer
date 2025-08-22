package com.fit2cloud.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fit2cloud.dao.entity.CDcard;
import com.fit2cloud.dao.entity.ConfrimPayment;
import com.fit2cloud.dao.entity.GoodsToCart;
import com.fit2cloud.dao.entity.LiveGoods;
import com.fit2cloud.dao.entity.contant.LogContants;
import com.fit2cloud.dao.goodsMapper.LiveGoodsMapper;
import com.fit2cloud.service.IGoodsService;
import com.fit2cloud.utils.LogUtils;
import com.fit2cloud.utils.UserContext;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import okhttp3.*;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GoodsServiceImpl implements IGoodsService {

    @Resource
    LiveGoodsMapper liveGoodsMapper;

    String sessionId = "";

    @Override
    public List<LiveGoods> getList(LiveGoods goods) {
        QueryWrapper<LiveGoods> wrapper = new QueryWrapper<>();
        wrapper.eq("is_on_sale", 1L)
                .eq("cat_id",goods.getCatId());
        return liveGoodsMapper.selectList(wrapper);
    }

    @Override
    public Map<String,String> getNativeQR(GoodsToCart goods) throws Exception {
        //判断是否只可购买一次


        //
        OkHttpClient client = new OkHttpClient();
        if("".equals(sessionId)){
            RequestBody req = new FormBody.Builder()
                .add("username", "ceshia")
                .add("password", "ceshia")
                .add("act", "act_login")
                .add("back_act", "https://shop.livepartner.fans/index.php")
                .add("submit", "登 录")
                .build();

            Request request = new Request.Builder()
                    .url("https://shop.livepartner.fans/user.php")
                    .post(req)
                    .build();
            Response response = null;
            try {
                response = client.newCall(request).execute();
                for(String item : response.headers("set-cookie")){
                    if(item.indexOf("ECS_ID") > -1){
                        this.sessionId = item;
                        break;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        //加入购物车

        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("goods", goods.toJSONString())
                .build();

        Request request = new Request.Builder()
                .url("https://shop.livepartner.fans/flow.php?step=add_to_cart")
                .addHeader("Cookie", sessionId)
                .post(formBody)
                .build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
            String resStr = response.body().string();
            System.out.println(resStr);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //支付

        formBody = new FormBody.Builder()
                .add("shipping", "39")
                .add("payment", "9")
                .add("integral", "0")
                .add("bonus", "0")
                .add("bonus_sn", "")
                .add("postscript", "")
                .add("x", "43")
                .add("y", "22")
                .add("step", "done")
                .build();

        // 构造请求
        request = new Request.Builder()
                .url("https://shop.livepartner.fans/flow.php?step=done")
                .post(formBody)
                .addHeader("Cookie", sessionId+"; ECS[visit_times]=1")
                .addHeader("User-Agent", "Apifox/1.0.0 (https://apifox.com)")
                .addHeader("Accept", "*/*")
                .addHeader("Host", "shop.livepartner.fans")
                .addHeader("Connection", "keep-alive")
                .build();

        String orderId = "";
        String qrcode = "";
        // 发送请求并打印响应
        try (Response responseDone = client.newCall(request).execute()) {
            String text = responseDone.body().string();
            System.out.println(text);
            Pattern orderPattern = Pattern.compile("订单号.*?<font[^>]*>(\\d+)</font>", Pattern.DOTALL);
            Matcher orderMatcher = orderPattern.matcher(text);
            if (orderMatcher.find()) {
                orderId = orderMatcher.group(1);
                System.out.println("订单号: " + orderId);
            } else {
                System.out.println("未找到订单号");
            }

            // 2. 提取微信支付链接
            Pattern wxPattern = Pattern.compile("if\\(\"([^\"]+)\"!==\"\"\\)");
            Matcher wxMatcher = wxPattern.matcher(text);
            if (wxMatcher.find()) {
                String wxPayUrl = wxMatcher.group(1);
                System.out.println("微信支付链接: " + wxPayUrl);
                qrcode = generateQRCode(wxPayUrl, 300, 300, "wx_qrcode.png");
                System.out.println("二维码已生成");
            } else {
                System.out.println("未找到微信支付链接");
                throw new Exception("未找到微信支付链接");
            }
        } catch (Exception e) {
            throw e;
        }

        Map<String,String> result = new HashMap<>();
        result.put("QRcode",qrcode);
        result.put("orderId",orderId);
        LogUtils.setLog(LogContants.ORDER.getCode(),JSONObject.toJSONString(result));
        return result;
    }

    @Override
    public Boolean confirmPayment(ConfrimPayment confrimPayment) {
        return liveGoodsMapper.selectPayStatus(confrimPayment.getOrderId());
    }

    @Override
    public Date getVaildTimeByToken(String token) {
        return Calendar.getInstance().getTime();
    }

    @Override
    public Map<String, String> writeoff(CDcard card) {
        Map<String,String> result = new HashMap<>();
        OkHttpClient client = new OkHttpClient();
        RequestBody req = new FormBody.Builder()
                .add("card_sn",card.getCard())
                .add("card_password", card.getKey())
                .build();
        Request okRequest = new Request.Builder()
                .url("http://ecshop-api.livepartner.fans/?service=V2.User.openService")
                .header("Weibo-Token", UserContext.getToken())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(req)
                .build();
        Response okResponse = null;
        String resStr = "";
        try {
            okResponse = client.newCall(okRequest).execute();
            resStr = okResponse.body().string();
            JSONObject resJo = JSONObject.parseObject(resStr);
            result.put("code",resJo.getString("ret"));
            result.put("msg",resJo.getString("msg"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public static String generateQRCode(String content, int width, int height, String filePath) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        byte[] bytes = outputStream.toByteArray();

        return Base64.getEncoder().encodeToString(bytes);
    }

}


