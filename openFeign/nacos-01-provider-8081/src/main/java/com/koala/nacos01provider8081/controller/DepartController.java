package com.koala.nacos01provider8081.controller;

import com.koala.nacos01provider8081.bean.Depart;
import com.koala.nacos01provider8081.service.DepartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;

@RequestMapping("/provider/depart")
@RestController
public class DepartController {
    @Autowired
    private DepartService service;

    @Autowired
    private DiscoveryClient client;

    @PostMapping("/save")
    public boolean saveHandle(@RequestBody Depart depart) {
        return service.saveDepart(depart);
    }

    @DeleteMapping("/del/{id}")
    public boolean deleteHandle(@PathVariable("id") int id) {
        return service.removeDepartById(id);
    }

    @PutMapping("/update")
    public boolean updateHandle(@RequestBody Depart depart) {
        return service.modifyDepart(depart);
    }

    @GetMapping("/get/{id}")
    public Depart getHandle(@PathVariable("id") int id, HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        int remotePort = request.getRemotePort();
        System.out.println("remoteAddr = " + remoteAddr);
        System.out.println("remotePort = " + remotePort);

        return service.getDepartById(id);
    }

    @GetMapping("/list")
    public List<Depart> listHandle() {
        return service.listAllDeparts();
    }

    @GetMapping("/discovery")
    public List<String> discoveryHandle() {
        // SCI模型：Service -> Cluster -> Instance
        // SNCI模型：Service -> namespace -> Cluster -> Instance
        // 获取到当前注册中心中注册表中的所有服务名称
        List<String> services = client.getServices();
        // 遍历所有Service
        for (String serviceName : services) {
            // 获取到当前遍历service的所有instance
            List<ServiceInstance> instances = client.getInstances(serviceName);
            // 遍历所有instance
            for (ServiceInstance instance : instances) {
                // 微服务名称
                String serviceId = instance.getServiceId();
                String host = instance.getHost();
                int port = instance.getPort();
                URI uri = instance.getUri();
                System.out.println("serviceId = " + serviceId);
                System.out.println("host = " + host);
                System.out.println("port = " + port);
                System.out.println("uri = " + uri);
            }
        }
        return services;
    }

}
