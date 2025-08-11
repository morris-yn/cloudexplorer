package com.fit2cloud.provider.impl.tencent.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fit2cloud.common.provider.exception.ReTryException;
import com.fit2cloud.common.provider.exception.SkipPageException;
import com.fit2cloud.common.provider.util.PageUtil;
import com.fit2cloud.common.utils.JsonUtil;
import com.fit2cloud.provider.impl.tencent.client.CeCosClient;
import com.fit2cloud.provider.impl.tencent.entity.request.*;
import com.fit2cloud.provider.impl.tencent.entity.response.BucketEncryptionResponse;
import com.fit2cloud.provider.impl.tencent.entity.response.BucketInstanceResponse;
import com.fit2cloud.provider.impl.tencent.entity.response.RamInstanceResponse;
import com.fit2cloud.provider.impl.tencent.entity.response.SecurityGroupInstanceResponse;
import com.fit2cloud.provider.impl.tencent.parser.CeXmlResponseSaxParser;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.http.CosHttpRequest;
import com.qcloud.cos.http.CosHttpResponse;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpResponseHandler;
import com.qcloud.cos.internal.CosServiceResponse;
import com.qcloud.cos.model.AccessControlList;
import com.qcloud.cos.model.Bucket;
import com.qcloud.cos.model.BucketRefererConfiguration;
import com.qcloud.cos.model.GenericBucketRequest;
import com.tencentcloudapi.cam.v20190116.CamClient;
import com.tencentcloudapi.cam.v20190116.models.DescribeSafeAuthFlagCollRequest;
import com.tencentcloudapi.cam.v20190116.models.DescribeSafeAuthFlagCollResponse;
import com.tencentcloudapi.cam.v20190116.models.ListUsersResponse;
import com.tencentcloudapi.cam.v20190116.models.SubAccountInfo;
import com.tencentcloudapi.cbs.v20170312.CbsClient;
import com.tencentcloudapi.cbs.v20170312.models.DescribeDisksRequest;
import com.tencentcloudapi.cbs.v20170312.models.Disk;
import com.tencentcloudapi.cdb.v20170320.CdbClient;
import com.tencentcloudapi.cdb.v20170320.models.InstanceInfo;
import com.tencentcloudapi.clb.v20180317.ClbClient;
import com.tencentcloudapi.clb.v20180317.models.DescribeLoadBalancersRequest;
import com.tencentcloudapi.clb.v20180317.models.LoadBalancer;
import com.tencentcloudapi.cvm.v20170312.CvmClient;
import com.tencentcloudapi.cvm.v20170312.models.DescribeInstancesRequest;
import com.tencentcloudapi.cvm.v20170312.models.Instance;
import com.tencentcloudapi.es.v20180416.EsClient;
import com.tencentcloudapi.mariadb.v20170312.MariadbClient;
import com.tencentcloudapi.mongodb.v20190725.MongodbClient;
import com.tencentcloudapi.mongodb.v20190725.models.DescribeDBInstancesRequest;
import com.tencentcloudapi.mongodb.v20190725.models.InstanceDetail;
import com.tencentcloudapi.postgres.v20170312.PostgresClient;
import com.tencentcloudapi.redis.v20180412.RedisClient;
import com.tencentcloudapi.redis.v20180412.models.InstanceSet;
import com.tencentcloudapi.sqlserver.v20180328.SqlserverClient;
import com.tencentcloudapi.sqlserver.v20180328.models.DBInstance;
import com.tencentcloudapi.vpc.v20170312.VpcClient;
import com.tencentcloudapi.vpc.v20170312.models.*;
import lombok.SneakyThrows;
import org.springframework.beans.BeanUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2022/12/6  12:01}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
public class TencentApi {
    /**
     * 获取腾讯云服务器实例列表
     *
     * @param request 请求对象
     * @return 腾讯云服务器实例列表
     */
    public static List<Instance> listEcsInstance(ListCvmInstanceRequest request) {
        CvmClient cvmClient = request.getCredential().getCvmClient(request.getRegionId());
        request.setOffset(PageUtil.DefaultCurrentPage.longValue() - 1);
        request.setLimit(PageUtil.DefaultPageSize.longValue());
        return PageUtil.page(request,
                req -> {
                    try {
                        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
                        BeanUtils.copyProperties(req, describeInstancesRequest);
                        return cvmClient.DescribeInstances(describeInstancesRequest);
                    } catch (Exception e) {
                        ReTryException.throwReTry(e);
                        SkipPageException.throwSkip(e);
                        throw new RuntimeException(e);
                    }
                },
                res -> Arrays.stream(res.getInstanceSet()).toList(),
                (req, res) -> req.getLimit() <= res.getInstanceSet().length,
                req -> req.setOffset(req.getOffset() + req.getLimit()));

    }

