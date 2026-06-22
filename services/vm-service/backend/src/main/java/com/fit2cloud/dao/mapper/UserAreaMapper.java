package com.fit2cloud.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fit2cloud.dao.entity.UserArea;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 用户区域关系表 Mapper 接口
 * </p>
 */
@Mapper
public interface UserAreaMapper extends BaseMapper<UserArea> {

    /**
     * 根据用户ID查询用户绑定的区域列表
     *
     * @param userId 用户ID
     * @return 用户区域关系列表
     */
    @Select("SELECT * FROM user_area WHERE user_id = #{userId}")
    List<UserArea> selectByUserId(@Param("userId") String userId);

    /**
     * 根据用户ID删除用户所有区域绑定
     *
     * @param userId 用户ID
     * @return 删除数量
     */
    @Delete("DELETE FROM user_area WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") String userId);
}
