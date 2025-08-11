package com.fit2cloud.provider.impl.vsphere;

import com.fit2cloud.common.provider.entity.F2CBalance;
import com.fit2cloud.common.provider.impl.vsphere.VsphereBaseCloudProvider;
import com.fit2cloud.common.utils.JsonUtil;
import com.fit2cloud.constants.ResourceTypeConstants;
import com.fit2cloud.constants.SyncDimensionConstants;
import com.fit2cloud.es.entity.ResourceInstance;
import com.fit2cloud.provider.AbstractCloudProvider;
import com.fit2cloud.provider.ICloudProvider;
import com.fit2cloud.provider.entity.InstanceSearchField;
import com.fit2cloud.provider.impl.vsphere.api.VsphereApi;
import com.fit2cloud.provider.impl.vsphere.api.VsphereInstanceSearchApi;
import com.fit2cloud.provider.impl.vsphere.entity.credential.VsphereComplianceCredential;
import com.fit2cloud.provider.impl.vsphere.entity.request.ListDataStoreInstanceRequest;
import com.fit2cloud.provider.impl.vsphere.entity.request.ListEcsInstanceRequest;
import com.fit2cloud.provider.impl.vsphere.entity.request.ListHostInstanceRequest;
import com.fit2cloud.provider.impl.vsphere.entity.request.ListResourcePoolRequest;
import com.fit2cloud.provider.util.ResourceUtil;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
import org.pf4j.Extension;

import java.util.List;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2023/2/6  10:27}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Extension
public class VsphereProvider extends AbstractCloudProvider<VsphereComplianceCredential> implements ICloudProvider {
    public static final VsphereBaseCloudProvider vsphereBaseCloudProvider = new VsphereBaseCloudProvider();
    public static final Info info = new Info("security-compliance", List.of(), Map.of());

    @Override
    public List<DefaultKeyValue<ResourceTypeConstants, SyncDimensionConstants>> getResourceSyncDimensionConstants() {
        return List.of(new DefaultKeyValue<>(ResourceTypeConstants.ECS, SyncDimensionConstants.REGION),
                new DefaultKeyValue<>(ResourceTypeConstants.DATA_STORE, SyncDimensionConstants.REGION),
                new DefaultKeyValue<>(ResourceTypeConstants.RESOURCE_POOL, SyncDimensionConstants.REGION),
                new DefaultKeyValue<>(ResourceTypeConstants.HOST, SyncDimensionConstants.REGION));
    }

    @Override
    public List<ResourceInstance> listEcsInstance(String req) {
        ListEcsInstanceRequest request = JsonUtil.parseObject(req, ListEcsInstanceRequest.class);
        return VsphereApi.listVirtualMachine(request).stream()
                .map(resource -> ResourceUtil.
                        toResourceInstance(getCloudAccountMeta().platform, ResourceTypeConstants.ECS,
                                resource.getInstanceId(), resource.getName(), resource)).toList();
    }

    @Override
    public List<InstanceSearchField> listEcsInstanceSearchField() {
        return VsphereInstanceSearchApi.listEcsInstanceSearchField();
    }

    @Override
    public List<ResourceInstance> listRedisInstance(String req) {
        return List.of();
    }

    @Override
    public List<InstanceSearchField> listRedisInstanceSearchField() {
        return List.of();
    }

    @Override
    public List<ResourceInstance> listMongodbInstance(String req) {
        return List.of();
    }

    @Override
    public List<InstanceSearchField> listMongodbInstanceSearchField() {
        return List.of();
    }

    @Override
    public List<ResourceInstance> listMysqlInstance(String req) {
        return List.of();
    }

    @Override
    public List<InstanceSearchField> listMysqlInstanceSearchField() {
        return List.of();
    }

    @Override
    public List<ResourceInstance> listSqlServerInstance(String req) {
        return List.of();
    }

