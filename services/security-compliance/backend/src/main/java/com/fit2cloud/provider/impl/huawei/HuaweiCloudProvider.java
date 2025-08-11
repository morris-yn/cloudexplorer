package com.fit2cloud.provider.impl.huawei;

import com.fit2cloud.common.provider.entity.F2CBalance;
import com.fit2cloud.common.provider.impl.huawei.HuaweiBaseCloudProvider;
import com.fit2cloud.common.provider.impl.huawei.entity.credential.HuaweiBaseCredential;
import com.fit2cloud.common.utils.JsonUtil;
import com.fit2cloud.constants.ResourceTypeConstants;
import com.fit2cloud.constants.SyncDimensionConstants;
import com.fit2cloud.es.entity.ResourceInstance;
import com.fit2cloud.provider.AbstractCloudProvider;
import com.fit2cloud.provider.ICloudProvider;
import com.fit2cloud.provider.entity.InstanceSearchField;
import com.fit2cloud.provider.impl.huawei.api.HuaweiApi;
import com.fit2cloud.provider.impl.huawei.api.HuaweiInstanceSearchFieldApi;
import com.fit2cloud.provider.impl.huawei.entity.request.*;
import com.fit2cloud.provider.impl.huawei.entity.response.BucketInstanceResponse;
import com.fit2cloud.provider.impl.huawei.entity.response.DiskInstanceResponse;
import com.fit2cloud.provider.util.ResourceUtil;
import com.huaweicloud.sdk.css.v1.model.ClusterList;
import com.huaweicloud.sdk.dcs.v2.model.InstanceListInfo;
import com.huaweicloud.sdk.dds.v3.model.QueryInstanceResponse;
import com.huaweicloud.sdk.ecs.v2.model.ServerDetail;
import com.huaweicloud.sdk.eip.v3.model.PublicipSingleShowResp;
import com.huaweicloud.sdk.elb.v3.model.LoadBalancer;
import com.huaweicloud.sdk.iam.v3.model.KeystoneListUsersResult;
import com.huaweicloud.sdk.iam.v3.model.LoginProtectResult;
import com.huaweicloud.sdk.rds.v3.model.InstanceResponse;
import com.huaweicloud.sdk.vpc.v3.model.SecurityGroup;
import com.huaweicloud.sdk.vpc.v3.model.SecurityGroupRule;
import com.huaweicloud.sdk.vpc.v3.model.Vpc;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
import org.apache.commons.lang3.StringUtils;
import org.pf4j.Extension;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2022/10/18  5:51 PM}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Extension
public class HuaweiCloudProvider extends AbstractCloudProvider<HuaweiBaseCredential> implements ICloudProvider {
    public final static HuaweiBaseCloudProvider huaweiBaseCloudProvider = new HuaweiBaseCloudProvider();

    public static final Info info = new Info("security-compliance", List.of(), Map.of());

    @Override
    public List<DefaultKeyValue<ResourceTypeConstants, SyncDimensionConstants>> getResourceSyncDimensionConstants() {
        return List.of(new DefaultKeyValue<>(ResourceTypeConstants.ECS, SyncDimensionConstants.REGION), new DefaultKeyValue<>(ResourceTypeConstants.REDIS, SyncDimensionConstants.REGION), new DefaultKeyValue<>(ResourceTypeConstants.MONGO_DB, SyncDimensionConstants.REGION), new DefaultKeyValue<>(ResourceTypeConstants.MYSQL, SyncDimensionConstants.REGION), new DefaultKeyValue<>(ResourceTypeConstants.SQL_SERVER, SyncDimensionConstants.REGION), new DefaultKeyValue<>(ResourceTypeConstants.POST_GRE_SQL, SyncDimensionConstants.REGION), new DefaultKeyValue<>(ResourceTypeConstants.ELASTIC_SEARCH, SyncDimensionConstants.REGION), new DefaultKeyValue<>(ResourceTypeConstants.DISK, SyncDimensionConstants.REGION), new DefaultKeyValue<>(ResourceTypeConstants.LOAD_BALANCER, SyncDimensionConstants.REGION), new DefaultKeyValue<>(ResourceTypeConstants.PUBLIC_IP, SyncDimensionConstants.REGION), new DefaultKeyValue<>(ResourceTypeConstants.VPC, SyncDimensionConstants.REGION), new DefaultKeyValue<>(ResourceTypeConstants.RAM, SyncDimensionConstants.CloudAccount), new DefaultKeyValue<>(ResourceTypeConstants.OSS, SyncDimensionConstants.CloudAccount), new DefaultKeyValue<>(ResourceTypeConstants.SECURITY_GROUP, SyncDimensionConstants.REGION));

    }

