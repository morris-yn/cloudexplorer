package com.fit2cloud.provider.impl.aliyun;

import com.aliyun.ecs20140526.models.DescribeRegionsResponseBody;
import com.aliyun.ecs20140526.models.DescribeSecurityGroupsResponseBody;
import com.aliyun.ecs20140526.models.DescribeZonesResponseBody;
import com.fit2cloud.common.form.util.FormUtil;
import com.fit2cloud.common.form.vo.FormObject;
import com.fit2cloud.common.provider.entity.F2CBalance;
import com.fit2cloud.common.provider.entity.F2CPerfMetricMonitorData;
import com.fit2cloud.common.provider.impl.aliyun.AliyunBaseCloudProvider;
import com.fit2cloud.common.utils.JsonUtil;
import com.fit2cloud.provider.impl.aliyun.api.AliyunSyncCloudApi;
import com.fit2cloud.provider.impl.aliyun.constants.*;
import com.fit2cloud.provider.impl.aliyun.entity.AliyunDiskTypeDTO;
import com.fit2cloud.provider.impl.aliyun.entity.AliyunInstanceType;
import com.fit2cloud.provider.impl.aliyun.entity.credential.AliyunVmCredential;
import com.fit2cloud.provider.impl.aliyun.entity.request.*;
import com.fit2cloud.vm.AbstractCloudProvider;
import com.fit2cloud.vm.ICloudProvider;
import com.fit2cloud.vm.ICreateServerRequest;
import com.fit2cloud.vm.constants.ActionInfoConstants;
import com.fit2cloud.vm.constants.DeleteWithInstance;
import com.fit2cloud.vm.entity.F2CDisk;
import com.fit2cloud.vm.entity.F2CImage;
import com.fit2cloud.vm.entity.F2CNetwork;
import com.fit2cloud.vm.entity.F2CVirtualMachine;
import com.fit2cloud.vm.entity.request.BaseDiskRequest;
import com.fit2cloud.vm.entity.request.GetMetricsRequest;
import com.fit2cloud.vm.entity.request.RenewInstanceRequest;
import org.pf4j.Extension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author:张少虎
 * @Date: 2022/9/20  10:55 AM
 * @Version 1.0
 * @注释:
 */
@Extension
public class AliyunCloudProvider extends AbstractCloudProvider<AliyunVmCredential> implements ICloudProvider {
    public static final AliyunBaseCloudProvider aliyunBaseCloudProvider = new AliyunBaseCloudProvider();

    private static final Info info = new Info("vm-service", ActionInfoConstants.all(
            ActionInfoConstants.SYNC_DATA_STORE_MACHINE_METRIC_MONITOR,
            ActionInfoConstants.SYNC_DATASTORE,
            ActionInfoConstants.SYNC_HOST,
            ActionInfoConstants.SYNC_HOST_MACHINE_METRIC_MONITOR), Map.of());

    @Override
    public Class<? extends ICreateServerRequest> getCreateServerRequestClass() {
        return AliyunVmCreateRequest.class;
    }

    // For Create VM [START]
    @Override
    public FormObject getCreateServerForm() {
        return FormUtil.toForm(AliyunVmCreateRequest.class);
    }

    @Override
    public BigDecimal renewInstancePrice(String req) {
        return AliyunSyncCloudApi.renewInstancePrice(JsonUtil.parseObject(req, RenewInstanceRequest.class));
    }

    @Override
    public String renewInstanceExpiresTime(String req) {
        return AliyunSyncCloudApi.renewInstanceExpiresTime(JsonUtil.parseObject(req, AliRenewInstanceExpiresTimeRequest.class));
    }

    @Override
    public FormObject getRenewInstanceForm() {
        return FormUtil.toForm(AliRenewInstanceRequest.class);
    }


    @Override
    public F2CVirtualMachine renewInstance(String req) {
        return AliyunSyncCloudApi.renewInstance(JsonUtil.parseObject(req, RenewInstanceRequest.class));
    }

    @Override
    public F2CVirtualMachine createVirtualMachine(String req) {
        return AliyunSyncCloudApi.createVirtualMachine(JsonUtil.parseObject(req, AliyunVmCreateRequest.class));
    }

