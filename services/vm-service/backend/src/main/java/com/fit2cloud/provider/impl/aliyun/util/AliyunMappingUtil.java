package com.fit2cloud.provider.impl.aliyun.util;

import com.aliyun.ecs20140526.models.DescribeDisksResponseBody;
import com.aliyun.ecs20140526.models.DescribeImagesResponseBody;
import com.aliyun.ecs20140526.models.DescribeInstanceAutoRenewAttributeResponseBody;
import com.aliyun.ecs20140526.models.DescribeInstancesResponseBody;
import com.fasterxml.jackson.databind.JsonNode;
import com.fit2cloud.common.provider.entity.F2CPerfMetricMonitorData;
import com.fit2cloud.common.provider.util.CommonUtil;
import com.fit2cloud.provider.impl.aliyun.constants.AliyunChargeType;
import com.fit2cloud.vm.constants.DeleteWithInstance;
import com.fit2cloud.vm.constants.F2CChargeType;
import com.fit2cloud.vm.constants.F2CDiskStatus;
import com.fit2cloud.vm.entity.F2CDisk;
import com.fit2cloud.vm.entity.F2CImage;
import com.fit2cloud.vm.entity.F2CVirtualMachine;
import com.google.gson.Gson;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * @Author:张少虎
 * @Date: 2022/9/21  9:09 AM
 * @Version 1.0
 * @注释:
 */
public class AliyunMappingUtil {

    /**
     * 将DescribeInstancesResponseBodyInstancesInstance对象转换为F2CVirtualMachine对象
     *
     * @param instance   实例
     * @param attributes 实例续费数据
     * @return 云管公共对象
     */
    public static F2CVirtualMachine toF2CVirtualMachine(DescribeInstancesResponseBody.DescribeInstancesResponseBodyInstancesInstance instance, List<DescribeInstanceAutoRenewAttributeResponseBody.DescribeInstanceAutoRenewAttributeResponseBodyInstanceRenewAttributesInstanceRenewAttribute> attributes) {
        boolean autoRenew = autoRenew(attributes, instance.getInstanceId());
        F2CVirtualMachine f2CVirtualMachine = toF2CVirtualMachine(instance);
        f2CVirtualMachine.setAutoRenew(autoRenew);
        return f2CVirtualMachine;
    }

    public static boolean autoRenew(List<DescribeInstanceAutoRenewAttributeResponseBody.DescribeInstanceAutoRenewAttributeResponseBodyInstanceRenewAttributesInstanceRenewAttribute> attributes, String instanceId) {
        return attributes
                .stream()
                .filter(attr -> StringUtils.equals(attr.instanceId, instanceId))
                .findFirst()
                .map(DescribeInstanceAutoRenewAttributeResponseBody.DescribeInstanceAutoRenewAttributeResponseBodyInstanceRenewAttributesInstanceRenewAttribute::getAutoRenewEnabled)
                .orElse(false);
    }

