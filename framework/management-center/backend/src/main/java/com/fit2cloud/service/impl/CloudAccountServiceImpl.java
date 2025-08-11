package com.fit2cloud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fit2cloud.autoconfigure.PluginsContextHolder;
import com.fit2cloud.autoconfigure.ServerInfo;
import com.fit2cloud.base.entity.JobRecord;
import com.fit2cloud.base.entity.VmCloudDisk;
import com.fit2cloud.base.entity.VmCloudImage;
import com.fit2cloud.base.entity.VmCloudServer;
import com.fit2cloud.base.mapper.BaseJobRecordResourceMappingMapper;
import com.fit2cloud.base.service.IBaseCloudAccountService;
import com.fit2cloud.base.service.IBaseVmCloudDiskService;
import com.fit2cloud.base.service.IBaseVmCloudImageService;
import com.fit2cloud.base.service.IBaseVmCloudServerService;
import com.fit2cloud.common.constants.CloudAccountConstants;
import com.fit2cloud.common.constants.JobTypeConstants;
import com.fit2cloud.common.constants.RedisConstants;
import com.fit2cloud.common.exception.Fit2cloudException;
import com.fit2cloud.common.platform.credential.Credential;
import com.fit2cloud.common.provider.IBaseCloudProvider;
import com.fit2cloud.common.utils.ColumnNameUtil;
import com.fit2cloud.common.utils.PageUtil;
import com.fit2cloud.common.utils.ServiceUtil;
import com.fit2cloud.constants.ErrorCodeConstants;
import com.fit2cloud.controller.handler.ResultHolder;
import com.fit2cloud.controller.request.cloud_account.*;
import com.fit2cloud.controller.response.cloud_account.CloudAccountJobDetailsResponse;
import com.fit2cloud.controller.response.cloud_account.CloudAccountResponse;
import com.fit2cloud.dao.entity.CloudAccount;
import com.fit2cloud.dao.mapper.CloudAccountMapper;
import com.fit2cloud.dto.PlatformResponse;
import com.fit2cloud.redis.RedisService;
import com.fit2cloud.request.cloud_account.CloudAccountModuleJob;
import com.fit2cloud.request.cloud_account.SyncRequest;
import com.fit2cloud.response.JobRecordResourceResponse;
import com.fit2cloud.response.cloud_account.ResourceCountResponse;
import com.fit2cloud.response.cloud_account.SyncResource;
import com.fit2cloud.service.ICloudAccountService;
import jakarta.annotation.Nullable;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author fit2cloud
 * @since
 */
@Service
public class CloudAccountServiceImpl extends ServiceImpl<CloudAccountMapper, CloudAccount> implements ICloudAccountService {
    @Resource(name = "workThreadPool")
    private ThreadPoolExecutor workThreadPool;
    @Resource
    private RestTemplate restTemplate;
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("yyyy/MM/dd");
    @Resource
    private IBaseCloudAccountService baseCloudAccountService;
    @Resource
    IBaseVmCloudServerService cloudServerService;
    @Resource
    IBaseVmCloudDiskService cloudDiskService;
    @Resource
    IBaseVmCloudImageService cloudImageService;
    @Resource
    private BaseJobRecordResourceMappingMapper baseJobRecordResourceMappingMapper;
    @Resource
    private RedisService redisService;

    /**
     * 获取模块云账号任务
     */
    private final BiFunction<String, String, ResultHolder<CloudAccountModuleJob>> getCloudAccountJobApi = (String moduleName, String accountId) -> {
        String httpUrl = ServiceUtil.getHttpUrl(moduleName, "/api/base/cloud_account/job/" + accountId);
        ResponseEntity<ResultHolder<CloudAccountModuleJob>> cloudAccountJob = restTemplate.exchange(httpUrl, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<ResultHolder<CloudAccountModuleJob>>() {
        });
        return cloudAccountJob.getBody();
    };

