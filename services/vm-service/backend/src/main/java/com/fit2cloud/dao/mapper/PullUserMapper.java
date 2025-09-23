package com.fit2cloud.dao.mapper;

import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit2cloud.base.entity.VmCloudServer;
import com.fit2cloud.controller.request.vm.CreateServerRequest;
import com.fit2cloud.dao.entity.PullUser;
import com.fit2cloud.dao.entity.UserValidtime;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PullUserMapper extends BaseMapper<PullUser> {

}
