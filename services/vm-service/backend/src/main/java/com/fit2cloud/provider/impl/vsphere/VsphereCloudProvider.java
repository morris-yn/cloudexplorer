package com.fit2cloud.provider.impl.vsphere;

import com.fit2cloud.common.constants.ChargeTypeConstants;
import com.fit2cloud.common.form.util.FormUtil;
import com.fit2cloud.common.form.vo.FormObject;
import com.fit2cloud.common.platform.credential.impl.VsphereCredential;
import com.fit2cloud.common.provider.entity.F2CBalance;
import com.fit2cloud.common.provider.entity.F2CPerfMetricMonitorData;
import com.fit2cloud.common.provider.impl.vsphere.VsphereBaseCloudProvider;
import com.fit2cloud.common.utils.JsonUtil;
import com.fit2cloud.provider.impl.vsphere.api.VsphereSyncCloudApi;
import com.fit2cloud.provider.impl.vsphere.entity.*;
import com.fit2cloud.provider.impl.vsphere.entity.constants.VsphereDiskMode;
import com.fit2cloud.provider.impl.vsphere.entity.constants.VsphereDiskType;
import com.fit2cloud.provider.impl.vsphere.entity.constants.VspherePeriodOption;
import com.fit2cloud.provider.impl.vsphere.entity.request.*;
import com.fit2cloud.provider.impl.vsphere.util.DiskType;
import com.fit2cloud.vm.AbstractCloudProvider;
import com.fit2cloud.vm.ICloudProvider;
import com.fit2cloud.vm.ICreateServerRequest;
import com.fit2cloud.vm.constants.ActionInfoConstants;
import com.fit2cloud.vm.entity.*;
import com.fit2cloud.vm.entity.request.GetMetricsRequest;
import com.fit2cloud.vm.entity.result.CheckCreateServerResult;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
import org.pf4j.Extension;

import java.util.*;

/**
 * Author: LiuDi
 * Date: 2022/9/20 5:20 PM
 */
@Extension
public class VsphereCloudProvider extends AbstractCloudProvider<VsphereCredential> implements ICloudProvider {

    public static final VsphereBaseCloudProvider vsphereBaseCloudProvider = new VsphereBaseCloudProvider();

    private static final Info info = new Info("vm-service", ActionInfoConstants.all(), Map.of());

    @Override
    public Class<? extends ICreateServerRequest> getCreateServerRequestClass() {
        return VsphereVmCreateRequest.class;
    }

    @Override
    public FormObject getCreateServerForm() {
        return FormUtil.toForm(VsphereVmCreateRequest.class);
    }

    @Override
    public List<F2CVirtualMachine> listVirtualMachine(String req) {
        return VsphereSyncCloudApi.listVirtualMachine(JsonUtil.parseObject(req, VsphereVmBaseRequest.class));
    }

    @Override
    public List<F2CImage> listImage(String req) {
        return VsphereSyncCloudApi.listImage(JsonUtil.parseObject(req, VsphereVmBaseRequest.class));
    }

    @Override
    public List<F2CDisk> listDisk(String req) {
        return VsphereSyncCloudApi.listDisk(JsonUtil.parseObject(req, VsphereVmBaseRequest.class));
    }

    @Override
    public List<F2CHost> listHost(String req) {
        return VsphereSyncCloudApi.listHost(JsonUtil.parseObject(req, VsphereVmBaseRequest.class));
    }

    @Override
    public List<F2CDatastore> listDataStore(String req) {
        return VsphereSyncCloudApi.listDataStore(JsonUtil.parseObject(req, VsphereVmBaseRequest.class));
    }

    @Override
    public boolean powerOff(String req) {
        return VsphereSyncCloudApi.powerOff(JsonUtil.parseObject(req, VsphereVmPowerRequest.class));
    }

    @Override
    public boolean powerOn(String req) {
        return VsphereSyncCloudApi.powerOn(JsonUtil.parseObject(req, VsphereVmPowerRequest.class));
    }


