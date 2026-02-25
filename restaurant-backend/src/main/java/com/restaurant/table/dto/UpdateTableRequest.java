package com.restaurant.table.dto;

import lombok.Data;

@Data
public class UpdateTableRequest {
    
    private String name;
    private Integer capacity;
    private Integer sortOrder;
}
