package com.fit2cloud.dao.entity;

import com.alibaba.fastjson2.JSON;
import jakarta.json.Json;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

@Getter
@Setter
public class GoodsToCart {
    private Integer quick;
    private Integer[] spec;
    private Integer goodsId;
    private String number;
    private Integer parent;

    public GoodsToCart(){
        this.quick = 1;
        this.spec = new Integer[0];
        this.number = "1";
        this.parent = 0;
    }

    public String toJSONString(){
        String jsonstr = JSON.toJSONString(this);
        jsonstr = jsonstr.replace("goodsId","goods_id");
        return jsonstr;
    }
}
