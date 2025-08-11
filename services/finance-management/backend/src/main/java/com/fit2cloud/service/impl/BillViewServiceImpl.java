package com.fit2cloud.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.ObjectBuilder;
import com.fit2cloud.base.service.IBaseOrganizationService;
import com.fit2cloud.common.cache.OrganizationCache;
import com.fit2cloud.common.cache.WorkSpaceCache;
import com.fit2cloud.common.exception.Fit2cloudException;
import com.fit2cloud.common.util.AuthUtil;
import com.fit2cloud.common.util.EsFieldUtil;
import com.fit2cloud.common.util.MappingUtil;
import com.fit2cloud.common.util.MonthUtil;
import com.fit2cloud.common.utils.JsonUtil;
import com.fit2cloud.constants.CalendarConstants;
import com.fit2cloud.controller.request.BillExpensesRequest;
import com.fit2cloud.controller.request.BillViewRequest;
import com.fit2cloud.controller.request.CurrencyRequest;
import com.fit2cloud.controller.request.HistoryTrendRequest;
import com.fit2cloud.controller.response.BillView;
import com.fit2cloud.controller.response.CurrencyResponse;
import com.fit2cloud.controller.response.ExpensesResponse;
import com.fit2cloud.controller.response.Trend;
import com.fit2cloud.dao.entity.BillCurrency;
import com.fit2cloud.dao.entity.BillRule;
import com.fit2cloud.dao.jentity.Group;
import com.fit2cloud.es.entity.CloudBill;
import com.fit2cloud.provider.constants.CurrencyConstants;
import com.fit2cloud.service.BillSearchService;
import com.fit2cloud.service.BillViewService;
import com.fit2cloud.service.IBillCurrencyService;
import com.fit2cloud.service.IBillRuleService;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@code @Author:张少虎}
 * {@code @Date: 2022/10/25  5:52 PM}
 * {@code @Version 1.0}
 * {@code @注释: }
 */
@Service
public class BillViewServiceImpl implements BillViewService {
    @Resource
    private IBillRuleService billRuleService;
    @Resource
    private IBaseOrganizationService organizationService;
    @Resource
    private BillSearchService billSearchService;
    @Resource
    private ElasticsearchClient elasticsearchClient;
    @Resource
    private IBillCurrencyService currencyService;


    @Override
    public ExpensesResponse getBillExpenses(CalendarConstants type, String value, BillExpensesRequest billExpensesRequest) {
        return billSearchService.expenses(type, value, billExpensesRequest);
    }


    @Override
    public List<Trend> getTrend(CalendarConstants type, Integer historyNum, HistoryTrendRequest historyTrendRequest) {
        return billSearchService.trend(type, historyNum, historyTrendRequest);
    }

    @Override
    public Map<String, List<BillView>> billViewByRuleId(String ruleId, String month, BillViewRequest request) {
        BillRule billRule = billRuleService.getById(ruleId);
        try {
            return searchBillView(billRule, month, 6, "realTotalCost", request);
        } catch (DataAccessResourceFailureException dataAccessResourceFailureException) {
            if (Objects.nonNull(dataAccessResourceFailureException.getMessage()) && dataAccessResourceFailureException.getMessage().contains("search.max_buckets")) {
                throw new Fit2cloudException(10000, "ElasticSearch[search.max_buckets]不能支持业务,请修改search.max_buckets【PUT /_cluster/settings\n" +
                        "{\"transient\": {\"search.max_buckets\": max_buckets_num}}】");
            }
            throw new Fit2cloudException(10001, dataAccessResourceFailureException.getMessage());
        }
    }

    @Override
    public Map<String, List<BillView>> currentMonthBillViewByCloudAccount() {
        BillRule cloudAccountBillRule = getCloudAccountBillRule();
        return searchBillView(cloudAccountBillRule, null, 1, "realTotalCost", new BillViewRequest());
    }

    @Override
    public List<CurrencyResponse> listCurrency() {
        return currencyService.listCurrency();
    }

