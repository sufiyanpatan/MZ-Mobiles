package mz.mobile.bussiness.dto;

public record OtpSentResponseDTO(String message, int resendAfterSeconds, int expiresInSeconds) {}
