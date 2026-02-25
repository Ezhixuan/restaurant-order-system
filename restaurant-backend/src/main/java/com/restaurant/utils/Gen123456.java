package com.restaurant.utils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.io.FileWriter;
import java.io.IOException;
public class Gen123456 {
    public static void main(String[] args) throws IOException {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String pwd = "123456";
        String encoded = encoder.encode(pwd);
        System.out.println("Generated: " + encoded);
        FileWriter fw = new FileWriter("/tmp/pwd123456.txt");
        fw.write(encoded);
        fw.close();
    }
}
