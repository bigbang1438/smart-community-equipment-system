package com.smart.community;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.smart.community.mapper")
public class SmartCommunityApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartCommunityApplication.class, args);
        System.out.println("==============================================");
        System.out.println("  智慧社区设备设施全生命周期管理系统 启动成功");
        System.out.println("  接口地址: http://localhost:8080/api");
        System.out.println("==============================================");
    }
}
