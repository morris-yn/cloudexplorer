package com.fit2cloud.service.impl;

import com.fit2cloud.controller.request.vm.CreateServerRequest;
import com.fit2cloud.dao.entity.DefaultVmConfig;
import com.fit2cloud.dao.mapper.VmDefaultConfigMapper;
import com.fit2cloud.dto.JobRecordDTO;
import com.fit2cloud.service.IVmDefaultService;
import jakarta.annotation.Resource;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class VmDefaultService implements IVmDefaultService {

    @Resource
    VmDefaultConfigMapper vmDefaultConfigMapper;

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
}