    @Override
    public boolean shutdownInstance(String req) {
        return VsphereSyncCloudApi.shutdownInstance(JsonUtil.parseObject(req, VsphereVmPowerRequest.class));
    }

    @Override
    public boolean rebootInstance(String req) {
        return VsphereSyncCloudApi.reboot(JsonUtil.parseObject(req, VsphereVmPowerRequest.class));
    }

    @Override
    public boolean deleteInstance(String req) {
        return VsphereSyncCloudApi.deleteInstance(JsonUtil.parseObject(req, VsphereVmPowerRequest.class));
    }

    @Override
    public boolean hardShutdownInstance(String req) {
        return VsphereSyncCloudApi.powerOff(JsonUtil.parseObject(req, VsphereVmPowerRequest.class));
    }

    @Override
    public boolean hardRebootInstance(String req) {
        return VsphereSyncCloudApi.hardReboot(JsonUtil.parseObject(req, VsphereVmPowerRequest.class));
    }

    public List<F2CVsphereCluster> getClusters(String req) {
        VsphereVmCreateRequest request = JsonUtil.parseObject(req, VsphereVmCreateRequest.class);
        request.setRegionId(request.getRegion());
        return VsphereSyncCloudApi.getClusters(request);
    }

    public List<F2CVsphereNetwork> getNetworks(String req) {
        return VsphereSyncCloudApi.getNetworks(JsonUtil.parseObject(req, VsphereVmCreateRequest.class));
    }

    public List<Map<String, String>> getLocations(String req) {
        VsphereVmCreateRequest request = JsonUtil.parseObject(req, VsphereVmCreateRequest.class);
        return VsphereSyncCloudApi.getLocations(request);
    }

    public List<VsphereHost> getHosts(String req) {
        VsphereVmCreateRequest request = JsonUtil.parseObject(req, VsphereVmCreateRequest.class);
        return VsphereSyncCloudApi.getHosts(request);
    }

    public List<VsphereResourcePool> geResourcePools(String req) {
        VsphereVmCreateRequest request = JsonUtil.parseObject(req, VsphereVmCreateRequest.class);
        return VsphereSyncCloudApi.geResourcePools(request);
    }

    public List<VsphereFolder> getFolders(String req) {
        VsphereVmCreateRequest request = JsonUtil.parseObject(req, VsphereVmCreateRequest.class);
        return VsphereSyncCloudApi.getFolders(request);
    }

    @Override
    public List<Map<String, String>> getDiskTypes(String req) {
        List<Map<String, String>> diskTypes = new ArrayList<>();
        Map<String, String> defaultMap = new HashMap<>();
        defaultMap.put("info", "与源格式相同");
        defaultMap.put("value", DiskType.DEFAULT);
        diskTypes.add(defaultMap);
        Map<String, String> thinMap = new HashMap<>();
        thinMap.put("info", "精简置备");
        thinMap.put("value", DiskType.THIN);
        diskTypes.add(thinMap);
        Map<String, String> Map = new HashMap<>();
        Map.put("info", "厚置备置零");
        Map.put("value", DiskType.EAGER_ZEROED);
        diskTypes.add(Map);
        return diskTypes;
    }

    @Override
    public boolean enlargeDisk(String req) {
        return VsphereSyncCloudApi.enlargeDisk(JsonUtil.parseObject(req, VsphereResizeDiskRequest.class));
    }


    @Override
    public FormObject getCreateDiskForm() {
        return FormUtil.toForm(VsphereCreateDiskForm.class);
    }

    /**
     * 获取磁盘类型 （单独创建磁盘时使用）
     *
     * @param req
     * @return
     */
    public List<Map<String, String>> getDiskTypesForCreateDisk(String req) {
        List<Map<String, String>> diskTypes = new ArrayList<>();

        Map<String, String> typeOne = new HashMap<>();
        typeOne.put("id", VsphereDiskType.THIN.getId());
        typeOne.put("name", VsphereDiskType.getName(VsphereDiskType.THIN.getId()));
        diskTypes.add(typeOne);

        Map<String, String> typeTwo = new HashMap<>();
        typeTwo.put("id", VsphereDiskType.THICK_EAGER_ZEROED.getId());
        typeTwo.put("name", VsphereDiskType.getName(VsphereDiskType.THICK_EAGER_ZEROED.getId()));
        diskTypes.add(typeTwo);

        Map<String, String> typeThree = new HashMap<>();
        typeThree.put("id", VsphereDiskType.THICK_LAZY_ZEROED.getId());
        typeThree.put("name", VsphereDiskType.getName(VsphereDiskType.THICK_LAZY_ZEROED.getId()));
        diskTypes.add(typeThree);
        return diskTypes;
    }

