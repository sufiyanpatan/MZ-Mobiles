package mz.mobile.bussiness.controller;

import jakarta.validation.Valid;
import mz.mobile.bussiness.dto.CreateInternalUserRequestDTO;
import mz.mobile.bussiness.service.AdminUserService;
import mz.mobile.bussiness.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminUserService adminUserService;

    public AdminController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @PostMapping("/create-user")
    public ResponseEntity<Map<String, String>> createUser(
            @Valid @RequestBody CreateInternalUserRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", adminUserService.createInternalUser(request)));
    }
}