package com.fit2cloud.dao.goodsMapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fit2cloud.dao.entity.LiveGoods;
import com.fit2cloud.dao.entity.PayStatus;
import com.fit2cloud.dao.entity.UserValidtime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface LiveGoodsMapper extends BaseMapper<LiveGoods> {

    @Select("select info.order_sn,log.is_paid,lorder.goods_id,goods.goods_brief\n" +
            "from live_order_info info\n" +
            "         left join live_pay_log log on info.order_id = log.order_id\n" +
            "         left join live_order_goods lorder on lorder.order_id = info.order_id\n" +
            "         left join live_goods goods on goods.goods_id = lorder.goods_id\n" +
            "where info.order_sn = #{orderSn}")
    public PayStatus selectPayStatus(@Param("orderSn") String orderSn);

    @Select("select userid from live_sessions where sessKey = #{sessionkey}")
    public String selectUserId(@Param("sessionkey") String sessionKey);
}
