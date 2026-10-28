package mz.mobile.bussiness.service.otp;

import mz.mobile.bussiness.security.OtpHasher;
import mz.mobile.bussiness.service.sms.SmsSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

// Required only when we need to print on console
@Component
@ConditionalOnProperty(name = "app.otp.provider", havingValue = "local", matchIfMissing = true)
public class LocalOtpGateway implements OtpGateway {

    private final SecureRandom random = new SecureRandom();
    private final OtpHasher hasher;
    private final SmsSender sms;

    public LocalOtpGateway(OtpHasher hasher, SmsSender sms) {
        this.hasher = hasher;
        this.sms = sms;
    }

    @Override
    public String send(String mobile, int validMinutes) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        sms.sendOtp(mobile, code, validMinutes);
        return hasher.hash(mobile, code);
    }

    @Override
    public boolean check(String mobile, String code, String storedHash) {
        return hasher.matches(mobile, code, storedHash);
    }
}