    /**
     * 获取磁盘模式 （单独创建磁盘时使用）
     *
     * @param req
     * @return
     */
    public List<Map<String, String>> getDiskModes(String req) {
        List<Map<String, String>> result = new ArrayList<>();

        Map<String, String> map = new HashMap<>();
        map.put("id", VsphereDiskMode.independent_persistent.getId());
        map.put("name", VsphereDiskMode.getName(VsphereDiskMode.independent_persistent.getId()));
        result.add(map);
        map = new HashMap<>();
        map.put("id", VsphereDiskMode.independent_nonpersistent.getId());
        map.put("name", VsphereDiskMode.getName(VsphereDiskMode.independent_nonpersistent.getId()));
        result.add(map);
        map = new HashMap<>();
        map.put("id", VsphereDiskMode.persistent.getId());
        map.put("name", VsphereDiskMode.getName(VsphereDiskMode.persistent.getId()));
        result.add(map);

        return result;
    }

    /**
     * 获取存储类型 （单独创建磁盘时使用）
     *
     * @param req
     * @return
     */
    public List<Map<String, String>> getDatastoreTypes(String req) {
        List<Map<String, String>> result = new ArrayList<>();
        Map<String, String> map = new HashMap<>();
        map.put("id", "only-a-flag");
        map.put("name", "与云主机同目录");
        result.add(map);
        map = new HashMap<>();
        map.put("id", "customize");
        map.put("name", "自定义");
        result.add(map);
        return result;
    }

    public List<VsphereDatastore> getDatastoreListByVm(String req) {
        return VsphereSyncCloudApi.getDatastoreListByVm(JsonUtil.parseObject(req, VsphereDiskRequest.class));
    }

    public List<VsphereDatastore> getDatastoreList(String req) {
        VsphereVmCreateRequest request = JsonUtil.parseObject(req, VsphereVmCreateRequest.class);
        return VsphereSyncCloudApi.getDatastoreList(request);
    }

    @Override
    public List<F2CDisk> createDisks(String req) {
        return VsphereSyncCloudApi.createDisks(JsonUtil.parseObject(req, VsphereCreateDisksRequest.class));
    }

    @Override
    public F2CDisk createDisk(String req) {
        return VsphereSyncCloudApi.createDisk(JsonUtil.parseObject(req, VsphereCreateDiskRequest.class));
    }

    @Override
    public CheckCreateServerResult validateServerCreateRequest(String req) {
        return VsphereSyncCloudApi.validateServerCreateRequest(JsonUtil.parseObject(req, VsphereVmCreateRequest.class));
    }

    @Override
    public F2CVirtualMachine getSimpleServerByCreateRequest(String req) {
        return VsphereSyncCloudApi.getSimpleServerByCreateRequest(JsonUtil.parseObject(req, VsphereVmCreateRequest.class));
    }

    @Override
    public F2CVirtualMachine createVirtualMachine(String req) {
        return VsphereSyncCloudApi.createServer(JsonUtil.parseObject(req, VsphereVmCreateRequest.class));
    }

    @Override
    public List<F2CPerfMetricMonitorData> getF2CPerfMetricMonitorData(String req) {
        return VsphereSyncCloudApi.getF2CPerfMetricList(JsonUtil.parseObject(req, GetMetricsRequest.class));
    }

    @Override
    public List<F2CPerfMetricMonitorData> getF2CHostPerfMetricMonitorData(String req) {
        return VsphereSyncCloudApi.getF2CHostPerfMetricList(JsonUtil.parseObject(req, GetMetricsRequest.class));
    }

