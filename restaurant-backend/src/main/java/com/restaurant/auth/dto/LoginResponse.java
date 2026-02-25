package com.restaurant.auth.dto;

import lombok.Data;

@Data
public class LoginResponse {
    
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private Integer role;
}
