package com.fit2cloud.provider.impl.vsphere.util;

import com.fit2cloud.common.constants.Language;
import com.fit2cloud.common.platform.credential.impl.VsphereCredential;
import com.fit2cloud.common.provider.impl.vsphere.utils.VsphereClient;
import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.*;

/**
 * Author: LiuDi
 * Date: 2022/9/22 11:06 AM
 */
@Slf4j
public class VsphereVmClient extends VsphereClient {

    public VsphereVmClient(VsphereCredential credential, String regionId, Language lang) {
        super(credential.getVCenterIp(), credential.getVUserName(), credential.getVPassword(), regionId, Optional.ofNullable(lang).orElse(Language.zh_CN));
    }

    /**
     * 获取云主机
     *
     * @return 云主机列表
     */
    public List<VirtualMachine> listVirtualMachines() {
        List<VirtualMachine> list = listResources(VirtualMachine.class);
        List<VirtualMachine> vmList = new ArrayList<>();
        for (VirtualMachine vm : list) {
            VirtualMachineConfigInfo cfg = vm.getConfig();
            if (cfg == null || !cfg.isTemplate()) {
                vmList.add(vm);
            }
        }
        return vmList;
    }

    /**
     * 获取镜像
     *
     * @return 镜像列表
     */
    public List<VirtualMachine> listTemplates() {
        List<VirtualMachine> list = listResources(VirtualMachine.class);
        List<VirtualMachine> templateList = new ArrayList<>();
        for (VirtualMachine vm : list) {
            VirtualMachineConfigInfo cfg = vm.getConfig();
            if (cfg != null && cfg.isTemplate()) {
                templateList.add(vm);
            }
        }
        return templateList;
    }

    /**
     * 获取云主机创建时间
     *
     * @param mor
     * @param runtime
     * @return
     */
    public Date getVmCreateDate(ManagedObjectReference mor, VirtualMachineRuntimeInfo runtime) {
        try {
            // reference: http://asvignesh.in/how-to-get-a-virtual-machine-created-time/
            final EventFilterSpec eventFilterSpec = new EventFilterSpec();
            eventFilterSpec.setEventTypeId(new String[]{"VmCreatedEvent", "VmBeingDeployedEvent", "VmRegisteredEvent", "VmClonedEvent"});

            EventFilterSpecByEntity entity = new EventFilterSpecByEntity();
            entity.setEntity(mor);
            EventFilterSpecRecursionOption recOption = EventFilterSpecRecursionOption.self;
            entity.setRecursion(recOption);

            eventFilterSpec.setEntity(entity);
            final Event[] events = getSi().getEventManager().queryEvents(eventFilterSpec);
            if (events != null) {
                for (Event event : events) {
                    Calendar calendar = event.createdTime;
                    if (calendar == null) {
                        continue;
                    } else {
                        return calendar.getTime();
                    }
                }
            }
        } catch (Exception e) {
            // do nothing
        }
        try {
            if (runtime != null) {
                Calendar bootTime = runtime.getBootTime();
                if (bootTime != null) {
                    return bootTime.getTime();
                }
            }
        } catch (Exception e) {
            // do nothing
        }
        return null;
    }

    public VirtualMachine getTemplateFromAll(String name) {
        return getResourceFromAll(VirtualMachine.class, name);
    }

    /**
     * 获取云主机具有的磁盘
     *
     * @param vm 云主机实例
     * @return 云主机的磁盘列表
     */
    public List<VirtualDisk> getVirtualDisks(VirtualMachine vm) {
        List<VirtualDisk> disks = new ArrayList<>();
        VirtualMachineConfigInfo config = vm.getConfig();
        if (config != null) {
            VirtualHardware hardware = config.getHardware();
            VirtualDevice[] devices = hardware.getDevice();
            if (devices != null && devices.length > 0) {
                for (VirtualDevice device : devices) {
                    if (device instanceof VirtualDisk) {
                        disks.add((VirtualDisk) device);
                    }
                }
            }
        }
        return disks;
    }

