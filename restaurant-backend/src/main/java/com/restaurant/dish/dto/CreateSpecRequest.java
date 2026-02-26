package com.restaurant.dish.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建规格请求
 */
@Data
public class CreateSpecRequest {
    
    @NotNull(message = "菜品ID不能为空")
    private Long dishId;
    
    @NotBlank(message = "规格名称不能为空")
    @Size(max = 50, message = "规格名称最多50字")
    private String name;
    
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;
    
    private Integer sortOrder = 0;
}
