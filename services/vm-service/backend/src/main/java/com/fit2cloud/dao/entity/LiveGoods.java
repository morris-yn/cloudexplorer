package com.fit2cloud.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@TableName("live_goods")
public class LiveGoods {

  private long goodsId;
  private long catId;
  private String goodsSn;
  private String goodsName;
//  private String goodsNameStyle;
//  private long clickCount;
//  private long brandId;
//  private String providerName;
//  private long goodsNumber;
//  private double goodsWeight;
  private double marketPrice;
//  private long virtualSales;
  private double shopPrice;
//  private double promotePrice;
//  private long promoteStartDate;
//  private long promoteEndDate;
//  private long warnNumber;
//  private String keywords;
  private String goodsBrief;
//  private String goodsDesc;
//  private String goodsThumb;
//  private String goodsImg;
//  private String originalImg;
//  private long isReal;
//  private String extensionCode;
//  private long isOnSale;
//  private long isAloneSale;
//  private long isShipping;
//  private long integral;
//  private long addTime;
//  private long sortOrder;
//  private long isDelete;
//  private long isBest;
//  private long isNew;
//  private long isHot;
//  private long isPromote;
//  private long bonusTypeId;
//  private long lastUpdate;
//  private long goodsType;
//  private String sellerNote;
//  private long giveIntegral;
//  private long rankIntegral;
//  private long suppliersId;
//  private long isCheck;
//  private String deliveryStatus;
//  private long isPintuan;
//  private double ptPrice;
//  private String salesVolumeCount;
//  private String active;
//  private String startTime;
//  private String endTime;
//  private long spikeCount;
//  private String spikeSum;
}
