package mz.mobile.bussiness.controller;

import mz.mobile.bussiness.entity.AppRoles;
import mz.mobile.bussiness.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService){
        this.roleService = roleService;
    }

    @PostMapping()
    public ResponseEntity<AppRoles> addRoles(@RequestBody AppRoles role){
        AppRoles role1 = this.roleService.addRole(role);

        return ResponseEntity.ok().body(role1);
    }

    @GetMapping()
    public ResponseEntity<List<AppRoles>> getRoles(){
        List<AppRoles> roles = this.roleService.getRoles();
        return ResponseEntity.ok().body(roles);
    }
}
