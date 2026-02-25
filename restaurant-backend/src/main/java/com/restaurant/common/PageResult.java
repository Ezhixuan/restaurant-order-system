package com.restaurant.common;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    
    private Long total;
    private List<T> list;
    
    public PageResult(Long total, List<T> list) {
        this.total = total;
        this.list = list;
    }
    
    public static <T> PageResult<T> of(Long total, List<T> list) {
        return new PageResult<>(total, list);
    }
}