    @Override
    public F2CVirtualMachine getSimpleServerByCreateRequest(String req) {
        return AliyunSyncCloudApi.getSimpleServerByCreateRequest(JsonUtil.parseObject(req, AliyunVmCreateRequest.class));
    }

    public List<DescribeRegionsResponseBody.DescribeRegionsResponseBodyRegionsRegion> getRegions(String req) {
        return AliyunSyncCloudApi.getRegions(JsonUtil.parseObject(req, AliyunGetRegionRequest.class));
    }

    public List<DescribeZonesResponseBody.DescribeZonesResponseBodyZonesZone> getZones(String req) {
        return AliyunSyncCloudApi.getZones(JsonUtil.parseObject(req, AliyunGetZoneRequest.class));
    }

    /**
     * 获取付费方式
     *
     * @param req
     * @return
     */
    public List<Map<String, String>> getChargeType(String req) {
        List<Map<String, String>> result = new ArrayList<>();
        for (AliyunChargeType chargeType : AliyunChargeType.values()) {
            Map<String, String> map = new HashMap<>();
            map.put("id", chargeType.getId());
            map.put("name", chargeType.getName());
            result.add(map);
        }
        return result;
    }

    /**
     * 获取付费周期
     *
     * @param req
     * @return
     */
    public List<Map<String, Object>> getPeriodOption(String req) {
        List<Map<String, Object>> periodList = new ArrayList<>();
        for (AliyunPeriodOption option : AliyunPeriodOption.values()) {
            Map<String, Object> map = new HashMap<>();
            map.put("period", option.getPeriod());
            map.put("periodDisplayName", option.getPeriodDisplayName());
            periodList.add(map);
        }
        return periodList;
    }

    /**
     * 获取操作系统类型
     *
     * @param req
     * @return
     */
    public List<Map<String, String>> getOsTypes(String req) {
        List<Map<String, String>> result = new ArrayList<>();
        for (AliyunOSType type : AliyunOSType.values()) {
            Map<String, String> map = new HashMap<>();
            map.put("id", type.getDisplayValue());
            map.put("name", type.getDisplayValue());
            result.add(map);
        }
        //添加自定义镜像
        Map<String, String> define = new HashMap<>();
        define.put("id", "self_define");
        define.put("name", "自定义镜像");
        result.add(define);
        return result;
    }

    /**
     * 获取磁盘类型
     *
     * @param req
     * @return
     */
    public AliyunDiskTypeDTO getDiskTypesForCreateVm(String req) {
        AliyunDiskTypeDTO aliyunDiskTypeDTO = new AliyunDiskTypeDTO();
        aliyunDiskTypeDTO.setSystemDiskTypes(AliyunSyncCloudApi.getSystemDiskType(JsonUtil.parseObject(req, AliyunGetDiskTypeRequest.class)));
        aliyunDiskTypeDTO.setDataDiskTypes(AliyunSyncCloudApi.getDataDiskType(JsonUtil.parseObject(req, AliyunGetDiskTypeRequest.class)));
        return aliyunDiskTypeDTO;
    }

    /**
     * 获取带宽计费类型
     *
     * @param req
     * @return
     */
    public List<Map<String, String>> getBandwidthChargeTypes(String req) {
        List<Map<String, String>> result = new ArrayList<>();
        for (AliyunBandwidthType type : AliyunBandwidthType.values()) {
            Map<String, String> map = new HashMap<>();
            map.put("id", type.getId());
            map.put("name", type.getName());
            result.add(map);
        }
        return result;
    }

    /**
     * 获取网络
     *
     * @param req
     * @return
     */
    public List<F2CNetwork> getNetworks(String req) {
        return AliyunSyncCloudApi.getNetworks(JsonUtil.parseObject(req, AliyunGetVSwitchRequest.class));
    }

    /**
     * 获取安全组
     *
     * @param req
     * @return
     */
    public List<DescribeSecurityGroupsResponseBody.DescribeSecurityGroupsResponseBodySecurityGroupsSecurityGroup> getSecurityGroups(String req) {
        return AliyunSyncCloudApi.getSecurityGroups(JsonUtil.parseObject(req, AliyunGetSecurityGroupRequest.class));
    }