    /**
     * 将DescribeInstancesResponseBodyInstancesInstance对象转换为F2CVirtualMachine对象
     *
     * @param instance 阿里云实例对象
     * @return 云管公共对象
     */
    public static F2CVirtualMachine toF2CVirtualMachine(DescribeInstancesResponseBody.DescribeInstancesResponseBodyInstancesInstance instance) {
        F2CVirtualMachine f2CVirtualMachine = new F2CVirtualMachine();
        f2CVirtualMachine.setCpu(instance.getCpu());
        f2CVirtualMachine.setMemory(instance.getMemory() / 1024);
        f2CVirtualMachine.setRegion(instance.getRegionId());
        f2CVirtualMachine.setHostname(instance.getHostName());
        f2CVirtualMachine.setImageId(instance.getImageId());
        f2CVirtualMachine.setInstanceId(instance.getInstanceId());
        f2CVirtualMachine.setProjectId(instance.getResourceGroupId());
        f2CVirtualMachine.setInstanceUUID(instance.getInstanceId());
        f2CVirtualMachine.setInstanceStatus(instance.getStatus());
        String instanceType = instance.getInstanceType();
        f2CVirtualMachine.setInstanceType(instanceType);
        f2CVirtualMachine.setInstanceTypeDescription(f2CVirtualMachine.getCpu() + "vCPU " + f2CVirtualMachine.getMemory() + "GB");
        String instanceChargeType = StringUtils.equalsIgnoreCase(instance.getInstanceChargeType(), "PostPaid") ? F2CChargeType.POST_PAID : F2CChargeType.PRE_PAID;
        f2CVirtualMachine.setInstanceChargeType(instanceChargeType);
        List<String> ipArray = new ArrayList<>();
        DescribeInstancesResponseBody.DescribeInstancesResponseBodyInstancesInstancePublicIpAddress publicIpAddress = instance.getPublicIpAddress();
        List<String> ipAddress = publicIpAddress.getIpAddress();
        if (ipAddress.size() > 0) {
            f2CVirtualMachine.setRemoteIP(ipAddress.get(0));
            addIpToArray(ipArray, ipAddress);
        }
        if (instance.getEipAddress() != null) {
            if (instance.getEipAddress().getIpAddress() != null && instance.getEipAddress().getIpAddress().trim().length() > 0) {
                f2CVirtualMachine.setRemoteIP(instance.getEipAddress().getIpAddress());
                ipArray.add(instance.getEipAddress().getIpAddress());
            }
        }
        DescribeInstancesResponseBody.DescribeInstancesResponseBodyInstancesInstanceInnerIpAddress innerIpAddress = instance.getInnerIpAddress();
        List<String> innerIpList = innerIpAddress.getIpAddress();
        if (innerIpList.size() > 0) {
            f2CVirtualMachine.setLocalIP(innerIpList.get(0));
            addIpToArray(ipArray, innerIpList);
        } else {
            DescribeInstancesResponseBody.DescribeInstancesResponseBodyInstancesInstanceVpcAttributes vpcAttributes = instance.getVpcAttributes();
            DescribeInstancesResponseBody.DescribeInstancesResponseBodyInstancesInstanceVpcAttributesPrivateIpAddress privateIpAddress = vpcAttributes.getPrivateIpAddress();
            if (CollectionUtils.isNotEmpty(privateIpAddress.ipAddress)) {
                f2CVirtualMachine.setLocalIP(privateIpAddress.ipAddress.get(0));
                addIpToArray(ipArray, privateIpAddress.ipAddress);
            }
        }
        f2CVirtualMachine.setIpArray(ipArray);

        f2CVirtualMachine.setName(instance.getInstanceName());
        if (StringUtils.isEmpty(instance.getInstanceName())) {
            f2CVirtualMachine.setName(instance.getInstanceId());
        }
        f2CVirtualMachine.setOsInfo(instance.getOSName());
        f2CVirtualMachine.setZone(instance.getZoneId());
        if (instance.getCreationTime() != null) {
            Date cTime = getFormatDateTime(instance.getCreationTime());
            if (cTime != null) {
                f2CVirtualMachine.setCreated(cTime);
                f2CVirtualMachine.setCreateTime(cTime.getTime());
            }
        }
        if (F2CChargeType.PRE_PAID.equalsIgnoreCase(instanceChargeType) && instance.getExpiredTime() != null) {
            Date cTime = getFormatDateTime(instance.getExpiredTime());
            if (cTime != null) {
                f2CVirtualMachine.setExpired(cTime);
                f2CVirtualMachine.setExpiredTime(cTime.getTime());
            }
        }
        // 安全组
        if (instance.getSecurityGroupIds() != null) {
            f2CVirtualMachine.setSecurityGroupIds(instance.getSecurityGroupIds().getSecurityGroupId());
        }
        instance.setCreationTime(null);
        instance.setDescription(null);
        instance.setInstanceId(null);
        instance.setInstanceName(null);
        instance.setHostName(null);
        instance.setStatus(null);
        instance.setInnerIpAddress(null);
        instance.setImageId(null);
        f2CVirtualMachine.setCustomData(new Gson().toJson(instance));
        f2CVirtualMachine.setVpcId(instance.getVpcAttributes().getVpcId());
        f2CVirtualMachine.setSubnetId(instance.getVpcAttributes().getVSwitchId());
        return f2CVirtualMachine;
    }


    /**
     * 将阿里云磁盘对象转换为云管磁盘对象
     *
     * @param disk 阿里云磁盘对象
     * @return 云管磁盘对象
     */
    public static F2CDisk toF2CDisk(DescribeDisksResponseBody.DescribeDisksResponseBodyDisksDisk disk) {
        F2CDisk f2cDisk = new F2CDisk();
        if (disk.getCategory() != null) {
            f2cDisk.setCategory(disk.getCategory());
            f2cDisk.setDiskType(disk.getCategory());
        }
        f2cDisk.setBootable(StringUtils.isNotEmpty(disk.getType()) && disk.getType().equalsIgnoreCase("system"));
        f2cDisk.setCreateTime(getUTCTime(disk.getCreationTime()));
        f2cDisk.setDescription(disk.getDescription());
        f2cDisk.setDevice(disk.getDevice());
        f2cDisk.setDiskChargeType(disk.getDiskChargeType());
        f2cDisk.setDiskId(disk.getDiskId());
        f2cDisk.setDiskName(disk.getDiskName());
        f2cDisk.setInstanceUuid(disk.getInstanceId());
        f2cDisk.setRegion(disk.getRegionId());
        f2cDisk.setZone(disk.getZoneId());
        f2cDisk.setSize(disk.getSize());
        f2cDisk.setStatus(toF2CDiskStatus(disk.getStatus()));
        if (StringUtils.isBlank(disk.getDiskName())) {
            f2cDisk.setDiskName(disk.getDiskId());
        }
        if (disk.getDeleteWithInstance()) {
            f2cDisk.setDeleteWithInstance(DeleteWithInstance.YES.name());
        } else {
            f2cDisk.setDeleteWithInstance(DeleteWithInstance.NO.name());
        }
        return f2cDisk;
    }

