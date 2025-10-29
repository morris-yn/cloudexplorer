package com.fit2cloud.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fit2cloud.base.entity.VmCloudServer;
import com.fit2cloud.dao.entity.GroupsInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface GroupsInfoMapper extends BaseMapper<GroupsInfo> {

    @Select("""
        select s.*
        from groups_info g
        left join user u on u.username = g.pusher_id
        left join vm_user v on v.user_id = substr(u.source,7)
        left join vm_cloud_server s on v.vm_server_id = s.id
        where g.id = #{groupId}
    """)
    VmCloudServer getVmIdByGroupId(@Param("groupId") String groupId);
}
