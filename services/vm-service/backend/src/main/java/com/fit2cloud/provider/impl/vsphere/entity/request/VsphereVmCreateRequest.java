package com.fit2cloud.provider.impl.vsphere.entity.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fit2cloud.common.form.annotaion.Form;
import com.fit2cloud.common.form.annotaion.FormConfirmInfo;
import com.fit2cloud.common.form.annotaion.FormGroupInfo;
import com.fit2cloud.common.form.annotaion.FormStepInfo;
import com.fit2cloud.common.form.constants.InputType;
import com.fit2cloud.common.provider.impl.vsphere.VsphereBaseCloudProvider;
import com.fit2cloud.provider.impl.vsphere.VsphereCloudProvider;
import com.fit2cloud.provider.impl.vsphere.util.ResourceConstants;
import com.fit2cloud.service.impl.VmCloudImageServiceImpl;
import lombok.Data;
import lombok.experimental.Accessors;
import com.fit2cloud.vm.ICreateServerRequest;

import java.util.List;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@FormStepInfo(step = 1, name = "基础配置")
@FormStepInfo(step = 2, name = "资源配置")
@FormStepInfo(step = 3, name = "网络配置")
@FormStepInfo(step = 4, name = "系统配置")
@FormConfirmInfo(group = 0, name = "云账号")
@FormConfirmInfo(group = 1, name = "基础配置")
@FormConfirmInfo(group = 2, name = "资源配置")
@FormConfirmInfo(group = 3, name = "网络配置")
@FormConfirmInfo(group = 4, name = "系统配置")
@FormGroupInfo(group = 1, name = "付费方式")
@FormGroupInfo(group = 10, name = "区域")
@FormGroupInfo(group = 20, name = "操作系统")
@FormGroupInfo(group = 30, name = "实例规格")
@FormGroupInfo(group = 40, name = "磁盘配置")
@FormGroupInfo(group = 50, name = "计算资源")
@FormGroupInfo(group = 60, name = "存储资源")
@FormGroupInfo(group = 70, name = "主机存放位置")
@FormGroupInfo(group = 80, name = "网络")
@FormGroupInfo(group = 90, name = "登录凭证")
@FormGroupInfo(group = 100, name = "主机命名")
public class VsphereVmCreateRequest extends VsphereVmBaseRequest implements ICreateServerRequest {

    @Form(inputType = InputType.LineNumber,
            label = "购买数量",
            unit = "台",
            defaultValue = "1",
            defaultJsonValue = true,
            attrs = "{\"min\":1,\"max\":10,\"step\":1}",
            confirmGroup = 1
    )
    private int count;

    private int index;

    /**
     * 数据库中ID
     */
    private String id;

    @Form(inputType = InputType.Radio,
            label = "付费方式",
            clazz = VsphereCloudProvider.class,
            method = "getChargeType",
            textField = "key",
            valueField = "value",
            defaultValue = "PostPaid",
            step = 1,
            group = 1,
            confirmGroup = 1
    )
    private String instanceChargeType;

    //step 1
    //数据中心datacenter
    @Form(inputType = InputType.Radio,
            label = "数据中心",
            clazz = VsphereBaseCloudProvider.class,
            method = "getRegions",
            textField = "name",
            valueField = "regionId",
            group = 10,
            step = 1,
            confirmGroup = 0
    )
    private String region;

    //集群
    @Form(inputType = InputType.VsphereClusterForm,
            label = "集群",
            clazz = VsphereCloudProvider.class,
            method = "getClusters",
            formatTextField = true,
            valueField = "name",
            relationTrigger = "region",
            group = 10,
            step = 1,
            confirmGroup = 0,
            textField = "${info}\n" +
                    "      <span style=\"color: var(--el-text-color-secondary); font-size: smaller\">\n" +
                    "        ${description}\n" +
                    "      </span>"
    )
    private String cluster;

    //模板
    @Form(inputType = InputType.VsphereTemplateSelectForm,
            label = "模板",
            clazz = VmCloudImageServiceImpl.class,
            serviceMethod = true,
            method = "listVmCloudImage",
            textField = "imageName",
            valueField = "imageName", //由于vc还是拿name作为快速索引，所以不用mor作为查询值
            relationTrigger = "region",
            propsInfo = "{\"style\":{\"width\":\"100%\"}}",
            attrs = "{\"placeholder\":\"请选择一个模板\"}",
            group = 20,
            step = 1,
            confirmGroup = 1
    )
    private String template;

    //cpu核数
    @Form(inputType = InputType.Number,
            label = "CPU",
            leftLabel = true,
            unit = "核",
            group = 30,
            step = 1,
            defaultValue = "1",
            defaultJsonValue = true,
            attrs = "{\"min\":1,\"max\":128,\"step\":1}",
            confirmGroup = 1
    )
    private int cpu;

    //内存GB
    @Form(inputType = InputType.Number,
            label = "内存",
            leftLabel = true,
            unit = "GB",
            group = 30,
            step = 1,
            defaultValue = "1",
            defaultJsonValue = true,
            attrs = "{\"min\":1,\"max\":512,\"step\":1}",
            confirmGroup = 1
    )
    private int ram;

    //磁盘配置
    @Form(inputType = InputType.VsphereDiskConfigForm,
            step = 1,
            group = 40,
            defaultValue = "[]",
            defaultJsonValue = true,
            relationTrigger = "template",
            confirmGroup = 1,
            confirmSpecial = true
    )
    private List<DiskConfig> disks;