    /**
     * 获取腾讯云 redis实例列表
     *
     * @param request 请求对象
     * @return redis实例列表
     */
    @SneakyThrows
    public static List<InstanceSet> listRedisInstance(ListRedisInstanceRequest request) {
        RedisClient redisClient = request.getCredential().getRedisClient(request.getRegionId());
        request.setOffset(PageUtil.DefaultCurrentPage.longValue() - 1);
        request.setLimit(PageUtil.DefaultPageSize.longValue());
        return PageUtil.page(request,
                req -> {
                    try {
                        com.tencentcloudapi.redis.v20180412.models.DescribeInstancesRequest describeInstancesRequest = new com.tencentcloudapi.redis.v20180412.models.DescribeInstancesRequest();
                        BeanUtils.copyProperties(req, describeInstancesRequest);
                        return redisClient.DescribeInstances(describeInstancesRequest);
                    } catch (Exception e) {
                        ReTryException.throwReTry(e);
                        SkipPageException.throwSkip(e);
                        throw new RuntimeException(e);
                    }
                },
                res -> Arrays.stream(res.getInstanceSet()).toList(),
                (req, res) -> req.getLimit() <= res.getInstanceSet().length,
                req -> req.setOffset(req.getOffset() + req.getLimit()));

    }


    /**
     * 获取腾讯云 mongodb 实例列表
     *
     * @param request 请求对象
     * @return mongodb实例列表
     */
    public static List<InstanceDetail> listMongodbInstance(ListMongoDBInstanceRequest request) {
        MongodbClient mongodbClient = request.getCredential().getMongodbClient(request.getRegionId());
        request.setOffset(PageUtil.DefaultCurrentPage.longValue() - 1);
        request.setLimit(PageUtil.DefaultPageSize.longValue());
        return PageUtil.page(request,
                req -> {
                    try {
                        DescribeDBInstancesRequest describeDBInstancesRequest = new DescribeDBInstancesRequest();
                        BeanUtils.copyProperties(req, describeDBInstancesRequest);
                        return mongodbClient.DescribeDBInstances(describeDBInstancesRequest);
                    } catch (Exception e) {
                        ReTryException.throwReTry(e);
                        SkipPageException.throwSkip(e);
                        throw new RuntimeException(e);
                    }
                },
                res -> Arrays.stream(res.getInstanceDetails()).toList(),
                (req, res) -> req.getLimit() <= res.getInstanceDetails().length,
                req -> req.setOffset(req.getOffset() + req.getLimit()));

    }

    /**
     * 获取mysql 实例列表
     *
     * @param request 请求对象
     * @return mysql实例列表
     */
    public static List<InstanceInfo> listMysqlInstance(ListMysqlInstanceRequest request) {
        CdbClient cdbClient = request.getCredential().getCdbClient(request.getRegionId());
        request.setOffset(PageUtil.DefaultCurrentPage.longValue() - 1);
        request.setLimit(PageUtil.DefaultPageSize.longValue());
        return PageUtil.page(request,
                req -> {
                    try {
                        com.tencentcloudapi.cdb.v20170320.models.DescribeDBInstancesRequest describeDBInstancesRequest =
                                new com.tencentcloudapi.cdb.v20170320.models.DescribeDBInstancesRequest();
                        BeanUtils.copyProperties(req, describeDBInstancesRequest);
                        return cdbClient.DescribeDBInstances(describeDBInstancesRequest);
                    } catch (Exception e) {
                        ReTryException.throwReTry(e);
                        SkipPageException.throwSkip(e);
                        throw new RuntimeException(e);
                    }
                },
                res -> Arrays.stream(res.getItems()).toList(),
                (req, res) -> req.getLimit() <= res.getItems().length,
                req -> req.setOffset(req.getOffset() + req.getLimit()));
    }

