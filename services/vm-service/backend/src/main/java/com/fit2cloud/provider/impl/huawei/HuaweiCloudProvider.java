package com.fit2cloud.provider.impl.huawei;

import com.fit2cloud.common.form.util.FormUtil;
import com.fit2cloud.common.form.vo.FormObject;
import com.fit2cloud.common.provider.entity.F2CBalance;
import com.fit2cloud.common.provider.entity.F2CPerfMetricMonitorData;
import com.fit2cloud.common.provider.impl.huawei.HuaweiBaseCloudProvider;
import com.fit2cloud.common.utils.JsonUtil;
import com.fit2cloud.provider.impl.huawei.api.HuaweiSyncCloudApi;
import com.fit2cloud.provider.impl.huawei.constants.HuaweiPeriodOption;
import com.fit2cloud.provider.impl.huawei.entity.*;
import com.fit2cloud.provider.impl.huawei.entity.credential.HuaweiVmCredential;
import com.fit2cloud.provider.impl.huawei.entity.request.*;
import com.fit2cloud.vm.AbstractCloudProvider;
import com.fit2cloud.vm.ICloudProvider;
import com.fit2cloud.vm.ICreateServerRequest;
import com.fit2cloud.vm.constants.ActionInfoConstants;
import com.fit2cloud.vm.constants.DeleteWithInstance;
import com.fit2cloud.vm.entity.F2CDisk;
import com.fit2cloud.vm.entity.F2CImage;
import com.fit2cloud.vm.entity.F2CVirtualMachine;
import com.fit2cloud.vm.entity.request.BaseDiskRequest;
import com.fit2cloud.vm.entity.request.GetMetricsRequest;
import com.huaweicloud.sdk.ecs.v2.model.NovaSimpleKeypair;
import org.apache.commons.lang3.StringUtils;
import org.pf4j.Extension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author:张少虎
 * @Date: 2022/9/20  11:06 AM
 * @Version 1.0
 * @注释:
 */
@Extension
public class HuaweiCloudProvider extends AbstractCloudProvider<HuaweiVmCredential> implements ICloudProvider {
    public static final HuaweiBaseCloudProvider huaweiBaseCloudProvider = new HuaweiBaseCloudProvider();

    private static final Info info = new Info("vm-service", ActionInfoConstants.all(
            ActionInfoConstants.SYNC_DATA_STORE_MACHINE_METRIC_MONITOR,
            ActionInfoConstants.SYNC_DATASTORE,
            ActionInfoConstants.SYNC_HOST,
            ActionInfoConstants.SYNC_HOST_MACHINE_METRIC_MONITOR), Map.of());

    @Override
    public Class<? extends ICreateServerRequest> getCreateServerRequestClass() {
        return HuaweiVmCreateRequest.class;
    }

    @Override
    public BigDecimal renewInstancePrice(String req) {
        return HuaweiSyncCloudApi.renewInstancePrice(JsonUtil.parseObject(req, HuaweiRenewInstanceRequest.class));
    }

    @Override
    public String renewInstanceExpiresTime(String req) {
        return HuaweiSyncCloudApi.renewInstanceExpiresTime(JsonUtil.parseObject(req, HuaweiRenewInstanceExpiresTimeRequest.class));
    }

    @Override
    public FormObject getRenewInstanceForm() {
        return FormUtil.toForm(HuaweiRenewInstanceRequest.class);
    }

    @Override
    public F2CVirtualMachine renewInstance(String req) {
        return HuaweiSyncCloudApi.renewInstance(JsonUtil.parseObject(req, HuaweiRenewInstanceRequest.class));
    }

    @Override
    public FormObject getCreateServerForm() {
        return FormUtil.toForm(HuaweiVmCreateRequest.class);
    }

    @Override
    public List<F2CVirtualMachine> listVirtualMachine(String req) {
        return HuaweiSyncCloudApi.listVirtualMachine(JsonUtil.parseObject(req, ListVirtualMachineRequest.class));
    }

    @Override
    public List<F2CImage> listImage(String req) {
        return HuaweiSyncCloudApi.lisImages(JsonUtil.parseObject(req, ListImageRequest.class));
    }

    @Override
    public List<F2CDisk> listDisk(String req) {
        return HuaweiSyncCloudApi.listDisk(JsonUtil.parseObject(req, ListDisksRequest.class));
    }

    @Override
    public boolean powerOff(String req) {
        HuaweiInstanceRequest request = JsonUtil.parseObject(req, HuaweiInstanceRequest.class);
        request.setForce(true);
        return HuaweiSyncCloudApi.powerOff(request);
    }

    @Override
    public boolean powerOn(String req) {
        return HuaweiSyncCloudApi.powerOn(JsonUtil.parseObject(req, HuaweiInstanceRequest.class));
    }

    @Override
    public boolean shutdownInstance(String req) {
        return HuaweiSyncCloudApi.powerOff(JsonUtil.parseObject(req, HuaweiInstanceRequest.class));
    }

