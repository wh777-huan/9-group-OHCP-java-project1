package com.ohcp.agri;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 入口。启动后访问 http://localhost:8080 打开前端页面。 */
@SpringBootApplication
public class AgriAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgriAgentApplication.class, args);
    }
}
