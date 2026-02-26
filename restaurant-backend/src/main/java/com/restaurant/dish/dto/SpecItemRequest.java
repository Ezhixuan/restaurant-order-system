package com.restaurant.dish.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 规格项请求 (用于批量更新)
 */
@Data
public class SpecItemRequest {
    
    private Long id;  // 新建时为null
    
    @NotBlank(message = "规格名称不能为空")
    @Size(max = 50, message = "规格名称最多50字")
    private String name;
    
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;
    
    private Integer sortOrder = 0;
    
    private Integer status = 1;
}