    /**
     * 获取sqlServer 实例列表
     *
     * @param request 请求对象
     * @return sqlServer实例列表
     */
    public static List<DBInstance> listSqlServerInstance(ListSqlServerInstanceRequest request) {
        SqlserverClient sqlServerClient = request.getCredential().getSqlServerClient(request.getRegionId());
        request.setOffset(PageUtil.DefaultCurrentPage.longValue() - 1);
        request.setLimit(PageUtil.DefaultPageSize.longValue());
        return PageUtil.page(request,
                req -> {
                    try {
                        com.tencentcloudapi.sqlserver.v20180328.models.DescribeDBInstancesRequest describeDBInstancesRequest = new com.tencentcloudapi.sqlserver.v20180328.models.DescribeDBInstancesRequest();
                        BeanUtils.copyProperties(req, describeDBInstancesRequest);
                        return sqlServerClient.DescribeDBInstances(describeDBInstancesRequest);
                    } catch (Exception e) {
                        ReTryException.throwReTry(e);
                        SkipPageException.throwSkip(e);
                        throw new RuntimeException(e);
                    }
                },
                res -> Arrays.stream(res.getDBInstances()).toList(),
                (req, res) -> req.getLimit() <= res.getDBInstances().length,
                req -> req.setOffset(req.getOffset() + req.getLimit()));
    }

    /**
     * 获取 postgresSql实例列表
     *
     * @param request 请求对象
     * @return PostGreSql 实例列表
     */
    public static List<com.tencentcloudapi.postgres.v20170312.models.DBInstance> listPostGreSqlInstance(ListPostGreSqlInstanceRequest request) {
        PostgresClient postgresClient = request.getCredential().getPostgresClient(request.getRegionId());
        request.setOffset(PageUtil.DefaultCurrentPage.longValue() - 1);
        request.setLimit(PageUtil.DefaultPageSize.longValue());
        return PageUtil.page(request,
                req -> {
                    try {
                        com.tencentcloudapi.postgres.v20170312.models.DescribeDBInstancesRequest describeDBInstancesRequest = new com.tencentcloudapi.postgres.v20170312.models.DescribeDBInstancesRequest();
                        BeanUtils.copyProperties(req, describeDBInstancesRequest);
                        return postgresClient.DescribeDBInstances(describeDBInstancesRequest);
                    } catch (Exception e) {
                        ReTryException.throwReTry(e);
                        SkipPageException.throwSkip(e);
                        throw new RuntimeException(e);
                    }
                },
                res -> Arrays.stream(res.getDBInstanceSet()).toList(),
                (req, res) -> req.getLimit() <= res.getDBInstanceSet().length,
                req -> req.setOffset(req.getOffset() + req.getLimit()));
    }

    /**
     * 获取 MariaDB 实例列表
     *
     * @param request 请求对象
     * @return MariaDB 实例列表
     */
    public static List<com.tencentcloudapi.mariadb.v20170312.models.DBInstance> listMariaDBInstance(ListMariaDBInstanceRequest request) {
        MariadbClient mariadbClient = request.getCredential().getMariadbClient(request.getRegionId());
        request.setOffset(PageUtil.DefaultCurrentPage.longValue() - 1);
        request.setLimit(PageUtil.DefaultPageSize.longValue());
        return PageUtil.page(request,
                req -> {
                    try {
                        com.tencentcloudapi.mariadb.v20170312.models.DescribeDBInstancesRequest describeDBInstancesRequest = new com.tencentcloudapi.mariadb.v20170312.models.DescribeDBInstancesRequest();
                        BeanUtils.copyProperties(req, describeDBInstancesRequest);
                        return mariadbClient.DescribeDBInstances(describeDBInstancesRequest);
                    } catch (Exception e) {
                        ReTryException.throwReTry(e);
                        SkipPageException.throwSkip(e);
                        throw new RuntimeException(e);
                    }
                },
                res -> Arrays.stream(res.getInstances()).toList(),
                (req, res) -> req.getLimit() <= res.getInstances().length,
                req -> req.setOffset(req.getOffset() + req.getLimit()));
    }


