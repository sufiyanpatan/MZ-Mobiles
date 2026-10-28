package mz.mobile.bussiness.dto;

import java.util.List;

// Mobile OTP
public record MeResponseDTO(Long id, String email, String mobile, List<String> roles) {}