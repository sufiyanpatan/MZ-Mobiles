package mz.mobile.bussiness.config;

import mz.mobile.bussiness.entity.AppRoles;
import mz.mobile.bussiness.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoleSeeder implements CommandLineRunner {
    private final RoleRepository roles;

    public RoleSeeder(RoleRepository roles) {
        this.roles = roles;
    }

    @Override
    public void run(String... args) {
        for (String name : List.of("USER", "STAFF", "ADMIN")) {
            if (roles.findByRole(name).isEmpty()) {
                AppRoles role = new AppRoles();
                role.setRole(name);
                roles.save(role);
            }
        }
    }
}
