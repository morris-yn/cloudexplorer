package com.fit2cloud.common.platform.credential.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fit2cloud.common.exception.Fit2cloudException;
import com.fit2cloud.common.form.annotaion.Form;
import com.fit2cloud.common.form.constants.InputType;
import com.fit2cloud.common.platform.credential.Credential;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * Author: LiuDi
 * Date: 2022/9/16 2:22 PM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class VsphereCredential implements Credential {

    /**
     * 用户名
     */
    @Form(inputType = InputType.Text, label = "用户名")
    @JsonProperty("vUserName")
    private String vUserName;
    /**
     * 密码
     */
    @Form(inputType = InputType.Password, label = "密码")
    @JsonProperty("vPassword")
    private String vPassword;
    /**
     * vCenter服务器IP
     */
    @Form(inputType = InputType.Text, label = "vCenter服务器IP")
    @JsonProperty("vCenterIp")
    private String vCenterIp;
    /**
     * 是否启用内容库镜像
     */
    private boolean useContentLibrary = false;

    @Override
    public boolean verification() {
        ServiceInstance serviceInstance = null;
        try {
            serviceInstance = initServiceInstance();
            return true;
        } catch (Exception e) {
            log.error("Cloud Account Verification failed!" + e.getMessage(), e);
            throw new Fit2cloudException(100001, "云账号校验失败:" + e.getMessage());
        } finally {
            closeConnection(serviceInstance);
        }
    }

    @Override
    public List<Region> regions() {
        ServiceInstance serviceInstance = null;
        try {
            serviceInstance = initServiceInstance();
            ManagedEntity rootEntity = serviceInstance.getRootFolder();
            ManagedEntity[] mes = new InventoryNavigator(rootEntity).searchManagedEntities(Datacenter.class.getSimpleName());
            List<Datacenter> datacenters = new ArrayList<>();
            if (mes != null) {
                for (ManagedEntity m : mes) {
                    datacenters.add((Datacenter) m);
                }
            }
            return datacenters.stream().map(this::toRegion).toList();
        } catch (Exception e) {
            log.error("Failed to Get Regions!" + e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        } finally {
            closeConnection(serviceInstance);
        }
    }

    private Region toRegion(Datacenter datacenter) {
        Region region = new Region();
        region.setRegionId(datacenter.getName());
        region.setName(datacenter.getName());
        return region;
    }

    /**
     * 初始化 vsphere 连接实例
     *
     * @return
     */
    private ServiceInstance initServiceInstance() {
        try {
            return new ServiceInstance(new URL("https://" + vCenterIp + "/sdk"), vUserName, vPassword, true, 60000, 12000);
        } catch (RemoteException e) {
            if (e.toString().contains("InvalidLogin")) {
                throw new RuntimeException("Account verification failed!" + e.getMessage(), e);
            }
            throw new RuntimeException("Failed to connect to Vsphere server!" + e.getMessage(), e);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Vsphere server address error!", e);
        }
    }

    /**
     * 关闭 vsphere 连接
     */
    private void closeConnection(ServiceInstance serviceInstance) {
        if (serviceInstance != null) {
            serviceInstance.getServerConnection().logout();
        }
    }
}
