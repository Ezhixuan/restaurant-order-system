package com.restaurant.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BatchAddDishRequest {
    
    @NotNull(message = "桌台ID不能为空")
    private Long tableId;
    
    @NotEmpty(message = "菜品列表不能为空")
    private List<AddDishItemRequest> items;
    
    @Data
    public static class AddDishItemRequest {
        @NotNull(message = "菜品ID不能为空")
        private Long dishId;
        
        @NotNull(message = "数量不能为空")
        private Integer quantity;
        
        private String remark;
    }
}