    @Override
    public Boolean batchUpdateCurrency(List<CurrencyRequest> currencyRequests) {
        List<BillCurrency> billCurrencies = Arrays.stream(CurrencyConstants.values())
                .map(currencyConstants -> currencyRequests.stream()
                        .filter(diskCurrency -> StringUtils.equals(diskCurrency.getCode(), currencyConstants.code()))
                        .findFirst()
                        .map(currencyRequest -> {
                            BillCurrency billCurrency = new BillCurrency();
                            billCurrency.setCode(currencyConstants.code());
                            billCurrency.setUnit(currencyConstants.unit());
                            billCurrency.setMessage(currencyConstants.getMessage());
                            billCurrency.setExchangeRate(currencyRequest.getExchangeRate());
                            billCurrency.setSymbol(currencyConstants.symbol());
                            return billCurrency;
                        })
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
        return currencyService.saveOrUpdateBatch(billCurrencies);
    }

    /**
     * 获取云账号规则分账规则
     *
     * @return 云账号分账规则
     */
    private BillRule getCloudAccountBillRule() {
        BillRule billRule = new BillRule();
        Group group = new Group();
        group.setField("cloudAccountId");
        group.setName("云账号");
        group.setMissName(null);
        billRule.setGroups(List.of(group));
        return billRule;
    }


    /**
     * 解析聚合结果
     *
     * @param aggregations    聚合对象
     * @param getSumAggregate 获取Sum聚合
     * @return 解析后的聚合结果
     */
    public Map<String, List<BillView>> analysisBillView(Map<String, Aggregate> aggregations, SearchRequest query, Function<Map<String, Aggregate>, SumAggregate> getSumAggregate) {
        OrganizationCache.updateCache();
        WorkSpaceCache.updateCache();
        DateHistogramAggregate dateHistogramAggregate = aggregations.get("billView").dateHistogram();
        return dateHistogramAggregate.buckets().array().stream().map(dateHistogramBucket -> {
            Aggregate group = dateHistogramBucket.aggregations().get("group");
            Aggregation groupQuery = getGroupQuery(query);
            List<BillView> billView = analysisBillView(group, groupQuery, 1, null, getSumAggregate);
            return new DefaultKeyValue<>(dateHistogramBucket.keyAsString(), billView);
        }).collect(Collectors.toMap(DefaultKeyValue::getKey, DefaultKeyValue::getValue));
    }

    private Aggregation getGroupQuery(SearchRequest query) {
        return query.aggregations().get("billView").aggregations().get("group");
    }


    /**
     * @param billRule         账单规则
     * @param historyNum       历史n
     * @param realTotalCostKey 聚合Key 一般使用realTotalCost
     * @return 聚合后对象
     */
    @SneakyThrows
    public Map<String, List<BillView>> searchBillView(BillRule billRule, String month, Integer historyNum, String realTotalCostKey, BillViewRequest request) {
        // todo 获取查询参数根据账单规则
        SearchRequest query = getSearchBillViewQueryByRule(billRule, month, historyNum, realTotalCostKey, request);
        // todo 查询
        SearchResponse<CloudBill> search = elasticsearchClient.search(query, CloudBill.class);
        // todo 解析查询结果
        Map<String, Aggregate> aggregations = search.aggregations();
        return analysisBillView(aggregations, query, a -> a.containsKey(realTotalCostKey) ? a.get(realTotalCostKey).sum() : null);
    }

    /**
     * 根据账单规则获取构建es查询条件
     *
     * @param billRule         账单规则
     * @param historyNum       指定历史n月
     * @param realTotalCostKey 聚合sum key
     * @return es查询条件
     */
    private SearchRequest getSearchBillViewQueryByRule(BillRule billRule, String monthValue, Integer historyNum, String realTotalCostKey, BillViewRequest request) {
        // todo 根据趋势月份构建查询条件
        // 构建区域查询
        String startTime = MonthUtil.getStartTime(CalendarConstants.MONTH, historyNum - 1, monthValue);
        Query q = new RangeQuery.Builder()
                .gte(JsonData.of(startTime))
                .lt(StringUtils.isEmpty(monthValue) ? null : JsonData.of(MonthUtil.addCalender(CalendarConstants.MONTH, monthValue)))
                .field("billingCycle")
                .format("yyyy-MM").build()._toQuery();
        Query authQuery = AuthUtil.getAuthQuery(org -> organizationService.getOrgLevel(org));
        List<Query> queries = new ArrayList<>(request.toQuery());
        queries.add(q);
        if (Objects.nonNull(authQuery)) {
            queries.add(authQuery);
        }
        q = new Query.Builder().bool(new BoolQuery.Builder().must(queries).build()).build();
        // todo 获取聚合对象
        Aggregation aggregationByGroups = getAggregationByGroups(null, billRule.getGroups(), 0, a -> a.aggregations(realTotalCostKey, new Aggregation.Builder().sum(new SumAggregation.Builder().field(realTotalCostKey).build()).build()));
        DateHistogramAggregation dateHistogramAggregation = new DateHistogramAggregation.Builder().field("billingCycle").format("yyyy-MM").calendarInterval(CalendarInterval.Month).build();
        Aggregation.Builder.ContainerBuilder group = new Aggregation.Builder().dateHistogram(dateHistogramAggregation).aggregations("group", aggregationByGroups);
        // todo 构建es查询加聚合对象
        return new SearchRequest.Builder().query(q).aggregations(Map.of("billView", group.build())).runtimeMappings(request.toRuntimeMappings(listCurrency())).build();
    }

    /**
     * @param aggregate   group聚合对象
     * @param groupIndex  group下标
     * @param superGroups 聚合副
     * @return 账单数据
     */
    private List<BillView> analysisBillView(Aggregate aggregate, Aggregation aggregation, Integer groupIndex, List<DefaultKeyValue<String, String>> superGroups, Function<Map<String, Aggregate>, SumAggregate> getSumAggregate) {
        return aggregate.sterms().buckets().array().stream().map(bu -> {
            Aggregate groupAggregate = bu.aggregations().get("group" + groupIndex);
            DefaultKeyValue<String, String> keyValue = new DefaultKeyValue<>();
            keyValue.setKey("group" + groupIndex);
            keyValue.setValue(MappingUtil.mapping(aggregation.terms().field(), bu.key().stringValue()));
            ArrayList<DefaultKeyValue<String, String>> defaultKeyValues = new ArrayList<>();
            if (groupAggregate != null) {
                if (CollectionUtils.isNotEmpty(superGroups)) {
                    defaultKeyValues.addAll(superGroups);
                }
                defaultKeyValues.add(keyValue);
                return analysisBillView(groupAggregate, aggregation.aggregations().get("group" + groupIndex), groupIndex + 1, defaultKeyValues, getSumAggregate);
            }
            SumAggregate sumAggregate = getSumAggregate.apply(bu.aggregations());
            if (Objects.nonNull(sumAggregate)) {
                if (CollectionUtils.isNotEmpty(superGroups)) {
                    defaultKeyValues.addAll(superGroups);
                }
                defaultKeyValues.add(keyValue);
                BillView billView = new BillView();
                billView.setValue(sumAggregate.value());
                billView.setBillGroupDetails(defaultKeyValues);
                return List.of(billView);
            }
            return null;
        }).filter(Objects::nonNull).flatMap(List::stream).toList();

    }


    /**
     * 根据Groups构建聚合查询
     *
     * @param superAggr       父级聚合
     * @param groups          账单规则groups
     * @param index           当前index
     * @param appendChildAggs 对最后一次分组进行聚合
     * @return 聚合对象
     */
    private Aggregation getAggregationByGroups(Aggregation.Builder.ContainerBuilder superAggr, List<Group> groups, Integer index, Consumer<Aggregation.Builder.ContainerBuilder> appendChildAggs) {
        if (Objects.nonNull(superAggr)) {
            if (index < groups.size()) {
                Aggregation.Builder.ContainerBuilder aggregationByGroup = getAggregationByGroup(groups.get(index));
                if (index.equals(groups.size() - 1)) {
                    appendChildAggs.accept(aggregationByGroup);
                    superAggr.aggregations("group" + index, aggregationByGroup.build());
                } else {
                    Aggregation aggregationByGroups = getAggregationByGroups(aggregationByGroup, groups, index + 1, appendChildAggs);
                    superAggr.aggregations("group" + index, aggregationByGroups);
                }
            } else {
                appendChildAggs.accept(superAggr);
            }
            return superAggr.build();
        } else {
            if (index < groups.size()) {
                Aggregation.Builder.ContainerBuilder aggregationByGroup = getAggregationByGroup(groups.get(index));
                return getAggregationByGroups(aggregationByGroup, groups, index + 1, appendChildAggs);
            } else {
                return null;
            }
        }
    }

    /**
     * 根据Group 构建聚合对象
     *
     * @param group group
     * @return es聚合对象
     */
    private Aggregation.Builder.ContainerBuilder getAggregationByGroup(Group group) {
        return new Aggregation.Builder().terms(new TermsAggregation.Builder().field(EsFieldUtil.getGroupKeyByField(group.getField())).size(Integer.MAX_VALUE).missing(StringUtils.isEmpty(group.getMissName()) ? "其他" : group.getMissName()).build());
    }


    /**
     * 使用脚本查询速度慢 慎用
     *
     * @param s      脚本构造器
     * @param months 月份
     * @return 构建后的脚本查询对象
     */
    private ObjectBuilder<Script> getTermsAggregationScript(Script.Builder s, List<String> months) {
        String script = JsonUtil.toJSONString(months) + ".contains(doc['billingCycle'].value.year.toString()+'-'+(doc['billingCycle'].value.monthValue<10?'0'+doc['billingCycle'].value.monthValue:doc['billingCycle'].value.monthValue.toString()))";
        return s.inline(inlineScript -> inlineScript.lang("painless").source(script));
    }


}