    /**
     * 初始化指定模块云账号定时任务
     */
    private final BiConsumer<String, String> initCloudAccountModuleJob = (String moduleName, String accountId) -> {
        String httpUrl = ServiceUtil.getHttpUrl(moduleName, "/api/base/cloud_account/job_init/" + accountId);
        ResponseEntity<ResultHolder<Object>> exchange = restTemplate.exchange(httpUrl, HttpMethod.POST, HttpEntity.EMPTY, new ParameterizedTypeReference<ResultHolder<Object>>() {
        });
        if (!Objects.requireNonNull(exchange.getBody()).getCode().equals(200)) {
            throw new Fit2cloudException(exchange.getBody().getCode(), exchange.getBody().getMessage());
        }
    };

    /**
     * 修改定时任务
     */
    private final BiFunction<CloudAccountModuleJob, String, CloudAccountModuleJob> updateJob = (CloudAccountModuleJob cloudAccountModuleJob, String accountId) -> {
        String httpUrl = ServiceUtil.getHttpUrl(cloudAccountModuleJob.getModule(), "/api/base/cloud_account/job/" + accountId);
        HttpEntity<CloudAccountModuleJob> cloudAccountModuleJobHttpEntity = new HttpEntity<>(cloudAccountModuleJob);
        ResponseEntity<ResultHolder<CloudAccountModuleJob>> exchange = restTemplate.exchange(httpUrl, HttpMethod.PUT, cloudAccountModuleJobHttpEntity, new ParameterizedTypeReference<ResultHolder<CloudAccountModuleJob>>() {
        });
        if (!Objects.requireNonNull(exchange.getBody()).getCode().equals(200)) {
            throw new Fit2cloudException(exchange.getBody().getCode(), exchange.getBody().getMessage());
        }
        return exchange.getBody().getData();
    };

    /**
     * 获取同步任务资源
     */
    private final BiFunction<String, String, List<SyncResource>> getResourceJob = (String module, String cloudAccountId) -> {
        String httpUrl = ServiceUtil.getHttpUrl(module, "/api/base/cloud_account/job/resource/" + cloudAccountId);
        ResponseEntity<ResultHolder<List<SyncResource>>> exchange = restTemplate.exchange(httpUrl, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<ResultHolder<List<SyncResource>>>() {
        });
        if (!Objects.requireNonNull(exchange.getBody()).getCode().equals(200)) {
            throw new Fit2cloudException(exchange.getBody().getCode(), exchange.getBody().getMessage());
        }
        return exchange.getBody().getData();
    };

    /**
     * 发送请求给各个模块触发同步任务
     */
    private final BiFunction<String, SyncRequest, Boolean> sync = (String module, SyncRequest request) -> {
        String httpUrl = ServiceUtil.getHttpUrl(module, "/api/base/cloud_account/sync");
        HttpEntity<SyncRequest> requestHttpEntity = new HttpEntity<>(request);
        ResponseEntity<ResultHolder<Boolean>> exchange = restTemplate.exchange(httpUrl, HttpMethod.POST, requestHttpEntity, new ParameterizedTypeReference<ResultHolder<Boolean>>() {
        });
        if (!Objects.requireNonNull(exchange.getBody()).getCode().equals(200)) {
            throw new Fit2cloudException(exchange.getBody().getCode(), exchange.getBody().getMessage());
        }
        return exchange.getBody().getData();
    };

    /**
     * 同步指定模块 指定云账的所有任务
     */
    private final BiFunction<String, String, Boolean> syncByCloudAccountId = (String module, String cloudAccountId) -> {
        String httpUrl = ServiceUtil.getHttpUrl(module, "/api/base/cloud_account/sync/" + cloudAccountId);
        ResponseEntity<ResultHolder<Boolean>> exchange = restTemplate.exchange(httpUrl, HttpMethod.POST, HttpEntity.EMPTY, new ParameterizedTypeReference<ResultHolder<Boolean>>() {
        });
        if (!Objects.requireNonNull(exchange.getBody()).getCode().equals(200)) {
            throw new Fit2cloudException(exchange.getBody().getCode(), exchange.getBody().getMessage());
        }
        return exchange.getBody().getData();
    };

