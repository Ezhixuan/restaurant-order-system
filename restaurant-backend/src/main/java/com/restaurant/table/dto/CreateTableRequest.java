package com.restaurant.table.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateTableRequest {
    
    @NotBlank(message = "桌号不能为空")
    private String tableNo;
    
    private String name;
    
    @NotNull(message = "类型不能为空")
    private Integer type;  // 1固定卡座 2临时座位
    
    private Integer capacity = 4;
    
    private Integer sortOrder = 0;
}
