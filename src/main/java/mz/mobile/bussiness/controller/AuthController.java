package mz.mobile.bussiness.controller;

import jakarta.validation.Valid;

import mz.mobile.bussiness.dto.AuthResponseDTO;
import mz.mobile.bussiness.dto.LoginRequestDTO;
import mz.mobile.bussiness.dto.MeResponseDTO;
import mz.mobile.bussiness.dto.RegisterRequestDTO;
import mz.mobile.bussiness.security.AuthUser;
import mz.mobile.bussiness.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody RegisterRequestDTO request) {

        String message = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("message", message));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request) {

        AuthResponseDTO response = authService.login(request);

        return ResponseEntity.ok(response);
    }

//    @GetMapping("/me")
//    public ResponseEntity<Map<String, String>> me(
//            org.springframework.security.core.Authentication authentication) {
//
//        return ResponseEntity.ok(
//                Map.of("email", authentication.getName()));
//    }

    @GetMapping("/me")
    public ResponseEntity<MeResponseDTO> me(Authentication authentication) {
        AuthUser user = (AuthUser) authentication.getPrincipal();
        return ResponseEntity.ok(new MeResponseDTO(user.getUsername(), user.getRoleNames()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        authService.logout(authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }


}