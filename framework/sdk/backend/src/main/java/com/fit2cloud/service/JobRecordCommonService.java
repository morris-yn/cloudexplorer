package com.fit2cloud.service;

import com.fit2cloud.base.entity.JobRecord;
import com.fit2cloud.base.entity.JobRecordResourceMapping;
import com.fit2cloud.base.service.IBaseJobRecordResourceMappingService;
import com.fit2cloud.base.service.IBaseJobRecordService;
import com.fit2cloud.common.log.utils.LogUtil;
import com.fit2cloud.common.utils.CurrentUserUtils;
import com.fit2cloud.dto.InitJobRecordDTO;
import com.fit2cloud.dto.UserDto;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.HashMap;

/**
 * @author jianneng
 * @date 2022/10/11 17:52
 **/
@Service
public class JobRecordCommonService {

    @Resource
    private IBaseJobRecordService baseJobRecordService;
    @Resource
    private IBaseJobRecordResourceMappingService jobRecordResourceMappingService;


    /**
     * 初始化任务
     *
     * @param initJobRecordDTO 任务初始化
     * @return
     */
    public JobRecord initJobRecord(InitJobRecordDTO initJobRecordDTO) {
        JobRecord jobRecord = new JobRecord();
        jobRecord.setDescription(initJobRecordDTO.getJobDescription());
        jobRecord.setStatus(initJobRecordDTO.getJobStatus());
        jobRecord.setParams(initJobRecordDTO.getParams() == null ? new HashMap<>() : initJobRecordDTO.getParams());
        jobRecord.setType(initJobRecordDTO.getJobType());
        jobRecord.setCreateTime(initJobRecordDTO.getCreateTime());

        try {
            UserDto currentUser = CurrentUserUtils.getUser();
            jobRecord.setOperateUserId(currentUser.getId());
        } catch (Exception e) {
            LogUtil.error("Can not find current login user.");
        }

        // 插入任务数据
        baseJobRecordService.save(jobRecord);
        // 插入关联关系
        JobRecordResourceMapping jobRecordResourceMapping = new JobRecordResourceMapping();
        jobRecordResourceMapping.setResourceId(initJobRecordDTO.getResourceId());
        jobRecordResourceMapping.setResourceType(initJobRecordDTO.getResourceType() == null ? null : initJobRecordDTO.getResourceType().getCode());
        jobRecordResourceMapping.setJobType(initJobRecordDTO.getJobType());
        jobRecordResourceMapping.setCreateTime(initJobRecordDTO.getCreateTime());
        jobRecordResourceMapping.setJobRecordId(jobRecord.getId());
        jobRecordResourceMappingService.save(jobRecordResourceMapping);
        return jobRecord;
    }

    public void modifyJobRecord(JobRecord jobRecord) {
        baseJobRecordService.saveOrUpdate(jobRecord);
    }
}
