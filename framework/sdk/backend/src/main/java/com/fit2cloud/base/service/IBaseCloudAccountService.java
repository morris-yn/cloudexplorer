package com.fit2cloud.base.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fit2cloud.base.entity.CloudAccount;
import com.fit2cloud.dto.PlatformResponse;
import com.fit2cloud.request.cloud_account.CloudAccountModuleJob;
import com.fit2cloud.request.cloud_account.SyncRequest;
import com.fit2cloud.response.cloud_account.ResourceCountResponse;
import com.fit2cloud.response.cloud_account.SyncResource;

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
public interface IBaseCloudAccountService extends IService<CloudAccount> {

    /**
     * 初始化云账号定时任务
     *
     * @param cloudAccountId 云账号id
     */
    void initCloudAccountJob(String cloudAccountId);

    /**
     * 获取模块任务根据云账号
     *
     * @param accountId 云账号id
     * @return 当前云账号的模块任务
     */
    CloudAccountModuleJob getCloudAccountJob(String accountId);

    /**
     * 修改定时任务
     *
     * @param cloudAccountModuleJob 模块定时任务
     * @param accountId             云账号id
     * @return 更新后的对象
     */
    CloudAccountModuleJob updateJob(CloudAccountModuleJob cloudAccountModuleJob, String accountId);

    /**
     * 删除当前模块的云账号下所有任务任务,根据云账号id
     *
     * @param cloudAccountId 云账号id
     * @return 是否删除成功
     */
    boolean deleteJobByCloudAccountId(String cloudAccountId);

    /**
     * 获取当前模块资源同步任务
     *
     * @return 同步任务
     */
    List<SyncResource> getModuleResourceJob(String cloudAccountId);

    void sync(SyncRequest syncRequest);

    void sync(String jobName, String groupName, String cloudAccountId, Map<String, Object> params);

    void sync(String cloudAccountId);

    /**
     * 获取当前模块的资源计数
     *
     * @param accountId
     * @return
     */
    List<ResourceCountResponse> getModuleResourceCount(String accountId);

    /*
     * 获取云账户余额
     * @param accountId
     * @return 未获取到余额将返回--
     */
    Object getAccountBalance(String accountId);

    List<PlatformResponse> getPlatforms();

    /**
     * 获取支持的云账号数据
     *
     * @return 云账号列表
     */
    List<CloudAccount> listSupportCloudAccount();

    List<CloudAccount> listSupportCloudAccount(Wrapper<CloudAccount> queryWrapper);
}
