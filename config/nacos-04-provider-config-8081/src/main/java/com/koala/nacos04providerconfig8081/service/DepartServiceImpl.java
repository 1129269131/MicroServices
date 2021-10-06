package com.koala.nacos04providerconfig8081.service;

import com.koala.nacos04providerconfig8081.bean.Depart;
import com.koala.nacos04providerconfig8081.repository.DepartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.util.List;

@RefreshScope
@Service
public class DepartServiceImpl implements DepartService {
    @Autowired
    private DepartRepository repository;

    @Value("${depart.name}")
    private String departName;

    @Override
    public boolean saveDepart(Depart depart) {
        Depart obj = repository.save(depart);
        if(obj != null) {
            return true;
        }
        return false;
    }

    @Override
    public boolean removeDepartById(int id) {
        if(repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean modifyDepart(Depart depart) {
        Depart obj = repository.save(depart);
        if(obj != null) {
            return true;
        }
        return false;
    }

    @Override
    public Depart getDepartById(int id) {
        if(repository.existsById(id)) {
            Depart depart = repository.getOne(id);
            depart.setName(depart.getName() + departName);
            return depart;
        }
        Depart depart = new Depart();
        depart.setName("no this depart " + departName);
        return depart;
    }

    @Override
    public List<Depart> listAllDeparts() {
        return repository.findAll();
    }
}
