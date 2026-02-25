package com.restaurant.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.restaurant.common.entity.PaymentSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PaymentSettingMapper extends BaseMapper<PaymentSetting> {
    
    @Select("SELECT * FROM payment_setting WHERE status = 1")
    List<PaymentSetting> selectActiveSettings();
}