    @Override
    public boolean rebootInstance(String req) {
        return HuaweiSyncCloudApi.rebootInstance(JsonUtil.parseObject(req, HuaweiInstanceRequest.class));
    }

    @Override
    public boolean deleteInstance(String req) {
        return HuaweiSyncCloudApi.deleteInstance(JsonUtil.parseObject(req, HuaweiInstanceRequest.class));
    }

    @Override
    public boolean hardShutdownInstance(String req) {
        HuaweiInstanceRequest request = JsonUtil.parseObject(req, HuaweiInstanceRequest.class);
        request.setForce(true);
        return HuaweiSyncCloudApi.powerOff(request);
    }

    @Override
    public boolean hardRebootInstance(String req) {
        HuaweiInstanceRequest request = JsonUtil.parseObject(req, HuaweiInstanceRequest.class);
        request.setForce(true);
        return HuaweiSyncCloudApi.rebootInstance(request);
    }

    @Override
    public FormObject getCreateDiskForm() {
        return FormUtil.toForm(HuaweiCreateDiskForm.class);
    }

    @Override
    public List<Map<String, String>> getDiskTypes(String req) {
        return HuaweiSyncCloudApi.getDiskTypes(JsonUtil.parseObject(req, HuaweiGetDiskTypeRequest.class));
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
        return HuaweiSyncCloudApi.createDisks(JsonUtil.parseObject(req, HuaweiCreateDisksRequest.class));
    }

    @Override
    public F2CDisk createDisk(String req) {
        return HuaweiSyncCloudApi.createDisk(JsonUtil.parseObject(req, HuaweiCreateDiskRequest.class));
    }

    @Override
    public boolean enlargeDisk(String req) {
        return HuaweiSyncCloudApi.enlargeDisk(JsonUtil.parseObject(req, HuaweiResizeDiskRequest.class));
    }

    @Override
    public F2CDisk attachDisk(String req) {
        return HuaweiSyncCloudApi.attachDisk(JsonUtil.parseObject(req, HuaweiAttachDiskRequest.class));
    }

    @Override
    public boolean detachDisk(String req) {
        return HuaweiSyncCloudApi.detachDisk(JsonUtil.parseObject(req, HuaweiDetachDiskRequest.class));
    }

    @Override
    public boolean deleteDisk(String req) {
        return HuaweiSyncCloudApi.deleteDisk(JsonUtil.parseObject(req, HuaweiDeleteDiskRequest.class));
    }

    @Override
    public List<F2CPerfMetricMonitorData> getF2CPerfMetricMonitorData(String req) {
        return HuaweiSyncCloudApi.getF2CPerfMetricList(JsonUtil.parseObject(req, GetMetricsRequest.class));
    }

    @Override
    public List<F2CPerfMetricMonitorData> getF2CDiskPerfMetricMonitorData(String req) {
        return new ArrayList<>();
    }

    public List<NovaAvailabilityZoneDTO> getAvailabilityZone(String req) {
        return HuaweiSyncCloudApi.getAvailabilityZone(JsonUtil.parseObject(req, HuaweiVmCreateRequest.class));
    }

    /**
     * 计费模式
     *
     * @return
     */
    public List<Map<String, String>> getBillingMode(String req) {
        List<Map<String, String>> billingModes = new ArrayList<>();
        Map<String, String> defaultMap = new HashMap<>();
        defaultMap.put("id", "postPaid");
        defaultMap.put("name", "按需计费");
        billingModes.add(defaultMap);
        Map<String, String> thinMap = new HashMap<>();
        thinMap.put("id", "prePaid");
        thinMap.put("name", "包年/包月");
        billingModes.add(thinMap);
        return billingModes;
    }

    /**
     * 公网带宽计费类型
     * 未传该字段,表示按带宽计费。
     * 字段值为空,表示按带宽计费。
     * 字段值为“traffic”,表示按流量计费。
     * 字段为其它值,会导致创建云服务器失败。
     * 特殊处理noTraffic
     *
     * @return
     */
    public List<Map<String, String>> getChargeMode(String req) {
        List<Map<String, String>> billingModes = new ArrayList<>();
        Map<String, String> thinMap = new HashMap<>();
        thinMap.put("id", "noTraffic");
        thinMap.put("name", "按固定带宽");
        billingModes.add(thinMap);
        Map<String, String> defaultMap = new HashMap<>();
        defaultMap.put("id", "traffic");
        defaultMap.put("name", "按流量计费");
        billingModes.add(defaultMap);
        return billingModes;
    }

    public List<Map<String, String>> getLoginMethod(String req) {
        List<Map<String, String>> loginMethod = new ArrayList<>();
        Map<String, String> defaultMap = new HashMap<>();
        defaultMap.put("id", "pwd");
        defaultMap.put("name", "自定义密码");
        loginMethod.add(defaultMap);
//        Map<String, String> thinMap = new HashMap<>();
//        thinMap.put("id", "keyPair");
//        thinMap.put("name", "密钥对");
//        loginMethod.add(thinMap);
        return loginMethod;
    }