    /**
     * 关闭电源
     *
     * @param uuid
     * @return
     */
    public boolean powerOff(String uuid) {
        VirtualMachine virtualMachine = getVirtualMachineByUuId(uuid);
        //电源已是关闭状态
        if (StringUtils.equalsIgnoreCase(VirtualMachinePowerState.poweredOff.name(), virtualMachine.getRuntime().getPowerState().name())) {
            throw new RuntimeException("The current state of the virtual machine is power off!");
        }
        try {
            Task task = virtualMachine.powerOffVM_Task();
            String result = task.waitForTask();
            if (!StringUtils.equalsIgnoreCase(TaskInfoState.success.name(), result)) {
                throw new RuntimeException("TaskInfo - " + task.getTaskInfo().getDescription());
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 开机
     *
     * @param uuid
     * @return
     */
    public boolean powerOn(String uuid) {
        VirtualMachine virtualMachine = getVirtualMachineByUuId(uuid);
        if (StringUtils.equalsIgnoreCase(VirtualMachinePowerState.poweredOn.name(), virtualMachine.getRuntime().getPowerState().name())) {
            throw new RuntimeException("The current state of the virtual machine is power on!");
        }
        try {
            HostSystem hostSystem = getHost(virtualMachine);
            Task task = virtualMachine.powerOnVM_Task(hostSystem);
            String result = task.waitForTask();
            if (!StringUtils.equalsIgnoreCase(TaskInfoState.success.name(), result)) {
                throw new RuntimeException("TaskInfo - " + task.getTaskInfo().getDescription());
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 关机
     *
     * @param uuid
     * @return
     */
    public boolean shutdownInstance(String uuid) {
        VirtualMachine virtualMachine = getVirtualMachineByUuId(uuid);
        if (StringUtils.equalsIgnoreCase(VirtualMachinePowerState.poweredOff.name(), virtualMachine.getRuntime().getPowerState().name())) {
            return true;
        }
        try {
            virtualMachine.shutdownGuest();
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 重启
     *
     * @param uuid
     * @return
     */
    public boolean reboot(String uuid) {
        VirtualMachine virtualMachine = getVirtualMachineByUuId(uuid);
        if (StringUtils.equalsIgnoreCase(VirtualMachinePowerState.poweredOff.name(), virtualMachine.getRuntime().getPowerState().name())) {
            throw new RuntimeException("The current state of the virtual machine is shutdown!");
        }
        try {
            virtualMachine.rebootGuest();
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 删除
     *
     * @param uuid
     * @return
     */
    public boolean deleteInstance(String uuid) {
        VirtualMachine virtualMachine = getVirtualMachineByUuId(uuid);
        if (StringUtils.equalsIgnoreCase(VirtualMachinePowerState.poweredOn.name(), virtualMachine.getRuntime().getPowerState().name())) {
            throw new RuntimeException("The current state of the virtual machine is running!");
        }
        try {
            Task task = virtualMachine.destroy_Task();
            String result = task.waitForTask();
            if (!StringUtils.equalsIgnoreCase(TaskInfoState.success.name(), result)) {
                throw new RuntimeException("TaskInfo - " + task.getTaskInfo().getDescription());
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 硬重启
     * 先关闭电源再打开
     *
     * @param uuid
     * @return
     */
    public boolean hardReboot(String uuid) {
        VirtualMachine virtualMachine = getVirtualMachineByUuId(uuid);
        if (StringUtils.equalsIgnoreCase(VirtualMachinePowerState.poweredOff.name(), virtualMachine.getRuntime().getPowerState().name())) {
            throw new RuntimeException("The current state of the virtual machine is shutdown!");
        }
        try {
            Task powerOffTask = virtualMachine.powerOffVM_Task();
            String powerOffResult = powerOffTask.waitForTask();
            if (!StringUtils.equalsIgnoreCase(TaskInfoState.success.name(), powerOffResult)) {
                throw new RuntimeException("hardReboot  powerOffTaskInfo - " + powerOffTask.getTaskInfo().getDescription());
            }
            HostSystem hostSystem = getHost(virtualMachine);
            Task powerOnTask = virtualMachine.powerOnVM_Task(hostSystem);
            String powerOnResult = powerOnTask.waitForTask();
            if (!StringUtils.equalsIgnoreCase(TaskInfoState.success.name(), powerOnResult)) {
                throw new RuntimeException("hardReboot  powerOnTaskInfo - " + powerOnTask.getTaskInfo().getDescription());
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }


    public VirtualMachine getVirtualMachineByMor(String mor) {
        if (mor != null) {
            List<VirtualMachine> vms = listVirtualMachines();
            for (VirtualMachine vm : vms) {
                if (vm.getMOR() == null || vm.getMOR().getVal() == null) {
                    continue;
                }
                if (mor.equals(vm.getMOR().getVal())) {
                    return vm;
                }
            }
        }
        return null;
    }

    public Datastore getDatastoreByMor(String datastoreMor) {
        List<Datastore> datastores = listDataStores();
        if (datastores != null) {
            for (Datastore datastore : datastores) {
                if (datastore.getMOR().getVal().equals(datastoreMor)) {
                    return datastore;
                }
            }
        }
        return null;
    }

    private boolean shutdownGuest(String instanceId) {
        try {
            VirtualMachine vm = getVirtualMachineById(instanceId);
            if (vm != null) {
                VirtualMachineRuntimeInfo runtime = vm.getRuntime();
                if (runtime != null) {
                    VirtualMachinePowerState state = runtime.getPowerState();
                    if (state == VirtualMachinePowerState.poweredOn) {
                        log.info("begin to shutdown vm'OS : " + instanceId);
                        vm.shutdownGuest();
                        int count = 0;
                        while (count++ < 60) {
                            Thread.sleep(1000 * 10);
                            String guestState = vm.getGuest().getGuestState();
                            log.info(guestState);
                            if ("NotRunning".equalsIgnoreCase(guestState)) {
                                return true;
                            }
                        }
                        return true;
                    } else {
                        return true;
                    }
                } else {
                    // TODO runtime?
                }
            }
        } catch (InvalidState e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new RuntimeException("invalid state!" + e.getLocalizedMessage(), e);
        } catch (Exception e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new RuntimeException("Error turning off the guest operating system!" + e.getLocalizedMessage(), e);
        }
        return false;
    }

    public boolean stopVm(String instanceId) {
        try {
            VirtualMachine vm = getVirtualMachineById(instanceId);
            if (vm != null) {
                VirtualMachineRuntimeInfo runtime = vm.getRuntime();

                if (runtime != null) {
                    VirtualMachinePowerState state = runtime.getPowerState();
                    if (state != VirtualMachinePowerState.poweredOff) {
                        log.info("begin to stop vm : " + instanceId);
                        Task task = vm.powerOffVM_Task();
                        String status = task.waitForTask();
                        if (!status.equals("") && !status.equals(Task.SUCCESS)) {
                            throw new RuntimeException("failed to stop vm : " + instanceId + ", errorMsg : " + task.getTaskInfo().getError().getLocalizedMessage());
                        } else {
                            log.info("success to stop vm : " + instanceId);
                            return true;
                        }
                    } else {
                        return true;
                    }
                } else {
                    // TODO runtime?
                }
            }
        } catch (InvalidState e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new RuntimeException("invalid state!" + e.getLocalizedMessage(), e);
        } catch (Exception e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new RuntimeException("failed to stop vm!" + e.getLocalizedMessage(), e);
        }
        return false;
    }

    public boolean startVm(String instanceId) {
        try {
            VirtualMachine vm = getVirtualMachineById(instanceId);
            if (vm != null) {
                VirtualMachineRuntimeInfo runtime = vm.getRuntime();
                if (runtime != null) {
                    VirtualMachinePowerState state = runtime.getPowerState();
                    if (state == VirtualMachinePowerState.poweredOff) {
                        log.info("begin to start vm : " + instanceId);
                        Task task = vm.powerOnVM_Task(getHost(vm));
                        String status = task.waitForTask();
                        if (!status.equals("") && !status.equals(Task.SUCCESS)) {
                            log.info("failed to start vm : " + instanceId + ", errorMsg : " + task.getTaskInfo().getError().getLocalizedMessage());
                            return false;
                        } else {
                            log.info("success to start vm : " + instanceId);
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            }
        } catch (VmConfigFault e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new RuntimeException("Virtual machine configuration error!" + e.getLocalizedMessage(), e);
        } catch (InvalidState e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new RuntimeException("Invalid state!" + e.getLocalizedMessage(), e);
        } catch (InvalidDatastore e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new RuntimeException("Invalid data storage settings!" + e.getLocalizedMessage(), e);
        } catch (Exception e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new RuntimeException("Error starting virtual machine!" + e.getLocalizedMessage(), e);
        }
        return false;
    }


    /**
     * 网络设置
     * //当在存储群集中创建云主机时，网络选择为模板的网络时需注意，当网络不合法时，创建云主机将会失败，错误提示形如：无法访问云主机配置，无法访问文件[datastore1 (35)]
     * //当时出现该错误提示时使用的模板的网络为vCenter的管理网络，不知道这个网络和datastore1 (35)有啥关系
     *
     * @param machineSpecs
     * @param template
     * @param network
     * @param networkCardName
     * @param isNetworkOn
     * @throws Exception
     */
    //todo 网卡需要根据传进来的改
    private void addNetwokSpec(List<VirtualDeviceConfigSpec> machineSpecs, VirtualMachine template, String network, String networkCardName, boolean isNetworkOn) throws Exception {
        Network net = getNetworkByMor(network);

        VirtualEthernetCard veCard = null;
        VirtualEthernetCard tmpCard = null;

        int controllerKey = 0;
        if (net == null) {
            System.out.println("cannot find network :::::::::: " + network);
        } else {
            VirtualDevice[] virtualDevices = template.getConfig().getHardware().getDevice();
            for (VirtualDevice virtualDevice : virtualDevices) {
                if (virtualDevice.getControllerKey() != null) {
                    controllerKey = virtualDevice.getControllerKey();
                }
                if (virtualDevice instanceof VirtualEthernetCard) {
                    tmpCard = (VirtualEthernetCard) virtualDevice;
                    if (FLAG_FOR_NULL_VALUE.equals(networkCardName) || (networkCardName != null && networkCardName.trim().length() > 0 && !networkCardName.equals(tmpCard.getDeviceInfo().getLabel()))) {
                        continue;
                    }
                    veCard = tmpCard;
                    break;
                }
            }
        }

        VirtualDeviceConfigSpec nicSpec = new VirtualDeviceConfigSpec();

        if (veCard == null) {
            veCard = new VirtualVmxnet3();

            nicSpec.setOperation(VirtualDeviceConfigSpecOperation.add);

            veCard.setControllerKey(tmpCard == null ? controllerKey : tmpCard.getControllerKey());

        } else {
            nicSpec.setOperation(VirtualDeviceConfigSpecOperation.edit);
            veCard.getDeviceInfo().setSummary(network);
        }

        ManagedObjectReference netMor = net.getMOR();

        if (netMor.getVal().startsWith("network")) {
            VirtualEthernetCardNetworkBackingInfo nicBacking = new VirtualEthernetCardNetworkBackingInfo();
            nicBacking.setDeviceName(net.getName());
            nicBacking.setNetwork(netMor);
            veCard.setBacking(nicBacking);
        } else {
            if (net instanceof DistributedVirtualPortgroup) {
                VirtualEthernetCardDistributedVirtualPortBackingInfo nicBacking = new VirtualEthernetCardDistributedVirtualPortBackingInfo();
                DistributedVirtualSwitchPortConnection connection = new DistributedVirtualSwitchPortConnection();
                //                                    connection.setPortgroupKey(netMor.getVal());
                connection.setPortgroupKey(((DistributedVirtualPortgroup) net).getKey());

                ManagedObjectReference mor = ((DistributedVirtualPortgroup) net).getConfig()
                        .getDistributedVirtualSwitch();
                DistributedVirtualSwitch dvs = new DistributedVirtualSwitch(net.getServerConnection(),
                        mor);
                connection.setSwitchUuid(dvs.getUuid());
                nicBacking.setPort(connection);
                veCard.setBacking(nicBacking);
            } else {
                System.out.println("unsupported network type :::::::::: " + net);
                return;
            }
        }

        VirtualDeviceConnectInfo connectInfo = new VirtualDeviceConnectInfo();
        connectInfo.setStartConnected(isNetworkOn);
        /*connectInfo.setAllowGuestControl(true);
        connectInfo.setConnected(false);
        connectInfo.setStatus(VirtualDeviceConnectInfoStatus.untried.name());*/
        veCard.setConnectable(connectInfo);

        nicSpec.setDevice(veCard);

        machineSpecs.add(nicSpec);
    }


}
