package com.fit2cloud.provider.impl.openstack.entity.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fit2cloud.common.form.annotaion.Form;
import com.fit2cloud.common.form.annotaion.FormConfirmInfo;
import com.fit2cloud.common.form.annotaion.FormGroupInfo;
import com.fit2cloud.common.form.annotaion.FormStepInfo;
import com.fit2cloud.common.form.constants.InputType;
import com.fit2cloud.common.provider.impl.openstack.OpenStackBaseCloudProvider;
import com.fit2cloud.common.provider.impl.openstack.entity.request.OpenStackBaseRequest;
import com.fit2cloud.provider.impl.openstack.OpenStackCloudProvider;
import com.fit2cloud.service.impl.VmCloudImageServiceImpl;
import lombok.Data;
import lombok.experimental.Accessors;
import com.fit2cloud.vm.ICreateServerRequest;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@FormStepInfo(step = 1, name = "基础配置")
@FormStepInfo(step = 2, name = "网络配置")
@FormStepInfo(step = 3, name = "系统配置")
@FormConfirmInfo(group = 0, name = "云账号")
@FormConfirmInfo(group = 1, name = "基础配置")
@FormConfirmInfo(group = 2, name = "网络配置")
@FormConfirmInfo(group = 3, name = "系统配置")
@FormGroupInfo(group = 1, name = "付费方式")
@FormGroupInfo(group = 10, name = "区域")
@FormGroupInfo(group = 20, name = "操作系统")
@FormGroupInfo(group = 30, name = "实例规格")
@FormGroupInfo(group = 40, name = "磁盘配置")
@FormGroupInfo(group = 50, name = "网络")
@FormGroupInfo(group = 60, name = "安全组")
@FormGroupInfo(group = 70, name = "登录凭证")
@FormGroupInfo(group = 80, name = "主机命名")
public class OpenStackServerCreateRequest extends OpenStackBaseRequest implements ICreateServerRequest {

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
            clazz = OpenStackCloudProvider.class,
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
    //区域
    @Form(inputType = InputType.Radio,
            label = "区域",
            clazz = OpenStackBaseCloudProvider.class,
            method = "getRegions",
            textField = "name",
            valueField = "regionId",
            group = 10,
            step = 1,
            confirmGroup = 0
    )
    private String region;

    @Form(inputType = InputType.Radio,
            label = "可用区",
            clazz = OpenStackBaseCloudProvider.class,
            method = "getZones",
            textField = "name",
            valueField = "id",
            relationTrigger = {"region"},
            group = 10,
            step = 1,
            confirmGroup = 0
    )
    private String zone;

    //镜像
    @Form(inputType = InputType.SingleSelect,
            label = "镜像",
            clazz = VmCloudImageServiceImpl.class,
            serviceMethod = true,
            method = "listVmCloudImage",
            textField = "imageName",
            valueField = "imageId",
            relationTrigger = {"region"},
            propsInfo = "{\"style\":{\"width\":\"100%\",\"height\":\"32px\"}}",
            attrs = "{\"placeholder\":\"请选择一个镜像\"}",
            group = 20,
            step = 1,
            confirmGroup = 1
    )
    private String imageId;

    @Form(inputType = InputType.OpenStackFlavorForm,
            label = "实例规格",
            clazz = OpenStackCloudProvider.class,
            method = "getFlavors",
            relationTrigger = {"region", "imageId"},
            hideLabel = true,
            group = 30,
            step = 1,
            textField = "name",
            valueField = "id",
            confirmGroup = 1
    )
    private String flavorId;


    @Form(inputType = InputType.SwitchBtn,
            label = "创建系统盘",
            defaultValue = "true",
            defaultJsonValue = true,
            attrs = "{\"active-text\":\"是\",\"inactive-text\":\"否\",\"inline-prompt\":true}",
//            clazz = OpenStackCloudProvider.class,
//            method = "getBooleanSelectList",
            group = 30,
            step = 1,
            confirmGroup = 1)
    private boolean bootFormVolume;

    //磁盘配置
    @Form(inputType = InputType.OpenStackDiskConfigForm,
            step = 1,
            group = 40,
            defaultValue = "[]",
            defaultJsonValue = true,
            confirmGroup = 1,
            confirmSpecial = true
    )
    private List<DiskConfig> disks;


    @Form(inputType = InputType.OpenStackNetworkConfigForm,
            label = "网络",
            step = 2,
            group = 50,
            hideLabel = true,
            defaultValue = "[]",
            defaultJsonValue = true,
            relationTrigger = {"region"},
            clazz = OpenStackCloudProvider.class,
            method = "getNetworks",
            textField = "name",
            valueField = "id",
            confirmGroup = 2
    )
    private List<String> networks;


    @Form(inputType = InputType.TableCheckbox,
            label = "安全组",
            step = 2,
            group = 60,
            relationTrigger = {"region"},
            clazz = OpenStackCloudProvider.class,
            method = "getSecurityGroups",
            textField = "name",
            valueField = "id",
            propsInfo = "{\"style\":{\"width\":\"100%\",\"height\":\"400px\"},\"showLabel\":false,\"activeMsg\":\"已选安全组\",\"title\":\"选择安全组\",\"tableColumns\":[{\"property\":\"name\",\"label\":\"安全组名称\",\"min-width\":\"120px\"},{\"property\":\"description\",\"label\":\"描述\"}]}",
            confirmGroup = 2
    )
    private List<String> securityGroups;

    @Form(inputType = InputType.SingleSelect,
            label = "时长",
            clazz = OpenStackCloudProvider.class,
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
            clazz = OpenStackCloudProvider.class,
            method = "calculateConfigPrice",
            relationTrigger = {"count", "periodNum", "instanceChargeType", "flavorId", "disks"},
            attrs = "{\"style\":\"color: red; font-size: large\"}",
            confirmGroup = 1,
            footerLocation = 1,
            confirmSpecial = true,
            required = false
    )
    private String configPrice;

    @Form(inputType = InputType.Radio,
            step = 3,
            group = 70,
            label = "登录方式",
            clazz = OpenStackCloudProvider.class,
            method = "getLoginModes",
            textField = "name",
            valueField = "id",
            confirmGroup = 3,
            defaultValue = "password"
    )
    private String loginMode;

    @Form(inputType = InputType.Password,
            step = 3,
            group = 70,
            label = "密码",
            defaultJsonValue = true,
            relationTrigger = {"loginMode"}
    )
    private String password;

    //step 3
    //云主机
    @Form(inputType = InputType.OpenStackServerInfoForm,
            step = 3,
            group = 80,
            defaultValue = "[]",
            defaultJsonValue = true,
            confirmGroup = 3,
            confirmSpecial = true,
            confirmPosition = Form.Position.TOP
    )
    private List<ServerInfo> serverInfos;

    @Data
    @Accessors(chain = true)
    public static class DiskConfig {

        private Integer size;

        private boolean deleteWithInstance;

        private boolean boot;

        private String volumeType;

    }

    @Data
    @Accessors(chain = true)
    public static class ServerInfo {

        private String name;

    }
}