    /**
     * 获取 Elasticsearch 实例列表
     *
     * @param request 请求对象
     * @return elasticsearch 实例列表
     */
    public static List<com.tencentcloudapi.es.v20180416.models.InstanceInfo> listElasticSearchInstance(ListElasticsearchInstanceRequest request) {
        EsClient esClient = request.getCredential().getEsClient(request.getRegionId());
        request.setOffset(PageUtil.DefaultCurrentPage.longValue() - 1);
        request.setLimit(PageUtil.DefaultPageSize.longValue());
        return PageUtil.page(request,
                req -> {
                    try {
                        com.tencentcloudapi.es.v20180416.models.DescribeInstancesRequest describeInstancesRequest = new com.tencentcloudapi.es.v20180416.models.DescribeInstancesRequest();
                        BeanUtils.copyProperties(req, describeInstancesRequest);
                        return esClient.DescribeInstances(describeInstancesRequest);
                    } catch (Exception e) {
                        ReTryException.throwReTry(e);
                        SkipPageException.throwSkip(e);
                        throw new RuntimeException(e);
                    }
                },
                res -> Arrays.stream(res.getInstanceList()).toList(),
                (req, res) -> req.getLimit() <= res.getInstanceList().length,
                req -> req.setOffset(req.getOffset() + req.getLimit()));

    }

    /**
     * 获取 云磁盘 实例列表
     *
     * @param request 请求对象
     * @return 磁盘实例列表
     */
    public static List<Disk> listDiskInstance(ListDiskInstanceRequest request) {
        CbsClient cbsClient = request.getCredential().getCbsClient(request.getRegionId());
        request.setReturnBindAutoSnapshotPolicy(true);
        request.setOffset(PageUtil.DefaultCurrentPage.longValue() - 1);
        request.setLimit(PageUtil.DefaultPageSize.longValue());
        return PageUtil.page(request,
                req -> {
                    try {
                        DescribeDisksRequest describeInstancesRequest = new DescribeDisksRequest();
                        BeanUtils.copyProperties(req, describeInstancesRequest);
                        return cbsClient.DescribeDisks(describeInstancesRequest);
                    } catch (Exception e) {
                        ReTryException.throwReTry(e);
                        SkipPageException.throwSkip(e);
                        throw new RuntimeException(e);
                    }
                },
                res -> Arrays.stream(res.getDiskSet()).toList(),
                (req, res) -> req.getLimit() <= res.getDiskSet().length,
                req -> req.setOffset(req.getOffset() + req.getLimit()));
    }

    /**
     * 获取 负载均衡 实例列表
     *
     * @param request 请求对象
     * @return 负载均衡实例列表
     */
    public static List<LoadBalancer> listLoadBalancerInstance(ListLoadBalancerInstanceRequest request) {
        ClbClient clbClient = request.getCredential().getClbClient(request.getRegionId());
        request.setOffset(PageUtil.DefaultCurrentPage.longValue() - 1);
        request.setLimit(PageUtil.DefaultPageSize.longValue());
        return PageUtil.page(request,
                req -> {
                    try {
                        DescribeLoadBalancersRequest describeInstancesRequest = new DescribeLoadBalancersRequest();
                        BeanUtils.copyProperties(req, describeInstancesRequest);
                        return clbClient.DescribeLoadBalancers(describeInstancesRequest);
                    } catch (Exception e) {
                        ReTryException.throwReTry(e);
                        SkipPageException.throwSkip(e);
                        throw new RuntimeException(e);
                    }
                },
                res -> Arrays.stream(res.getLoadBalancerSet()).toList(),
                (req, res) -> req.getLimit() <= res.getLoadBalancerSet().length,
                req -> req.setOffset(req.getOffset() + req.getLimit()));
    }

    /**
     * 获取 弹性公网ip 实例列表
     *
     * @param request 请求对象
     * @return 弹性公网ip实例列表
     */
    public static List<Address> listPublicIpInstance(ListPublicIpInstanceRequest request) {
        VpcClient vpcClient = request.getCredential().getVpcClient(request.getRegionId());
        request.setOffset(PageUtil.DefaultCurrentPage.longValue() - 1);
        request.setLimit(PageUtil.DefaultPageSize.longValue());
        return PageUtil.page(request,
                req -> {
                    try {
                        DescribeAddressesRequest describeInstancesRequest = new DescribeAddressesRequest();
                        BeanUtils.copyProperties(req, describeInstancesRequest);
                        return vpcClient.DescribeAddresses(describeInstancesRequest);
                    } catch (Exception e) {
                        ReTryException.throwReTry(e);
                        SkipPageException.throwSkip(e);
                        throw new RuntimeException(e);
                    }
                },
                res -> Arrays.stream(res.getAddressSet()).toList(),
                (req, res) -> req.getLimit() <= res.getAddressSet().length,
                req -> req.setOffset(req.getOffset() + req.getLimit()));
    }

