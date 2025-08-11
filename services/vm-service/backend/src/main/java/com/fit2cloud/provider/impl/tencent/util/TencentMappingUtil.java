package com.fit2cloud.provider.impl.tencent.util;

import com.fit2cloud.common.log.utils.LogUtil;
import com.fit2cloud.common.provider.entity.F2CPerfMetricMonitorData;
import com.fit2cloud.common.provider.util.CommonUtil;
import com.fit2cloud.provider.impl.tencent.constants.TencentChargeType;
import com.fit2cloud.provider.impl.tencent.constants.TencentPerfMetricConstants;
import com.fit2cloud.vm.constants.DeleteWithInstance;
import com.fit2cloud.vm.constants.F2CChargeType;
import com.fit2cloud.vm.constants.F2CDiskStatus;
import com.fit2cloud.vm.constants.F2CInstanceStatus;
import com.fit2cloud.vm.entity.F2CDisk;
import com.fit2cloud.vm.entity.F2CImage;
import com.fit2cloud.vm.entity.F2CVirtualMachine;
import com.tencentcloudapi.cbs.v20170312.models.Disk;
import com.tencentcloudapi.cvm.v20170312.models.DataDisk;
import com.tencentcloudapi.cvm.v20170312.models.Image;
import com.tencentcloudapi.cvm.v20170312.models.Instance;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Author:张少虎
 * @Date: 2022/9/23  4:05 PM
 * @Version 1.0
 * @注释:
 */
public class TencentMappingUtil {
    /**
     * 将腾讯云对象转换为系统实例对象
     *
     * @param instance 腾讯云实例对象
     * @return 系统云主机实例对象
     */
    public static F2CVirtualMachine toF2CVirtualMachine(Instance instance) {
        F2CVirtualMachine f2CInstance = new F2CVirtualMachine();
        f2CInstance.setInstanceId(instance.getInstanceId());
        f2CInstance.setInstanceUUID(instance.getInstanceId());
        f2CInstance.setOsInfo(instance.getOsName());
        f2CInstance.setInstanceType(instance.getInstanceType());
        f2CInstance.setCpu(instance.getCPU().intValue());
        // 设置企业项目id 如果是0 则为默认企业项目
        f2CInstance.setProjectId(instance.getPlacement().getProjectId().toString());
        f2CInstance.setMemory(instance.getMemory().intValue());
        f2CInstance.setInstanceTypeDescription(instance.getCPU() + "vCPU " + instance.getMemory() + "GB");
        f2CInstance.setZone(instance.getPlacement().getZone());
        List<String> ipArray = new ArrayList<>();
        if (instance.getPrivateIpAddresses() != null && instance.getPrivateIpAddresses().length > 0) {
            f2CInstance.setLocalIP(instance.getPrivateIpAddresses()[0]);
            ipArray.addAll(Arrays.asList(instance.getPrivateIpAddresses()));
        }
        f2CInstance.setImageId(instance.getImageId());
        if (instance.getPublicIpAddresses() != null && instance.getPublicIpAddresses().length > 0) {
            f2CInstance.setRemoteIP(instance.getPublicIpAddresses()[0]);
            ipArray.addAll(Arrays.asList(instance.getPublicIpAddresses()));
        }
        f2CInstance.setHostname(instance.getInstanceName());
        f2CInstance.setName(instance.getInstanceName());
        if (StringUtils.isNotEmpty(instance.getCreatedTime())) {
            long utcTime = CommonUtil.getUTCTime(instance.getCreatedTime(), "yyyy-MM-dd'T'HH:mm:ss'Z'");
            f2CInstance.setCreated(new Date(utcTime));
            f2CInstance.setCreateTime(utcTime);
        }
        f2CInstance.setInstanceStatus(toF2CStatus(instance.getInstanceState()));
        f2CInstance.setIpArray(ipArray);
        f2CInstance.setVpcId(instance.getVirtualPrivateCloud().getVpcId());
        f2CInstance.setSubnetId(instance.getVirtualPrivateCloud().getSubnetId());
        // 计费方式
        String instanceChargeType = null;
        switch (instance.getInstanceChargeType()) {
            case "PREPAID" -> instanceChargeType = F2CChargeType.PRE_PAID;
            case "POSTPAID_BY_HOUR" -> instanceChargeType = F2CChargeType.POST_PAID;
            case "SPOTPAID" -> instanceChargeType = F2CChargeType.SPOT_PAID;
            default -> {
            }
        }
        f2CInstance.setAutoRenew(StringUtils.equals(instance.getRenewFlag(), "NOTIFY_AND_AUTO_RENEW"));
        f2CInstance.setInstanceChargeType(instanceChargeType);
        if (F2CChargeType.PRE_PAID.equalsIgnoreCase(f2CInstance.getInstanceChargeType()) && StringUtils.isNotEmpty(instance.getExpiredTime())) {
            long utcTime = CommonUtil.getUTCTime(instance.getExpiredTime(), "yyyy-MM-dd'T'HH:mm:ss'Z'");
            f2CInstance.setExpired(new Date(utcTime));
            f2CInstance.setExpiredTime(utcTime);
        }
        f2CInstance.setSecurityGroupIds(Arrays.asList(instance.getSecurityGroupIds()));
        long dataSize = Arrays.stream(instance.getDataDisks()).mapToLong(DataDisk::getDiskSize).sum();
        f2CInstance.setDisk(dataSize + (instance.getSystemDisk() != null ? instance.getSystemDisk().getDiskSize() : 0));
        return f2CInstance;
    }

