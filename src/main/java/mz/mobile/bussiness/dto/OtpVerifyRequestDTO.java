package mz.mobile.bussiness.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OtpVerifyRequestDTO(
        @NotBlank @Size(max = 20) String mobile,
        @NotBlank @Pattern(regexp = "\\d{6}",
                message = "OTP must be 6 digits") String otp
) {}

