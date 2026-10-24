package mz.mobile.bussiness.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

//public record RegisterRequestDTO(
//        @NotBlank
//        @Email
//        String email,
//
//        @NotBlank
//        @Size(min = 8, max = 72)
//        String password
//) {
//}

public record RegisterRequestDTO(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password) {
}