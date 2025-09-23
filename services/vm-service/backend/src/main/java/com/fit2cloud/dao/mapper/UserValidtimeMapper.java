package com.fit2cloud.dao.mapper;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit2cloud.base.entity.VmCloudServer;
import com.fit2cloud.controller.request.vm.CreateServerRequest;
import com.fit2cloud.dao.entity.DefaultVmConfig;
import com.fit2cloud.dao.entity.UserValidtime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserValidtimeMapper extends BaseMapper<UserValidtime> {

    public IPage<UserValidtime> selectUserValidtime(Page<?> page, @Param("req") CreateServerRequest req);

    public VmCloudServer selectVmCloudServerByUserId(@Param("userId") String userId);

    public JSONArray selectAllVmCloudServerByUserId(@Param("userId") String userId);

    @Select("""
            select enabled from user where source = #{id}
            """)
    public Boolean getUserEnabled(String id);

}
