package mz.mobile.bussiness.dto;

import java.util.List;

// Mobile OTP
public record AuthResponseDTO(String token, String tokenType, long expiresIn,
                              String email, String mobile, List<String> roles) {}