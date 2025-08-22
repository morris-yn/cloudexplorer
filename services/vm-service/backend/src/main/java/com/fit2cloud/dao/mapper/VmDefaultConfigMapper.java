package com.fit2cloud.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fit2cloud.dao.entity.DefaultVmConfig;
import com.fit2cloud.dao.entity.VmCloudServerStatusTiming;
import org.apache.ibatis.annotations.Insert;

public interface VmDefaultConfigMapper extends BaseMapper<DefaultVmConfig> {

    @Insert("insert into vm_logs('type','info') values (#{arg1},#{arg2})")
    public void setLog(Integer type,String msg);

}
