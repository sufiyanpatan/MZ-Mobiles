package mz.mobile.bussiness.service.otp;

public interface OtpGateway {

    /**
     * Sends an OTP to the mobile number.
     * Returns the hash of the code if WE generated it, or a marker if the provider owns the code.
     */
    String send(String mobile, int validMinutes);

    /** Checks the code the user typed. storedHash is whatever send() returned. */
    boolean check(String mobile, String code, String storedHash);
}
