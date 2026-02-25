package com.restaurant.common.service;

import com.restaurant.common.entity.PaymentSetting;
import com.restaurant.common.mapper.PaymentSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentSettingService {

    private final PaymentSettingMapper paymentSettingMapper;

    public List<PaymentSetting> listAll() {
        return paymentSettingMapper.selectActiveSettings();
    }

    public PaymentSetting getById(Long id) {
        return paymentSettingMapper.selectById(id);
    }

    public void save(PaymentSetting setting) {
        if (setting.getId() == null) {
            paymentSettingMapper.insert(setting);
        } else {
            paymentSettingMapper.updateById(setting);
        }
    }

    public void delete(Long id) {
        paymentSettingMapper.deleteById(id);
    }
}
