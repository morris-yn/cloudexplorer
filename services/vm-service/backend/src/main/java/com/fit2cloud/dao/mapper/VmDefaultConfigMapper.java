package com.fit2cloud.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fit2cloud.dao.entity.DefaultVmConfig;
import com.fit2cloud.dao.entity.UserValidtime;
import com.fit2cloud.dao.entity.VmCloudServerStatusTiming;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

public interface VmDefaultConfigMapper extends BaseMapper<DefaultVmConfig> {

    @Insert("insert into vm_logs('type','info') values (#{arg1},#{arg2})")
    public void setLog(Integer type,String msg);

    @Select("select * from user_validtime where user_id = #{arg1}")
    public UserValidtime selectUserValidtime(String userid);

}
