package com.fit2cloud.provider.impl.openstack.api;

import com.fit2cloud.base.entity.BillPolicyDetails;
import com.fit2cloud.common.constants.ChargeTypeConstants;
import com.fit2cloud.common.exception.Fit2cloudException;
import com.fit2cloud.common.platform.credential.impl.OpenStackCredential;
import com.fit2cloud.common.provider.entity.F2CEntityType;
import com.fit2cloud.common.provider.entity.F2CPerfMetricMonitorData;
import com.fit2cloud.common.provider.impl.openstack.entity.request.OpenStackBaseRequest;
import com.fit2cloud.common.provider.util.CommonUtil;
import com.fit2cloud.common.utils.DateUtil;
import com.fit2cloud.common.utils.JsonUtil;
import com.fit2cloud.common.utils.ServiceUtil;
import com.fit2cloud.common.utils.SpringUtil;
import com.fit2cloud.controller.handler.ResultHolder;
import com.fit2cloud.provider.impl.openstack.entity.CheckStatusResult;
import com.fit2cloud.provider.impl.openstack.entity.OpenStackFlavor;
import com.fit2cloud.provider.impl.openstack.entity.VolumeType;
import com.fit2cloud.provider.impl.openstack.entity.request.*;
import com.fit2cloud.provider.impl.openstack.util.OpenStackPerfMetricConstants;
import com.fit2cloud.provider.impl.openstack.util.OpenStackUtils;
import com.fit2cloud.common.utils.ChargingUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.types.ServiceType;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.*;
import org.openstack4j.model.compute.builder.BlockDeviceMappingBuilder;
import org.openstack4j.model.compute.builder.ServerCreateBuilder;
import org.openstack4j.model.compute.ext.Hypervisor;
import org.openstack4j.model.image.v2.Image;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.model.network.State;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.builder.VolumeBuilder;
import org.openstack4j.model.telemetry.Resource;
import org.openstack4j.model.telemetry.SampleCriteria;
import org.openstack4j.model.telemetry.Statistics;
import org.openstack4j.openstack.storage.block.domain.VolumeBackendPool;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import com.fit2cloud.vm.constants.F2CInstanceStatus;
import com.fit2cloud.vm.entity.*;
import com.fit2cloud.vm.entity.request.BaseDiskRequest;
import com.fit2cloud.vm.entity.request.GetMetricsRequest;
import com.fit2cloud.vm.entity.result.CheckCreateServerResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class OpenStackCloudApi {


    public static List<F2CVirtualMachine> listVirtualMachine(OpenStackBaseRequest request) {
        List<F2CVirtualMachine> list = new ArrayList<>();
        try {
            OSClient.OSClientV3 osClient = request.getOSClient();

            List<String> regions = OpenStackUtils.getRegionList(osClient);

            for (String region : regions) {
                if (request.getRegionId() != null && !StringUtils.equals(request.getRegionId(), region)) {
                    continue;
                }
                osClient.useRegion(region);
                List<? extends Server> instances = osClient.compute().servers().list(true);
                Map<String, Image> imageMap = osClient.imagesV2().list().stream().collect(Collectors.toMap(Image::getId, image -> image));
                for (Server instance : instances) {
                    list.add(OpenStackUtils.toF2CVirtualMachine(osClient, instance, region, imageMap));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return list;
    }

    public static List<F2CImage> listImage(OpenStackBaseRequest request) {
        List<F2CImage> list = new ArrayList<>();

        try {
            OSClient.OSClientV3 osClient = request.getOSClient();

            List<String> regions = OpenStackUtils.getRegionList(osClient);
            for (String region : regions) {
                if (request.getRegionId() != null && !StringUtils.equals(request.getRegionId(), region)) {
                    continue;
                }
                osClient.useRegion(region);
                for (Image image : osClient.imagesV2().list()) {
                    list.add(OpenStackUtils.toF2CImage(image, region));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return list;
    }

    public static List<F2CDisk> listDisk(OpenStackBaseRequest request) {
        List<F2CDisk> list = new ArrayList<>();

        try {
            OSClient.OSClientV3 osClient = request.getOSClient();

            List<String> regions = OpenStackUtils.getRegionList(osClient);
            for (String region : regions) {
                if (request.getRegionId() != null && !StringUtils.equals(request.getRegionId(), region)) {
                    continue;
                }
                osClient.useRegion(region);
                for (Volume volume : osClient.blockStorage().volumes().list()) {
                    list.add(OpenStackUtils.toF2CDisk(volume, region));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return list;

    }

    public static boolean powerOff(OpenStackInstanceActionRequest request) {
        try {
            OSClient.OSClientV3 osClient = request.getOSClient();
            osClient.useRegion(request.getRegionId());

            Server server = osClient.compute().servers().get(request.getUuid());
            if (server == null) {
                throw new RuntimeException("server not exist");
            }
            if (F2CInstanceStatus.Stopped.name().equalsIgnoreCase(OpenStackUtils.getStatus(server.getStatus()).name())) {
                return true;
            }

            ActionResponse response = osClient.compute().servers().action(request.getUuid(), Action.STOP);
            if (!response.isSuccess()) {
                throw new RuntimeException(response.getFault());
            }
            CheckStatusResult result = OpenStackUtils.checkServerStatus(osClient, server, Server.Status.SHUTOFF);
            if (result.isSuccess()) {
                return true;
            } else {
                throw new RuntimeException(result.getFault());
            }

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static boolean powerOn(OpenStackInstanceActionRequest request) {
        try {
            OSClient.OSClientV3 osClient = request.getOSClient();
            osClient.useRegion(request.getRegionId());

            Server server = osClient.compute().servers().get(request.getUuid());
            if (server == null) {
                throw new RuntimeException("server not exist");
            }
            if (F2CInstanceStatus.Running.name().equalsIgnoreCase(OpenStackUtils.getStatus(server.getStatus()).name())) {
                return true;
            }
            ActionResponse response = osClient.compute().servers().action(request.getUuid(), Action.START);
            if (!response.isSuccess()) {
                throw new RuntimeException(response.getFault());
            }
            CheckStatusResult result = OpenStackUtils.checkServerStatus(osClient, server, Server.Status.ACTIVE);
            if (result.isSuccess()) {
                return true;
            } else {
                throw new RuntimeException(result.getFault());
            }

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static boolean rebootInstance(OpenStackInstanceActionRequest request) {
        try {
            OSClient.OSClientV3 osClient = request.getOSClient();
            osClient.useRegion(request.getRegionId());

            Server server = osClient.compute().servers().get(request.getUuid());
            if (server == null) {
                throw new RuntimeException("server not exist");
            }

            ActionResponse response;
            if (!request.getForce()) {
                response = osClient.compute().servers().reboot(request.getUuid(), RebootType.SOFT);
            } else {
                response = osClient.compute().servers().reboot(request.getUuid(), RebootType.HARD);
            }
            if (!response.isSuccess()) {
                throw new RuntimeException(response.getFault());
            }
            CheckStatusResult result = OpenStackUtils.checkServerStatus(osClient, server, Server.Status.ACTIVE);
            if (result.isSuccess()) {
                return true;
            } else {
                throw new RuntimeException(result.getFault());
            }

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static boolean deleteInstance(OpenStackInstanceActionRequest request) {
        if (StringUtils.isBlank(request.getUuid())) {
            return true;
        }
        try {
            OSClient.OSClientV3 osClient = request.getOSClient();
            osClient.useRegion(request.getRegionId());

            Server server = osClient.compute().servers().get(request.getUuid());
            if (server == null) {
                return true;
            }
            ActionResponse response;
            if (!request.getForce()) {
                response = osClient.compute().servers().delete(request.getUuid());
            } else {
                //force DElETE
                response = osClient.compute().servers().action(request.getUuid(), Action.FORCEDELETE);
            }
            if (response.isSuccess()) {
                return true;
            } else {
                throw new RuntimeException(response.getFault());
            }

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static F2CDisk attachDisk(OpenStackDiskActionRequest request) {
        try {
            OSClient.OSClientV3 osClient = request.getOSClient();
            osClient.useRegion(request.getRegionId());

            Volume volume = osClient.blockStorage().volumes().get(request.getDiskId());
            if (volume == null) {
                throw new RuntimeException("volume not exist");
            }
            Server server = osClient.compute().servers().get(request.getInstanceUuid());
            if (server == null) {
                throw new RuntimeException("server not exist");
            }

            CheckStatusResult result = doAttachVolume(osClient, volume, server.getId(), request.getDevice());
            if (result.isSuccess()) {
                return OpenStackUtils.toF2CDisk((Volume) result.getObject(), request.getRegionId());
            } else {
                throw new RuntimeException(result.getFault());
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static CheckStatusResult doAttachVolume(OSClient.OSClientV3 osClient, Volume volume, String serverId, String device) {
        osClient.compute().servers().attachVolume(serverId, volume.getId(), device);
        return OpenStackUtils.checkDiskStatus(osClient, volume, Volume.Status.IN_USE);
    }

    public static boolean detachDisk(OpenStackDiskActionRequest request) {
        try {
            OSClient.OSClientV3 osClient = request.getOSClient();
            osClient.useRegion(request.getRegionId());

            Volume volume = osClient.blockStorage().volumes().get(request.getDiskId());
            if (volume == null) {
                throw new RuntimeException("volume not exist");
            }
            CheckStatusResult result = doDetachVolume(osClient, volume);
            if (result.isSuccess()) {
                return true;
            } else {
                throw new RuntimeException(result.getFault());
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static CheckStatusResult doDetachVolume(OSClient.OSClientV3 osClient, Volume volume) {
        ActionResponse response = osClient.compute().servers().detachVolume(volume.getAttachments().get(0).getServerId(), volume.getAttachments().get(0).getId());
        if (!response.isSuccess()) {
            throw new RuntimeException(response.getFault());
        }
        return OpenStackUtils.checkDiskStatus(osClient, volume, Volume.Status.AVAILABLE);
    }

    private static CheckStatusResult resetVolumeState(OSClient.OSClientV3 osClient, Volume volume, Volume.Status status) {
        ActionResponse response = osClient.blockStorage().volumes().resetState(volume.getId(), status);
        if (!response.isSuccess()) {
            throw new RuntimeException(response.getFault());
        }
        return OpenStackUtils.checkDiskStatus(osClient, volume, status);
    }


    public static boolean deleteDisk(OpenStackDiskActionRequest request) {
        if (StringUtils.isBlank(request.getDiskId())) {
            return true;
        }
        try {
            OSClient.OSClientV3 osClient = request.getOSClient();
            osClient.useRegion(request.getRegionId());

            Volume volume = osClient.blockStorage().volumes().get(request.getDiskId());
            if (volume == null) {
                return true;
            }

            ActionResponse response;
            if (!request.isForce()) {
                response = osClient.blockStorage().volumes().delete(request.getDiskId());
            } else {
                //force
                response = osClient.blockStorage().volumes().forceDelete(request.getDiskId());
            }
            if (response.isSuccess()) {
                return true;
            } else {
                throw new RuntimeException(response.getFault());
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static boolean enlargeDisk(OpenStackDiskEnlargeRequest request) {
        String serverId = null;
        boolean needAttach = false;
        OSClient.OSClientV3 osClient = null;
        Volume volume = null;
        String device = null;
        try {
            osClient = request.getOSClient();
            osClient.useRegion(request.getRegionId());

            volume = osClient.blockStorage().volumes().get(request.getDiskId());
            if (volume == null) {
                throw new RuntimeException("volume not exist");
            }
            CheckStatusResult result;
            //只有api >= 3.42 时，in_use的盘可以直接扩，这里只能先卸载再挂载了
            if (volume.getStatus().equals(Volume.Status.IN_USE)) {
                //serverId = volume.getAttachments().get(0).getServerId();
                //device = volume.getAttachments().get(0).getDevice();
                //卸载
                //result = doDetachVolume(osClient, volume);
                //不卸载，直接改状态
                result = resetVolumeState(osClient, volume, Volume.Status.AVAILABLE);
                if (result.isSuccess()) {
                    needAttach = true;
                } else {
                    throw new RuntimeException(result.getFault());
                }
            }

            //扩容
            ActionResponse response = osClient.blockStorage().volumes().extend(request.getDiskId(), request.getNewDiskSize());
            if (!response.isSuccess()) {
                throw new RuntimeException(response.getFault());
            }
            //可能扩容完直接就变成使用中了
            result = OpenStackUtils.checkDiskStatus(osClient, volume, Volume.Status.AVAILABLE, Volume.Status.IN_USE);
            if (!result.isSuccess()) {
                throw new RuntimeException(result.getFault());
            }

            volume = (Volume) result.getObject();

            if (needAttach) {
                needAttach = false; //防止catch重复挂载
                if (Volume.Status.AVAILABLE.equals(volume.getStatus())) {
                    //挂载
                    //result = doAttachVolume(osClient, volume, serverId, device);
                    result = resetVolumeState(osClient, volume, Volume.Status.IN_USE);
                    if (!result.isSuccess()) {
                        throw new RuntimeException(result.getFault());
                    }
                }
            }
            return true;

        } catch (Exception e) {
            if (needAttach) {
                try {
                    //doAttachVolume(osClient, volume, serverId, device);
                    resetVolumeState(osClient, volume, Volume.Status.IN_USE);
                } catch (Exception e1) {
                    log.error(e1.getMessage(), e1);
                }
            }
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static F2CDisk createDisk(OpenStackDiskCreateRequest request) {
        try {
            OSClient.OSClientV3 osClient = request.getOSClient();
            osClient.useRegion(request.getRegionId());

            VolumeBuilder builder = Builders.volume()
                    .name(request.getDiskName())
                    .description(request.getDescription())
                    .size(request.getSize());
            //.zone(request.getZone());
            if (StringUtils.isNotBlank(request.getDiskType())) {
                builder.volumeType(request.getDiskType());
            }
            Volume volume = osClient.blockStorage().volumes().create(builder.build());
            CheckStatusResult result = OpenStackUtils.checkDiskStatus(osClient, volume, Volume.Status.AVAILABLE);
            if (!result.isSuccess()) {
                throw new RuntimeException(result.getFault());
            }

            //创建出来后挂载
            if (StringUtils.isNotBlank(request.getInstanceUuid())) {
                Server server = osClient.compute().servers().get(request.getInstanceUuid());
                if (server == null) {
                    throw new RuntimeException("server not exist");
                }
                osClient.compute().servers().attachVolume(request.getInstanceUuid(), volume.getId(), null);
                result = OpenStackUtils.checkDiskStatus(osClient, volume, Volume.Status.IN_USE);
                if (!result.isSuccess()) {
                    throw new RuntimeException(result.getFault());
                }
            }

            return OpenStackUtils.toF2CDisk((Volume) result.getObject(), request.getRegionId());

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    public static List<VolumeType> listVolumeType(OpenStackBaseRequest request) {
        List<VolumeType> list = new ArrayList<>();
        try {
            OSClient.OSClientV3 osClient = request.getOSClient();
            osClient.useRegion(request.getRegionId());

            for (org.openstack4j.model.storage.block.VolumeType type : osClient.blockStorage().volumes().listVolumeTypes()) {
                list.add(new VolumeType().setId(type.getId()).setName(type.getName()));
            }

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return list;
    }

    public static List<Flavor> getFlavors(OpenStackServerCreateRequest request) {
        List<Flavor> list = new ArrayList<>();
        try {
            OSClient.OSClientV3 osClient = request.getOSClient();
            osClient.useRegion(request.getRegionId());

            Image image = osClient.imagesV2().get(request.getImageId());

            for (Flavor flavor : osClient.compute().flavors().list()) {
                if (flavor.isDisabled()) {
                    continue;
                }
                //排除内存小于1G的
                if (flavor.getRam() < 1024) {
                    continue;
                }
                //根据镜像过滤
                if (image != null) {
                    if (image.getMinDisk() > flavor.getDisk()) {
                        continue;
                    }
                    if (image.getMinRam() > flavor.getRam()) {
                        continue;
                    }
                }
                list.add(flavor);
            }

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return list.stream().sorted(Comparator.comparingInt(Flavor::getVcpus).thenComparingInt(Flavor::getRam).thenComparingInt(Flavor::getDisk)).collect(Collectors.toList());
    }

    public static List<SecurityGroup> getSecurityGroups(OpenStackServerCreateRequest request) {
        List<SecurityGroup> list = new ArrayList<>();
        try {
            OSClient.OSClientV3 osClient = request.getOSClient();
            osClient.useRegion(request.getRegionId());

            Map<String, String> filteringParams = new HashMap<>();
            //管理员账权限号可以拿到所有安全组？暂时无法区分是否为共享的，所以只拿这个project下的
            filteringParams.put("project_id", request.getOpenStackCredential().getProject());

            for (SecurityGroup securityGroup : osClient.networking().securitygroup().list(filteringParams)) {
                //排除其他project下非共享的
                list.add(securityGroup);
            }


        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return list;
    }

    public static List<Network> getNetworks(OpenStackServerCreateRequest request) {
        List<Network> list = new ArrayList<>();
        try {
            OSClient.OSClientV3 osClient = request.getOSClient();
            osClient.useRegion(request.getRegionId());

            Map<String, String> filteringParams = new HashMap<>();
            //filteringParams.put("project_id", request.getOpenStackCredential().getProject());
            //filteringParams.put("shared", "true");

            for (Network network : osClient.networking().network().list(filteringParams)) {
                if (State.ACTIVE.equals(network.getStatus())) {
                    list.add(network);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return list;
    }

    public static F2CVirtualMachine getSimpleServerByCreateRequest(OpenStackServerCreateRequest request) {
        F2CVirtualMachine virtualMachine = new F2CVirtualMachine();

        int index = request.getIndex();

        try {
            OSClient.OSClientV3 osClient = request.getOSClient();
            osClient.useRegion(request.getRegionId());

            Flavor flavor = osClient.compute().flavors().get(request.getFlavorId());

            String instanceType = flavor.getVcpus() + "vCpu " + flavor.getRam() / 1024 + "GB";

            virtualMachine
                    .setId(request.getId())
                    .setName(request.getServerInfos().get(index).getName())
                    .setCpu(flavor.getVcpus())
                    .setMemory(flavor.getRam() / 1024)
                    .setIpArray(new ArrayList<>())
                    .setInstanceType(instanceType)
                    .setRegion(request.getRegion())
                    .setInstanceTypeDescription(instanceType)
                    .setInstanceChargeType(request.getInstanceChargeType());

            if (StringUtils.equals(ChargeTypeConstants.PREPAID.getCode(), request.getInstanceChargeType())) {
                virtualMachine.setExpiredTime(CommonUtil.getExpiredTimeEpochMilli(request.getPeriodNum()));
            }

            if (!request.isBootFormVolume()) {
                virtualMachine.setDisk(flavor.getDisk());
            }

            return virtualMachine;

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static CheckCreateServerResult validateServerCreateRequest(OpenStackServerCreateRequest request) {
        //todo

        return CheckCreateServerResult.success();
    }


    public static F2CVirtualMachine createVirtualMachine(OpenStackServerCreateRequest request) {
        int index = request.getIndex();
        String serverName = request.getServerInfos().get(index).getName();

        try {
            OSClient.OSClientV3 osClient = request.getOSClient();
            osClient.useRegion(request.getRegionId());

            ServerCreateBuilder builder = Builders.server();
            builder.addAdminPass(request.getPassword());
            builder.userData(OpenStackUtils.getCloudInitUserData(request.getPassword()));

            builder.name(serverName);
            builder.availabilityZone(request.getZone());

            builder.configDrive(true);
            builder.flavor(request.getFlavorId());

            builder.networks(request.getNetworks());
            //安全组
            for (String securityGroupId : request.getSecurityGroups()) {
                SecurityGroup sg = osClient.networking().securitygroup().get(securityGroupId);
                if (sg != null) {
                    builder.addSecurityGroup(sg.getName());
                }
            }

            if (!request.isBootFormVolume()) {
                builder.image(request.getImageId());
            } else {
                AtomicInteger atomicInteger = new AtomicInteger(0);
                for (OpenStackServerCreateRequest.DiskConfig disk : request.getDisks()) {
                    //没有volumeType, 所以要自己先建盘出来
                    BlockDeviceMappingBuilder blockDeviceMappingBuilder = Builders.blockDeviceMapping();
                    //blockDeviceMappingBuilder.deviceName(serverName + "-volume-" + atomicInteger.getAndIncrement());
                    //blockDeviceMappingBuilder.volumeSize(disk.getSize());
                    /*if (disk.isBoot()) {
                        blockDeviceMappingBuilder.uuid(request.getImageId())
                                .sourceType(BDMSourceType.IMAGE)
                                .bootIndex(0)
                                .deleteOnTermination(disk.isDeleteWithInstance());
                    }*/
                    VolumeBuilder volumeBuilder = Builders.volume()
                            .name(serverName + "-volume-" + atomicInteger.getAndIncrement())
                            .description("create with server")
                            .size(disk.getSize());
                    if (StringUtils.isNotBlank(disk.getVolumeType())) {
                        volumeBuilder.volumeType(disk.getVolumeType());
                    }
                    if (disk.isBoot()) {
                        volumeBuilder.bootable(true)
                                .imageRef(request.getImageId());
                    }
                    Volume volume = osClient.blockStorage().volumes().create(volumeBuilder.build());
                    CheckStatusResult result = OpenStackUtils.checkDiskStatus(osClient, volume, Volume.Status.AVAILABLE);
                    if (!result.isSuccess()) {
                        throw new RuntimeException(result.getFault());
                    }

                    blockDeviceMappingBuilder.uuid(volume.getId()).deleteOnTermination(disk.isDeleteWithInstance());
                    if (disk.isBoot()) {
                        blockDeviceMappingBuilder.bootIndex(0);
                    }

                    builder.blockDevice(blockDeviceMappingBuilder.build());

                }
            }

            Server server = osClient.compute().servers().boot(builder.build());

            if (server == null) {
                throw new RuntimeException("Launch openStack server error, boot result is null");
            }
            CheckStatusResult result = OpenStackUtils.checkServerStatus(osClient, server, Server.Status.ACTIVE);
            return OpenStackUtils.toF2CVirtualMachine(osClient, (Server) result.getObject(), request.getRegionId(), null)
                    .setId(request.getId()).setInstanceChargeType(request.getInstanceChargeType());

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static List<F2CHost> listHost(OpenStackBaseRequest request) {
        List<F2CHost> list = new ArrayList<>();
        try {
            OSClient.OSClientV3 osClient = request.getOSClient();
            List<String> regions = OpenStackUtils.getRegionList(osClient);
            if (OpenStackUtils.isAdmin(osClient)) {
                regions.forEach(region -> {
                    if (request.getRegionId() != null && !StringUtils.equals(request.getRegionId(), region)) {
                        return;
                    }
                    osClient.useRegion(region);
                    List<? extends HostAggregate> hostAggregates = osClient.compute().hostAggregates().list();
                    List<? extends Hypervisor> hypervisors = osClient.compute().hypervisors().list();
                    for (Hypervisor hypervisor : hypervisors) {
                        if (!hypervisor.getType().equalsIgnoreCase("ironic")) {
                            list.add(OpenStackUtils.toF2CHost(hostAggregates, hypervisor, region));
                        }
                    }
                });
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return list;
    }

    public static List<F2CDatastore> listDataStore(OpenStackBaseRequest request) {
        List<F2CDatastore> list = new ArrayList<>();
        try {
            OSClient.OSClientV3 osClient = request.getOSClient();
            List<String> regions = OpenStackUtils.getRegionList(osClient);
            if (OpenStackUtils.isAdmin(osClient)) {
                regions.forEach(region -> {
                    if (request.getRegionId() != null && !StringUtils.equals(request.getRegionId(), region)) {
                        return;
                    }
                    osClient.useRegion(region);
                    if (OpenStackUtils.isSupport(osClient, ServiceType.BLOCK_STORAGE)) {
                        List<? extends VolumeBackendPool> backendPools = osClient.blockStorage().schedulerStatsPools().poolsDetail();
                        for (VolumeBackendPool backendPool : backendPools) {
                            F2CDatastore f2CDataStore = OpenStackUtils.toF2CDatastore(backendPool, region);
                            f2CDataStore.setDataCenterName(region);
                            f2CDataStore.setDataCenterId(region);
                            f2CDataStore.setDataStoreName(region + '-' + f2CDataStore.getDataStoreName());
                            f2CDataStore.setDataStoreId(region + '-' + f2CDataStore.getDataStoreId());
                            list.add(f2CDataStore);
                        }
                    }

                });
            }

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return list;
    }

    public static List<F2CPerfMetricMonitorData> getF2CPerfMetricMonitorData(GetMetricsRequest request) {

        List<F2CPerfMetricMonitorData> result = new ArrayList<>();
        try {
            //设置时间，根据syncTimeStampStr,默认一个小时
            request.setStartTime(DateUtil.beforeOneHourToTimestamp(Long.valueOf(request.getSyncTimeStampStr())));
            request.setEndTime(Long.parseLong(request.getSyncTimeStampStr()));
            OSClient.OSClientV3 osClient = JsonUtil.parseObject(JsonUtil.toJSONString(request), OpenStackBaseRequest.class).getOSClient();

            List<String> regions = OpenStackUtils.getRegionList(osClient);

            for (String region : regions) {

                if (request.getRegionId() != null && !StringUtils.equals(request.getRegionId(), region)) {
                    continue;
                }

                osClient.useRegion(region);

                SampleCriteria sc = new SampleCriteria();
                Date start, end;
                if (Objects.isNull(request.getStartTime()) || Objects.isNull(request.getEndTime())) {
                    Calendar calendar = Calendar.getInstance();
                    end = calendar.getTime();
                    calendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE) - 40);
                    start = calendar.getTime();
                } else {
                    start = new Date(request.getStartTime());
                    end = new Date(request.getEndTime());
                }
                sc.timestamp(SampleCriteria.Oper.LTE, end);
                sc.timestamp(SampleCriteria.Oper.GTE, start);

                List<? extends Server> servers = osClient.compute().servers().list(true);

                for (Server server : servers) {
                    sc.resource(server.getId());
                    List<? extends Statistics> cpuStatistics = osClient.telemetry().meters().statistics(OpenStackPerfMetricConstants.CloudServerPerfMetricEnum.CPU_USED_UTILIZATION.getMetricName(), sc);

                    if (CollectionUtils.isNotEmpty(cpuStatistics)) {
                        F2CPerfMetricMonitorData cpuData = new F2CPerfMetricMonitorData();
                        cpuData.setTimestamp(cpuStatistics.get(0).getDurationEnd().getTime());
                        cpuData.setAverage(BigDecimal.valueOf(cpuStatistics.get(0).getAvg()));
                        cpuData.setMinimum(BigDecimal.valueOf(cpuStatistics.get(0).getMin()));
                        cpuData.setMaximum(BigDecimal.valueOf(cpuStatistics.get(0).getMax()));
                        cpuData.setEntityType(F2CEntityType.VIRTUAL_MACHINE.name());
                        cpuData.setMetricName(OpenStackPerfMetricConstants.CloudServerPerfMetricEnum.CPU_USED_UTILIZATION.name());
                        cpuData.setPeriod(cpuStatistics.get(0).getPeriod());
                        cpuData.setInstanceId(server.getId());
                        cpuData.setUnit(OpenStackPerfMetricConstants.CloudServerPerfMetricEnum.CPU_USED_UTILIZATION.getUnit());

                        result.add(cpuData);
                    }

                    List<? extends Statistics> memoryStatistics = osClient.telemetry().meters().statistics(OpenStackPerfMetricConstants.CloudServerPerfMetricEnum.MEMORY_USED_UTILIZATION.getMetricName(), sc);

                    if (CollectionUtils.isNotEmpty(memoryStatistics)) {
                        F2CPerfMetricMonitorData memoryData = new F2CPerfMetricMonitorData();
                        memoryData.setTimestamp(cpuStatistics.get(0).getDurationEnd().getTime());
                        memoryData.setAverage(BigDecimal.valueOf(cpuStatistics.get(0).getAvg()).divide(BigDecimal.valueOf(server.getFlavor().getRam()), 4, RoundingMode.HALF_UP));
                        memoryData.setMinimum(BigDecimal.valueOf(cpuStatistics.get(0).getMin()).divide(BigDecimal.valueOf(server.getFlavor().getRam()), 4, RoundingMode.HALF_UP));
                        memoryData.setMaximum(BigDecimal.valueOf(cpuStatistics.get(0).getMax()).divide(BigDecimal.valueOf(server.getFlavor().getRam()), 4, RoundingMode.HALF_UP));
                        memoryData.setEntityType(F2CEntityType.VIRTUAL_MACHINE.name());
                        memoryData.setMetricName(OpenStackPerfMetricConstants.CloudServerPerfMetricEnum.MEMORY_USED_UTILIZATION.name());
                        memoryData.setPeriod(cpuStatistics.get(0).getPeriod());
                        memoryData.setInstanceId(server.getId());
                        memoryData.setUnit(OpenStackPerfMetricConstants.CloudServerPerfMetricEnum.MEMORY_USED_UTILIZATION.getUnit());

                        result.add(memoryData);
                    }

                    List<? extends InterfaceAttachment> interfaces = null;
                    try {
                        interfaces = osClient.compute().servers().interfaces().list(server.getId());
                    } catch (Exception e) {
                    }
                    if (CollectionUtils.isNotEmpty(interfaces)) {
                        String resourceId = null;
                        Resource resource = osClient.telemetry().resources().get(server.getId());
                        // 获取所有监控信息，遍历返回值获取当前云主机的resourceId(该resourceId格式为：instance-xxxxxxx-instanceId-tap网卡id前10位
                        if (resource != null) {
                            Map<String, Object> metaData = resource.getMeataData() == null ? new HashMap<>() : resource.getMeataData();
                            resourceId = metaData.get("name") == null ? null
                                    : metaData.get("name").toString() + "-" + server.getId() + "-tap";
                        }
                        if (StringUtils.isNotBlank(resourceId)) {
                            // 可能存在多个网卡，所以resourceId可能有多个，根据resourceId的格式，当获取到一个resourceId后就可以根据interfaceId获取其他resourceId
                            resourceId = resourceId.substring(0, resourceId.indexOf("tap") + 3);

                            boolean hasInData = false, hasOutData = false;

                            F2CPerfMetricMonitorData inData = new F2CPerfMetricMonitorData();
                            inData.setEntityType(F2CEntityType.VIRTUAL_MACHINE.name());
                            inData.setMetricName(OpenStackPerfMetricConstants.CloudServerPerfMetricEnum.INTERNET_IN_RATE.name());
                            inData.setInstanceId(server.getId());
                            inData.setAverage(BigDecimal.ZERO);

                            F2CPerfMetricMonitorData outData = new F2CPerfMetricMonitorData();
                            outData.setEntityType(F2CEntityType.VIRTUAL_MACHINE.name());
                            outData.setMetricName(OpenStackPerfMetricConstants.CloudServerPerfMetricEnum.INTERNET_OUT_RATE.name());
                            outData.setInstanceId(server.getId());
                            outData.setAverage(BigDecimal.ZERO);

                            for (InterfaceAttachment interfaceAttachment : interfaces) {
                                sc.getCriteriaParams().remove(sc.getCriteriaParams().size() - 1);
                                sc.resource(resourceId + interfaceAttachment.getPortId().substring(0, 11));
                                List<? extends Statistics> networkIns = osClient.telemetry().meters()
                                        .statistics(OpenStackPerfMetricConstants.CloudServerPerfMetricEnum.INTERNET_IN_RATE.getMetricName(), sc);
                                List<? extends Statistics> networkOuts = osClient.telemetry().meters()
                                        .statistics(OpenStackPerfMetricConstants.CloudServerPerfMetricEnum.INTERNET_OUT_RATE.getMetricName(), sc);
                                if (null != networkIns && !networkIns.isEmpty()) {
                                    inData.setAverage(inData.getAverage().add(BigDecimal.valueOf(networkIns.get(0).getAvg().intValue())));
                                    hasInData = true;
                                }
                                if (null != networkOuts && !networkOuts.isEmpty()) {
                                    outData.setAverage(outData.getAverage().add(BigDecimal.valueOf(networkOuts.get(0).getAvg().intValue())));
                                    hasOutData = true;
                                }
                            }

                            if (hasInData) {
                                result.add(inData);
                            }
                            if (hasOutData) {
                                result.add(outData);
                            }
                        }
                    }

                    sc.getCriteriaParams().remove(sc.getCriteriaParams().size() - 1);
                }

            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return result;

    }

    public static List<OpenStackFlavor> getInstanceTypesForConfigUpdate(OpenStackConfigUpdateRequest request) {
        List<OpenStackFlavor> result = new ArrayList<>();
        try {
            OSClient.OSClientV3 osClient = request.getOSClient();
            osClient.useRegion(request.getRegionId());

            Server server = osClient.compute().servers().get(request.getInstanceUuid());
            if (server == null) {
                throw new RuntimeException("server not exist");
            }

            boolean bootFromVolume = StringUtils.isBlank(server.getImageId());

            List<? extends Flavor> flavors = osClient.compute().flavors().list();

            for (Flavor flavor : flavors) {
                if (flavor.isDisabled()) {
                    continue;
                }
                //排除内存小于1G的
                if (flavor.getRam() < 1024) {
                    continue;
                }
                if (flavor.getVcpus() == server.getFlavor().getVcpus() && flavor.getRam() == server.getFlavor().getRam()) {
                    continue;
                }
                if (!bootFromVolume) {
                    //需要判断系统盘
                    if (flavor.getDisk() < server.getFlavor().getDisk()) {
                        continue;
                    }
                }

                result.add(OpenStackFlavor.copy(flavor));
            }

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return result.stream().sorted(Comparator.comparingInt(OpenStackFlavor::getVcpus).thenComparingInt(OpenStackFlavor::getRam).thenComparingInt(OpenStackFlavor::getDisk)).collect(Collectors.toList());
    }

    public static F2CVirtualMachine changeVmConfig(OpenStackConfigUpdateRequest request) {
        try {
            OSClient.OSClientV3 osClient = request.getOSClient();
            osClient.useRegion(request.getRegionId());

            Server server = osClient.compute().servers().get(request.getInstanceUuid());
            if (server == null) {
                throw new RuntimeException("server not exist");
            }

            boolean bootFromVolume = StringUtils.isBlank(server.getImageId());

            Flavor flavor = osClient.compute().flavors().get(request.getNewInstanceType());
            if (flavor == null) {
                throw new RuntimeException("flavor not exist");
            }

            if (StringUtils.equals(server.getFlavor().getId(), flavor.getId())) {
                throw new RuntimeException("flavor not change");
            }

            if (!bootFromVolume) {
                //需要判断系统盘
                if (flavor.getDisk() < server.getFlavor().getDisk()) {
                    throw new RuntimeException("disk size can not be reduced");
                }
            }
            ActionResponse response = osClient.compute().servers().resize(server.getId(), flavor.getId());
            if (!response.isSuccess()) {
                throw new RuntimeException(response.getFault());
            }

            Server.Status status = server.getStatus();

            CheckStatusResult result = OpenStackUtils.checkServerStatus(osClient, server, Server.Status.VERIFY_RESIZE);
            if (!result.isSuccess() && result.getObject() == null) {
                throw new RuntimeException(result.getFault());
            } else {
                response = osClient.compute().servers().confirmResize(server.getId());
                if (!response.isSuccess()) {
                    String fault = response.getFault();
                    response = osClient.compute().servers().revertResize(server.getId());
                    if (!response.isSuccess()) {
                        throw new RuntimeException(result.getFault());
                    }
                    throw new RuntimeException(fault);
                }
            }
            result = OpenStackUtils.checkServerStatus(osClient, server, status);
            if (!result.isSuccess()) {
                if (result.getObject() != null && ((Server) result.getObject()).getStatus().equals(Server.Status.ERROR)) {
                    response = osClient.compute().servers().resetState(server.getId(), status);
                    if (!response.isSuccess()) {
                        throw new RuntimeException(response.getFault());
                    }
                    result = OpenStackUtils.checkServerStatus(osClient, server, status);
                    if (!result.isSuccess()) {
                        throw new RuntimeException(result.getFault());
                    }
                } else {
                    throw new RuntimeException(result.getFault());
                }
            }
            return OpenStackUtils.toF2CVirtualMachine(osClient, (Server) result.getObject(), request.getRegionId(), null);

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static List<F2CPerfMetricMonitorData> getF2CHostPerfMetricList(String req, GetMetricsRequest request) {
        List<F2CPerfMetricMonitorData> result = new ArrayList<>();
        //设置时间，根据syncTimeStampStr,默认一个小时
        request.setStartTime(DateUtil.beforeOneHourToTimestamp(Long.valueOf(request.getSyncTimeStampStr())));
        request.setEndTime(Long.parseLong(request.getSyncTimeStampStr()));
        try {
            List<F2CHost> f2CHosts = listHost(JsonUtil.parseObject(req, OpenStackBaseRequest.class));
            for (F2CHost f2CHost : f2CHosts) {
                //CPU
                F2CPerfMetricMonitorData cpuData = new F2CPerfMetricMonitorData();
                cpuData.setAverage(
                        BigDecimal.valueOf(f2CHost.getCpuMHzAllocated())
                                .divide(BigDecimal.valueOf(f2CHost.getCpuMHzTotal()), 2, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(OpenStackPerfMetricConstants.CloudServerPerfMetricEnum.CPU_USED_UTILIZATION.getDivisor()))
                );
                cpuData.setTimestamp(request.getEndTime());
                cpuData.setEntityType(F2CEntityType.HOST.name());
                cpuData.setMetricName(OpenStackPerfMetricConstants.CloudServerPerfMetricEnum.CPU_USED_UTILIZATION.name());
                cpuData.setPeriod(request.getPeriod());
                cpuData.setInstanceId(f2CHost.getHostId());
                cpuData.setUnit(OpenStackPerfMetricConstants.CloudServerPerfMetricEnum.CPU_USED_UTILIZATION.getUnit());
                result.add(cpuData);

                //内存
                F2CPerfMetricMonitorData memData = new F2CPerfMetricMonitorData();
                memData.setAverage(
                        BigDecimal.valueOf(f2CHost.getMemoryAllocated())
                                .divide(BigDecimal.valueOf(f2CHost.getMemoryTotal()), 2, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(OpenStackPerfMetricConstants.CloudServerPerfMetricEnum.MEMORY_USED_UTILIZATION.getDivisor()))
                );
                memData.setTimestamp(request.getEndTime());
                memData.setEntityType(F2CEntityType.HOST.name());
                memData.setMetricName(OpenStackPerfMetricConstants.CloudServerPerfMetricEnum.MEMORY_USED_UTILIZATION.name());
                memData.setPeriod(request.getPeriod());
                memData.setInstanceId(f2CHost.getHostId());
                memData.setUnit(OpenStackPerfMetricConstants.CloudServerPerfMetricEnum.MEMORY_USED_UTILIZATION.getUnit());
                result.add(memData);
            }


        } catch (Exception e) {
            throw new Fit2cloudException(100021, "获取监控数据失败-" + request.getRegionId() + "-" + e.getMessage());
        }

        return result;
    }

    public static List<F2CPerfMetricMonitorData> getF2CDatastorePerfMetricList(String req, GetMetricsRequest request) {
        List<F2CPerfMetricMonitorData> result = new ArrayList<>();
        //设置时间，根据syncTimeStampStr,默认一个小时
        request.setStartTime(DateUtil.beforeOneHourToTimestamp(Long.valueOf(request.getSyncTimeStampStr())));
        request.setEndTime(Long.parseLong(request.getSyncTimeStampStr()));
        try {
            List<F2CDatastore> f2CDataStores = listDataStore(JsonUtil.parseObject(req, OpenStackBaseRequest.class));
            for (F2CDatastore datastore : f2CDataStores) {
                F2CPerfMetricMonitorData f2CEntityPerfMetric = new F2CPerfMetricMonitorData();

                f2CEntityPerfMetric.setTimestamp(Long.valueOf(request.getEndTime()));
                BigDecimal useAvg = BigDecimal.valueOf(datastore.getCapacity()).subtract(BigDecimal.valueOf(datastore.getFreeSpace()));
                BigDecimal totalBig = BigDecimal.valueOf(datastore.getCapacity());
                f2CEntityPerfMetric.setAverage(useAvg
                        .multiply(BigDecimal.valueOf(OpenStackPerfMetricConstants.CloudServerPerfMetricEnum.DATASTORE_USED_UTILIZATION.getDivisor()))
                        .divide(totalBig, 2, RoundingMode.HALF_UP));

                f2CEntityPerfMetric.setEntityType(F2CEntityType.DATASTORE.name());
                f2CEntityPerfMetric.setMetricName(OpenStackPerfMetricConstants.CloudServerPerfMetricEnum.DATASTORE_USED_UTILIZATION.name());
                f2CEntityPerfMetric.setPeriod(request.getPeriod());
                f2CEntityPerfMetric.setInstanceId(datastore.getDataStoreId());
                f2CEntityPerfMetric.setUnit(OpenStackPerfMetricConstants.CloudServerPerfMetricEnum.DATASTORE_USED_UTILIZATION.getUnit());
                result.add(f2CEntityPerfMetric);
            }
        } catch (Exception e) {
            throw new Fit2cloudException(100021, "获取监控数据失败-" + request.getRegionId() + "-" + e.getMessage());
        }
        return result;
    }

    /**
     * 获取云主机关联的磁盘
     *
     * @param request
     * @return
     */
    public static List<F2CDisk> getVmF2CDisks(BaseDiskRequest request) {
        try {
            OpenStackCredential openStackCredential = JsonUtil.parseObject(request.getCredential(), OpenStackCredential.class);
            OSClient.OSClientV3 osClient = openStackCredential.getOSClient();
            Server server = osClient.compute().servers().get(request.getInstanceUuid());
            Optional.ofNullable(server).orElseThrow(() -> new RuntimeException(String.format("Virtual machine: %s not found", request.getInstanceUuid())));

            List<String> diskIds = server.getOsExtendedVolumesAttached();
            List<F2CDisk> disks = new ArrayList<>();

            if (!CollectionUtils.isEmpty(diskIds)) {
                for (String diskId : diskIds) {
                    Volume volume = osClient.blockStorage().volumes().get(diskId);
                    Optional.ofNullable(volume).orElseThrow(() -> new RuntimeException(String.format("The volume: %s of virtual machine: %s not found", diskId, request.getInstanceUuid())));
                    disks.add(OpenStackUtils.toF2CDisk(volume, request.getRegionId()));
                }
            }
            return disks;
        } catch (Exception e) {
            throw new RuntimeException("GetVmF2CDisks Error!" + e.getMessage(), e);
        }
    }

    public static String calculateConfigPrice(OpenstackCalculateConfigPriceRequest request) {
        if (StringUtils.isEmpty(request.getFlavorId())) {
            return "--";
        }
        List<Flavor> flavors = OpenStackCloudApi.getFlavors(request);
        Optional<Flavor> first = flavors.stream().filter(flavor -> StringUtils.equals(flavor.getId(), request.getFlavorId())).findFirst();
        if (first.isEmpty()) {
            return "--";
        }
        RestTemplate restTemplate = SpringUtil.getBean(RestTemplate.class);
        Set<String> servicesExcludeGateway = ServiceUtil.getServicesExcludeGateway();
        if (servicesExcludeGateway.contains("finance-management")) {
            String url = ServiceUtil.getHttpUrl("finance-management", "api/billing_policy/calculate_config_price/" + request.getAccountId());
            ResponseEntity<ResultHolder<List<BillPolicyDetails>>> exchange = restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<>() {
            });
            List<BillPolicyDetails> billPolicyDetailsList = Objects.requireNonNull(exchange.getBody()).getData();

            Optional<BigDecimal> ecs = billPolicyDetailsList.stream()
                    .map(billPolicyDetails -> {
                        if ("ECS".equals(billPolicyDetails.getResourceType())) {
                            return ChargingUtil.getBigDecimal(request.getInstanceChargeType(), Map.of("cpu", first.get().getVcpus(), "memory", new BigDecimal(first.get().getRam()).divide(new BigDecimal(1024), 0, RoundingMode.CEILING).intValue()), billPolicyDetails);
                        }
                        if ("DISK".equals(billPolicyDetails.getResourceType())) {
                            List<OpenStackServerCreateRequest.DiskConfig> disks = request.getDisks();
                            return disks.stream().map(disk -> ChargingUtil.getBigDecimal(request.getInstanceChargeType(), Map.of("size", disk.getSize()), billPolicyDetails))
                                    .reduce(BigDecimal::add).orElse(new BigDecimal(0));
                        }
                        return new BigDecimal(0);
                    }).reduce(BigDecimal::add);
            return ecs.map(bigDecimal -> {
                        if (ChargeTypeConstants.PREPAID.getCode().equals(request.getInstanceChargeType()) && StringUtils.isNotEmpty(request.getPeriodNum())) {
                            return bigDecimal.multiply(new BigDecimal(request.getPeriodNum())).multiply(new BigDecimal(request.getCount()));
                        }
                        return bigDecimal.multiply(new BigDecimal(request.getCount()));
                    }).map(bigDecimal -> bigDecimal.setScale(3, RoundingMode.HALF_UP) +
                            (ChargeTypeConstants.POSTPAID.getCode().equals(request.getInstanceChargeType()) ? "元/小时" : "元"))
                    .orElse("--");
        }
        return "-";
    }
}
