package com.fit2cloud.dao.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fit2cloud.dao.entity.VmUser;
import com.fit2cloud.dto.JobRecordDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author fit2cloud
 * @since
 */
public interface VmUserMapper extends BaseMapper<VmUser> {

    @Insert("INSERT INTO user_sub (user_id, sub_user_id) VALUES (#{mainUid}, #{subUid})")
    public void createSubUser(@Param("mainUid") String muid, @Param("subUid") String subuid);

    @Select("select count(1) from user_sub where user_id = #{mainUid} and sub_user_id = #{subUid}")
    public int querySubUserCondition(@Param("mainUid") String muid, @Param("subUid") String subuid);

    @Select("select count(1) from user_sub where user_id = #{mainUid}")
    public int queryUserCondition(@Param("mainUid") String muid);
}