    public List<InstanceSpecType> getInstanceSpecTypes(String req) {
        return HuaweiSyncCloudApi.getInstanceSpecTypes(JsonUtil.parseObject(req, HuaweiVmCreateRequest.class));
    }

    public List<Map<String, String>> getAllDiskTypes(String req) {
        return HuaweiSyncCloudApi.getAllDiskTypes(JsonUtil.parseObject(req, HuaweiVmCreateRequest.class));
    }

    @Override
    public String calculateConfigPrice(String req) {
        return HuaweiSyncCloudApi.calculatedPrice(false, JsonUtil.parseObject(req, HuaweiVmCreateRequest.class));
    }

    public String calculateBandwidthConfigPrice(String req) {
        return HuaweiSyncCloudApi.calculatedPrice(true, JsonUtil.parseObject(req, HuaweiVmCreateRequest.class));
    }

    public List<F2CHuaweiSubnet> listSubnet(String req) {
        return HuaweiSyncCloudApi.listSubnet(JsonUtil.parseObject(req, HuaweiVmCreateRequest.class));
    }

    public List<F2CHuaweiSecurityGroups> listSecurityGroups(String req) {
        return HuaweiSyncCloudApi.listSecurityGroups(JsonUtil.parseObject(req, HuaweiVmCreateRequest.class));
    }

    public List<NovaSimpleKeypair> listKeyPairs(String req) {
        return HuaweiSyncCloudApi.listKeyPairs(JsonUtil.parseObject(req, HuaweiVmCreateRequest.class));
    }

    @Override
    public F2CVirtualMachine getSimpleServerByCreateRequest(String req) {
        return HuaweiSyncCloudApi.getSimpleServerByCreateRequest(JsonUtil.parseObject(req, HuaweiVmCreateRequest.class));
    }

    @Override
    public F2CVirtualMachine createVirtualMachine(String req) {
        return HuaweiSyncCloudApi.createServer(JsonUtil.parseObject(req, HuaweiVmCreateRequest.class));
    }

    public List<OsConfig> listOsVersion(String req) {
        return HuaweiSyncCloudApi.listOsVersion(JsonUtil.parseObject(req, HuaweiVmCreateRequest.class));
    }

    public List<Map<String, String>> listOs(String req) {
        return HuaweiSyncCloudApi.listOs(req);
    }

    public String getLoginName(String req) {
        HuaweiVmCreateRequest request = JsonUtil.parseObject(req, HuaweiVmCreateRequest.class);
        if (StringUtils.equalsIgnoreCase(request.getOs(), "Windows")) {
            return "Administrator";
        }
        return "root";
    }

    @Override
    public F2CVirtualMachine changeVmConfig(String req) {
        return HuaweiSyncCloudApi.changeVmConfig(JsonUtil.parseObject(req, HuaweiUpdateConfigRequest.class));
    }

    @Override
    public FormObject getConfigUpdateForm() {
        return FormUtil.toForm(HuaweiConfigUpdateForm.class);
    }

    public List<InstanceSpecType> getInstanceTypesForConfigUpdate(String req) {
        return HuaweiSyncCloudApi.getInstanceTypesForConfigUpdate(JsonUtil.parseObject(req, HuaweiUpdateConfigRequest.class));
    }

    @Override
    public String calculateConfigUpdatePrice(String req) {
        return HuaweiSyncCloudApi.calculateConfigUpdatePrice(JsonUtil.parseObject(req, HuaweiUpdateConfigRequest.class));
    }

    /**
     * 获取付费周期
     *
     * @param req
     * @return
     */
    public List<Map<String, Object>> getPeriodOption(String req) {
        List<Map<String, Object>> periodList = new ArrayList<>();
        for (HuaweiPeriodOption option : HuaweiPeriodOption.values()) {
            Map<String, Object> map = new HashMap<>();
            map.put("period", option.getPeriod());
            map.put("periodDisplayName", option.getPeriodDisplayName());
            periodList.add(map);
        }
        return periodList;
    }

    @Override
    public List<F2CDisk> getVmF2CDisks(String req) {
        return HuaweiSyncCloudApi.getVmF2CDisks(JsonUtil.parseObject(req, BaseDiskRequest.class));
    }

    @Override
    public F2CBalance getAccountBalance(String getAccountBalanceRequest) {
        return huaweiBaseCloudProvider.getAccountBalance(getAccountBalanceRequest);
    }

    @Override
    public CloudAccountMeta getCloudAccountMeta() {
        return huaweiBaseCloudProvider.getCloudAccountMeta();
    }

    @Override
    public Info getInfo() {
        return info;
    }
}
