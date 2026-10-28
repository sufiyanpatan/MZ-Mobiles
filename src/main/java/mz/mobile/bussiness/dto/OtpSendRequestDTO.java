package mz.mobile.bussiness.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OtpSendRequestDTO(@NotBlank @Size(max = 20) String mobile) {}