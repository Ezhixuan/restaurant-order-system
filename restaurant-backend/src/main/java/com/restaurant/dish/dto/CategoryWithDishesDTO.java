package com.restaurant.dish.dto;

import com.restaurant.dish.entity.Dish;
import lombok.Data;

import java.util.List;

@Data
public class CategoryWithDishesDTO {
    
    private Long id;
    private String name;
    private List<Dish> dishes;
}