    /**
     * 获取实例类型
     *
     * @param req
     * @return
     */
    public List<AliyunInstanceType> getInstanceTypes(String req) {
        return AliyunSyncCloudApi.getInstanceTypes(JsonUtil.parseObject(req, AliyunGetAvailableResourceRequest.class));
    }

    /**
     * 获取镜像
     *
     * @param req
     * @return
     */
    public List<F2CImage> getImages(String req) {
        return AliyunSyncCloudApi.getImages(JsonUtil.parseObject(req, AliyunGetImageRequest.class));
    }

    /**
     * 获取登录方式
     *
     * @param req
     * @return
     */
    public List<Map<String, String>> getLoginTypes(String req) {
        List<Map<String, String>> result = new ArrayList<>();
        for (AliyunLoginType type : AliyunLoginType.values()) {
            Map<String, String> map = new HashMap<>();
            map.put("id", type.getId());
            map.put("name", type.getName());
            result.add(map);
        }
        return result;
    }

    /**
     * 获取登录用户
     *
     * @param req
     * @return
     */
    public String getLoginUser(String req) {
        AliyunVmCreateRequest request = JsonUtil.parseObject(req, AliyunVmCreateRequest.class);
        if (request.getOs() != null && request.getOs().toLowerCase().indexOf("windows") > -1) {
            return "Administrator";
        }
        return "root";
    }

    /**
     * 获取密钥对
     *
     * @param req
     * @return
     */
    public List<Map<String, String>> getKeyPairs(String req) {
        return AliyunSyncCloudApi.getKeyPairs(JsonUtil.parseObject(req, AliyunBaseRequest.class));
    }

    /**
     * 基础配置询价
     *
     * @param req
     * @return
     */
    @Override
    public String calculateConfigPrice(String req) {
        return AliyunSyncCloudApi.calculateConfigPrice(JsonUtil.parseObject(req, AliyunPriceRequest.class));
    }

    /**
     * 公网IP流量配置询价
     *
     * @param req
     * @return
     */
    public String calculateTrafficPrice(String req) {
        return AliyunSyncCloudApi.calculateTrafficPrice(JsonUtil.parseObject(req, AliyunPriceRequest.class));
    }
    // For Create VM [END]

    @Override
    public List<F2CVirtualMachine> listVirtualMachine(String req) {
        return AliyunSyncCloudApi.listVirtualMachine(JsonUtil.parseObject(req, ListVirtualMachineRequest.class));
    }

    @Override
    public List<F2CImage> listImage(String req) {
        return AliyunSyncCloudApi.listImage(JsonUtil.parseObject(req, ListImageRequest.class));
    }

    @Override
    public List<F2CDisk> listDisk(String req) {
        return AliyunSyncCloudApi.listDisk(JsonUtil.parseObject(req, ListDisksRequest.class));
    }

    @Override
    public boolean powerOff(String req) {
        AliyunInstanceRequest request = JsonUtil.parseObject(req, AliyunInstanceRequest.class);
        request.setForce(true);
        return AliyunSyncCloudApi.powerOff(request);
    }

    @Override
    public boolean powerOn(String req) {
        return AliyunSyncCloudApi.powerOn(JsonUtil.parseObject(req, AliyunInstanceRequest.class));
    }

    @Override
    public boolean shutdownInstance(String req) {
        return AliyunSyncCloudApi.powerOff(JsonUtil.parseObject(req, AliyunInstanceRequest.class));
    }

    @Override
    public boolean rebootInstance(String req) {
        return AliyunSyncCloudApi.rebootInstance(JsonUtil.parseObject(req, AliyunInstanceRequest.class));
    }

    @Override
    public boolean deleteInstance(String req) {
        AliyunInstanceRequest request = JsonUtil.parseObject(req, AliyunInstanceRequest.class);
        request.setForce(true);
        return AliyunSyncCloudApi.deleteInstance(request);
    }

    @Override
    public boolean hardShutdownInstance(String req) {
        AliyunInstanceRequest request = JsonUtil.parseObject(req, AliyunInstanceRequest.class);
        request.setForce(true);
        return AliyunSyncCloudApi.powerOff(request);
    }

    @Override
    public boolean hardRebootInstance(String req) {
        AliyunInstanceRequest request = JsonUtil.parseObject(req, AliyunInstanceRequest.class);
        request.setForce(true);
        return AliyunSyncCloudApi.rebootInstance(request);
    }

