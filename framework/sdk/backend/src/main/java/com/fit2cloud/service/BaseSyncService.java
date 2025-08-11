package com.fit2cloud.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fit2cloud.base.entity.CloudAccount;
import com.fit2cloud.base.entity.JobRecord;
import com.fit2cloud.base.service.IBaseCloudAccountService;
import com.fit2cloud.base.service.IBaseJobRecordService;
import com.fit2cloud.common.constants.JobStatusConstants;
import com.fit2cloud.common.platform.credential.Credential;
import com.fit2cloud.common.provider.exception.SkipPageException;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.functions.Consumer;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2022/10/18  4:19 PM}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Service
public abstract class BaseSyncService {
    @Resource
    protected RedissonClient redissonClient;
    @Resource
    protected IBaseCloudAccountService cloudAccountService;
    @Resource
    protected IBaseJobRecordService baseJobRecordService;

    protected abstract Credential getCredential(CloudAccount cloudAccount);

    /**
     * 代理同步数据
     *
     * @param cloudAccountId    云账号id
     * @param jobDescription    定时任务描述
     * @param getCloudProvider  获取定时任务执行器
     * @param initJobRecord     初始化任务记录
     * @param execMethod        执行函数
     * @param getExecMethodArgs 获取执行函数参数
     * @param saveBatchOrUpdate 插入或者修改数据
     * @param writeJobRecord    写入任务记录参数
     * @param remote            云账号不存在的时候 删除云账号资源
     * @param <T>               同步数据泛型
     * @param <P>               执行器泛型
     */
    @SneakyThrows
    protected <T, P> void proxy(String cloudAccountId,
                                String jobDescription,
                                List<String> months,
                                Function<String, P> getCloudProvider,
                                Function<LocalDateTime, JobRecord> initJobRecord,
                                BiFunction<P, String, List<T>> execMethod,
                                BiFunction<CloudAccount, String, String> getExecMethodArgs,
                                Consumer<BiSaveBatchOrUpdateParams<T>> saveBatchOrUpdate,
                                Consumer<BiSaveBatchOrUpdateParams<T>> writeJobRecord,
                                Runnable postHandler,
                                Consumer<String> remote) {
        RLock lock = redissonClient.getLock(cloudAccountId + jobDescription);
        try {
            if (lock.tryLock()) {
                CloudAccount cloudAccount = cloudAccountService.getById(cloudAccountId);
                if (Objects.nonNull(cloudAccount)) {
                    Credential credential = getCredential(cloudAccount);
                    // 如果云账号无效 跳过执行
                    try {
                        credential.verification();
                    } catch (Exception e) {
                        cloudAccount.setState(false);
                        cloudAccountService.updateById(cloudAccount);
                        return;
                    }
                    LocalDateTime syncTime = getSyncTime();
                    cloudAccount.setUpdateTime(syncTime);
                    cloudAccountService.updateById(cloudAccount);
                    // 初始化一条定时任务记录
                    JobRecord jobRecord = initJobRecord.apply(syncTime);
                    P apply = getCloudProvider.apply(cloudAccount.getPlatform());
                    try {
                        for (String month : months) {
                            String params = getExecMethodArgs.apply(cloudAccount, month);
                            try {
                                // 同步数据
                                List<T> syncRecord = execMethod.apply(apply, params);
                                BiSaveBatchOrUpdateParams<T> tSaveBillBatchOrUpdateParams = new BiSaveBatchOrUpdateParams<>(cloudAccount, syncTime, params, syncRecord, jobRecord);
                                // 因为账单数据量比较大,如果在同步的过程中删除了云账号,那么直接不进行插入并且跳过同步循环
                                CloudAccount c = cloudAccountService.getById(cloudAccountId);
                                if (Objects.isNull(c)) {
                                    remote.accept(cloudAccountId);
                                    break;
                                }
                                // 插入并且更新数据
                                saveBatchOrUpdate.accept(tSaveBillBatchOrUpdateParams);
                                // 记录同步日志
                                writeJobRecord.accept(tSaveBillBatchOrUpdateParams);
                            } catch (SkipPageException ignored) {
                                writeJobRecord.accept(new BiSaveBatchOrUpdateParams<>(cloudAccount, syncTime, params, new ArrayList<>(), jobRecord));
                            }
                        }
                        postHandler.run();
                        // 修改同步状态为成功
                        baseJobRecordService.update(new LambdaUpdateWrapper<JobRecord>().eq(JobRecord::getId, jobRecord.getId()).set(JobRecord::getStatus, JobStatusConstants.SUCCESS));
                    } catch (Throwable e) {
                        e.printStackTrace();
                        baseJobRecordService.update(new LambdaUpdateWrapper<JobRecord>().eq(JobRecord::getId, jobRecord.getId()).set(JobRecord::getStatus, JobStatusConstants.FAILED));
                    }
                } else {
                    // 删除云账号相关的资源
                    try {
                        remote.accept(cloudAccountId);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                    // 删除定时任务
                    cloudAccountService.deleteJobByCloudAccountId(cloudAccountId);
                }
            }
        } finally {
            CloudAccount c = cloudAccountService.getById(cloudAccountId);
            if (Objects.isNull(c)) {
                if (Objects.nonNull(remote)) {
                    remote.accept(cloudAccountId);
                }
            }
            if (lock.isLocked()) {
                lock.unlock();
            }
        }
    }

    /**
     * 代理同步数据
     *
     * @param cloudAccountId    云账号id
     * @param jobDescription    定时任务描述
     * @param getCloudProvider  获取定时任务执行器
     * @param initJobRecord     初始化任务记录
     * @param execMethod        执行函数
     * @param getExecMethodArgs 获取执行函数参数
     * @param saveBatchOrUpdate 插入或者修改数据
     * @param writeJobRecord    写入任务记录参数
     * @param remote            云账号不存在的时候 删除云账号资源
     * @param <T>               同步数据泛型
     * @param <P>               执行器泛型
     */
    protected <T, P> void proxy(String cloudAccountId,
                                String jobDescription,
                                List<String> months,
                                Function<String, P> getCloudProvider,
                                Function<LocalDateTime, JobRecord> initJobRecord,
                                BiFunction<P, String, List<T>> execMethod,
                                BiFunction<CloudAccount, String, String> getExecMethodArgs,
                                Consumer<BiSaveBatchOrUpdateParams<T>> saveBatchOrUpdate,
                                Consumer<BiSaveBatchOrUpdateParams<T>> writeJobRecord,
                                Consumer<String> remote) {
        proxy(cloudAccountId, jobDescription, months, getCloudProvider, initJobRecord, execMethod, getExecMethodArgs, saveBatchOrUpdate, writeJobRecord, () -> {
        }, remote);
    }

    /**
     * @param cloudAccountId    云账号id
     * @param regions           区域
     * @param jobDescription    任务描述
     * @param getCloudProvider  获取任务处理器
     * @param initJobRecord     初始化任务记录
     * @param execMethod        执行定时任务函数
     * @param getExecMethodArgs 任务执行参数
     * @param saveBatchOrUpdate 插入或者更新同步数据
     * @param writeJobRecord    写入区域任务记录
     * @param remote            删除相关资源
     * @param <T>               同步记录泛型
     * @param <P>               执行处理器
     */
    protected <T, P> void proxy(String cloudAccountId,
                                List<Credential.Region> regions,
                                String jobDescription,
                                Function<String, P> getCloudProvider,
                                Function<LocalDateTime, JobRecord> initJobRecord,
                                BiFunction<P, String, List<T>> execMethod,
                                BiFunction<CloudAccount, Credential.Region, String> getExecMethodArgs,
                                Consumer<SaveBatchOrUpdateParams<T>> saveBatchOrUpdate,
                                Consumer<SaveBatchOrUpdateParams<T>> writeJobRecord,
                                Runnable remote) {

        RLock lock = redissonClient.getLock(cloudAccountId + jobDescription);
        try {
            if (lock.tryLock()) {
                CloudAccount cloudAccount = cloudAccountService.getById(cloudAccountId);
                if (Objects.nonNull(cloudAccount)) {
                    Credential credential = getCredential(cloudAccount);
                    // 如果云账号无效 跳过执行
                    try {
                        credential.verification();
                    } catch (Exception e) {
                        cloudAccount.setState(false);
                        cloudAccountService.updateById(cloudAccount);
                        return;
                    }
                    LocalDateTime syncTime = getSyncTime();
                    cloudAccount.setUpdateTime(syncTime);
                    cloudAccountService.updateById(cloudAccount);
                    //转换为时间戳字符串
                    cloudAccount.setSyncTimeStampStr(String.valueOf(syncTime.toInstant(ZoneOffset.of("+8")).toEpochMilli()));
                    // 初始化一条定时任务记录
                    JobRecord jobRecord = initJobRecord.apply(syncTime);
                    P apply = getCloudProvider.apply(cloudAccount.getPlatform());
                    try {
                        for (Credential.Region region : regions) {
                            try {
                                // 同步数据
                                List<T> syncRecord = execMethod.apply(apply, getExecMethodArgs.apply(cloudAccount, region));
                                SaveBatchOrUpdateParams<T> tSaveBatchOrUpdateParams = new SaveBatchOrUpdateParams<>(cloudAccountId, syncTime, region, syncRecord, jobRecord);
                                // 插入并且更新数据
                                saveBatchOrUpdate.accept(tSaveBatchOrUpdateParams);
                                // 记录同步日志
                                writeJobRecord.accept(tSaveBatchOrUpdateParams);
                            } catch (SkipPageException ignored) { // 如果发生跳过异常,那么就不同步当前区域
                                jobRecord.setResult(region + "-" + ignored.getMessage());
                                writeJobRecord.accept(new SaveBatchOrUpdateParams<>(cloudAccountId, syncTime, region, new ArrayList<>(), jobRecord));
                            }
                        }
                        // 修改同步状态为成功
                        baseJobRecordService.update(new LambdaUpdateWrapper<JobRecord>().eq(JobRecord::getId, jobRecord.getId()).set(JobRecord::getStatus, JobStatusConstants.SUCCESS));
                    } catch (Throwable e) {
                        baseJobRecordService.update(new LambdaUpdateWrapper<JobRecord>().eq(JobRecord::getId, jobRecord.getId()).set(JobRecord::getStatus, JobStatusConstants.FAILED).set(JobRecord::getResult, e.getMessage()));
                    }
                } else {
                    // 删除云账号相关的资源
                    remote.run();
                    // 删除定时任务
                    cloudAccountService.deleteJobByCloudAccountId(cloudAccountId);
                }
            }
        } finally {
            // todo 同步中删除云账号,数据入库问题
            CloudAccount cloudAccount = cloudAccountService.getById(cloudAccountId);
            if (Objects.isNull(cloudAccount)) {
                remote.run();
            }
            // 解锁
            if (lock.isLocked()) {
                lock.unlock();
            }
        }

    }

    /**
     * 批量插入并且逻辑删除
     *
     * @param service             服务
     * @param dataList            需要插入的数据
     * @param getUpdateWrapper    获取更新的mapper
     * @param updateDeleteWrapper 删除mapper
     * @param <T>                 数据泛型
     */
    protected <T> void saveBatchOrUpdate(IService<T> service, List<T> dataList, Function<T, Wrapper<T>> getUpdateWrapper, Wrapper<T> updateDeleteWrapper) {
        for (T entity : dataList) {
            Wrapper<T> updateWrapper = getUpdateWrapper.apply(entity);
            // 插入或者更新数据
            service.saveOrUpdate(entity, updateWrapper);
        }
        // 删除数据,因为是逻辑删除所以更新status字段
        service.update(updateDeleteWrapper);
    }

    /**
     * 根据云账号获取区域
     *
     * @param accountId 云账号id
     * @return 云账号区域
     */
    protected List<Credential.Region> getRegions(String accountId) {
        CloudAccount cloudAccount = cloudAccountService.getById(accountId);
        return getCredential(cloudAccount).regions();
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SaveBatchOrUpdateParams<T> {
        /**
         * 云账户id
         */
        private String cloudAccountId;
        /**
         * 更新时间
         */
        private LocalDateTime syncTime;
        /**
         * 区域
         */
        private Credential.Region region;
        /**
         * 数据
         */
        private List<T> syncRecord;
        /**
         * 任务记录
         */
        private JobRecord jobRecord;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BiSaveBatchOrUpdateParams<T> {
        /**
         * 云账号数据
         */
        private CloudAccount cloudAccount;
        /**
         * 同步时间
         */
        private LocalDateTime syncTime;
        /**
         * 参数
         */
        private String requestParams;
        /**
         * 数据
         */
        private List<T> syncRecord;
        /**
         * 任务记录
         */
        private JobRecord jobRecord;

    }

    /**
     * 更新的时候精确到秒 因为数据插入也是精确到秒
     *
     * @return 更新时间
     */
    protected LocalDateTime getSyncTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(now.format(dateTimeFormatter), dateTimeFormatter);
    }


}