    /**
     * 获取 vpc 实例列表
     *
     * @param request 请求对象
     * @return vpc 实例列表
     */
    public static List<Vpc> listVpcInstance(ListVpcInstanceRequest request) {
        VpcClient vpcClient = request.getCredential().getVpcClient(request.getRegionId());
        request.setOffset(String.valueOf(PageUtil.DefaultCurrentPage.longValue() - 1));
        request.setLimit(String.valueOf(PageUtil.DefaultPageSize.longValue()));
        return PageUtil.page(request,
                req -> {
                    try {
                        DescribeVpcsRequest describeInstancesRequest = new DescribeVpcsRequest();
                        BeanUtils.copyProperties(req, describeInstancesRequest);
                        return vpcClient.DescribeVpcs(describeInstancesRequest);
                    } catch (Exception e) {
                        ReTryException.throwReTry(e);
                        SkipPageException.throwSkip(e);
                        throw new RuntimeException(e);
                    }
                },
                res -> Arrays.stream(res.getVpcSet()).toList(),
                (req, res) -> Integer.parseInt(req.getLimit()) <= res.getVpcSet().length,
                req -> req.setOffset(req.getOffset() + req.getLimit()));

    }

    /**
     * 获取 bucket 桶 实例列表
     *
     * @param request 请求对象
     * @return 桶
     */
    public static List<Bucket> listBucketInstance(ListBucketInstanceRequest request) {
        COSClient cosClient = request.getCredential().getCOSClient(null);
        return cosClient.listBuckets(request);
    }


    /**
     * 获取 bucket 桶实例(桶对象,加密数据,防盗链数据,)列表
     *
     * @param request 请求对象
     * @return 对象存储桶实例数据
     */
    public static List<BucketInstanceResponse> listBucketInstanceCollection(ListBucketInstanceRequest request) {
        CeCosClient cosClient = request.getCredential().getCeCosClient(null);
        List<Bucket> buckets = cosClient.listBuckets(request);
        return buckets.stream().map(bucket -> {
            CeCosClient ceCosClient = request.getCredential().getCeCosClient(bucket.getLocation());
            return mergeBucketInstanceResponse(bucket, ceCosClient);
        }).toList();

    }


    /**
     * 合并数据
     *
     * @param bucket    桶对象
     * @param cosClient 客户端对象
     * @return 对象桶实例数据信息
     */
    private static BucketInstanceResponse mergeBucketInstanceResponse(Bucket bucket, CeCosClient cosClient) {
        BucketInstanceResponse bucketInstanceResponse = new BucketInstanceResponse(bucket);
        BucketRefererConfiguration bucketRefererConfiguration = PageUtil.reTry(() -> {
            try {
                return cosClient.getBucketRefererConfiguration(bucket.getName());
            } catch (Exception e) {
                ReTryException.throwReTry(e);
                return null;
            }
        }, 5);

        AccessControlList accessControlList = PageUtil.reTry(() -> {
            try {
                return cosClient.getBucketAcl(bucket.getName());
            } catch (Exception e) {
                ReTryException.throwReTry(e);
                return null;
            }
        }, 5);
        BucketEncryptionResponse.ServerSideEncryptionConfiguration serverSideEncryptionConfiguration = PageUtil.reTry(() -> {
            try {
                return getBucketEncryption(cosClient, bucket.getName());
            } catch (Exception e) {
                ReTryException.throwReTry(e);
                return null;
            }
        }, 5);
        bucketInstanceResponse.setEncryption(serverSideEncryptionConfiguration);
        bucketInstanceResponse.setAccess(JsonUtil.parseObject(JsonUtil.toJSONString(accessControlList), new TypeReference<>() {
        }));
        bucketInstanceResponse.setReferer(bucketRefererConfiguration);
        return bucketInstanceResponse;
    }


    /**
     * 获取桶访问权限
     *
     * @param request 请求对象
     * @return 存储桶访问权限
     */
    public static AccessControlList getBucketAcl(GetBucketAclRequest request) {
        COSClient cosClient = request.getCredential().getCOSClient(request.getRegionId());
        return cosClient.getBucketAcl(request.getBucketName());
    }

