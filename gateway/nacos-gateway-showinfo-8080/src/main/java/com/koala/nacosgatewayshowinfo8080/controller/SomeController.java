package com.koala.nacosgatewayshowinfo8080.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/info")
public class SomeController {

    @RequestMapping("/header")
    public String headerHandler(HttpServletRequest request) {
        String header = request.getHeader("X-Request-red");
        return "X-Request-red: " + header;
    }

    @RequestMapping("/param")
    public String paramHandler(String color) {
        return "color: " + color;
    }

}
