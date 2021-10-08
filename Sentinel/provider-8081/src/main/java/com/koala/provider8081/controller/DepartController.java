package com.koala.provider8081.controller;

import com.koala.provider8081.bean.Depart;
import com.koala.provider8081.service.DepartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DepartController {
    @Autowired
    private DepartService service;

    @GetMapping("/provider/depart/get/{id}")
    public Depart getHandle(@PathVariable("id") int id) {
        return service.getDepartById(id);
    }

}
