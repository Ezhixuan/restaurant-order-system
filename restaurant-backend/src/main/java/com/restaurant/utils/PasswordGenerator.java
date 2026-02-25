package com.restaurant.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "admin123";
        String encoded = encoder.encode(password);
        System.out.println("Password: " + password);
        System.out.println("Encoded: " + encoded);
        
        // Verify
        boolean matches = encoder.matches(password, encoded);
        System.out.println("Matches: " + matches);
    }
}
