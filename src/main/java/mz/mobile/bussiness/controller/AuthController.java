package mz.mobile.bussiness.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import mz.mobile.bussiness.dto.*;
import mz.mobile.bussiness.security.AuthUser;
import mz.mobile.bussiness.service.AuthService;
import mz.mobile.bussiness.service.MobileAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final MobileAuthService mobileAuthService;

    public AuthController(AuthService authService, MobileAuthService mobileAuthService) {
        this.authService = authService;
        this.mobileAuthService = mobileAuthService;
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

    // Mobile OTP
    @GetMapping("/me")
    public ResponseEntity<MeResponseDTO> me(Authentication authentication) {
        AuthUser u = (AuthUser) authentication.getPrincipal();
        return ResponseEntity.ok(new MeResponseDTO(u.getId(), u.getEmail(), u.getMobile(), u.getRoleNames()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        authService.logout(((AuthUser) authentication.getPrincipal()).getId());
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @PostMapping("/otp/send")
    public ResponseEntity<OtpSentResponseDTO> sendOtp(
            @Valid @RequestBody OtpSendRequestDTO request, HttpServletRequest http) {
        return ResponseEntity.ok(mobileAuthService.requestOtp(request.mobile(), http.getRemoteAddr()));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<AuthResponseDTO> verifyOtp(
            @Valid @RequestBody OtpVerifyRequestDTO request, HttpServletRequest http) {
        return ResponseEntity.ok(
                mobileAuthService.verifyOtp(request.mobile(), request.otp(), http.getRemoteAddr()));
    }

}