    /**
     * 获取桶的防盗链信息
     *
     * @param request 请求对象
     * @return 桶的防盗链信息
     */
    public static BucketRefererConfiguration getBucketReferer(GetBucketReferRequest request) {
        COSClient cosClient = request.getCredential().getCOSClient(request.getRegionId());
        return getBucketReferer(cosClient, request.getBucketName());
    }

    /**
     * 获取桶是否加密
     *
     * @param request 请求对象
     * @return 桶加密信息
     */
    public static BucketEncryptionResponse.ServerSideEncryptionConfiguration getBucketEncryption(GetBucketEncryptionRequest request) {
        CeCosClient ceCosClient = request.getCredential().getCeCosClient(request.getRegionId());
        return getBucketEncryption(ceCosClient, request.getBucketName());
    }

    /**
     * 获取Bucket防盗链信息
     *
     * @param cosClient  客户端
     * @param bucketName 桶名称
     * @return 当前桶的防盗链信息
     */
    private static BucketRefererConfiguration getBucketReferer(COSClient cosClient, String bucketName) {
        return cosClient.getBucketRefererConfiguration(bucketName);
    }

    /**
     * 获取cos 加密情况
     *
     * @param cosClient  客户端
     * @param bucketName 桶m名称
     * @return cos桶加密数据
     */
    private static BucketEncryptionResponse.ServerSideEncryptionConfiguration getBucketEncryption(CeCosClient cosClient, String bucketName) {
        GenericBucketRequest getBucketEncryptionRequest = new GenericBucketRequest(bucketName);
        CosHttpRequest<GenericBucketRequest> request = cosClient.createRequest(bucketName, null, getBucketEncryptionRequest, HttpMethodName.GET);
        request.addParameter("encryption", null);
        try {
            return cosClient.invoke(request, new HttpResponseHandler<>() {
                @Override
                public CosServiceResponse<BucketEncryptionResponse.ServerSideEncryptionConfiguration> handle(CosHttpResponse cosHttpResponse) throws Exception {
                    BucketEncryptionResponse bucketEncryptionResponse = (new CeXmlResponseSaxParser()).parseBucketEncryptionResponse(cosHttpResponse.getContent());
                    BucketEncryptionResponse.ServerSideEncryptionConfiguration response = bucketEncryptionResponse.getResponse();
                    CosServiceResponse<BucketEncryptionResponse.ServerSideEncryptionConfiguration> objectCosServiceResponse = new CosServiceResponse<>();
                    objectCosServiceResponse.setResult(response);
                    return objectCosServiceResponse;
                }

                @Override
                public boolean needsConnectionLeftOpen() {
                    return false;
                }
            });
        } catch (Exception e) {
            if (e instanceof CosServiceException cosServiceException) {
                if ("NoSuchEncryptionConfiguration".equals(cosServiceException.getErrorCode())) {
                    return null;
                }
            }
            throw e;
        }

    }

    /**
     * 获取安全组 安全组规则合集实例
     *
     * @param request 请求对象
     * @return 安全组 安全组规则合集实例
     */
    public static List<SecurityGroupInstanceResponse> listSecurityGroupCollectionInstance(ListSecurityGroupInstanceRequest request) {
        return listSecurityGroupInstance(request).stream().map(securityGroup -> {
            SecurityGroupInstanceResponse securityGroupInstanceResponse = new SecurityGroupInstanceResponse(securityGroup);
            String securityGroupId = securityGroup.getSecurityGroupId();
            DescribeSecurityGroupPoliciesResponse securityGroupRuleInstance = getSecurityGroupRuleInstance(request.getCredential().getVpcClient(request.getRegionId()), securityGroupId);
            securityGroupInstanceResponse.setRule(securityGroupRuleInstance.getSecurityGroupPolicySet());
            return securityGroupInstanceResponse;
        }).toList();
    }

