package mz.mobile.bussiness.config;

import mz.mobile.bussiness.entity.AppRoles;
import mz.mobile.bussiness.entity.AppUser;
import mz.mobile.bussiness.repository.AppUserRepository;
import mz.mobile.bussiness.repository.RoleRepository;
import mz.mobile.bussiness.utils.EmailUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final AppUserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    @Value("${app.bootstrap.admin-email}") private String adminEmail;
    @Value("${app.bootstrap.admin-password}") private String adminPassword;
    @Value("${app.bootstrap.staff-email}") private String staffEmail;
    @Value("${app.bootstrap.staff-password}") private String staffPassword;

    public AdminSeeder(AppUserRepository users, RoleRepository roles, PasswordEncoder encoder) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        seed(adminEmail, adminPassword, "ADMIN");
        seed(staffEmail, staffPassword, "STAFF");
    }

    private void seed(String rawEmail, String password, String roleName) {
        if (rawEmail == null || rawEmail.isBlank() || password == null || password.length() < 12) {
            return;   // not configured (or password too short): skip
        }
        String email = EmailUtils.normalize(rawEmail);
        if (users.existsByEmail(email)) return;

        AppRoles role = roles.findByRole(roleName)
                .orElseThrow(() -> new IllegalStateException(roleName + " role missing"));

        AppUser u = new AppUser();
        u.setEmail(email);
        u.setPassword(encoder.encode(password));
        u.getRoles().add(role);
        u.setEnabled(true);
        users.save(u);
        log.info("Seeded {} account: {}", roleName, email);
    }
}
