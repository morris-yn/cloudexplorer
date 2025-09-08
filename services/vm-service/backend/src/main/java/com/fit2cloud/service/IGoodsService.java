package com.fit2cloud.service;

import com.fit2cloud.dao.entity.CDcard;
import com.fit2cloud.dao.entity.ConfrimPayment;
import com.fit2cloud.dao.entity.GoodsToCart;
import com.fit2cloud.dao.entity.LiveGoods;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author fit2cloud
 * @since
 */
public interface IGoodsService {

    List<LiveGoods> getList(LiveGoods goods);

    Map<String,Object> getNativeQR(GoodsToCart goods) throws Exception;

    Boolean confirmPayment(ConfrimPayment confrimPayment);

    Long getVaildTimeByToken(String token);

    Map<String,String> writeoff(CDcard card);
}
