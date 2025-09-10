package com.fit2cloud.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fit2cloud.base.entity.User;
import com.fit2cloud.base.entity.VmCloudDisk;
import com.fit2cloud.base.entity.VmCloudServer;
import com.fit2cloud.base.mapper.BaseVmCloudDiskMapper;
import com.fit2cloud.common.utils.ColumnNameUtil;
import com.fit2cloud.common.utils.PageUtil;
import com.fit2cloud.controller.request.vm.CreateServerRequest;
import com.fit2cloud.dao.entity.DefaultVmConfig;
import com.fit2cloud.dao.entity.LiveUser;
import com.fit2cloud.dao.entity.UserValidtime;
import com.fit2cloud.dao.entity.VmUser;
import com.fit2cloud.dao.goodsMapper.LiveGoodsMapper;
import com.fit2cloud.dao.mapper.UserValidtimeMapper;
import com.fit2cloud.dao.mapper.VmDefaultConfigMapper;
import com.fit2cloud.dao.mapper.VmUserMapper;
import com.fit2cloud.dto.JobRecordDTO;
import com.fit2cloud.dto.VmCloudDiskDTO;
import com.fit2cloud.service.IVmCloudServerService;
import com.fit2cloud.service.IVmDefaultService;
import com.fit2cloud.utils.UserContext;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import okhttp3.*;
import org.joda.time.LocalTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Time;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class VmDefaultService extends ServiceImpl<VmDefaultConfigMapper, DefaultVmConfig> implements IVmDefaultService {

    public static Cache<String, Object> onlineList = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    public static Cache<String, LocalDateTime> onlineConnectList = CacheBuilder.newBuilder()
            .build();

    public final String secretSalt = "ce-sjhdaiw1dgfxv";

    private static  Cipher cipher;

    static {
        try {
            cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    @Resource
    VmDefaultConfigMapper vmDefaultConfigMapper;

    @Resource
    UserValidtimeMapper validtimeMapper;

    @Resource
    LiveGoodsMapper liveGoodsMapper;

    @Resource
    IVmCloudServerService vmCloudServerService;

    @Resource
    VmUserMapper vmUserMapper;

    private static OkHttpClient client = new OkHttpClient();

    @Override
    public void save(CreateServerRequest request) {
        DefaultVmConfig config = new DefaultVmConfig();
        config.setAccountId(request.getAccountId());
        config.setFormReq(request.getFromInfo());
        config.setCreateServerReq(request.getCreateRequest());
        config.setConfigName(request.getConfigName());
        config.setDesignator(request.getDesignator());
        vmDefaultConfigMapper.insert(config);
    }

    @Override
    public IPage<DefaultVmConfig> pageDefaultConfig(CreateServerRequest request) {
        Page<DefaultVmConfig> page = PageUtil.of(request, DefaultVmConfig.class, new OrderItem(ColumnNameUtil.getColumnName(DefaultVmConfig::getCreateTime, true), false), true);
        IPage<DefaultVmConfig> result = vmDefaultConfigMapper.selectDefaultConfig(page, null);
        return result;
    }

    @Override
    public JobRecordDTO getRecord(String id) {
        return null;
    }

    @Override
    public Boolean heart(HttpServletRequest request) {
        JSONObject user = JSONObject.parseObject(UserContext.getUser().toString());
        String id = liveGoodsMapper.selectUserId(UserContext.getToken());
        if (ObjectUtils.isEmpty(onlineList.getIfPresent(id))) {
            QueryWrapper<UserValidtime> wrapper = new QueryWrapper<UserValidtime>().select().eq("user_id", id);
            if (ObjectUtils.isEmpty(validtimeMapper.selectOne(wrapper))) {
                UserValidtime userValidtime = new UserValidtime();
                userValidtime.setUserId(id);
                userValidtime.setUserName(user.getString("user_name"));
                validtimeMapper.insert(userValidtime);
            }
        }
        //String ipCurrent = this.toIPv4(request.getRemoteHost());
        String ipCurrent = this.toIPv4(request.getHeader("X-Forwarded-For"));
        System.out.println("-------------客户机ip-start--------------");
        System.out.println(ipCurrent);
        System.out.println("-------------客户机ip-end--------------");
        if(!ipCurrent.equals(user.getString("remoteIp"))){
            RequestBody req = new FormBody.Builder()
                    .add("ip",ipCurrent)
                    .add("accessKey","alibaba-inc")
                    .build();
            Request okrequest = new Request.Builder()
                    .url("https://ip.taobao.com/outGetIpInfo")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .post(req)
                    .build();
            Response response = null;
            try {
                response = client.newCall(okrequest).execute();
                String resStr = response.body().string();
                JSONObject dataInfo = JSONObject.parseObject(resStr);
                JSONObject ipInfo = dataInfo.getJSONObject("data");
                user.put("location",ipInfo.getString("country")+"-"+ipInfo.getString("city"));
                user.put("netOperator",ipInfo.getString("isp"));
                user.put("remoteIp",ipCurrent);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if(onlineList.getIfPresent(id) == null){
            onlineConnectList.put(id,LocalDateTime.now());
        }
        onlineList.put(id, user);
        return true;
    }

    @Override
    public Map startVm() {
        Map result = new HashMap();
        JSONObject user = (JSONObject) UserContext.getUser();
        String id = liveGoodsMapper.selectUserId(UserContext.getToken());
        QueryWrapper<UserValidtime> wrapper = new QueryWrapper<UserValidtime>().select().eq("user_id", id);
        UserValidtime userValidtime = validtimeMapper.selectOne(wrapper);
        if (userValidtime.getVaildTime().isBefore(LocalDateTime.now())) {
            result.put("code", 400);
            result.put("msg", "剩余时间不足！");
            return result;
        }

        QueryWrapper<VmUser> vmwrapper = new QueryWrapper<VmUser>().select().eq("user_id", id);
        List<VmUser> vmUserList = vmUserMapper.selectList(vmwrapper);
        if(vmUserList.isEmpty()){
            QueryWrapper<DefaultVmConfig> queryWrapper = new QueryWrapper<DefaultVmConfig>().select().eq("designator", liveGoodsMapper.selectUserId(UserContext.getToken()));
            List<DefaultVmConfig> list = vmDefaultConfigMapper.selectList(queryWrapper);
            DefaultVmConfig defaultVmConfig = null;
            if (!list.isEmpty()) {
                defaultVmConfig = list.get(0);
            } else {
                QueryWrapper<DefaultVmConfig> query = new QueryWrapper<DefaultVmConfig>().select().eq("is_default", 1);
                defaultVmConfig = vmDefaultConfigMapper.selectOne(query);
            }

            CreateServerRequest req = new CreateServerRequest();
            req.setAccountId(defaultVmConfig.getAccountId());
            req.setCreateRequest(defaultVmConfig.getCreateServerReq());
            req.setFromInfo(defaultVmConfig.getFormReq());
            if(vmCloudServerService.createServerForVm(req,id)){
                result.put("code", 200);
                result.put("msg", "已启动");
                return result;
            }else {
                result.put("code", 400);
                result.put("msg", "创建失败！");
                return result;
            }
        }
        else {
            result.put("code", 201);
            result.put("msg", "已存在启动服务器！服务器创建终止");
            return result;
        }
    }

    @Override
    public Boolean set(DefaultVmConfig config) {
        config.setIsDefault(true);
        vmDefaultConfigMapper.updateAllDefault();
        vmDefaultConfigMapper.updateById(config);
        return Boolean.TRUE;
    }

    @Override
    public Boolean del(DefaultVmConfig request) {
        vmDefaultConfigMapper.deleteById(request);
        return Boolean.TRUE;
    }

    @Override
    public List<UserValidtime> getDesignators() {
        return validtimeMapper.selectList(null);
    }

    @Override
    public JSONArray getServiceList() {
        Map yunboRow = new HashMap();
        String id = liveGoodsMapper.selectUserId(UserContext.getToken());
        QueryWrapper<UserValidtime> wrapper = new QueryWrapper<UserValidtime>().select().eq("user_id", id);
        UserValidtime userValidtime = validtimeMapper.selectOne(wrapper);
        if (userValidtime.getVaildTime().isBefore(LocalDateTime.now())) {
            yunboRow.put("is_open", false);
        } else {
            yunboRow.put("is_open", true);
        }
        Request request = new Request.Builder()
                .url("http://ecshop-api.livepartner.fans//?service=Category.serviceAppGoodsListApi")
                .build();
        Response response = null;
        JSONArray newRows = new JSONArray();
        try {
            response = client.newCall(request).execute();
            String resStr = response.body().string();
            JSONObject resJo = JSONObject.parseObject(resStr);
            JSONArray rows = resJo.getJSONObject("data").getJSONArray("rows");

            yunboRow.put("id", 99991);
            yunboRow.put("cat_id",47);
            yunboRow.put("goods_name", "云播");
            yunboRow.put("desc","通过云技术，实现不同直播账号的无缝切换，异地切换主播而不需停播。");
            yunboRow.put("original_img_url","http://weibo-app.oss-cn-hangzhou.aliyuncs.com/images/202508/source_img/6499_G_1754908393217.jpg");
            newRows.add(yunboRow);
            for (Object item : rows) {
                JSONObject jitem = (JSONObject) item;
                jitem.put("is_open", false);
                if(jitem.getInteger("id") == 6194 || jitem.getInteger("id") == 6345){

                    if(jitem.getInteger("id") == 6194){
                        if (userValidtime.getServerAVt().isBefore(LocalDateTime.now())) {
                            jitem.put("is_open", false);
                        } else {
                            jitem.put("is_open", true);
                        }
                    }
                    if(jitem.getInteger("id") == 6345){
                        if (userValidtime.getServerBVt().isBefore(LocalDateTime.now())) {
                            jitem.put("is_open", false);
                        } else {
                            jitem.put("is_open", true);
                        }
                    }
                    newRows.add(item);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return newRows;
    }

    @Override
    public IPage<LiveUser> getInfoList(@ModelAttribute CreateServerRequest request) {
        Page<UserValidtime> page = PageUtil.of(request, UserValidtime.class);
        IPage result = validtimeMapper.selectUserValidtime(page, null);
        List<UserValidtime> validtimesList = result.getRecords();
        List<LiveUser> liveUserList = new ArrayList<>();
        for(UserValidtime item : validtimesList){
            LiveUser liveUser = new LiveUser();
            JSONObject currentUserInfo = (JSONObject) onlineList.getIfPresent(item.getUserId());
            if(currentUserInfo == null){
                liveUser.setIsOnline(Boolean.FALSE);
                liveUser.setBelong(item.getUserName());
                liveUser.setSendIp("-");
                liveUser.setSendLocation("-");
                VmCloudServer vmCloudServer = validtimeMapper.selectVmCloudServerByUserId(item.getUserId());
                if(vmCloudServer.getInstanceName() == null){
                    liveUser.setReceiveServer("-");
                    liveUser.setReceiveStatus("-");
                    liveUser.setReceiveIp("-");
                    liveUser.setCreateTime("-");
                }else{
                    liveUser.setReceiveServer(vmCloudServer.getInstanceName());
                    liveUser.setReceiveStatus(vmCloudServer.getInstanceStatus());
                    liveUser.setReceiveIp(vmCloudServer.getRemoteIp());
                    liveUser.setCreateTime(vmCloudServer.getCreateTime().toString());
                }
                liveUser.setSendOperator("-");
            }else {
                liveUser.setIsOnline(Boolean.TRUE);
                liveUser.setBelong(item.getUserName());
                liveUser.setSendIp(currentUserInfo.getString("remoteIp"));
                liveUser.setSendLocation(currentUserInfo.getString("location"));
                VmCloudServer vmCloudServer = validtimeMapper.selectVmCloudServerByUserId(item.getUserId());
                if(vmCloudServer.getInstanceName() == null){
                    liveUser.setReceiveServer("-");
                    liveUser.setReceiveStatus("-");
                    liveUser.setReceiveIp("-");
                    liveUser.setCreateTime("-");
                }else{
                    liveUser.setReceiveServer(vmCloudServer.getInstanceName());
                    liveUser.setReceiveStatus(vmCloudServer.getInstanceStatus());
                    liveUser.setReceiveIp(vmCloudServer.getRemoteIp());
                    liveUser.setCreateTime(vmCloudServer.getCreateTime().toString());
                }
                liveUser.setSendOperator(currentUserInfo.getString("netOperator"));
            }
            liveUserList.add(liveUser);
        }
        result.setRecords(liveUserList);
        return result;
    }

    @Override
    public JSONArray getEquipmentList() {
        return validtimeMapper.selectAllVmCloudServerByUserId(liveGoodsMapper.selectUserId(UserContext.getToken()));
    }

    @Override
    public JSONObject getEquipmentDetail(String uid) {
        JSONObject result = new JSONObject();
        JSONObject currentUserInfo = (JSONObject) onlineList.getIfPresent(uid);
        if(currentUserInfo == null){
            result.put("code","403");
            result.put("msg","当前用户不在线");
            return result;
        }
        result.put("play_side_ip",currentUserInfo.getString("remoteIp"));
        result.put("play_side_create_time",onlineConnectList.getIfPresent(uid).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        result.put("play_side_area",currentUserInfo.getString("location"));
        result.put("play_side_operator",currentUserInfo.getString("netOperator"));
        VmCloudServer vmCloudServer = validtimeMapper.selectVmCloudServerByUserId(uid);
        result.put("server_connect_time",onlineConnectList.getIfPresent(uid).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        result.put("server_area","杭州");
        result.put("server_ip","121.37.97.46");
        result.put("live_ip",vmCloudServer.getRemoteIp());
        result.put("live_area",vmCloudServer.getRegion());
        result.put("live_ping","10ms");
        result.put("live_time",3600);
        return result;
    }

    @Override
    public JSONObject getUserQR() {
        String userId = liveGoodsMapper.selectUserId(UserContext.getToken());
        LocalDateTime retireTime = LocalDateTime.now().plusMinutes(6);
        //不常用 未优化
        String code = userId+"&"+retireTime;
        String encStr = "";
        try {

            SecretKeySpec secretKey = new SecretKeySpec(secretSalt.getBytes(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(code.getBytes());
            encStr = Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String url = "http://121.37.97.46:9011/vm-service/goods/addSubUser?cecode="+encStr;

        JSONObject result = new JSONObject();
        result.put("code",200);
        result.put("msg","ok");
        result.put("url",url);
        try {
            result.put("qrcode",GoodsServiceImpl.generateQRCode(url,300,300,"abc.png"));
        } catch (Exception e) {
            result.put("qrcode","二维码生成失败，请重试！");
        }
        return result;
    }

    @Override
    public JSONObject addSubUser(String info) {
        String mainUid = liveGoodsMapper.selectUserId(UserContext.getToken());
        JSONObject result = new JSONObject();
        SecretKeySpec secretKey = new SecretKeySpec(secretSalt.getBytes(), "AES");
        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(info.replace(" ", "+")));
            String decrypInfo = new String(decrypted);
            String[] infos = decrypInfo.split("&");
            if(!LocalDateTime.parse(infos[1]).isAfter(LocalDateTime.now())){
                result.put("code",416);
                result.put("msg","二维码过期！");
            }
            vmUserMapper.createSubUser(mainUid,infos[0]);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            result.put("code",415);
            result.put("msg","信息传递异常,请刷新二维码重试");
        }
        return result;
    }


    public String toIPv4(String ip) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);

            // 如果是 IPv6 回环地址，返回 127.0.0.1
            if (inetAddress.isLoopbackAddress()) {
                return "124.93.1.121";
            }

            // 如果是 IPv4，直接返回
            if (inetAddress.getAddress().length == 4) {
                if("127.0.0.1".equals(inetAddress.getHostAddress())){
                    return "124.93.1.121";
                }
                return inetAddress.getHostAddress();
            }

            // 如果是 IPv6 映射 IPv4 (::ffff:192.168.0.1)，提取最后部分
            String hostAddress = inetAddress.getHostAddress();
            if (hostAddress.contains(":")) {
                int lastColon = hostAddress.lastIndexOf(":");
                String possibleIPv4 = hostAddress.substring(lastColon + 1);
                if (possibleIPv4.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                    return possibleIPv4;
                }
            }

            // 默认返回原始地址
            return hostAddress;

        } catch (UnknownHostException e) {
            return ip; // 解析失败就返回原始值
        }
    }
}
