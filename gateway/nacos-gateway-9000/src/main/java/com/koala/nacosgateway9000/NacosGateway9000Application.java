package com.koala.nacosgateway9000;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class NacosGateway9000Application {

    public static void main(String[] args) {
        SpringApplication.run(NacosGateway9000Application.class, args);
    }

     // day01：gateway API 方式
     /*@Bean
     public RouteLocator someRouteLocaltor(RouteLocatorBuilder builder) {
         return builder.routes()
                 .route(ps -> ps.header("X-Request-Id", "\\d+")
                                 .filters(fs -> fs.addRequestParameter("color", "green"))
                                .uri("http://localhost:8080/info/param")
                                .id("baidu_route"))
                 .build();
     }*/

}
