package mz.mobile.bussiness.service;


import mz.mobile.bussiness.dto.AuthResponseDTO;
import mz.mobile.bussiness.dto.LoginRequestDTO;
import mz.mobile.bussiness.dto.RegisterRequestDTO;
import mz.mobile.bussiness.entity.AppRoles;
import mz.mobile.bussiness.entity.AppUser;
import mz.mobile.bussiness.repository.AppUserRepository;
import mz.mobile.bussiness.repository.RoleRepository;
import mz.mobile.bussiness.security.AuthUser;
import mz.mobile.bussiness.utils.EmailUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final RoleRepository roleRepository;
    private final LoginAttemptService loginAttemptService;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, CustomUserDetailsService userDetailsService, RoleRepository roleRepository, LoginAttemptService loginAttemptService, JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional
    public String register(RegisterRequestDTO request) {
        String email = EmailUtils.normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        AppUser user = new AppUser();

        AppRoles userRole = this.roleRepository.findByRole("USER").orElseThrow(
                () -> new RuntimeException("Error: Default USER role not found in database!")
        );

        user.getRoles().add(userRole);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(true);

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        return "Registration successful. Please log in.";
    }

    // Deliberately NOT @Transactional (failure counts must never roll back)
    public AuthResponseDTO login(LoginRequestDTO request) {
        String email = EmailUtils.normalize(request.email());

        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailure(email);
            throw e;
        }

        // Mobile - OTP
        AuthUser user = (AuthUser) auth.getPrincipal();
        loginAttemptService.recordSuccess(email);

        return new AuthResponseDTO(jwtService.generateToken(user), "Bearer",
                jwtService.getExpirationMs(), user.getEmail(), user.getMobile(), user.getRoleNames());

    }

    // Mobile - OTP
    @Transactional
    public void logout(Long userId) {
        userRepository.incrementTokenVersion(userId);
    }
}