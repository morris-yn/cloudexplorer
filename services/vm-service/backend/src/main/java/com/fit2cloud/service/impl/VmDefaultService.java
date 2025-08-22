package com.fit2cloud.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fit2cloud.controller.request.vm.CreateServerRequest;
import com.fit2cloud.dao.entity.DefaultVmConfig;
import com.fit2cloud.dao.entity.UserValidtime;
import com.fit2cloud.dao.goodsMapper.LiveGoodsMapper;
import com.fit2cloud.dao.mapper.UserValidtimeMapper;
import com.fit2cloud.dao.mapper.VmDefaultConfigMapper;
import com.fit2cloud.dto.JobRecordDTO;
import com.fit2cloud.service.IVmDefaultService;
import com.fit2cloud.utils.UserContext;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.Resource;
import org.joda.time.LocalTime;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class VmDefaultService implements IVmDefaultService {

    public static Cache<String,Object> onlineList = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    @Resource
    VmDefaultConfigMapper vmDefaultConfigMapper;

    @Resource
    UserValidtimeMapper validtimeMapper;

    @Resource
    LiveGoodsMapper liveGoodsMapper;

    @Override
    public void save(CreateServerRequest request) {
        DefaultVmConfig config = new DefaultVmConfig();
        config.setAccountId(request.getAccountId());
        config.setFormReq(request.getFromInfo());
        config.setCreateServerReq(request.getCreateRequest());
        vmDefaultConfigMapper.insert(config);
    }
    @Override
    public JobRecordDTO getRecord(String id) {
        return null;
    }

    @Override
    public Boolean heart() {
        JSONObject user = JSONObject.parseObject(UserContext.getUser().toString());
        String id = liveGoodsMapper.selectUserId(UserContext.getToken());
        if(ObjectUtils.isEmpty(onlineList.getIfPresent(id))){
            QueryWrapper<UserValidtime> wrapper = new QueryWrapper<UserValidtime>().select().eq("user_id",id);
            if(ObjectUtils.isEmpty(validtimeMapper.selectOne(wrapper))){
                UserValidtime userValidtime = new UserValidtime();
                userValidtime.setUserId(id);
                userValidtime.setUserName(user.getString("user_name"));
                validtimeMapper.insert(userValidtime);
            }
        }
        onlineList.put(id,user);
        return true;
    }

    @Override
    public Map startVm() {
        Map result = new HashMap();
        JSONObject user = (JSONObject)UserContext.getUser();
        String id = liveGoodsMapper.selectUserId(UserContext.getToken());
        QueryWrapper<UserValidtime> wrapper = new QueryWrapper<UserValidtime>().select().eq("user_id",id);
        UserValidtime userValidtime = validtimeMapper.selectOne(wrapper);
        if(userValidtime.getVaildTime().isBefore(LocalDateTime.now())){
            result.put("code", 400);
            result.put("msg","剩余时间不足！");
            return result;
        }





        result.put("code", 200);
        result.put("msg","已启动");
        return result;
    }
}
