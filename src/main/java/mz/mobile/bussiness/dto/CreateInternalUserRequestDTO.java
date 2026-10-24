package mz.mobile.bussiness.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateInternalUserRequestDTO(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 12, max = 72) String password,
        @NotBlank @Pattern(regexp = "STAFF|ADMIN", message = "Role must be STAFF or ADMIN") String role
) {}