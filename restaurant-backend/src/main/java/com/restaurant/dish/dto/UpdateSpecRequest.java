package com.restaurant.dish.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新规格请求
 */
@Data
public class UpdateSpecRequest {
    
    @Size(max = 50, message = "规格名称最多50字")
    private String name;
    
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;
    
    private Integer sortOrder;
    
    private Integer status;
}
