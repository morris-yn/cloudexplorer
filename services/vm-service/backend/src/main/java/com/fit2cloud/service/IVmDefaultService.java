package com.fit2cloud.service;

import com.fit2cloud.controller.request.vm.CreateServerRequest;
import com.fit2cloud.dto.JobRecordDTO;

import java.util.Map;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author fit2cloud
 * @since
 */
public interface IVmDefaultService {

    void save(CreateServerRequest request);

    JobRecordDTO getRecord(String id);

    Boolean heart();

    Map startVm();
}
