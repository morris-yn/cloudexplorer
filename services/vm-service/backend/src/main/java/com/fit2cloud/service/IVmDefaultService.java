package com.fit2cloud.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fit2cloud.base.entity.VmCloudDisk;
import com.fit2cloud.controller.request.vm.CreateServerRequest;
import com.fit2cloud.dao.entity.DefaultVmConfig;
import com.fit2cloud.dao.entity.UserValidtime;
import com.fit2cloud.dto.JobRecordDTO;

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

    Boolean heart();

    Map startVm();

    Boolean set(DefaultVmConfig request);

    List<UserValidtime> getDesignators();
}
