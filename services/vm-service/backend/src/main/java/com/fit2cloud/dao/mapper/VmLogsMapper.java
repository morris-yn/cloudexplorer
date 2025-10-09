package com.fit2cloud.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fit2cloud.dao.entity.User;
import com.fit2cloud.dao.entity.VmLogs;
import com.fit2cloud.dao.entity.VmUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author fit2cloud
 * @since
 */
public interface VmLogsMapper extends BaseMapper<VmLogs> {

    @Select("select count(*) from vm_logs where `type` = 2 and `create_user` = #{uid} and `info` = '直播开启'")
    public Integer getLiveSessions(@Param("uid") String uid);

    @Select("select * from vm_logs where `type` = 2 and `create_user` = #{uid} and `create_time` >= DATE_SUB(NOW(), INTERVAL 7 DAY)")
    public List<VmLogs> getRecentLogs(@Param("uid") String uid);

}
