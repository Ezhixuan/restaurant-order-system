package com.restaurant.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {
    
    @NotNull(message = "桌台ID不能为空")
    private Long tableId;
    
    private Integer customerCount = 1;
    
    @NotEmpty(message = "购物车不能为空")
    private List<CartItemDTO> cartItems;
    
    private String remark;
}