    private final Function<String, List<PlatformResponse>> getPlatformResponse = (String module) -> {
        String httpUrl = ServiceUtil.getHttpUrl(module, "/api/base/cloud_account/platform");
        ResponseEntity<ResultHolder<List<PlatformResponse>>> exchange = restTemplate.exchange(httpUrl, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<>() {
        });
        if (!Objects.requireNonNull(exchange.getBody()).getCode().equals(200)) {
            throw new Fit2cloudException(exchange.getBody().getCode(), exchange.getBody().getMessage());
        }
        return exchange.getBody().getData();
    };

    @Override
    public IPage<CloudAccountResponse> page(CloudAccountRequest cloudAccountRequest) {
        Page<CloudAccountResponse> cloudAccountPage = PageUtil.of(cloudAccountRequest, CloudAccountResponse.class, new OrderItem("create_time", true));
        QueryWrapper<CloudAccountResponse> wrapper = new QueryWrapper<>();
        wrapper.like(StringUtils.isNotEmpty(cloudAccountRequest.getName()), ColumnNameUtil.getColumnName(CloudAccount::getName, false), cloudAccountRequest.getName())
                .in(CollectionUtils.isNotEmpty(cloudAccountRequest.getPlatform()), ColumnNameUtil.getColumnName(CloudAccount::getPlatform, false), cloudAccountRequest.getPlatform())
                .in(CollectionUtils.isNotEmpty(cloudAccountRequest.getState()), ColumnNameUtil.getColumnName(CloudAccount::getState, false), cloudAccountRequest.getState())
                .in(CollectionUtils.isNotEmpty(cloudAccountRequest.getStatus()), ColumnNameUtil.getColumnName(CloudAccountResponse::getStatus, false), CollectionUtils.isNotEmpty(cloudAccountRequest.getStatus()) ? cloudAccountRequest.getStatus().stream().map(CloudAccountConstants.Status::valueOf).collect(Collectors.toSet()) : new ArrayList<>())
                .between(CollectionUtils.isNotEmpty(cloudAccountRequest.getUpdateTime()), ColumnNameUtil.getColumnName(CloudAccount::getUpdateTime, false), CollectionUtils.isNotEmpty(cloudAccountRequest.getUpdateTime()) ? simpleDateFormat.format(cloudAccountRequest.getUpdateTime().get(0)) : "", CollectionUtils.isNotEmpty(cloudAccountRequest.getUpdateTime()) ? simpleDateFormat.format(cloudAccountRequest.getUpdateTime().get(1)) : "")
                .between(CollectionUtils.isNotEmpty(cloudAccountRequest.getCreateTime()), ColumnNameUtil.getColumnName(CloudAccount::getCreateTime, false), CollectionUtils.isNotEmpty(cloudAccountRequest.getCreateTime()) ? simpleDateFormat.format(cloudAccountRequest.getCreateTime().get(0)) : "", CollectionUtils.isNotEmpty(cloudAccountRequest.getCreateTime()) ? simpleDateFormat.format(cloudAccountRequest.getCreateTime().get(1)) : "");
        return baseMapper.pageCloudAccount(cloudAccountPage, wrapper);

    }

    @Override
    public List<PlatformResponse> getPlatforms() {
        return  this.baseCloudAccountService.getPlatforms();
    }

    private List<PlatformResponse> getPlatform(String module) {
        if (StringUtils.equals(module, ServerInfo.module)) {
            return this.baseCloudAccountService.getPlatforms();
        }
        return getPlatformResponse.apply(module);
    }

    @Override
    public CloudAccount save(AddCloudAccountRequest addCloudAccountRequest) {
        CloudAccount cloudAccount = new CloudAccount();
        checkCredential(addCloudAccountRequest.getCredential());
        cloudAccount.setCredential(addCloudAccountRequest.getCredential().enCode());
        cloudAccount.setPlatform(addCloudAccountRequest.getPlatform());
        cloudAccount.setName(addCloudAccountRequest.getName());
        cloudAccount.setState(addCloudAccountRequest.getCredential().verification());
        save(cloudAccount);
        return getById(cloudAccount.getId());
    }

