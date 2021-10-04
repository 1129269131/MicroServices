package com.koala.nacos03consumerfeign8080;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class Nacos03ConsumerFeign8080Application {

    public static void main(String[] args) {
        SpringApplication.run(Nacos03ConsumerFeign8080Application.class, args);
    }

}
