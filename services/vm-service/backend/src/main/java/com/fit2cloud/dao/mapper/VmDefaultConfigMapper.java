package com.fit2cloud.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit2cloud.controller.request.vm.CreateServerRequest;
import com.fit2cloud.dao.entity.DefaultVmConfig;
import com.fit2cloud.dao.entity.UserValidtime;
import com.fit2cloud.dao.entity.VmCloudServerStatusTiming;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface VmDefaultConfigMapper extends BaseMapper<DefaultVmConfig> {

    @Insert("insert into vm_logs('type','info') values (#{arg1},#{arg2})")
    public void setLog(Integer type,String msg);

    @Select("select * from user_validtime where user_id = #{arg1}")
    public UserValidtime selectUserValidtime(String userid);

    @Update("update default_vm_config set is_default = 0 where 1=1")
    public void updateAllDefault();

    public IPage<DefaultVmConfig> selectDefaultConfig(Page<?> page, @Param("req") CreateServerRequest req);
}
