package mz.mobile.bussiness.service;

import mz.mobile.bussiness.entity.OtpToken;
import mz.mobile.bussiness.repository.OtpTokenRepository;
import mz.mobile.bussiness.service.otp.OtpGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class OtpService {

    private final OtpTokenRepository repo;
    private final OtpGateway gateway;

    @Value("${app.otp.expiry-minutes}") private int expiryMinutes;
    @Value("${app.otp.max-verify-attempts}") private int maxVerifyAttempts;
    @Value("${app.otp.resend-cooldown-seconds}") private int cooldownSeconds;
    @Value("${app.otp.max-sends-per-hour}") private int maxSendsPerHour;
    @Value("${app.otp.daily-global-limit}") private int dailyGlobalLimit;

    public OtpService(OtpTokenRepository repo, OtpGateway gateway) {
        this.repo = repo;
        this.gateway = gateway;
    }

    public int getCooldownSeconds() { return cooldownSeconds; }

    public int getExpirySeconds() { return expiryMinutes * 60; }

    public void sendNew(String mobile) {
        LocalDateTime now = LocalDateTime.now();

        if (repo.countByCreatedAtAfter(now.minusDays(1)) >= dailyGlobalLimit) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "OTP service is busy. Please try again later.");
        }

        repo.findTopByMobileOrderByIdDesc(mobile).ifPresent(last -> {
            long wait = cooldownSeconds - Duration.between(last.getCreatedAt(), now).getSeconds();
            if (wait > 0) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Please wait " + wait + " seconds before requesting a new OTP");
            }
        });

        if (repo.countByMobileAndCreatedAtAfter(mobile, now.minusHours(1)) >= maxSendsPerHour) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many OTP requests. Please try again later.");
        }

        String codeHash;
        try {
            codeHash = gateway.send(mobile, expiryMinutes);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not send OTP. Please try again.");
        }

        // A row is saved only after the SMS was accepted, so failed sends never use up the limits.
        OtpToken token = new OtpToken();
        token.setMobile(mobile);
        token.setCodeHash(codeHash);
        token.setCreatedAt(now);
        token.setExpiresAt(now.plusMinutes(expiryMinutes));
        repo.save(token);
    }

    /** Returns true only for a correct, unexpired, unused code within the attempt limit. */
    public boolean verify(String mobile, String code) {
        OtpToken t = repo.findTopByMobileOrderByIdDesc(mobile).orElse(null);
        if (t == null || t.isUsed() || t.getExpiresAt().isBefore(LocalDateTime.now())) return false;

        if (repo.tryConsumeAttempt(t.getId(), maxVerifyAttempts) == 0) return false;
        if (!gateway.check(mobile, code, t.getCodeHash())) return false;

        return repo.markUsed(t.getId()) == 1;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void cleanup() {
        repo.deleteOlderThan(LocalDateTime.now().minusDays(2));
    }
}

//@Service
//public class OtpService {
//
//    private final OtpTokenRepository repo;
//    private final OtpHasher hasher;
//    private final SmsSender sms;
////    private final TwilioSmsSender sms;
//    private final SecureRandom random = new SecureRandom();
//
//    @Value("${app.otp.expiry-minutes}") private int expiryMinutes;
//    @Value("${app.otp.max-verify-attempts}") private int maxVerifyAttempts;
//    @Value("${app.otp.resend-cooldown-seconds}") private int cooldownSeconds;
//    @Value("${app.otp.max-sends-per-hour}") private int maxSendsPerHour;
//    @Value("${app.otp.daily-global-limit}") private int dailyGlobalLimit;
//
//    public OtpService(
//            OtpTokenRepository repo,
//            OtpHasher hasher,
//            SmsSender sms
////            TwilioSmsSender twilioSmsSender
//    ) {
//        this.repo = repo;
//        this.hasher = hasher;
//        this.sms = sms;
////        this.sms = twilioSmsSender;
//    }
//
//    public int getCooldownSeconds() { return cooldownSeconds; }
//
//    public int getExpirySeconds() { return expiryMinutes * 60; }
//
//    public void sendNew(String mobile) {
//        LocalDateTime now = LocalDateTime.now();
//
//        // Cost guard against SMS pumping: a hard cap on all OTPs per 24 hours
//        if (repo.countByCreatedAtAfter(now.minusDays(1)) >= dailyGlobalLimit) {
//            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
//                    "OTP service is busy. Please try again later.");
//        }
//
//        repo.findTopByMobileOrderByIdDesc(mobile).ifPresent(last -> {
//            long wait = cooldownSeconds - Duration.between(last.getCreatedAt(), now).getSeconds();
//            if (wait > 0) {
//                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
//                        "Please wait " + wait + " seconds before requesting a new OTP");
//            }
//        });
//
//        if (repo.countByMobileAndCreatedAtAfter(mobile, now.minusHours(1)) >= maxSendsPerHour) {
//            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
//                    "Too many OTP requests. Please try again later.");
//        }
//
//        String code = String.format("%06d", random.nextInt(1_000_000));
//
//        OtpToken token = new OtpToken();
//        token.setMobile(mobile);
//        token.setCodeHash(hasher.hash(mobile, code));
//        token.setCreatedAt(now);
//        token.setExpiresAt(now.plusMinutes(expiryMinutes));
//        repo.save(token);   // the newest row is the only active one; older rows are ignored
//
//        try {
//            sms.sendOtp(mobile, code, expiryMinutes);
//        } catch (RuntimeException e) {
//            repo.delete(token);
//            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not send OTP. Please try again.");
//        }
//    }
//
//    /** Returns true only for a correct, unexpired, unused code within the attempt limit. */
//    public boolean verify(String mobile, String code) {
//        OtpToken t = repo.findTopByMobileOrderByIdDesc(mobile).orElse(null);
//        if (t == null || t.isUsed() || t.getExpiresAt().isBefore(LocalDateTime.now())) return false;
//
//        if (repo.tryConsumeAttempt(t.getId(), maxVerifyAttempts) == 0) return false;   // attempts used up
//        if (!hasher.matches(mobile, code, t.getCodeHash())) return false;
//
//        return repo.markUsed(t.getId()) == 1;
//    }
//
//    @Scheduled(cron = "0 0 * * * *")
//    public void cleanup() {
//        repo.deleteOlderThan(LocalDateTime.now().minusDays(2));
//    }
//}
