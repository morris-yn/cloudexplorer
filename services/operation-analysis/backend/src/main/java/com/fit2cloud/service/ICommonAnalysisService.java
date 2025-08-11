package com.fit2cloud.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fit2cloud.dto.AnalysisCloudAccountDTO;

/**
 * @author jianneng
 * @date 2023/3/20 14:22
 **/
public interface ICommonAnalysisService {

    long countCloudAccount(String cloudAccountId);

    /**
     * 统计云账号资源数量
     *
     * @param cloudAccountId 云账号ID
     */
    public IPage<AnalysisCloudAccountDTO> cloudAccountDetailed(Integer currentPage, Integer limit, String cloudAccountId);

}
