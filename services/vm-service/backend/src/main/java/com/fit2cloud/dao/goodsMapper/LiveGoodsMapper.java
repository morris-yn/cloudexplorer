package com.fit2cloud.dao.goodsMapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fit2cloud.dao.entity.LiveGoods;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface LiveGoodsMapper extends BaseMapper<LiveGoods> {

    @Select("select is_paid\n" +
            "from live_order_info info\n" +
            "         left join live_pay_log log on info.order_id = log.order_id\n" +
            "where info.order_sn = #{orderSn}")
    public Boolean selectPayStatus(@Param("orderSn") String orderSn);

}