    @Override
    public List<ResourceInstance> listEcsInstance(String req) {
        ListEcsInstanceRequest listEcsInstanceRequest = JsonUtil.parseObject(req, ListEcsInstanceRequest.class);
        List<ServerDetail> serverDetails = HuaweiApi.listEcsInstance(listEcsInstanceRequest);
        return serverDetails.stream().map(instance -> ResourceUtil.toResourceInstance(huaweiBaseCloudProvider.getCloudAccountMeta().platform, ResourceTypeConstants.ECS, instance.getId(), instance.getName(), Map.of("os-extended-volumes:volumes_attached", instance.getOsExtendedVolumesVolumesAttached(), "tags", instance.getTags().stream().map(this::mapTag).filter(Objects::nonNull).toList()), instance)).toList();
    }

    private DefaultKeyValue<String, String> mapTag(String tag) {
        //业务环境=开发环境
        if (StringUtils.isNotEmpty(tag)) {
            String[] split = tag.split("=");
            if (split.length > 1) {
                return new DefaultKeyValue<>(split[0], split[1]);
            } else if (split.length > 0) {
                return new DefaultKeyValue<>(split[0], split[0]);
            }
        }
        return null;
    }

    @Override
    public List<InstanceSearchField> listEcsInstanceSearchField() {
        return HuaweiInstanceSearchFieldApi.listEcsInstanceSearchField();
    }

    @Override
    public List<ResourceInstance> listRedisInstance(String req) {
        ListRedisInstanceRequest listRedisInstanceRequest = JsonUtil.parseObject(req, ListRedisInstanceRequest.class);
        List<InstanceListInfo> instanceListInfos = HuaweiApi.listRedisInstance(listRedisInstanceRequest);
        return instanceListInfos.stream().map(instance -> ResourceUtil.toResourceInstance(huaweiBaseCloudProvider.getCloudAccountMeta().platform, ResourceTypeConstants.REDIS, instance.getInstanceId(), instance.getName(), Map.of("tags", instance.getTags()), instance)).toList();
    }

    @Override
    public List<InstanceSearchField> listRedisInstanceSearchField() {
        return HuaweiInstanceSearchFieldApi.listRedisInstanceSearchField();
    }

    @Override
    public List<ResourceInstance> listMongodbInstance(String req) {
        ListMongodbInstanceRequest listMongodbInstanceRequest = JsonUtil.parseObject(req, ListMongodbInstanceRequest.class);
        List<QueryInstanceResponse> queryInstanceResponses = HuaweiApi.listMongodbInstance(listMongodbInstanceRequest);
        return queryInstanceResponses.stream().map(instance -> ResourceUtil.toResourceInstance(huaweiBaseCloudProvider.getCloudAccountMeta().platform, ResourceTypeConstants.MONGO_DB, instance.getId(), instance.getName(), Map.of("tags", instance.getTags()), instance)).toList();
    }

    @Override
    public List<InstanceSearchField> listMongodbInstanceSearchField() {
        return HuaweiInstanceSearchFieldApi.listMongodbInstanceSearchField();
    }

    @Override
    public List<ResourceInstance> listMysqlInstance(String req) {
        ListRdsInstanceRequest listRdsInstanceRequest = JsonUtil.parseObject(req, ListRdsInstanceRequest.class);
        List<InstanceResponse> instanceResponses = HuaweiApi.listMysqlInstance(listRdsInstanceRequest);
        return instanceResponses.stream().map(instance -> ResourceUtil.toResourceInstance(huaweiBaseCloudProvider.getCloudAccountMeta().platform, ResourceTypeConstants.MYSQL, instance.getId(), instance.getName(), Map.of("tags", instance.getTags()), instance)).toList();
    }

    @Override
    public List<InstanceSearchField> listMysqlInstanceSearchField() {
        return HuaweiInstanceSearchFieldApi.listMysqlInstanceSearchField();
    }

    @Override
    public List<ResourceInstance> listSqlServerInstance(String req) {
        ListRdsInstanceRequest listRdsInstanceRequest = JsonUtil.parseObject(req, ListRdsInstanceRequest.class);
        List<InstanceResponse> instanceResponses = HuaweiApi.listSqlServer(listRdsInstanceRequest);
        return instanceResponses.stream().map(instance -> ResourceUtil.toResourceInstance(huaweiBaseCloudProvider.getCloudAccountMeta().platform, ResourceTypeConstants.SQL_SERVER, instance.getId(), instance.getName(), Map.of("tags", instance.getTags()), instance)).toList();
    }

    @Override
    public List<InstanceSearchField> listSqlServerInstanceSearchField() {
        return HuaweiInstanceSearchFieldApi.listSqlServerInstanceSearchField();
    }

