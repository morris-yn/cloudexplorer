package com.fit2cloud.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fit2cloud.base.entity.VmCloudDisk;
import com.fit2cloud.controller.request.vm.CreateServerRequest;
import com.fit2cloud.dao.entity.DefaultVmConfig;
import com.fit2cloud.dao.entity.LiveUser;
import com.fit2cloud.dao.entity.UserValidtime;
import com.fit2cloud.dto.JobRecordDTO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author fit2cloud
 * @since
 */
public interface IVmDefaultService extends IService<DefaultVmConfig> {

    void save(CreateServerRequest request);


    IPage<DefaultVmConfig> pageDefaultConfig(CreateServerRequest request);

    JobRecordDTO getRecord(String id);

    Boolean heart(HttpServletRequest request);

    Map startVm();

    Boolean set(DefaultVmConfig request);

    Boolean del(DefaultVmConfig request);

    List<UserValidtime> getDesignators();

    JSONArray getServiceList();

    IPage<LiveUser> getInfoList(CreateServerRequest request);

    JSONArray getEquipmentList();

    JSONObject getEquipmentDetail(String uid);

    JSONObject getUserQR();

    JSONObject addSubUser(String info);

    JSONObject getLiveUrl();

    JSONObject getLiveManageInfo();
}
