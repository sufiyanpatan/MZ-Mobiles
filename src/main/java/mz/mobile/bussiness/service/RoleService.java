package mz.mobile.bussiness.service;

import mz.mobile.bussiness.entity.AppRoles;
import mz.mobile.bussiness.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository){
        this.roleRepository = roleRepository;
    }

    @Transactional
    public AppRoles addRole(AppRoles role){
        return roleRepository.save(role);
    }

    public List<AppRoles> getRoles() {
        return this.roleRepository.findAll();
    }
}
