package mz.mobile.bussiness.service;

import mz.mobile.bussiness.dto.CreateInternalUserRequestDTO;
import mz.mobile.bussiness.entity.AppRoles;
import mz.mobile.bussiness.entity.AppUser;
import mz.mobile.bussiness.repository.AppUserRepository;
import mz.mobile.bussiness.repository.RoleRepository;
import mz.mobile.bussiness.utils.EmailUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminUserService {

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(
            AppUserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ){
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String createInternalUser(CreateInternalUserRequestDTO createInternalUserRequestDTO) {
        String email = EmailUtils.normalize(createInternalUserRequestDTO.email());
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
        AppRoles role = roleRepository.findByRole(createInternalUserRequestDTO.role())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown role"));

        AppUser u = new AppUser();
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(createInternalUserRequestDTO.password()));
        u.getRoles().add(role);
        u.setEnabled(true);
        userRepository.save(u);
        return createInternalUserRequestDTO.role() + " account created";
    }

}