    @Override
    public List<F2CPerfMetricMonitorData> getF2CDiskPerfMetricMonitorData(String req) {
        return new ArrayList<>();
    }

    @Override
    public List<F2CPerfMetricMonitorData> getF2CDatastorePerfMetricMonitorData(String req) {
        return VsphereSyncCloudApi.getF2CDatastorePerfMetricList(JsonUtil.parseObject(req, GetMetricsRequest.class));
    }


    @Override
    public F2CVirtualMachine changeVmConfig(String req) {
        return VsphereSyncCloudApi.changeVmConfig(JsonUtil.parseObject(req, VsphereUpdateConfigRequest.class));
    }

    @Override
    public FormObject getConfigUpdateForm() {
        return FormUtil.toForm(VsphereConfigUpdateForm.class);
    }

    @Override
    public List<F2CDisk> getVmF2CDisks(String req) {
        return VsphereSyncCloudApi.getVmF2CDisks(JsonUtil.parseObject(req, VsphereDiskRequest.class));
    }

    @Override
    public boolean supportResetPassword() {
        return true;
    }

    @Override
    public boolean resetPassword(String req) {
        return VsphereSyncCloudApi.resetPassword(JsonUtil.parseObject(req, VsphereVmResetPasswordRequest.class));
    }

    @Override
    public String calculateConfigPrice(String req) {
        VsphereCalculateConfigPriceRequest vsphereCalculateConfigPriceRequest =
                JsonUtil.parseObject(req, VsphereCalculateConfigPriceRequest.class);
        return VsphereSyncCloudApi.calculateConfigPrice(vsphereCalculateConfigPriceRequest);
    }

    public List<Map<String, Object>> getPeriodOption(String req) {
        List<Map<String, Object>> periodList = new ArrayList<>();
        for (VspherePeriodOption option : VspherePeriodOption.values()) {
            Map<String, Object> map = new HashMap<>();
            map.put("period", option.getPeriod());
            map.put("periodDisplayName", option.getPeriodDisplayName());
            periodList.add(map);
        }
        return periodList;
    }

    @Override
    public String calculateConfigUpdatePrice(String req) {
        CalculateConfigUpdatePriceRequest calculateConfigUpdatePriceRequest = JsonUtil.parseObject(req, CalculateConfigUpdatePriceRequest.class);
        VsphereCalculateConfigPriceRequest vsphereCalculateConfigPriceRequest = new VsphereCalculateConfigPriceRequest();
        vsphereCalculateConfigPriceRequest.setRam(calculateConfigUpdatePriceRequest.getMemory());
        vsphereCalculateConfigPriceRequest.setCpu(calculateConfigUpdatePriceRequest.getCpu());
        vsphereCalculateConfigPriceRequest.setCount(1);
        vsphereCalculateConfigPriceRequest.setPeriodNum("1");
        vsphereCalculateConfigPriceRequest.setAccountId(calculateConfigUpdatePriceRequest.getCloudAccountId());
        vsphereCalculateConfigPriceRequest.setDisks(List.of());
        vsphereCalculateConfigPriceRequest.setInstanceChargeType(calculateConfigUpdatePriceRequest.getInstanceChargeType());
        return VsphereSyncCloudApi.calculateConfigPrice(vsphereCalculateConfigPriceRequest);
    }

    @Override
    public F2CBalance getAccountBalance(String getAccountBalanceRequest) {
        return vsphereBaseCloudProvider.getAccountBalance(getAccountBalanceRequest);
    }

    @Override
    public CloudAccountMeta getCloudAccountMeta() {
        return vsphereBaseCloudProvider.getCloudAccountMeta();
    }

    @Override
    public Info getInfo() {
        return info;
    }

    public List<DefaultKeyValue<String, String>> getChargeType(String req) {
        return Arrays.stream(ChargeTypeConstants.values())
                .map(model -> new DefaultKeyValue<>(model.getMessage(), model.getCode())).toList();
    }
}
