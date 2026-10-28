package mz.mobile.bussiness.service.sms;

public interface SmsSender {
    void sendOtp(String mobile, String code, int validMinutes);
}