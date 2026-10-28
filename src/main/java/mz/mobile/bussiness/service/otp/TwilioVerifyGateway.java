package mz.mobile.bussiness.service.otp;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@ConditionalOnProperty(name = "app.otp.provider", havingValue = "twilio-verify")
public class TwilioVerifyGateway implements OtpGateway {

    private static final Logger log = LoggerFactory.getLogger(TwilioVerifyGateway.class);
    private static final String MARKER = "TWILIO_VERIFY";     // we never store a hash for this mode

    private final String serviceSid;

    public TwilioVerifyGateway(@Value("${app.twilio.account-sid}") String accountSid,
                               @Value("${app.twilio.auth-token}") String authToken,
                               @Value("${app.twilio.verify-service-sid}") String serviceSid) {
        if (accountSid.isBlank() || authToken.isBlank() || serviceSid.isBlank()) {
            throw new IllegalStateException("Twilio credentials are missing. Check your environment variables.");
        }
        if (!serviceSid.startsWith("VA")) {
            throw new IllegalStateException("app.twilio.verify-service-sid must start with VA.");
        }
        Twilio.init(accountSid, authToken);
        this.serviceSid = serviceSid;
    }

    @Override
    public String send(String mobile, int validMinutes) {
        try {
            Verification v = Verification.creator(serviceSid, mobile, "sms").create();
            log.info("Twilio Verify started for {}: status={}", mask(mobile), v.getStatus());
            return MARKER;
        } catch (ApiException e) {
            log.error("Twilio Verify send failed for {}: code={} message={}",
                    mask(mobile), e.getCode(), e.getMessage());
            if (Integer.valueOf(60203).equals(e.getCode())) {     // Twilio's own send limit reached
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Too many OTP requests. Please try again later.");
            }
            throw new IllegalStateException("SMS provider failure");
        } catch (RuntimeException e) {
            log.error("Twilio Verify send failed for {}: {}", mask(mobile), e.getClass().getSimpleName());
            throw new IllegalStateException("SMS provider failure");
        }
    }

    @Override
    public boolean check(String mobile, String code, String storedHash) {
        try {
            VerificationCheck check = VerificationCheck.creator(serviceSid)
                    .setTo(mobile)
                    .setCode(code)
                    .create();
            return "approved".equals(check.getStatus());      // a wrong code returns "pending"
        } catch (ApiException e) {
            // e.g. 20404: no pending verification (expired, already approved, or too many attempts)
            log.warn("Twilio Verify check rejected for {}: code={}", mask(mobile), e.getCode());
            return false;
        } catch (RuntimeException e) {
            log.error("Twilio Verify check failed for {}: {}", mask(mobile), e.getClass().getSimpleName());
            return false;
        }
    }

    private String mask(String mobile) {
        return mobile.length() < 4 ? "****" : "*******" + mobile.substring(mobile.length() - 4);
    }
}
