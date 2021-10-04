package com.koala.nacos03consumerfeign8080.service;

import com.koala.nacos03consumerfeign8080.bean.Depart;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

// day01：1)Feign接口名无需与业务接口名称相同，叫什么都可以，
// day01：但一般起名与业务接口名相同，好用
// day01：2)方法签名要与业务接口的相同，但具体的方法名称不一定
// day01：非要与业务方法名称相同，叫什么都可以。
// day01：但一般起名与业务方法名相同，好用
// day01：3)必须要有@FeignClient注解
//@FeignClient(value = "abcmsc-provider-depart", url = "localhost:8081")// day04：直连方式
@FeignClient(value = "abcmsc-provider-depart")// day04：负载均衡调用方式
@RequestMapping("/provider/depart")
public interface DepartService {
    @PostMapping("/save")
    boolean saveDepart(@RequestBody Depart depart);

    @DeleteMapping("/del/{id}")
    boolean removeDepartById(@PathVariable("id") int id);

    @PutMapping("/update")
    boolean modifyDepart(@RequestBody Depart depart);

    @GetMapping("/get/{id}")
    Depart getDepartById(@PathVariable("id") int id);

    @GetMapping("/list")
    List<Depart> listAllDeparts();
}