    @Override
    public FormObject getCreateDiskForm() {
        return FormUtil.toForm(AliyunCreateDiskForm.class);
    }

    public List<Map<String, String>> getDiskTypesForCreateDisk(String req) {
        return AliyunSyncCloudApi.getDataDiskType(JsonUtil.parseObject(req, AliyunGetDiskTypeRequest.class)).stream().map((diskType) -> {
            Map<String, String> result = new HashMap<>();
            result.put("id", diskType.getDiskType());
            result.put("name", diskType.getDiskTypeName());
            return result;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, String>> getDeleteWithInstance(String req) {
        List<Map<String, String>> deleteWithInstance = new ArrayList<>();
        Map<String, String> yes = new HashMap<>();
        yes.put("id", DeleteWithInstance.YES.name());
        yes.put("name", "是");
        deleteWithInstance.add(yes);

        Map<String, String> no = new HashMap<>();
        no.put("id", DeleteWithInstance.NO.name());
        no.put("name", "否");
        deleteWithInstance.add(no);
        return deleteWithInstance;
    }

    @Override
    public List<F2CDisk> createDisks(String req) {
        return AliyunSyncCloudApi.createDisks(JsonUtil.parseObject(req, AliyunCreateDisksRequest.class));
    }

    @Override
    public F2CDisk createDisk(String req) {
        return AliyunSyncCloudApi.createDisk(JsonUtil.parseObject(req, AliyunCreateDiskRequest.class));
    }

    @Override
    public boolean enlargeDisk(String req) {
        return AliyunSyncCloudApi.enlargeDisk(JsonUtil.parseObject(req, AliyunResizeDiskRequest.class));
    }

    @Override
    public F2CDisk attachDisk(String req) {
        return AliyunSyncCloudApi.attachDisk(JsonUtil.parseObject(req, AliyunAttachDiskRequest.class));
    }

    @Override
    public boolean detachDisk(String req) {
        return AliyunSyncCloudApi.detachDisk(JsonUtil.parseObject(req, AliyunDetachDiskRequest.class));
    }

    @Override
    public boolean deleteDisk(String req) {
        return AliyunSyncCloudApi.deleteDisk(JsonUtil.parseObject(req, AliyunDeleteDiskRequest.class));
    }

    @Override
    public List<F2CPerfMetricMonitorData> getF2CPerfMetricMonitorData(String req) {
        return AliyunSyncCloudApi.getF2CPerfMetricList(JsonUtil.parseObject(req, GetMetricsRequest.class));
    }

    @Override
    public List<F2CPerfMetricMonitorData> getF2CDiskPerfMetricMonitorData(String req) {
        return new ArrayList<>();
    }

    @Override
    public F2CVirtualMachine changeVmConfig(String req) {
        return AliyunSyncCloudApi.changeVmConfig(JsonUtil.parseObject(req, AliyunUpdateConfigRequest.class));
    }

    @Override
    public FormObject getConfigUpdateForm() {
        return FormUtil.toForm(AliyunConfigUpdateForm.class);
    }

    public List<AliyunInstanceType> getInstanceTypesForConfigUpdate(String req) {
        return AliyunSyncCloudApi.getInstanceTypesForConfigUpdate(JsonUtil.parseObject(req, AliyunUpdateConfigRequest.class));
    }

    @Override
    public String calculateConfigUpdatePrice(String req) {
        return AliyunSyncCloudApi.calculateConfigUpdatePrice(JsonUtil.parseObject(req, AliyunUpdateConfigRequest.class));
    }

    @Override
    public List<F2CDisk> getVmF2CDisks(String req) {
        return AliyunSyncCloudApi.getVmF2CDisks(JsonUtil.parseObject(req, BaseDiskRequest.class));
    }

    @Override
    public F2CBalance getAccountBalance(String getAccountBalanceRequest) {
        return aliyunBaseCloudProvider.getAccountBalance(getAccountBalanceRequest);
    }

    @Override
    public CloudAccountMeta getCloudAccountMeta() {
        return aliyunBaseCloudProvider.getCloudAccountMeta();
    }

    @Override
    public Info getInfo() {
        return info;
    }
}