    /**
     * 获取 安全组实例
     *
     * @param request 请求对象
     * @return 安全组实例
     */
    public static List<SecurityGroup> listSecurityGroupInstance(ListSecurityGroupInstanceRequest request) {
        VpcClient vpcClient = request.getCredential().getVpcClient(request.getRegionId());
        request.setOffset(String.valueOf(PageUtil.DefaultCurrentPage.longValue() - 1));
        request.setLimit(String.valueOf(PageUtil.DefaultPageSize.longValue()));
        return PageUtil.page(request,
                req -> {
                    try {
                        DescribeSecurityGroupsRequest describeInstancesRequest = new DescribeSecurityGroupsRequest();
                        BeanUtils.copyProperties(req, describeInstancesRequest);
                        return vpcClient.DescribeSecurityGroups(describeInstancesRequest);
                    } catch (Exception e) {
                        ReTryException.throwReTry(e);
                        SkipPageException.throwSkip(e);
                        throw new RuntimeException(e);
                    }
                },
                res -> Arrays.stream(res.getSecurityGroupSet()).toList(),
                (req, res) -> Integer.parseInt(req.getLimit()) <= res.getSecurityGroupSet().length,
                req -> req.setOffset(req.getOffset() + req.getLimit()));
    }

    /**
     * 获取安全组规则实例
     *
     * @param request 请求对象
     * @return 安全组规则实例
     */
    public static DescribeSecurityGroupPoliciesResponse getSecurityGroupRuleInstance(GetSecurityGroupRuleInstanceRequest request) {
        VpcClient vpcClient = request.getCredential().getVpcClient(request.getRegionId());
        return getSecurityGroupRuleInstance(vpcClient, request.getSecurityGroupId());
    }

    /**
     * 获取安全组规则实例
     *
     * @param vpcClient       vpc客户端
     * @param securityGroupId 安全组id
     * @return 安全组规则实例
     */
    private static DescribeSecurityGroupPoliciesResponse getSecurityGroupRuleInstance(VpcClient vpcClient, String securityGroupId) {
        DescribeSecurityGroupPoliciesRequest req = new DescribeSecurityGroupPoliciesRequest();
        req.setSecurityGroupId(securityGroupId);
        return PageUtil.reTry(() -> {
            try {
                return vpcClient.DescribeSecurityGroupPolicies(req);
            } catch (Exception e) {
                ReTryException.throwReTry(e);
                SkipPageException.throwSkip(e);
                throw new RuntimeException(e);
            }
        }, 5);

    }

    /**
     * 获取子用户 实例列表
     *
     * @param request 请求对象
     * @return 子用户实例列表
     */
    public static List<RamInstanceResponse> listSubUserInstanceCollection(ListUsersInstanceRequest request) {
        List<SubAccountInfo> subAccountInfos = listSubUserInstanceRequest(request);
        CamClient camClient = request.getCredential().getCamClient("");
        return subAccountInfos.stream().map(user -> {
            RamInstanceResponse ramInstanceResponse = new RamInstanceResponse(user);
            DescribeSafeAuthFlagCollResponse userAuth = getUserAuthInstance(camClient, user.getUin());
            ramInstanceResponse.setUserAuth(userAuth);
            return ramInstanceResponse;
        }).toList();
    }

    /**
     * 拉取子用户实例列表
     *
     * @param request 请求对象
     * @return 子用户实例列表
     */
    public static List<SubAccountInfo> listSubUserInstanceRequest(ListUsersInstanceRequest request) {
        CamClient camClient = request.getCredential().getCamClient("");
        ListUsersResponse listUsersResponse = PageUtil.reTry(() -> {
            try {
                return camClient.ListUsers(request);
            } catch (Exception e) {
                ReTryException.throwReTry(e);
                throw new RuntimeException(e);
            }
        }, 5);
        assert listUsersResponse != null;
        return Arrays.stream(listUsersResponse.getData()).toList();

    }

    /**
     * 获取用户安全设置信息
     *
     * @param request 请求对象
     * @return 用户安全设置数据
     */
    public static DescribeSafeAuthFlagCollResponse getUserAuthInstance(GetUserSafeAuthInstanceRequest request) {
        CamClient camClient = request.getCredential().getCamClient("");
        return getUserAuthInstance(camClient, request.getSubUin());
    }

    /**
     * 获取用户安全设置信息
     *
     * @param client 客户端
     * @param userId 用户id
     * @return 用户安全设置数据
     */
    private static DescribeSafeAuthFlagCollResponse getUserAuthInstance(CamClient client, Long userId) {
        DescribeSafeAuthFlagCollRequest req = new DescribeSafeAuthFlagCollRequest();
        req.setSubUin(userId);
        return PageUtil.reTry(() -> {
            try {
                return client.DescribeSafeAuthFlagColl(req);
            } catch (Exception e) {
                ReTryException.throwReTry(e);
                throw new RuntimeException(e);
            }
        }, 5);

    }

}
