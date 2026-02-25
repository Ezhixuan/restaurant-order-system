package com.restaurant.dish.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCategoryRequest {
    
    @NotBlank(message = "分类名称不能为空")
    private String name;
    
    private Integer sortOrder = 0;
}
