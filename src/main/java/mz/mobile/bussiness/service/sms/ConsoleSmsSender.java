package mz.mobile.bussiness.service.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "console", matchIfMissing = true)
public class ConsoleSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSmsSender.class);

    public ConsoleSmsSender(Environment env) {
        if (env.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException(
                    "The console SMS provider.");
        }
    }

    @Override
    public void sendOtp(String mobile, String code, int validMinutes) {
        log.warn("[DEV ONLY] OTP for {} is {} (valid {} min)", mobile, code, validMinutes);
    }
}
