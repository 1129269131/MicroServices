package com.koala.nacos03consumerfeign8080.codeconfig;

import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.RandomRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DepartCodeConfig {

    // @LoadBalanced
    // @Bean
    // public RestTemplate restTemplate() {
    //     return new RestTemplate();
    // }

    @Bean
    public IRule loadBalancer() {
        return new RandomRule();
    }

}
