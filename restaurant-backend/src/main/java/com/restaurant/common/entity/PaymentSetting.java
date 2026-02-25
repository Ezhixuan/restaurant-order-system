package com.restaurant.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.restaurant.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_setting")
public class PaymentSetting extends BaseEntity {
    
    private Integer type;        // 类型: 1微信 2支付宝
    private String name;         // 名称
    private String qrcodeImage;  // 收款码图片URL
    private Integer isDefault;   // 是否默认: 0否 1是
    private Integer status;      // 状态
}