    @Override
    public List<Credential.Region> listRegions(String accountId) {
        CloudAccount cloudAccount = getById(accountId);
        List<IBaseCloudProvider> extensions = PluginsContextHolder.getExtensions(IBaseCloudProvider.class);
        Optional<IBaseCloudProvider> first = extensions.stream()
                .filter(p -> p.getCloudAccountMeta().platform.equals(cloudAccount.getPlatform()))
                .findFirst();
        if (first.isPresent()) {
            try {
                return first.get().getCloudAccountMeta().credential.getConstructor().newInstance().deCode(cloudAccount.getCredential()).regions();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("不存在的云平台");
        }
    }

    @Override
    public CloudAccountJobDetailsResponse jobs(String accountId) {
        CloudAccountJobDetailsResponse cloudAccountJobDetailsResponse = new CloudAccountJobDetailsResponse();
        List<CloudAccountModuleJob> cloudAccountModuleJobs = ServiceUtil.getServicesExcludeGatewayAndIncludeSelf(ServerInfo.module)
                .stream()
                .map(moduleName -> CompletableFuture.supplyAsync(() -> this.getCloudModuleJob(accountId, moduleName), new DelegatingSecurityContextExecutor(workThreadPool)))
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();
        cloudAccountJobDetailsResponse.setCloudAccountModuleJobs(cloudAccountModuleJobs);
        return cloudAccountJobDetailsResponse;
    }


    /**
     * 获取不同模块到云账号定时任务
     *
     * @param cloudAccountId 云账号id
     * @param moduleName     模块名称
     * @return 模块的云账号定时任务
     */
    private CloudAccountModuleJob getCloudModuleJob(String cloudAccountId, String moduleName) {
        // 如果是当前模块,直接获取
        if (moduleName.equals(ServerInfo.module)) {
            return baseCloudAccountService.getCloudAccountJob(cloudAccountId);
        } else {
            ResultHolder<CloudAccountModuleJob> apply = getCloudAccountJobApi.apply(moduleName, cloudAccountId);
            if (apply.getCode().equals(200)) {
                return apply.getData();
            }
            throw new Fit2cloudException(apply.getCode(), apply.getMessage());
        }
    }

    private void initCLoudModuleJob(String cloudAccountId, String moduleName) {
        if (moduleName.equals(ServerInfo.module)) {
            baseCloudAccountService.initCloudAccountJob(cloudAccountId);
        } else {
            initCloudAccountModuleJob.accept(moduleName, cloudAccountId);
        }
    }

    private CloudAccountModuleJob updateCloudModuleJob(CloudAccountModuleJob cloudAccountModuleJob, String cloudAccountId, String moduleName) {
        if (moduleName.equals(ServerInfo.module)) {
            return baseCloudAccountService.updateJob(cloudAccountModuleJob, cloudAccountId);
        } else {
            return updateJob.apply(cloudAccountModuleJob, cloudAccountId);
        }
    }

    @Override
    public CloudAccountJobDetailsResponse updateJob(UpdateJobsRequest updateJobsRequest) {
        List<CloudAccountModuleJob> cloudAccountModuleJobs = ServiceUtil.getServicesExcludeGatewayAndIncludeSelf(ServerInfo.module)
                .stream()
                .map(module -> CompletableFuture.supplyAsync(() -> getCloudAccountModuleJob(updateJobsRequest, module), new DelegatingSecurityContextExecutor(workThreadPool)))
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();
        CloudAccountJobDetailsResponse cloudAccountJobDetailsResponse = new CloudAccountJobDetailsResponse();
        cloudAccountJobDetailsResponse.setCloudAccountModuleJobs(cloudAccountModuleJobs);
        return cloudAccountJobDetailsResponse;
    }

    @Nullable
    private CloudAccountModuleJob getCloudAccountModuleJob(UpdateJobsRequest updateJobsRequest, String module) {
        Optional<CloudAccountModuleJob> first = updateJobsRequest.getCloudAccountModuleJobs().stream().filter(moduleJob -> moduleJob.getModule().equals(module)).findFirst();
        return first.map(cloudAccountModuleJob -> this.updateCloudModuleJob(cloudAccountModuleJob, updateJobsRequest.getCloudAccountId(), module)).orElse(null);
    }


    @Override
    public CloudAccount update(UpdateCloudAccountRequest updateCloudAccountRequest) {
        CloudAccount cloudAccount = new CloudAccount();
        checkCredential(updateCloudAccountRequest.getCredential());
        cloudAccount.setCredential(updateCloudAccountRequest.getCredential().enCode());
        cloudAccount.setPlatform(updateCloudAccountRequest.getPlatform());
        cloudAccount.setName(updateCloudAccountRequest.getName());
        cloudAccount.setState(updateCloudAccountRequest.getCredential().verification());
        cloudAccount.setId(updateCloudAccountRequest.getId());
        updateById(cloudAccount);
        return this.getById(updateCloudAccountRequest.getId());
    }

    /**
     * 校验认证信息
     *
     * @param credential 认证对象
     */
    private void checkCredential(Credential credential) {
        // 校验ak sk
        credential.verification();
    }

    @Override
    public boolean delete(String accountId) {
        redisService.publish(RedisConstants.Topic.CLOUD_ACCOUNT_DELETE, accountId);
        return removeById(accountId);
    }

    @Override
    public boolean delete(ArrayList<String> cloudAccountIds) {
        for (String cloudAccountId : cloudAccountIds) {
            delete(cloudAccountId);
        }
        return true;
    }

    @Override
    public CloudAccount verification(String accountId) {
        CloudAccount cloudAccount = new CloudAccount()
                .setId(accountId)
                .setState(true);
        try {
            listRegions(accountId);
        } catch (Exception e) {
            cloudAccount.setState(false);
            updateById(cloudAccount);
            throw e;
        }
        updateById(cloudAccount);
        // 校验成功更新数据
        return getById(accountId);
    }

    @Override
    public List<SyncResource> getModuleResourceJob(String cloudAccountId) {
        return ServiceUtil.getServicesExcludeGatewayAndIncludeSelf(ServerInfo.module)
                .stream()
                .map(moduleId -> CompletableFuture.supplyAsync(() -> getSyncResource(moduleId, cloudAccountId), new DelegatingSecurityContextExecutor(workThreadPool)))
                .map(CompletableFuture::join)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public void sync(SyncRequest syncRequest) {
        ServiceUtil.getServicesExcludeGatewayAndIncludeSelf(ServerInfo.module).forEach(moduleId -> CompletableFuture.runAsync(() -> sync(moduleId, syncRequest), new DelegatingSecurityContextExecutor(workThreadPool)));
    }

    /**
     * 同步函数
     *
     * @param moduleId    模块id
     * @param syncRequest 同步请求参数
     */
    private void sync(String moduleId, SyncRequest syncRequest) {
        Optional.of(syncRequest).map(sync -> {
            List<SyncRequest.Job> jobs = sync.getSyncJob().stream().filter(j -> j.getModule().equals(moduleId)).toList();
            if (CollectionUtils.isNotEmpty(jobs)) {
                SyncRequest r = new SyncRequest();
                r.setCloudAccountId(syncRequest.getCloudAccountId());
                r.setSyncJob(jobs);
                r.setParams(syncRequest.getParams());
                return r;
            } else {
                return null;
            }
        }).ifPresent(moduleSyncRequest -> sync.apply(moduleId, moduleSyncRequest));
    }

    /**
     * 获取同步数据
     *
     * @param module 模块名称
     * @return 模块的任务
     */
    public List<SyncResource> getSyncResource(String module, String cloudAccountId) {
        if (module.equals(ServerInfo.module)) {
            return baseCloudAccountService.getModuleResourceJob(cloudAccountId);
        } else {
            return getResourceJob.apply(module, cloudAccountId);
        }
    }

    @Override
    public Boolean updateAccountName(UpdateAccountNameRequest updateAccountNameRequest) {
        CloudAccount cloudAccount = new CloudAccount();
        cloudAccount.setId(updateAccountNameRequest.getId());
        cloudAccount.setName(updateAccountNameRequest.getName());
        updateById(cloudAccount);
        return true;
    }

    /**
     * 资源计数（静态获取）
     *
     * @param accountId
     * @return
     */
    @Override
    public List<ResourceCountResponse> resourceCount(String accountId) {
        List<ResourceCountResponse> list = new ArrayList<>();
        // 云主机
        QueryWrapper<VmCloudServer> vmQueryWrapper = Wrappers.query();
        vmQueryWrapper.lambda().ne(VmCloudServer::getInstanceStatus, "deleted").eq(VmCloudServer::getAccountId, accountId);
        ResourceCountResponse vm = new ResourceCountResponse("xuniyunzhuji", "云主机", cloudServerService.count(vmQueryWrapper), "台");
        list.add(vm);
        // 磁盘
        QueryWrapper<VmCloudDisk> diskQueryWrapper = Wrappers.query();
        diskQueryWrapper.lambda().ne(VmCloudDisk::getStatus, "deleted").eq(VmCloudDisk::getAccountId, accountId);
        ResourceCountResponse disk = new ResourceCountResponse("yuncunchu", "磁盘", cloudDiskService.count(diskQueryWrapper), "个");
        list.add(disk);
        // 镜像
        QueryWrapper<VmCloudImage> imageQueryWrapper = Wrappers.query();
        imageQueryWrapper.lambda().ne(VmCloudImage::getStatus, "deleted").eq(VmCloudImage::getAccountId, accountId);
        ResourceCountResponse image = new ResourceCountResponse("jingxiang", "镜像", cloudImageService.count(imageQueryWrapper), "个");
        list.add(image);
        return list;
    }

    @Override
    public List<JobRecordResourceResponse> findCloudAccountSyncStatus(List<String> cloudAccountIds) {
        return baseJobRecordResourceMappingMapper.findLastResourceJobRecord(cloudAccountIds, List.of(JobTypeConstants.CLOUD_ACCOUNT_SYNC_JOB.getCode(), JobTypeConstants.CLOUD_ACCOUNT_SYNC_BILL_JOB.getCode()));
    }

    @Override
    public void sync(ArrayList<String> cloudAccountIds) {
        List<CloudAccount> cloudAccounts = listByIds(cloudAccountIds).stream().filter(c -> !c.getState()).toList();
        if (CollectionUtils.isNotEmpty(cloudAccounts)) {
            throw new Fit2cloudException(ErrorCodeConstants.CLOUD_ACCOUNT_INVALID_UNABLE_SYNC.getCode(), ErrorCodeConstants.CLOUD_ACCOUNT_INVALID_UNABLE_SYNC.getMessage() + cloudAccounts.stream().map(CloudAccount::getName).toList());
        }
        for (String cloudAccountId : cloudAccountIds) {
            syncByCloudAccountId(cloudAccountId);
        }
    }

    /**
     * 根据云账号同步
     *
     * @param cloudAccountId 云账号
     */
    private List<CompletableFuture<Boolean>> syncByCloudAccountId(String cloudAccountId) {
        return ServiceUtil.getServicesExcludeGatewayAndIncludeSelf(ServerInfo.module)
                .stream()
                .map(moduleId -> CompletableFuture.supplyAsync(() -> syncByCloudAccountId.apply(moduleId, cloudAccountId), new DelegatingSecurityContextExecutor(workThreadPool)))
                .toList();
    }

    /**
     * 资源计数（动态获取）
     *
     * @return
     */
    @Override
    public List<ResourceCountResponse> getModuleResourceCount(String accountId) {
        return ServiceUtil.getServicesExcludeGatewayAndIncludeSelf(ServerInfo.module)
                .stream()
                .map(moduleId -> CompletableFuture.supplyAsync(() -> getResourceCount(moduleId, accountId), new DelegatingSecurityContextExecutor(workThreadPool)))
                .map(CompletableFuture::join)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 获取资源计数
     *
     * @param module
     * @return
     */
    private List<ResourceCountResponse> getResourceCount(String module, String accountId) {
        if (module.equals(ServerInfo.module)) {
            return baseCloudAccountService.getModuleResourceCount(accountId);
        } else {
            return getResourceCount.apply(module, accountId);
        }
    }

    /**
     * 获取资源计数
     */
    private final BiFunction<String, String, List<ResourceCountResponse>> getResourceCount = (String module, String accountId) -> {
        String httpUrl = ServiceUtil.getHttpUrl(module, "/api/base/cloud_account/count/resource/" + accountId);
        ResponseEntity<ResultHolder<List<ResourceCountResponse>>> exchange = restTemplate.exchange(httpUrl, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<>() {
        });
        if (!Objects.requireNonNull(exchange.getBody()).getCode().equals(200)) {
            throw new Fit2cloudException(exchange.getBody().getCode(), exchange.getBody().getMessage());
        }
        return exchange.getBody().getData();
    };

    @Override
    public IPage<JobRecordResourceResponse> pageSyncRecord(SyncRecordRequest syncRecordRequest) {
        Page<JobRecordResourceResponse> syncRecordPage = PageUtil.of(syncRecordRequest, JobRecordResourceResponse.class);
        QueryWrapper wrapper = Wrappers.query().in(ColumnNameUtil.getColumnName(JobRecord::getType, false), List.of(JobTypeConstants.CLOUD_ACCOUNT_SYNC_JOB, JobTypeConstants.CLOUD_ACCOUNT_SYNC_BILL_JOB, JobTypeConstants.CLOUD_ACCOUNT_SYNC_METRIC_MONITOR));
        wrapper.eq("resource_id", syncRecordRequest.getCloudAccountId());

        wrapper.eq(StringUtils.isNotBlank(syncRecordRequest.getDescription()), ColumnNameUtil.getColumnName(JobRecord::getDescription, false), syncRecordRequest.getDescription());

        wrapper.eq(syncRecordRequest.getStatus() != null, ColumnNameUtil.getColumnName(JobRecord::getStatus, false), syncRecordRequest.getStatus());

        if (syncRecordRequest.getCreateTime() != null) {
            Calendar cal1 = Calendar.getInstance();
            cal1.setTime(syncRecordRequest.getCreateTime());
            cal1.set(Calendar.HOUR_OF_DAY, 0);
            cal1.set(Calendar.SECOND, 0);
            cal1.set(Calendar.MINUTE, 0);
            cal1.set(Calendar.MILLISECOND, 0);

            Calendar cal2 = Calendar.getInstance();
            cal2.setTime(syncRecordRequest.getCreateTime());
            cal2.set(Calendar.HOUR_OF_DAY, 24);
            cal2.set(Calendar.SECOND, 0);
            cal2.set(Calendar.MINUTE, 0);
            cal2.set(Calendar.MILLISECOND, 0);

            wrapper.between(ColumnNameUtil.getColumnName(JobRecord::getCreateTime, false), cal1.getTime(), cal2.getTime());
        }

        return baseMapper.pageSyncRecord(syncRecordPage, wrapper);
    }

    @Override
    public List<String> listTypes() {
        LambdaQueryWrapper<JobRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(JobRecord::getType, List.of(JobTypeConstants.CLOUD_ACCOUNT_SYNC_JOB, JobTypeConstants.CLOUD_ACCOUNT_SYNC_BILL_JOB));

        return baseMapper.listTypes(wrapper);
    }
}