    /**
     * 将腾讯云磁盘对象转换为系统磁盘对象
     *
     * @param disk 腾讯云磁盘对象
     * @return 系统磁盘对象
     */
    public static F2CDisk toF2CDisk(Disk disk) {
        F2CDisk f2CDisk = new F2CDisk();
        f2CDisk.setSize(disk.getDiskSize());
        if (StringUtils.isNotEmpty(disk.getCreateTime())) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                f2CDisk.setCreateTime(sdf.parse(disk.getCreateTime()).getTime());
            } catch (ParseException e) {
                LogUtil.error("Failed to format disk create time" + e.getMessage(), e);
            }
        }
        f2CDisk.setDiskName(disk.getDiskName());
        f2CDisk.setStatus(toF2cDiskStatus(disk.getDiskState()));
        f2CDisk.setDiskId(disk.getDiskId());
        f2CDisk.setInstanceUuid(disk.getInstanceId());
        f2CDisk.setDiskType(disk.getDiskType());
        //计费方式
        String instanceChargeType = null;
        switch (disk.getDiskChargeType()) {
            case "PREPAID":
                instanceChargeType = F2CChargeType.PRE_PAID;
                break;
            case "POSTPAID_BY_HOUR":
                instanceChargeType = F2CChargeType.POST_PAID;
                break;
            case "SPOTPAID":
                instanceChargeType = F2CChargeType.SPOT_PAID;
                break;
            default:
        }
        f2CDisk.setDiskChargeType(instanceChargeType);
        f2CDisk.setCategory(disk.getDiskUsage());
        if (disk.getDeleteWithInstance()) {
            f2CDisk.setDeleteWithInstance(DeleteWithInstance.YES.name());
        } else {
            f2CDisk.setDeleteWithInstance(DeleteWithInstance.NO.name());
        }
        if ("SYSTEM_DISK".equalsIgnoreCase(disk.getDiskUsage())) {
            f2CDisk.setBootable(true);
        }
        com.tencentcloudapi.cbs.v20170312.models.Placement placement = disk.getPlacement();
        if (placement != null) {
            f2CDisk.setZone(placement.getZone());
        }
        return f2CDisk;
    }

    /**
     * 将腾讯云镜像实例转换为系统镜像实例
     *
     * @param image 腾讯云镜像服务
     * @return 系统镜像
     */
    public static F2CImage toF2CImage(Image image) {
        F2CImage f2CImage = new F2CImage();
        f2CImage.setId(image.getImageId());
        f2CImage.setName(image.getImageName());
        f2CImage.setOs(image.getOsName());
        f2CImage.setDescription(image.getImageDescription());
        f2CImage.setDiskSize(image.getImageSize());
        if (StringUtils.isNotEmpty(image.getCreatedTime())) {
            long utcTime = CommonUtil.getUTCTime(image.getCreatedTime(), "yyyy-MM-dd'T'HH:mm:ss'Z'");
            f2CImage.setCreated(utcTime);
        }
        return f2CImage;
    }


    /**
     * 将腾讯云磁盘状态转换为系统磁盘状态
     *
     * @param diskStatus 腾讯云磁盘状态
     * @return 系统磁盘状态
     */
    public static String toF2cDiskStatus(String diskStatus) {
        switch (diskStatus) {
            case "UNATTACHED":
                return F2CDiskStatus.AVAILABLE;
            case "ATTACHING":
                return F2CDiskStatus.ATTACHING;
            case "ATTACHED":
                return F2CDiskStatus.IN_USE;
            case "DETACHING":
                return F2CDiskStatus.DETACHING;
            case "EXPANDING":
                return F2CDiskStatus.CREATING;
            case "ROLLBACKING":
                return F2CDiskStatus.REINITING;
            default:
                return F2CDiskStatus.UNKNOWN;
        }
    }

    /**
     * 将腾讯云云主机实例状态转换为系统实例状态
     *
     * @param qcloudStatus 腾讯云实例状态
     * @return 系统实例状态
     */
    public static String toF2CStatus(String qcloudStatus) {
        /**
         * PENDING：表示创建中
         * LAUNCH_FAILED：表示创建失败
         * RUNNING：表示运行中
         * STOPPED：表示关机
         * STARTING：表示开机中
         * STOPPING：表示关机中
         * REBOOTING：表示重启中
         * SHUTDOWN：表示停止待销毁
         * TERMINATING：表示销毁中。
         */

        switch (qcloudStatus) {
            case "STOPPED":
                return F2CInstanceStatus.Stopped.name();
            case "RUNNING":
                return F2CInstanceStatus.Running.name();
            default:
                return F2CInstanceStatus.Unknown.name();
        }
    }

    /**
     * 将云管系统付费方式转换为腾讯云付费方式
     *
     * @param f2cChargeType 系统标记付费方式
     * @return 腾讯标记付费方式
     */
    public static String toTencentChargeType(String f2cChargeType) {
        /**
         * 云硬盘计费类型。
         * PREPAID：预付费，即包年包月
         * POSTPAID_BY_HOUR：按小时后付费
         * CDCPAID：独享集群付费
         */
        if (f2cChargeType == null) {
            return TencentChargeType.POSTPAID.getId();
        }
        switch (f2cChargeType.toUpperCase()) {
            case "PREPAID":
                return TencentChargeType.PREPAID.getId();
            default:
                return TencentChargeType.POSTPAID.getId();
        }
    }

    public static F2CPerfMetricMonitorData toF2CPerfMetricMonitorData(Map<Long, BigDecimal> map, Long k, String unit) {
        F2CPerfMetricMonitorData f2CEntityPerfMetric = new F2CPerfMetricMonitorData();
        f2CEntityPerfMetric.setTimestamp(k);
        //Mbps Byte 128
        if (StringUtils.equalsIgnoreCase(TencentPerfMetricConstants.CloudServerPerfMetricEnum.INTERNET_IN_RATE.getUnit(), unit)) {
            f2CEntityPerfMetric.setAverage(map.get(k).multiply(new BigDecimal(1024 / 8)).setScale(3, RoundingMode.HALF_UP));
        } else {
            f2CEntityPerfMetric.setAverage(map.get(k).setScale(3, RoundingMode.HALF_UP));
        }

        return f2CEntityPerfMetric;
    }
}