    @Override
    public List<ResourceInstance> listPostGreSqlInstance(String req) {
        ListRdsInstanceRequest listRdsInstanceRequest = JsonUtil.parseObject(req, ListRdsInstanceRequest.class);
        List<InstanceResponse> instanceResponses = HuaweiApi.listPostGreSqlInstance(listRdsInstanceRequest);
        return instanceResponses.stream().map(instance -> ResourceUtil.toResourceInstance(huaweiBaseCloudProvider.getCloudAccountMeta().platform, ResourceTypeConstants.POST_GRE_SQL, instance.getId(), instance.getName(), Map.of("tags", instance.getTags()), instance)).toList();

    }

    @Override
    public List<InstanceSearchField> listPostGreSqlInstanceSearchField() {
        return HuaweiInstanceSearchFieldApi.listPostGreSqlInstanceSearchField();
    }

    @Override
    public List<ResourceInstance> listMariaDBInstance(String req) {
        return List.of();
    }

    @Override
    public List<InstanceSearchField> listMariaDBInstanceSearchField() {
        return List.of();
    }

    @Override
    public List<ResourceInstance> listElasticSearchInstance(String req) {
        ListElasticSearchInstanceRequest listElasticSearchInstanceRequest = JsonUtil.parseObject(req, ListElasticSearchInstanceRequest.class);
        List<ClusterList> clusterLists = HuaweiApi.listElasticSearchInstance(listElasticSearchInstanceRequest);
        return clusterLists.stream().map(instance -> ResourceUtil.toResourceInstance(huaweiBaseCloudProvider.getCloudAccountMeta().platform, ResourceTypeConstants.ELASTIC_SEARCH, instance.getId(), instance.getName(), Map.of("tags", instance.getTags()), instance)).toList();
    }

    @Override
    public List<InstanceSearchField> listElasticSearchInstanceSearchField() {
        return HuaweiInstanceSearchFieldApi.listElasticSearchInstanceSearchField();
    }

    @Override
    public List<ResourceInstance> listDiskInstance(String req) {
        ListDiskInstanceRequest listDiskInstanceRequest = JsonUtil.parseObject(req, ListDiskInstanceRequest.class);
        List<DiskInstanceResponse> volumeDetails = HuaweiApi.listDiskInstanceCollection(listDiskInstanceRequest);
        return volumeDetails.stream().map(instance -> ResourceUtil.toResourceInstance(huaweiBaseCloudProvider.getCloudAccountMeta().platform, ResourceTypeConstants.DISK, instance.getId(), instance.getName(), Map.of("tags", instance.getTags().entrySet().stream().map(entry -> new DefaultKeyValue<>(entry.getKey(), entry.getValue())).toList()), instance)).toList();
    }

    @Override
    public List<InstanceSearchField> listDiskInstanceSearchField() {
        return HuaweiInstanceSearchFieldApi.listDiskInstanceSearchField();
    }

    @Override
    public List<ResourceInstance> listLoadBalancerInstance(String req) {
        ListLoadBalancerInstanceRequest listLoadBalancerInstanceRequest = JsonUtil.parseObject(req, ListLoadBalancerInstanceRequest.class);
        List<LoadBalancer> loadBalancers = HuaweiApi.listLoadBalancerInstance(listLoadBalancerInstanceRequest);
        return loadBalancers.stream().map(instance -> ResourceUtil.toResourceInstance(huaweiBaseCloudProvider.getCloudAccountMeta().platform, ResourceTypeConstants.LOAD_BALANCER, instance.getId(), instance.getName(), Map.of("tags", instance.getTags()), instance)).toList();
    }

    @Override
    public List<InstanceSearchField> listLoadBalancerInstanceSearchField() {
        return HuaweiInstanceSearchFieldApi.listLoadBalancerInstanceSearchField();
    }

    @Override
    public List<ResourceInstance> listPublicIpInstance(String req) {
        ListPublicIpInstanceRequest listPublicIpInstanceRequest = JsonUtil.parseObject(req, ListPublicIpInstanceRequest.class);
        List<PublicipSingleShowResp> publicIpSingleShowList = HuaweiApi.listPublicIpInstance(listPublicIpInstanceRequest);
        return publicIpSingleShowList.stream().map(instance -> ResourceUtil.toResourceInstance(huaweiBaseCloudProvider.getCloudAccountMeta().platform, ResourceTypeConstants.PUBLIC_IP, instance.getId(), instance.getAlias(), Map.of("tags", instance.getTags().stream().map(this::mapTag).filter(Objects::nonNull).toList()), instance)).toList();
    }

    @Override
    public List<InstanceSearchField> listPublicIpInstanceSearchField() {
        return HuaweiInstanceSearchFieldApi.listPublicIpInstanceSearchField();
    }