    //step 2
    @Form(inputType = InputType.VsphereComputeConfigForm,
            step = 2,
            group = 50,
            defaultValue = "{\"location\": \"host\"}",
            defaultJsonValue = true,
            relationTrigger = "cluster",
            confirmGroup = 2,
            confirmSpecial = true
    )
    private ComputeConfig computeConfig;

    //
    @Form(inputType = InputType.Radio,
            label = "磁盘格式",
            propsInfo = "{\"radioType\":\"radio\",\"style\":{\"width\":\"100%\"}}",
            clazz = VsphereCloudProvider.class,
            method = "getDiskTypes",
            textField = "info",
            valueField = "value",
            defaultValue = "DEFAULT",
            step = 2,
            group = 60,
            confirmGroup = 2
    )
    private String diskType;

    //存储器
    @Form(inputType = InputType.VsphereDatastoreForm,
            label = "存储器",
            clazz = VsphereCloudProvider.class,
            method = "getDatastoreList",
            step = 2,
            group = 60,
            relationTrigger = "computeConfig",
            confirmGroup = 2,
            confirmSpecial = true
    )
    private String datastore;

    //文件夹
    @Form(inputType = InputType.SingleSelect,
            label = "文件夹",
            clazz = VsphereCloudProvider.class,
            method = "getFolders",
            textField = "name",
            valueField = "name", //由于vc还是拿name作为快速索引，所以不用mor作为查询值
            relationTrigger = "cluster",
            propsInfo = "{\"style\":{\"width\":\"100%\",\"height\":\"32px\"}}",
            attrs = "{\"placeholder\":\"请选择主机存放文件夹\"}",
            group = 70,
            step = 2,
            confirmGroup = 2
    )
    private String folder;


    //step 3
    //网卡
    @Form(inputType = InputType.VsphereNetworkAdapterForm,
            step = 3,
            group = 80,
            defaultValue = "[]",
            defaultJsonValue = true,
            relationTrigger = {"cluster", "computeConfig"},
            confirmGroup = 3,
            confirmSpecial = true,
            confirmPosition = Form.Position.TOP
    )
    private List<NetworkConfig> networkConfigs;

    @Form(inputType = InputType.SingleSelect,
            label = "时长",
            clazz = VsphereCloudProvider.class,
            method = "getPeriodOption",
            textField = "periodDisplayName",
            valueField = "period",
            defaultValue = "1",
            relationShowValues = "PrePaid",
            relationShows = "instanceChargeType",
            propsInfo = "{\"style\":{\"height\":\"30px\"}}",
            confirmGroup = 1
    )
    private String periodNum;

    @Form(inputType = InputType.LabelText,
            label = "配置费用",
            clazz = VsphereCloudProvider.class,
            method = "calculateConfigPrice",
            relationTrigger = {"count", "periodNum", "instanceChargeType", "cpu", "ram", "disks"},
            attrs = "{\"style\":\"color: red; font-size: large\"}",
            confirmGroup = 1,
            footerLocation = 1,
            confirmSpecial = true,
            required = false
    )
    private String configPrice;

    //step 4
    //云主机密码
    //todo
    @Form(inputType = InputType.VspherePasswordInfoForm,
            step = 4,
            group = 90,
            defaultValue = "",
            defaultJsonValue = true,
            confirmGroup = 4,
            confirmSpecial = true,
            confirmPosition = Form.Position.TOP
    )
    private PasswordObject passwordSetting;

    //step 4
    //云主机名称
    @Form(inputType = InputType.VsphereServerInfoForm,
            step = 4,
            group = 100,
            defaultValue = "[]",
            defaultJsonValue = true,
            confirmGroup = 4,
            confirmSpecial = true,
            confirmPosition = Form.Position.TOP
    )
    private List<ServerInfo> serverInfos;


    @Data
    @Accessors(chain = true)
    public static class DiskConfig {

        private Integer size;

        private boolean deleteWithInstance;

    }

    @Data
    @Accessors(chain = true)
    public static class ComputeConfig {
        //计算资源类型
        private String location;

        //主机/资源池的Mor
        //private String mor; //由于vc还是拿name作为快速索引，所以不用mor作为查询值

        //主机/资源池的名称
        private String name;

    }

    @Data
    @Accessors(chain = true)
    public static class NetworkConfig {
        private List<NetworkAdapter> adapters;

        private String dns1;
        private String dns2;

    }

    @Data
    @Accessors(chain = true)
    public static class NetworkAdapter {

        private String ipType = ResourceConstants.ipv4;

        private String vlan;

        private boolean dhcp;

        private String ipAddr;

        private String gateway;

        private String netmask;

        private String ipAddrV6;

        private String gatewayV6;

        private String netmaskV6;

    }

    @Data
    @Accessors(chain = true)
    public static class ServerInfo {

        private String name;

        private String hostname;

        private String remark;

        //username
        //password
        //hostname

        private boolean addDomain;
        private String domain;
        private String domainAdmin;
        private String domainPassword;

    }

    @Data
    @Accessors(chain = true)
    public static class PasswordObject {

        public enum TYPE {
            NONE, LINUX, WINDOWS
        }

        public static final String NEW_PASSWORD_PLACEHOLDER = "@[NEW_PASSWORD]";

        public static final String LOGIN_USER_PLACEHOLDER = "@[LOGIN_USER]";

        private TYPE type;

        private String imagePassword;

        private String imageUser;

        private String loginUser;

        private String loginPassword;

        private String script;

        private String programPath;

    }


}