    /**
     * 将阿里云镜像对象转化为云管镜像
     *
     * @param image 阿里云镜像信息
     * @return 云管镜像
     */
    public static F2CImage toF2CImage(DescribeImagesResponseBody.DescribeImagesResponseBodyImagesImage image, String region) {
        return new F2CImage(image.getImageId(), image.getImageName(), image.getDescription(), image.getOSName(), region, image.getSize().longValue(), getUTCTime(image.getCreationTime()));
    }

    public static F2CPerfMetricMonitorData toF2CPerfMetricMonitorData(JsonNode v) {
        F2CPerfMetricMonitorData f2CEntityPerfMetric = new F2CPerfMetricMonitorData();
        f2CEntityPerfMetric.setTimestamp(v.get("timestamp").longValue());
        f2CEntityPerfMetric.setAverage(BigDecimal.valueOf(v.get("Average").doubleValue()).setScale(3, RoundingMode.HALF_UP));
        f2CEntityPerfMetric.setMinimum(BigDecimal.valueOf(v.get("Minimum").doubleValue()).setScale(3, RoundingMode.HALF_UP));
        f2CEntityPerfMetric.setMaximum(BigDecimal.valueOf(v.get("Maximum").doubleValue()).setScale(3, RoundingMode.HALF_UP));
        return f2CEntityPerfMetric;
    }

    /**
     * 将阿里云磁盘状态转换为当前系统磁盘状态
     *
     * @param status 阿里云磁盘状态
     * @return 系统磁盘状态
     */
    public static String toF2CDiskStatus(String status) {
        if (F2CDiskStatus.ATTACHING.equalsIgnoreCase(status)) {
            return F2CDiskStatus.ATTACHING;
        } else if (F2CDiskStatus.AVAILABLE.equalsIgnoreCase(status)) {
            return F2CDiskStatus.AVAILABLE;
        } else if (F2CDiskStatus.CREATING.equalsIgnoreCase(status)) {
            return F2CDiskStatus.CREATING;
        } else if (F2CDiskStatus.DETACHING.equalsIgnoreCase(status)) {
            return F2CDiskStatus.DETACHING;
        } else if (F2CDiskStatus.IN_USE.equalsIgnoreCase(status)) {
            return F2CDiskStatus.IN_USE;
        } else if (F2CDiskStatus.REINITING.equalsIgnoreCase(status)) {
            return F2CDiskStatus.REINITING;
        } else {
            return F2CDiskStatus.UNKNOWN;
        }
    }

    /**
     * 格式化时间
     */
    private static Date getFormatDateTime(String time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date cTime = null;
        try {
            cTime = sdf.parse(time);
        } catch (ParseException e) {
            e.getStackTrace();
        }
        return cTime;
    }

    /**
     * 获取utc时间根据时间字符串
     *
     * @param dateStr 时间字符串
     * @return 转化后的时间戳
     */
    private static long getUTCTime(String dateStr) {
        return CommonUtil.getUTCTime(dateStr, "yyyy-MM-dd'T'HH:mm:ss'Z'");
    }

    /**
     * 向ipArray 添加ip
     *
     * @param ipArray 需要添加到ip
     * @param ipList  从ipList存在需要添加到ip
     */
    private static void addIpToArray(List<String> ipArray, List<String> ipList) {
        for (String ip : ipList) {
            if (ip.contains(".")) {
                ipArray.add(ip);
            }
        }
    }

    /**
     * 将系统付费方式转换为阿里云云付费方式
     *
     * @param f2cChargeType 系统标记付费方式
     * @return 腾讯标记付费方式
     */
    public static String toAliyunChargeType(String f2cChargeType) {
        /**
         *云硬盘计费类型。
         *PrePaid：预付费，即包年包月
         *PostPaid：按小时后付费
         */
        switch (f2cChargeType) {
            case "PrePaid":
                return AliyunChargeType.PREPAID.getId();
            default:
                return AliyunChargeType.POSTPAID.getId();
        }
    }
}