    @Override
    public List<ResourceInstance> listVpcInstance(String req) {
        ListVpcInstanceRequest listVpcInstanceRequest = JsonUtil.parseObject(req, ListVpcInstanceRequest.class);
        List<Vpc> instances = HuaweiApi.listVpcInstance(listVpcInstanceRequest);
        return instances.stream().map(instance -> ResourceUtil.toResourceInstance(huaweiBaseCloudProvider.getCloudAccountMeta().platform, ResourceTypeConstants.VPC, instance.getId(), instance.getName(), Map.of("tags", instance.getTags()), instance)).toList();
    }

    @Override
    public List<InstanceSearchField> listVpcInstanceSearchField() {
        return HuaweiInstanceSearchFieldApi.listVpcInstanceSearchField();
    }

    @Override
    public List<ResourceInstance> listRamInstance(String req) {
        ListRamInstanceRequest listRamInstanceRequest = JsonUtil.parseObject(req, ListRamInstanceRequest.class);
        List<KeystoneListUsersResult> instances = HuaweiApi.listRamInstance(listRamInstanceRequest);
        ListLoginProfileInstanceRequest listLoginProfileInstanceRequest = new ListLoginProfileInstanceRequest();
        listLoginProfileInstanceRequest.setCredential(listRamInstanceRequest.getCredential());
        List<LoginProtectResult> loginProtectResults = HuaweiApi.listLoginProfileInstance(listLoginProfileInstanceRequest);
        return instances.stream().map(instance -> ResourceUtil.toResourceInstance(huaweiBaseCloudProvider.getCloudAccountMeta().platform, ResourceTypeConstants.RAM, instance.getId(), instance.getName(), instance, "loginProfile", loginProtectResults.stream().filter(l -> l.getUserId().equals(instance.getId())).findFirst().orElse(new LoginProtectResult().withEnabled(false)))).toList();
    }

    @Override
    public List<InstanceSearchField> listRamInstanceSearchField() {
        return HuaweiInstanceSearchFieldApi.listRamInstanceSearchField();
    }

    @Override
    public List<ResourceInstance> listBucketInstance(String req) {
        ListBucketInstanceRequest listBucketInstanceRequest = JsonUtil.parseObject(req, ListBucketInstanceRequest.class);
        List<BucketInstanceResponse> instances = HuaweiApi.listBucketInstance(listBucketInstanceRequest);
        return instances.stream().map(instance -> ResourceUtil.toResourceInstance(huaweiBaseCloudProvider.getCloudAccountMeta().platform, ResourceTypeConstants.OSS, instance.getBucketName(), instance.getBucketName(), instance)).toList();
    }

    @Override
    public List<InstanceSearchField> listBucketInstanceSearchField() {
        return HuaweiInstanceSearchFieldApi.listOssInstanceSearchField();
    }

    @Override
    public List<ResourceInstance> listSecurityGroupInstance(String req) {
        ListSecurityGroupInstanceRequest listSecurityGroupInstanceRequest = JsonUtil.parseObject(req, ListSecurityGroupInstanceRequest.class);
        List<SecurityGroup> instances = HuaweiApi.listSecurityGroupInstance(listSecurityGroupInstanceRequest);
        ListSecurityGroupRuleInstanceRequest listSecurityGroupRuleInstanceRequest = new ListSecurityGroupRuleInstanceRequest();
        BeanUtils.copyProperties(listSecurityGroupInstanceRequest, listSecurityGroupRuleInstanceRequest);
        List<SecurityGroupRule> securityGroupRules = HuaweiApi.listSecurityGroupRuleInstance(listSecurityGroupRuleInstanceRequest);
        return instances.stream().map(instance -> ResourceUtil.toResourceInstance(huaweiBaseCloudProvider.getCloudAccountMeta().platform, ResourceTypeConstants.SECURITY_GROUP, instance.getId(), instance.getName(), Map.of("group_rule", securityGroupRules.stream().filter(rule -> StringUtils.equals(rule.getSecurityGroupId(), instance.getId())).toList()), instance)).toList();
    }

    @Override
    public List<InstanceSearchField> listSecurityGroupInstanceSearchField() {
        return HuaweiInstanceSearchFieldApi.listSecurityGroupInstanceSearchField();
    }

    @Override
    public List<ResourceInstance> listHostInstance(String req) {
        return List.of();
    }

    @Override
    public List<InstanceSearchField> listHostInstanceSearchField() {
        return List.of();
    }

    @Override
    public List<ResourceInstance> listDataStoreInstance(String req) {
        return List.of();
    }

    @Override
    public List<InstanceSearchField> listDataStoreInstanceSearchField() {
        return List.of();
    }

    @Override
    public List<ResourceInstance> listResourcePoolInstance(String req) {
        return List.of();
    }

    @Override
    public List<InstanceSearchField> listResourcePoolInstanceSearchField() {
        return List.of();
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