    @Override
    public List<InstanceSearchField> listSqlServerInstanceSearchField() {
        return List.of();
    }

    @Override
    public List<ResourceInstance> listPostGreSqlInstance(String req) {
        return List.of();
    }

    @Override
    public List<InstanceSearchField> listPostGreSqlInstanceSearchField() {
        return List.of();
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
        return List.of();
    }

    @Override
    public List<InstanceSearchField> listElasticSearchInstanceSearchField() {
        return List.of();
    }

    @Override
    public List<ResourceInstance> listDiskInstance(String req) {
        return List.of();
    }

    @Override
    public List<InstanceSearchField> listDiskInstanceSearchField() {
        return List.of();
    }

    @Override
    public List<ResourceInstance> listLoadBalancerInstance(String req) {
        return List.of();
    }

    @Override
    public List<InstanceSearchField> listLoadBalancerInstanceSearchField() {
        return List.of();
    }

    @Override
    public List<ResourceInstance> listPublicIpInstance(String req) {
        return List.of();
    }

    @Override
    public List<InstanceSearchField> listPublicIpInstanceSearchField() {
        return List.of();
    }

    @Override
    public List<ResourceInstance> listVpcInstance(String req) {
        return List.of();
    }

    @Override
    public List<InstanceSearchField> listVpcInstanceSearchField() {
        return List.of();
    }

    @Override
    public List<ResourceInstance> listRamInstance(String req) {
        return List.of();
    }

    @Override
    public List<InstanceSearchField> listRamInstanceSearchField() {
        return List.of();
    }

    @Override
    public List<ResourceInstance> listBucketInstance(String req) {
        return List.of();
    }

    @Override
    public List<InstanceSearchField> listBucketInstanceSearchField() {
        return List.of();
    }

    @Override
    public List<ResourceInstance> listSecurityGroupInstance(String req) {
        return List.of();
    }

    @Override
    public List<InstanceSearchField> listSecurityGroupInstanceSearchField() {
        return List.of();
    }

    @Override
    public List<ResourceInstance> listHostInstance(String req) {
        ListHostInstanceRequest request = JsonUtil.parseObject(req, ListHostInstanceRequest.class);
        return VsphereApi.listHost(request).stream()
                .map(resource -> ResourceUtil.
                        toResourceInstance(getCloudAccountMeta().platform, ResourceTypeConstants.HOST,
                                resource.getHostId(), resource.getHostName(), resource)).toList();
    }

    @Override
    public List<InstanceSearchField> listHostInstanceSearchField() {
        return VsphereInstanceSearchApi.listHostInstanceSearchField();
    }

    @Override
    public List<ResourceInstance> listDataStoreInstance(String req) {
        ListDataStoreInstanceRequest request = JsonUtil.parseObject(req, ListDataStoreInstanceRequest.class);
        return VsphereApi.listDataStore(request).stream()
                .map(resource -> ResourceUtil.
                        toResourceInstance(getCloudAccountMeta().platform, ResourceTypeConstants.DATA_STORE,
                                resource.getDataStoreId(), resource.getDataStoreName(), resource)).toList();
    }

    @Override
    public List<InstanceSearchField> listDataStoreInstanceSearchField() {
        return VsphereInstanceSearchApi.listDataStoreInstanceSearchField();
    }

    @Override
    public List<ResourceInstance> listResourcePoolInstance(String req) {
        ListResourcePoolRequest request = JsonUtil.parseObject(req, ListResourcePoolRequest.class);
        return VsphereApi.listResourcePool(request).stream()
                .map(resource -> ResourceUtil.
                        toResourceInstance(getCloudAccountMeta().platform, ResourceTypeConstants.RESOURCE_POOL,
                                resource.getMor(), resource.getName(), resource)).toList();
    }

    @Override
    public List<InstanceSearchField> listResourcePoolInstanceSearchField() {
        return VsphereInstanceSearchApi.listResourcePoolInstanceSearchField();
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
}
